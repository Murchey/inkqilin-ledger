package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.NotificationHelper
import com.inkqilin.ledger.ui.screens.AppleDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

enum class CycleFilter { ALL, DUE_SOON, OVERDUE }

fun FormatAmount(amount: Double): String = String.format(Locale.CHINA, "%.2f", amount)

fun CalculateProgress(bill: CycleBill, now: Long): Float {
    if (bill.currentCycleEnd <= bill.currentCycleStart) return 0f
    val clamped = now.coerceIn(bill.currentCycleStart, bill.currentCycleEnd)
    return ((clamped - bill.currentCycleStart).toDouble() / (bill.currentCycleEnd - bill.currentCycleStart)).toFloat().coerceIn(0f, 1f)
}

fun FormatDate(dateMillis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

fun FormatTime(hour: Int, minute: Int): String {
    return String.format(Locale.CHINA, "%02d:%02d", hour, minute)
}

/** Returns the boundary after [periods] calendar-based billing cycles. */
fun CycleBoundary(startDate: Long, cycleType: CycleType, periods: Int = 1): Long {
    return Calendar.getInstance(TimeZone.getDefault()).run {
        timeInMillis = startDate
        when (cycleType) {
            CycleType.DAILY -> add(Calendar.DAY_OF_YEAR, periods)
            CycleType.WEEKLY -> add(Calendar.WEEK_OF_YEAR, periods)
            CycleType.MONTHLY -> add(Calendar.MONTH, periods)
            CycleType.YEARLY -> add(Calendar.YEAR, periods)
        }
        timeInMillis
    }
}

/** Returns updated start/end/nextTrigger for a bill based on current time */
fun ComputeCycleRange(bill: CycleBill, now: Long): Triple<Long, Long, Long> {
    var periods = 0
    var start = bill.startDate
    var end = CycleBoundary(bill.startDate, bill.cycleType, periods + 1)
    while (end <= now) {
        periods++
        start = end
        end = CycleBoundary(bill.startDate, bill.cycleType, periods + 1)
    }
    return Triple(start, end, end)
}
