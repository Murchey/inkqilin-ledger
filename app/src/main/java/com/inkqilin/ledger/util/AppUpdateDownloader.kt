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

/** 内置代理源选项（GitHub 加速） */
val PROXY_SOURCES = listOf(
    "https://gh-proxy.org/",
    "https://v4.gh-proxy.org/",
    "https://cdn.gh-proxy.org/"
)

/** 构建 GitHub Release 下载基址；repo 支持 owner/repo 或完整地址 */
fun buildGithubReleaseBase(repo: String): String {
    val path = normalizeGithubRepo(repo).ifBlank { DEFAULT_GITHUB_REPO }
    return "https://github.com/$path/releases/download"
}

/** 构建 Gitee Release 下载基址；repo 支持 owner/repo 或完整地址 */
fun buildGiteeReleaseBase(repo: String): String {
    val path = normalizeGiteeRepo(repo).ifBlank { DEFAULT_UPDATE_REPO }
    return "https://gitee.com/$path/releases/download"
}

/**
 * 内嵌 APK 下载 / 安装 / 清理工具。
 * 通过 OkHttp 流式下载到 app 外部文件目录，FileProvider 安装。
 */
object AppUpdateDownloader {

    private const val APK_FILE_PREFIX = "InkQilin-ledgerV"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /** 构建下载链接 */
    fun buildDownloadUrl(
        versionName: String,
        source: DownloadSource,
        proxyPrefix: String? = null,
        giteeRepo: String = DEFAULT_UPDATE_REPO,
        githubRepo: String = DEFAULT_GITHUB_REPO
    ): String {
        val tag = "V$versionName"
        val fileName = "$APK_FILE_PREFIX$versionName.apk"
        val base = when (source) {
            DownloadSource.GITEE -> buildGiteeReleaseBase(giteeRepo)
            DownloadSource.GITHUB -> buildGithubReleaseBase(githubRepo)
            DownloadSource.PROXY -> {
                val prefix = proxyPrefix ?: PROXY_SOURCES.first()
                "$prefix${buildGithubReleaseBase(githubRepo)}"
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
     *
     * 注意：callbackFlow 所有路径都必须走到末尾 awaitClose，否则会闪退。
     */
    fun download(
        context: Context,
        versionName: String,
        source: DownloadSource,
        proxyPrefix: String? = null,
        giteeRepo: String = DEFAULT_UPDATE_REPO,
        githubRepo: String = DEFAULT_GITHUB_REPO
    ): Flow<DownloadProgress> = callbackFlow {
        var downloadUrl: String? = null
        try {
            val url = buildDownloadUrl(versionName, source, proxyPrefix, giteeRepo, githubRepo)
            downloadUrl = url
            android.util.Log.i("AppUpdateDownloader", "开始下载 source=$source url=$url")
            val file = apkFile(context, versionName)

            if (file.exists() && file.length() > 0) {
                android.util.Log.i("AppUpdateDownloader", "命中本地缓存 ${file.absolutePath} size=${file.length()}")
                trySend(DownloadProgress.Completed)
            } else {
                if (file.exists()) file.delete()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "InkQilin-ledger-Update")
                    .build()
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                try {
                    android.util.Log.i(
                        "AppUpdateDownloader",
                        "HTTP ${response.code} contentLength=${response.body?.contentLength() ?: -1}"
                    )
                    if (!response.isSuccessful) {
                        trySend(DownloadProgress.Failed)
                    } else {
                        val body = response.body
                        if (body == null) {
                            trySend(DownloadProgress.Failed)
                        } else {
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

                            android.util.Log.i(
                                "AppUpdateDownloader",
                                "写入完成 bytes=$downloadedBytes file=${file.length()}"
                            )
                            if (file.exists() && file.length() > 0) {
                                trySend(DownloadProgress.Completed)
                            } else {
                                trySend(DownloadProgress.Failed)
                            }
                        }
                    }
                } finally {
                    response.close()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateDownloader", "下载失败 url=$downloadUrl", e)
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
            val dir = context.getExternalFilesDir(null)?.let { File(it, "updates") }
            dir?.listFiles()?.forEach {
                val deleted = it.delete()
                if (!deleted) {
                    it.deleteOnExit()
                }
            }
        } catch (_: Exception) {}
    }
}
