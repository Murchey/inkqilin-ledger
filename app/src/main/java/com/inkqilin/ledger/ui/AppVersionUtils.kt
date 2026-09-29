package com.inkqilin.ledger.ui

import android.content.Context
import android.content.pm.PackageManager

internal object AppVersionUtils {
    fun Get(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }
}
