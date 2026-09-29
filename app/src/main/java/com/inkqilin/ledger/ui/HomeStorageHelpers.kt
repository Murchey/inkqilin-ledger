package com.inkqilin.ledger.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import com.inkqilin.ledger.util.StorageUsageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 首页背景图与本地清理工具。 */
internal object HomeStorageHelpers {
    suspend fun CopyHomeBackground(context: Context, uri: Uri): File {
        val target = File(context.filesDir, "home_bg_image")
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("无法读取图片")
        }
        return target
    }

    fun DeleteQuietly(path: String?) {
        if (!path.isNullOrBlank()) {
            runCatching { File(path).delete() }
        }
    }

    suspend fun ClearAlbumDirAndDb(context: Context, deleteAllPhotos: suspend () -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                StorageUsageManager.clearDir(File(context.filesDir, "album_photos"))
                deleteAllPhotos()
                true
            } catch (e: Exception) {
                Log.e("TransactionVM", "clear album failed", e)
                false
            }
        }

    suspend fun ClearLocalBackupDir(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            StorageUsageManager.clearDir(
                com.inkqilin.ledger.util.CloudBackupManager.localBackupDir(context)
            )
            true
        } catch (_: Exception) {
            false
        }
    }
}
