package com.inkqilin.ledger.ui

import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.service.AIAnalysisService
import com.inkqilin.ledger.service.AiAnalysisResult
import com.inkqilin.ledger.util.AiDataRange
import com.inkqilin.ledger.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/** AI 分析状态与执行（与账单域解耦）。 */
class AiAnalysisController(
    private val themeManager: ThemeManager,
    private val scope: CoroutineScope,
    private val loadTransactions: suspend () -> List<Transaction>,
) {
    private val _aiAnalysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiAnalysisResult: StateFlow<AiAnalysisResult?> = _aiAnalysisResult.asStateFlow()

    private val _aiAnalysisLoading = MutableStateFlow(false)
    val aiAnalysisLoading: StateFlow<Boolean> = _aiAnalysisLoading.asStateFlow()

    private val _aiAnalysisFailed = MutableStateFlow(false)
    val aiAnalysisFailed: StateFlow<Boolean> = _aiAnalysisFailed.asStateFlow()

    fun loadCachedAiResult() {
        scope.launch {
            val score = themeManager.aiScore.first()
            val label = themeManager.aiScoreLabel.first()
            val explanation = themeManager.aiScoreExplanation.first()
            val alertsJson = themeManager.aiAlertsJson.first()
            val failed = themeManager.aiAnalysisFailed.first()

            if (score != null) {
                _aiAnalysisResult.value = AiAnalysisResult(
                    score = score,
                    scoreLabel = label,
                    scoreExplanation = explanation,
                    alerts = AIAnalysisService.deserializeAlerts(alertsJson),
                )
            }
            _aiAnalysisFailed.value = failed
        }
    }

    fun checkAndRunDailyAnalysis(
        isSmartMode: () -> Boolean,
        apiKey: () -> String,
        runAnalysis: () -> Unit,
    ) {
        scope.launch {
            if (!isSmartMode()) return@launch
            if (apiKey().isBlank()) return@launch

            val lastDate = themeManager.aiLastAnalysisDate.first()
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (lastDate >= today) return@launch

            runAnalysis()
        }
    }

    fun runAiAnalysis(
        isSmartMode: () -> Boolean,
        apiKey: () -> String,
        baseUrl: () -> String,
        model: () -> String,
        dataRange: () -> AiDataRange,
    ) {
        if (_aiAnalysisLoading.value) return
        if (!isSmartMode()) return
        scope.launch {
            if (apiKey().isBlank()) {
                _aiAnalysisFailed.value = true
                return@launch
            }

            _aiAnalysisLoading.value = true
            _aiAnalysisFailed.value = false

            try {
                val transactions = loadTransactions()
                val result = AIAnalysisService.analyze(
                    transactions,
                    dataRange(),
                    apiKey(),
                    baseUrl(),
                    model(),
                )
                val alertsJson = AIAnalysisService.serializeAlerts(result.alerts)
                themeManager.saveAiAnalysisResult(
                    result.score,
                    result.scoreLabel,
                    result.scoreExplanation,
                    alertsJson,
                )
                _aiAnalysisResult.value = result
                _aiAnalysisFailed.value = false
            } catch (_: Exception) {
                themeManager.markAiAnalysisFailed()
                _aiAnalysisFailed.value = true
            } finally {
                _aiAnalysisLoading.value = false
            }
        }
    }
}
