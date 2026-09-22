package com.inkqilin.ledger.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val tagName: String
)

object AppUpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun buildLatestApiUrl(repo: String): String {
        val path = normalizeGiteeRepo(repo).ifBlank { DEFAULT_UPDATE_REPO }
        return "https://gitee.com/api/v5/repos/$path/releases/latest"
    }

    /**
     * 从 Gitee Release 检查更新。
     * @param repo owner/repo 或完整 Gitee 仓库地址；默认使用内置仓库
     */
    suspend fun checkForUpdate(
        context: Context,
        repo: String = DEFAULT_UPDATE_REPO
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val request = Request.Builder()
                .url(buildLatestApiUrl(repo))
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            val latestVersion = tagName.removePrefix("v").removePrefix("V")

            if (latestVersion.isBlank()) return@withContext null

            if (isNewerVersion(latestVersion, currentVersion)) {
                UpdateInfo(
                    versionName = latestVersion,
                    releaseNotes = releaseNotes,
                    downloadUrl = htmlUrl,
                    tagName = tagName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
