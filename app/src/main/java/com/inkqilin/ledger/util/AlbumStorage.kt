package com.inkqilin.ledger.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 记账相册本地文件：统一放在 filesDir/album_photos/。
 * 数据库仅存文件名（或兼容历史绝对路径），读取时解析到当前私有目录，避免换机/恢复后路径失效。
 */
object AlbumStorage {
    const val DIR_NAME = "album_photos"

    fun albumDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** 取出可安全删除/展示的本地文件；不在相册目录内则返回 null。 */
    fun resolvePhotoFile(context: Context, uriString: String): File? {
        if (uriString.isBlank()) return null
        val dir = albumDir(context).canonicalFile
        val candidates = buildList {
            // 1) 纯文件名
            if (!uriString.contains("://") && !uriString.contains("/")) {
                add(File(dir, uriString))
            }
            // 2) file:// 或绝对路径
            val path = runCatching { Uri.parse(uriString).path }.getOrNull()
            if (!path.isNullOrBlank()) add(File(path))
            // 3) 兼容历史：只取文件名再拼到当前相册目录
            val name = uriString.substringAfterLast('/').substringAfterLast('\\')
            if (name.isNotBlank() && !name.contains("://")) {
                add(File(dir, name))
            }
        }
        return candidates.firstOrNull { f ->
            runCatching {
                f.isFile && f.length() > 0 && f.canonicalFile.path.startsWith(dir.path)
            }.getOrDefault(false)
        }
    }

    /** 展示用：解析为可读的 file Uri；失败则回退原字符串。 */
    fun resolveUri(context: Context, uriString: String): Uri {
        val file = resolvePhotoFile(context, uriString)
        return if (file != null) Uri.fromFile(file) else Uri.parse(uriString)
    }

    /** 入库用的稳定标识：优先相对文件名。 */
    fun stableUriString(file: File): String = file.name

    /** 将历史绝对路径/旧 URI 规范为当前目录下的文件名；文件不存在则原样返回。 */
    fun normalizeUriString(context: Context, uriString: String): String {
        val file = resolvePhotoFile(context, uriString) ?: return uriString
        return stableUriString(file)
    }
}
