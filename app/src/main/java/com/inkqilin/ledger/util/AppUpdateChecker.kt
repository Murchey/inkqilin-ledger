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
    val downloadUrl: String
)

object AppUpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/Murchey/inkqilin-ledger/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            val latestVersion = tagName.removePrefix("v").removePrefix("V")

            if (isNewerVersion(latestVersion, currentVersion)) {
                UpdateInfo(
                    versionName = latestVersion,
                    releaseNotes = releaseNotes,
                    downloadUrl = htmlUrl
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