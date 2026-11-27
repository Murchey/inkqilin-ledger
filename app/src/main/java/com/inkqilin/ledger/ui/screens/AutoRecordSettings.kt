@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.inkqilin.ledger.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.inkqilin.ledger.ui.theme.Space
import com.inkqilin.ledger.util.NotificationCaptureMonitor
import java.text.DateFormat
import java.util.Date

@Composable
internal fun AutoRecordSettings(
    autoRecordEnabled: Boolean,
    harmonyCompatMode: Boolean,
    onAutoRecordEnabledChange: (Boolean) -> Unit,
    onHarmonyCompatModeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val captureState by NotificationCaptureMonitor.state.collectAsState()
    var hasPermission by remember(context) { mutableStateOf(isNotificationServiceEnabled(context)) }
    var pendingEnable by rememberSaveable { mutableStateOf(false) }
    var automaticRebindRequested by rememberSaveable { mutableStateOf(false) }
    var showModeConfirmation by rememberSaveable { mutableStateOf(false) }
    val currentCompatMode by rememberUpdatedState(harmonyCompatMode)
    val currentOnEnabledChange by rememberUpdatedState(onAutoRecordEnabledChange)

    // Only the explicit enable action may turn the feature on after authorization.
    val refreshPermission by rememberUpdatedState<() -> Unit> {
        hasPermission = isNotificationServiceEnabled(context)
        if (!hasPermission) {
            NotificationCaptureMonitor.setConnected(false)
            automaticRebindRequested = false
        }
        if (pendingEnable) {
            pendingEnable = false
            if (hasPermission && !currentCompatMode) currentOnEnabledChange(true)
        }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Request once per settings visit, including recreation, without retrying on every event.
    LaunchedEffect(hasPermission, autoRecordEnabled, harmonyCompatMode, captureState.connected) {
        if (hasPermission && autoRecordEnabled && !harmonyCompatMode &&
            !captureState.connected && !automaticRebindRequested
        ) {
            automaticRebindRequested = true
            NotificationCaptureMonitor.requestRebind(context)
        }
    }

    fun openAuthorization(enableAfterGrant: Boolean) {
        pendingEnable = enableAfterGrant
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }.onFailure {
            pendingEnable = false
            Toast.makeText(context, "无法打开通知读取设置，请从系统设置中授权", Toast.LENGTH_LONG).show()
        }
    }

    ListItem(
        headlineContent = { Text("自动记账") },
        supportingContent = {
            Text(
                when {
                    harmonyCompatMode -> "鸿蒙 / 兼容模式下无法使用通知自动记账"
                    autoRecordEnabled && !hasPermission -> "功能已开启但当前权限不足，请重新授予通知读取权限"
                    autoRecordEnabled -> "功能已开启，读取支付通知后自动记账"
                    pendingEnable -> "等待系统授权，返回后确认是否启用"
                    else -> "功能已关闭"
                }
            )
        },
        trailingContent = {
            Switch(
                checked = autoRecordEnabled && hasPermission && !harmonyCompatMode,
                enabled = !harmonyCompatMode,
                onCheckedChange = { enabled ->
                    pendingEnable = false
                    hasPermission = isNotificationServiceEnabled(context)
                    when {
                        !enabled -> onAutoRecordEnabledChange(false)
                        harmonyCompatMode -> Unit
                        hasPermission -> onAutoRecordEnabledChange(true)
                        else -> {
                            onAutoRecordEnabledChange(false)
                            openAuthorization(enableAfterGrant = true)
                        }
                    }
                },
            )
        },
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = Space.PageHorizontal,
            end = Space.PageHorizontal,
            bottom = 12.dp,
        ),
    ) {
        Text(
            text = if (hasPermission) "系统授权 · 已授予通知读取权限" else "系统授权 · 需要通知读取权限",
            style = MaterialTheme.typography.bodyMedium,
            color = if (hasPermission) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
        Text(
            text = if (captureState.connected) "监听连接 · 已连接" else "监听连接 · 未连接",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val eventTimeFormat = remember {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        }
        val lastEvent = if (captureState.lastEventTime > 0L) {
            val time = eventTimeFormat.format(Date(captureState.lastEventTime))
            "最近事件 · " + captureState.lastEvent + "\n" + time
        } else {
            "最近事件 · " + captureState.lastEvent
        }
        Text(
            text = lastEvent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val lastAlipayEvent = if (captureState.lastAlipayEventTime > 0L) {
            val time = eventTimeFormat.format(Date(captureState.lastAlipayEventTime))
            "支付宝最近事件 · " + captureState.lastAlipayEvent + "\n" + time
        } else {
            "支付宝最近事件 · " + captureState.lastAlipayEvent
        }
        Text(
            text = lastAlipayEvent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { openAuthorization(enableAfterGrant = false) }) {
            Text(if (hasPermission) "管理系统授权" else "授予通知读取权限")
        }
        TextButton(
            enabled = hasPermission && !harmonyCompatMode && !captureState.connected,
            onClick = {
                hasPermission = isNotificationServiceEnabled(context)
                automaticRebindRequested = true
                val requested = NotificationCaptureMonitor.requestRebind(context)
                Toast.makeText(
                    context,
                    if (requested) "已请求重新连接，请查看监听连接状态" else "重新连接请求失败，请检查系统授权",
                    Toast.LENGTH_LONG,
                ).show()
            },
        ) {
            Text("重新连接监听")
        }
        if (harmonyCompatMode) {
            Text(
                "若首次选择有误，可将运行环境改为标准 Android，再开启自动记账。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { showModeConfirmation = true }) {
                Text("纠正兼容模式 · 切回标准 Android")
            }
        }
    }
    if (showModeConfirmation) {
        AlertDialog(
            onDismissRequest = { showModeConfirmation = false },
            title = { Text("切回标准 Android？") },
            text = { Text("请确认本机运行标准 Android。确认后关闭鸿蒙 / 兼容模式，自动记账可在获得系统授权后手动开启。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingEnable = false
                    onHarmonyCompatModeChange(false)
                    showModeConfirmation = false
                }) { Text("确认切换") }
            },
            dismissButton = {
                TextButton(onClick = { showModeConfirmation = false }) { Text("取消") }
            },
        )
    }
}
