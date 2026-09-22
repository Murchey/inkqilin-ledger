package com.inkqilin.ledger.util

import android.content.Context
import java.io.File

/** 本地占用统计与清理（相册、备份、缓存、更新包） */
object StorageUsageManager {

    data class UsageItem(
        val key: String,
        val label: String,
        val sizeBytes: Long,
        val fileCount: Int = 0
    )

    fun dirSizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { dirSizeBytes(it) } ?: 0L
    }

    fun dirFileCount(dir: File?): Int {
        if (dir == null || !dir.exists()) return 0
        if (dir.isFile) return 1
        return dir.listFiles()?.sumOf { dirFileCount(it) } ?: 0
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
        return String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
    }

    fun collectUsage(context: Context): List<UsageItem> {
        val albumDir = File(context.filesDir, "album_photos")
        val backupDir = File(context.filesDir, "backups")
        val homeBg = File(context.filesDir, "home_bg_image")
        val cacheDir = context.cacheDir
        val updateDir = context.getExternalFilesDir(null)?.let { File(it, "updates") }
        return listOf(
            UsageItem(
                key = "album",
                label = "记账相册照片",
                sizeBytes = dirSizeBytes(albumDir),
                fileCount = dirFileCount(albumDir)
            ),
            UsageItem(
                key = "backup",
                label = "本地备份文件",
                sizeBytes = dirSizeBytes(backupDir),
                fileCount = dirFileCount(backupDir)
            ),
            UsageItem(
                key = "cache",
                label = "应用缓存",
                sizeBytes = dirSizeBytes(cacheDir),
                fileCount = dirFileCount(cacheDir)
            ),
            UsageItem(
                key = "update",
                label = "下载的更新包",
                sizeBytes = dirSizeBytes(updateDir),
                fileCount = dirFileCount(updateDir)
            ),
            UsageItem(
                key = "home_bg",
                label = "首页背景图",
                sizeBytes = if (homeBg.exists()) homeBg.length() else 0L,
                fileCount = if (homeBg.exists()) 1 else 0
            )
        )
    }

    fun totalBytes(items: List<UsageItem>): Long = items.sumOf { it.sizeBytes }

    /** 删除目录内全部文件（保留目录本身） */
    fun clearDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        return try {
            dir.listFiles()?.forEach { it.deleteRecursively() }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearCache(context: Context): Boolean = clearDir(context.cacheDir)

    fun clearUpdatePackages(context: Context): Boolean {
        val dir = context.getExternalFilesDir(null)?.let { File(it, "updates") }
        return clearDir(dir)
    }
}
