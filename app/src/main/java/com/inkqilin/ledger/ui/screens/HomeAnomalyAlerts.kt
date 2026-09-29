package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.AppMode
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*


@Composable
internal fun AnomalyAlertCard(
    transactions: List<Transaction>,
    allTransactions: List<Transaction>,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val anomalies = remember(transactions, allTransactions) {
        DetectAnomalies(transactions, allTransactions)
    }

    if (anomalies.isEmpty()) return

    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(shape, isDark),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "消费提醒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${anomalies.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        AppleLoadingIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2f,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            anomalies.forEach { anomaly ->
                AnomalyAlertItem(anomaly)
                if (anomaly != anomalies.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun AnomalyAlertCard(
    aiAlerts: List<com.inkqilin.ledger.service.AiAlert>,
    isFailed: Boolean,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(shape, isDark),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "消费提醒",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isFailed && aiAlerts.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${aiAlerts.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isFailed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "分析失败",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        AppleLoadingIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2f,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isFailed) {
                Text(
                    text = "AI 分析暂不可用，请检查 API 配置或点击刷新按钮重试",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (aiAlerts.isEmpty()) {
                Text(
                    text = "暂无消费提醒，继续保持良好习惯！",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                aiAlerts.forEachIndexed { index, alert ->
                    AiAnomalyAlertItem(alert)
                    if (index < aiAlerts.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun AiAnomalyAlertItem(alert: com.inkqilin.ledger.service.AiAlert) {
    var expanded by remember { mutableStateOf(false) }
    val alertColor = when (alert.severity) {
        "warning" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }
    val alertIcon = when (alert.severity) {
        "warning" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = alertColor.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        alertIcon,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = alert.percent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = alertColor
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = alert.detail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnomalyAlertItem(anomaly: AnomalyInfo) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = anomaly.color.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        anomaly.icon,
                        contentDescription = null,
                        tint = anomaly.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = anomaly.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = anomaly.changePercent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = anomaly.color
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = anomaly.detail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
