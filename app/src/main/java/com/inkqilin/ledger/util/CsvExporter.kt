package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.data.UserAsset
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV 导出：大数据量时远快于 POI Excel，适合备份/外部分析。
 * 带 UTF-8 BOM，Excel/WPS 直接双击可识别中文。
 */
object CsvExporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    data class Progress(val fraction: Float, val message: String)

    /**
     * 导出账单 CSV（主表）。
     * 若 [assets]/[flows] 非空，在同一文件追加「资产」「流转」段落，便于一次带走。
     */
    fun exportTransactionsCsv(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        assets: List<UserAsset> = emptyList(),
        flows: List<AssetFlow> = emptyList(),
        onProgress: ((Progress) -> Unit)? = null
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { raw ->
                BufferedWriter(OutputStreamWriter(raw, Charsets.UTF_8), 1 shl 16).use { w ->
                    // BOM，方便 Excel 识别 UTF-8
                    w.write(0xFEFF)

                    val txCount = transactions.size
                    onProgress?.invoke(Progress(0f, "写入账单 $txCount 条…"))

                    // 表头
                    w.write("日期,类型,分类,金额,币种,备注,UUID")
                    w.newLine()

                    // 按日期倒序，一次排序
                    val sorted = if (txCount > 1) transactions.sortedByDescending { it.date } else transactions
                    val typeIncome = TransactionType.INCOME
                    // 缓存 StringBuilder，减少分配
                    val sb = StringBuilder(128)
                    val step = (txCount / 50).coerceAtLeast(1)

                    for (i in sorted.indices) {
                        val tx = sorted[i]
                        sb.setLength(0)
                        sb.append(dateTimeFormat.format(Date(tx.date))).append(',')
                        sb.append(if (tx.type == typeIncome) "收入" else "支出").append(',')
                        sb.append(csvField(tx.category)).append(',')
                        // 金额固定两位，避免科学计数
                        sb.append(String.format(Locale.US, "%.2f", tx.amount)).append(',')
                        sb.append(csvField(tx.currency.ifBlank { "CNY" })).append(',')
                        sb.append(csvField(tx.note)).append(',')
                        sb.append(csvField(tx.uuid ?: ""))
                        w.write(sb.toString())
                        w.newLine()

                        if ((i + 1) % step == 0 || i == txCount - 1) {
                            val frac = (i + 1).toFloat() / txCount.coerceAtLeast(1)
                            onProgress?.invoke(Progress((frac * 0.85f).coerceIn(0f, 0.85f), "写入账单 ${i + 1} / $txCount"))
                        }
                    }

                    if (assets.isNotEmpty()) {
                        w.newLine()
                        w.write("资产名称,类型,当前估值,创建日期,最后更新,备注")
                        w.newLine()
                        assets.forEach { a ->
                            w.write(
                                listOf(
                                    csvField(a.name),
                                    csvField(a.type.label),
                                    String.format(Locale.US, "%.2f", a.currentValue),
                                    dateFormat.format(Date(a.createdAt)),
                                    dateFormat.format(Date(a.lastUpdated)),
                                    csvField(a.note)
                                ).joinToString(",")
                            )
                            w.newLine()
                        }
                    }

                    if (flows.isNotEmpty()) {
                        w.newLine()
                        w.write("资产名称,变动类型,变动金额,变动后价值,日期,备注,UUID")
                        w.newLine()
                        flows.forEach { f ->
                            w.write(
                                listOf(
                                    csvField(f.assetName),
                                    csvField(f.flowType.label),
                                    String.format(Locale.US, "%.2f", f.amount),
                                    String.format(Locale.US, "%.2f", f.newValue),
                                    dateFormat.format(Date(f.date)),
                                    csvField(f.note),
                                    csvField(f.uuid ?: "")
                                ).joinToString(",")
                            )
                            w.newLine()
                        }
                    }

                    onProgress?.invoke(Progress(1f, "完成"))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** CSV 字段转义：含逗号/引号/换行时加引号 */
    private fun csvField(value: String): String {
        if (value.isEmpty()) return ""
        val needQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0 ||
            value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
        return if (!needQuote) value
        else "\"" + value.replace("\"", "\"\"") + "\""
    }
}
