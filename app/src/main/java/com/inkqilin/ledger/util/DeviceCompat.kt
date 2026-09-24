package com.inkqilin.ledger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.max

/** 设备/图片兼容工具：鸿蒙（卓易通）启发式判断 + 安全采样解码 */
object DeviceCompat {

    /** 启发式猜测是否可能是鸿蒙/华为兼容运行环境，仅用于首启对话框预填。 */
    fun guessHarmonyOs(): Boolean {
        val manufacturer = Build.MANUFACTURER?.uppercase().orEmpty()
        val brand = Build.BRAND?.uppercase().orEmpty()
        if (manufacturer == "HUAWEI" || manufacturer == "HONOR" ||
            brand == "HUAWEI" || brand == "HONOR"
        ) return true
        val blob = listOfNotNull(
            Build.DISPLAY, Build.HARDWARE, Build.PRODUCT, Build.DEVICE, Build.MODEL
        ).joinToString("|").lowercase()
        return blob.contains("harmony") || blob.contains("ohos") || blob.contains("hmos")
    }

    /**
     * 按最长边 [maxDim] 采样解码文件，避免相机全分辨率图 OOM。
     */
    fun decodeBitmapSampled(file: File, maxDim: Int = 2048): Bitmap? {
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }

    fun decodeBitmapSampled(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? {
        // file:// 走文件采样；content:// 走 ContentResolver
        val path = uri.path
        if (uri.scheme == "file" && !path.isNullOrBlank()) {
            return decodeBitmapSampled(File(path), maxDim)
        }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / sample > maxDim) sample *= 2
        return sample.coerceAtLeast(1)
    }
}
