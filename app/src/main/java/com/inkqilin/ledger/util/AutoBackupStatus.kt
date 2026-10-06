package com.inkqilin.ledger.util

import org.json.JSONObject

enum class AutoBackupTarget {
    LOCAL,
    CLOUD
}

/** 本地 / 云端各自维护的自动备份状态。 */
data class AutoBackupStatus(
    val lastAttemptAt: Long = 0L,
    val lastSuccessAt: Long = 0L,
    val lastSuccessName: String? = null,
    val lastSuccessSize: Long = 0L,
    val lastErrorAt: Long = 0L,
    val lastError: String? = null,
    val cleanupWarning: String? = null
) {
    fun encode(): String = JSONObject().apply {
        put("lastAttemptAt", lastAttemptAt)
        put("lastSuccessAt", lastSuccessAt)
        put("lastSuccessName", lastSuccessName ?: JSONObject.NULL)
        put("lastSuccessSize", lastSuccessSize)
        put("lastErrorAt", lastErrorAt)
        put("lastError", lastError ?: JSONObject.NULL)
        put("cleanupWarning", cleanupWarning ?: JSONObject.NULL)
    }.toString()

    companion object {
        fun decode(raw: String?): AutoBackupStatus {
            if (raw.isNullOrBlank()) return AutoBackupStatus()
            return runCatching {
                val json = JSONObject(raw)
                AutoBackupStatus(
                    lastAttemptAt = json.optLong("lastAttemptAt", 0L),
                    lastSuccessAt = json.optLong("lastSuccessAt", 0L),
                    lastSuccessName = json.optNullableString("lastSuccessName"),
                    lastSuccessSize = json.optLong("lastSuccessSize", 0L),
                    lastErrorAt = json.optLong("lastErrorAt", 0L),
                    lastError = json.optNullableString("lastError"),
                    cleanupWarning = json.optNullableString("cleanupWarning")
                )
            }.getOrDefault(AutoBackupStatus())
        }

        private fun JSONObject.optNullableString(name: String): String? =
            if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null
    }
}
