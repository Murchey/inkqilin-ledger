package com.inkqilin.ledger.service

import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.util.AiDataRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class AiAlert(
    val title: String,
    val detail: String,
    val percent: String,
    val severity: String
)

data class AiAnalysisResult(
    val score: Int,
    val scoreLabel: String,
    val scoreExplanation: String,
    val alerts: List<AiAlert>
)

object AIAnalysisService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(
        allTransactions: List<Transaction>,
        dataRange: AiDataRange,
        apiKey: String,
        baseUrl: String,
        model: String
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val (currentStart, currentEnd, previousStart, previousEnd, rangeLabel) = when (dataRange) {
            AiDataRange.THIS_WEEK_AND_LAST -> {
                val weekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val lastWeekStart = weekStart - 7 * 86400000L
                listOf(weekStart, todayStart + 86400000L, lastWeekStart, weekStart, "本周(${sdf.format(Date(weekStart))}~今天) 和 上周(${sdf.format(Date(lastWeekStart))}~${sdf.format(Date(weekStart - 1))})")
            }
            AiDataRange.TODAY_AND_YESTERDAY -> {
                val yesterdayStart = todayStart - 86400000L
                listOf(todayStart, todayStart + 86400000L, yesterdayStart, todayStart, "本日(${sdf.format(Date(todayStart))}) 和 昨日(${sdf.format(Date(yesterdayStart))})")
            }
            AiDataRange.THIS_MONTH_AND_LAST -> {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val lastMonthStart = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                listOf(monthStart, todayStart + 86400000L, lastMonthStart, monthStart, "本月(${sdf.format(Date(monthStart))}~今天) 和 上月(${sdf.format(Date(lastMonthStart))}~${sdf.format(Date(monthStart - 1))})")
            }
        }

        @Suppress("UNCHECKED_CAST")
        val cs = currentStart as Long
        @Suppress("UNCHECKED_CAST")
        val ce = currentEnd as Long
        @Suppress("UNCHECKED_CAST")
        val ps = previousStart as Long
        @Suppress("UNCHECKED_CAST")
        val pe = previousEnd as Long
        @Suppress("UNCHECKED_CAST")
        val rl = rangeLabel as String

        val currentTx = allTransactions.filter { it.date in cs until ce }
        val previousTx = allTransactions.filter { it.date in ps until pe }

        val currentExpense = currentTx.filter { it.type == TransactionType.EXPENSE }
        val currentIncome = currentTx.filter { it.type == TransactionType.INCOME }
        val previousExpense = previousTx.filter { it.type == TransactionType.EXPENSE }
        val previousIncome = previousTx.filter { it.type == TransactionType.INCOME }

        val currentExpenseTotal = currentExpense.sumOf { it.amount }
        val currentIncomeTotal = currentIncome.sumOf { it.amount }
        val previousExpenseTotal = previousExpense.sumOf { it.amount }
        val previousIncomeTotal = previousIncome.sumOf { it.amount }

        val currentCategorySummary = currentExpense.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .joinToString("\n") { "  - ${it.key}: ¥${String.format("%.2f", it.value)} (${currentExpense.size}笔中${currentExpense.count { t -> t.category == it.key }}笔)" }

        val previousCategorySummary = previousExpense.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .joinToString("\n") { "  - ${it.key}: ¥${String.format("%.2f", it.value)}" }

        val dailySummary = currentExpense.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            sdf.format(cal.time)
        }.entries.sortedByDescending { it.key }
            .joinToString("\n") { "  ${it.key}: ¥${String.format("%.2f", it.value.sumOf { t -> t.amount })} (${it.value.size}笔)" }

        val prompt = """你是一个专业的个人财务分析助手。请根据以下用户账单数据进行分析。

## 分析时间范围
$rl

## 当前周期数据
- 总收入: ¥${String.format("%.2f", currentIncomeTotal)} (${currentIncome.size}笔)
- 总支出: ¥${String.format("%.2f", currentExpenseTotal)} (${currentExpense.size}笔)
- 结余: ¥${String.format("%.2f", currentIncomeTotal - currentExpenseTotal)}

## 当前周期分类支出
$currentCategorySummary

## 当前周期每日支出明细
${dailySummary.ifBlank { "  无数据" }}

## 上一周期数据（用于对比）
- 总收入: ¥${String.format("%.2f", previousIncomeTotal)} (${previousIncome.size}笔)
- 总支出: ¥${String.format("%.2f", previousExpenseTotal)} (${previousExpense.size}笔)
- 分类支出:
$previousCategorySummary

## 任务要求
请返回严格的JSON格式（不要包含markdown代码块标记或任何额外文字），格式如下：
{
  "score": 75,
  "scoreLabel": "良好",
  "scoreExplanation": "简短说明评分的主要依据",
  "alerts": [
    {
      "title": "简短的提醒标题",
      "detail": "具体的分析说明",
      "percent": "+38%",
      "severity": "warning"
    }
  ]
}

评分规则：
- score: 0-100整数，综合储蓄率、支出合理性、消费结构等因素
- scoreLabel: "优秀"(>=80) / "良好"(>=70) / "一般"(>=60) / "需关注"(<60)
- scoreExplanation: 一句话总结评分理由
- alerts: 0-3条消费提醒，每条包含：
  - title: 简短标题（如"本月餐饮支出偏高"）
  - detail: 具体数据对比说明
  - percent: 变化百分比或金额（如"+38%"或"¥500"）
  - severity: "warning"(需要关注) 或 "info"(一般信息)
- 如果数据不足以分析，score默认为60，alerts为空数组"""

        val json = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
        }

        val request = Request.Builder()
            .url("${baseUrl.removeSuffix("/")}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API请求失败: ${response.code}")
        }

        val body = response.body?.string() ?: throw Exception("API返回为空")
        val responseJson = JSONObject(body)
        val content = responseJson.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        parseAiResponse(content)
    }

    private fun parseAiResponse(content: String): AiAnalysisResult {
        val jsonRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.MULTILINE)
        val match = jsonRegex.find(content)
        val jsonString = (match?.groupValues?.get(1) ?: content).trim()

        val cleanedJson = jsonString
            .replace(Regex("^[^{]*"), "")
            .replace(Regex("[^}]*$"), "")

        val obj = JSONObject(cleanedJson)

        val score = obj.optInt("score", 60).coerceIn(0, 100)
        val scoreLabel = obj.optString("scoreLabel", when {
            score >= 80 -> "优秀"
            score >= 70 -> "良好"
            score >= 60 -> "一般"
            else -> "需关注"
        })
        val scoreExplanation = obj.optString("scoreExplanation", "")

        val alertsArray = obj.optJSONArray("alerts") ?: JSONArray()
        val alerts = (0 until alertsArray.length()).mapNotNull { i ->
            try {
                val alertObj = alertsArray.getJSONObject(i)
                AiAlert(
                    title = alertObj.optString("title", ""),
                    detail = alertObj.optString("detail", ""),
                    percent = alertObj.optString("percent", ""),
                    severity = alertObj.optString("severity", "info")
                )
            } catch (_: Exception) {
                null
            }
        }

        return AiAnalysisResult(
            score = score,
            scoreLabel = scoreLabel,
            scoreExplanation = scoreExplanation,
            alerts = alerts
        )
    }

    fun serializeAlerts(alerts: List<AiAlert>): String {
        val array = JSONArray()
        alerts.forEach { alert ->
            array.put(JSONObject().apply {
                put("title", alert.title)
                put("detail", alert.detail)
                put("percent", alert.percent)
                put("severity", alert.severity)
            })
        }
        return array.toString()
    }

    fun deserializeAlerts(json: String): List<AiAlert> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                try {
                    val obj = array.getJSONObject(i)
                    AiAlert(
                        title = obj.optString("title", ""),
                        detail = obj.optString("detail", ""),
                        percent = obj.optString("percent", ""),
                        severity = obj.optString("severity", "info")
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
