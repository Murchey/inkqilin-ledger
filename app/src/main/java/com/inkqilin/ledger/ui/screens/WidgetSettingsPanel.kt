package com.inkqilin.ledger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.LedgerApplication
import com.inkqilin.ledger.ui.TransactionViewModel

/** 桌面小组件设置面板（用于侧边抽屉） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsPanel(viewModel: TransactionViewModel) {
    val widgetShowAmount by viewModel.widgetShowAmount.collectAsState()
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            ListItem(
                headlineContent = { Text("余额小组件显示金额") },
                supportingContent = {
                    Text(
                        if (widgetShowAmount) "显示收入、支出和余额的具体金额"
                        else "已隐藏金额，仅显示 ¥ •••"
                    )
                },
                trailingContent = {
                    Switch(checked = widgetShowAmount, onCheckedChange = { viewModel.setWidgetShowAmount(it) })
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "仅对桌面上的余额概览小组件生效；快捷记账分类会根据最近使用记录自动安排。",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    LedgerApplication.refreshWidgets()
                    Toast.makeText(context, "小组件已刷新", Toast.LENGTH_SHORT).show()
                }) {
                    Text("立即刷新小组件")
                }
            }
        }
    }
}