package com.inkqilin.ledger.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** 下载进度状态 */
sealed class DownloadProgress {
    data class Progress(val fraction: Float) : DownloadProgress()
    data object Completed : DownloadProgress()
    data object Failed : DownloadProgress()
}

/** 下载源类型 */
enum class DownloadSource {
    GITEE,
    GITHUB,
    PROXY
}

/** 内置代理源选项 */
val PROXY_SOURCES = listOf(
    "https://gh-proxy.org/",
    "https://v4.gh-proxy.org/",
    "https://cdn.gh-proxy.org/"
)

private const val GITHUB_RELEASE_BASE = "https://github.com/Murchey/inkqilin-ledger/releases/download"
private const val GITEE_RELEASE_BASE = "https://gitee.com/Murchey/inkqinlin-ledger/releases/download"

/**
 * 内嵌 APK 下载 / 安装 / 清理工具。
 * 通过 OkHttp 流式下载到 app 外部文件目录，FileProvider 安装。
 */
object AppUpdateDownloader {

    private const val APK_FILE_PREFIX = "InkQilin-ledgerV"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /** 构建下载链接 */
    fun buildDownloadUrl(versionName: String, source: DownloadSource, proxyPrefix: String? = null): String {
        val tag = "V$versionName"
        val fileName = "$APK_FILE_PREFIX$versionName.apk"
        val base = when (source) {
            DownloadSource.GITEE -> GITEE_RELEASE_BASE
            DownloadSource.GITHUB -> GITHUB_RELEASE_BASE
            DownloadSource.PROXY -> {
                val prefix = proxyPrefix ?: PROXY_SOURCES.first()
                "$prefix$GITHUB_RELEASE_BASE"
            }
        }
        return "$base/$tag/$fileName"
    }

    /** 获取 APK 保存的 File */
    private fun apkFile(context: Context, versionName: String): File {
        val dir = File(context.getExternalFilesDir(null), "updates")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$APK_FILE_PREFIX$versionName.apk")
    }

    /** 通过 FileProvider 获取 APK 的 content URI */
    private fun apkUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * 下载 APK 并实时发射进度。
     * 完成后由调用方通过 [install] 触发安装。
     */
    fun download(
        context: Context,
        versionName: String,
        source: DownloadSource,
        proxyPrefix: String? = null
    ): Flow<DownloadProgress> = callbackFlow {
        val url = buildDownloadUrl(versionName, source, proxyPrefix)
        val file = apkFile(context, versionName)

        // 如果已有完整文件且大小 >0，跳过下载
        if (file.exists() && file.length() > 0) {
            trySend(DownloadProgress.Completed)
            return@callbackFlow
        }

        try {
            val request = Request.Builder().url(url).build()
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                trySend(DownloadProgress.Failed)
                return@callbackFlow
            }

            val body = response.body ?: run {
                trySend(DownloadProgress.Failed)
                return@callbackFlow
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            withContext(Dispatchers.IO) {
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val fraction = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                                trySend(DownloadProgress.Progress(fraction))
                            }
                        }
                    }
                }
            }

            response.close()

            // 验证文件完整性
            if (file.exists() && file.length() > 0) {
                trySend(DownloadProgress.Completed)
            } else {
                trySend(DownloadProgress.Failed)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateDownloader", "下载失败", e)
            trySend(DownloadProgress.Failed)
        }

        awaitClose {}
    }

    /** 触发 APK 安装 */
    fun install(context: Context, versionName: String) {
        val file = apkFile(context, versionName)
        if (!file.exists()) return

        val uri = apkUri(context, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    /** 清理 updates 目录下所有历史 APK */
    fun cleanOldApks(context: Context) {
        try {
            val dir = File(context.getExternalFilesDir(null), "updates")
            dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
