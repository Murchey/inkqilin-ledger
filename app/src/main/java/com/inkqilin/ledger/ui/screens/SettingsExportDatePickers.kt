@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.*
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.*
import com.inkqilin.ledger.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun SettingsExportDatePickers(
    showExportStartPicker: Boolean,
    showExportEndPicker: Boolean,
    showRenQingExportStartPicker: Boolean,
    showRenQingExportEndPicker: Boolean,
    onDismissExportStart: () -> Unit,
    onDismissExportEnd: () -> Unit,
    onDismissRenQingExportStart: () -> Unit,
    onDismissRenQingExportEnd: () -> Unit,
    onSelectExportStart: (Long) -> Unit,
    onSelectExportEnd: (Long) -> Unit,
    onSelectRenQingExportStart: (Long) -> Unit,
    onSelectRenQingExportEnd: (Long) -> Unit,
) {
    if (showExportStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = 0L)
        AppleDatePickerDialog(
            onDismissRequest = { onDismissExportStart() },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSelectExportStart(it) }
                    onDismissExportStart()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { onDismissExportStart() }) { Text("取消") } }
        )
    }

    if (showExportEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = 0L)
        AppleDatePickerDialog(
            onDismissRequest = { onDismissExportEnd() },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSelectExportEnd(it) }
                    onDismissExportEnd()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { onDismissExportEnd() }) { Text("取消") } }
        )
    }

    if (showRenQingExportStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = 0L)
        AppleDatePickerDialog(
            onDismissRequest = { onDismissRenQingExportStart() },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSelectRenQingExportStart(it) }
                    onDismissRenQingExportStart()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { onDismissRenQingExportStart() }) { Text("取消") } }
        )
    }

    if (showRenQingExportEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = 0L)
        AppleDatePickerDialog(
            onDismissRequest = { onDismissRenQingExportEnd() },
            state = datePickerState,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSelectRenQingExportEnd(it) }
                    onDismissRenQingExportEnd()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { onDismissRenQingExportEnd() }) { Text("取消") } }
        )
    }

}
