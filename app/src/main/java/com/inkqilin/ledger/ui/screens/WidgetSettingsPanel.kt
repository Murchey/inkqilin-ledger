package com.inkqilin.ledger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.LedgerApplication
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel

/** 桌面小组件设置面板（用于侧边抽屉） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsPanel(viewModel: TransactionViewModel) {
    val widgetShowAmount by viewModel.widgetShowAmount.collectAsState()
    val widgetQuickCategories by viewModel.widgetQuickCategories.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val expenseWidgetCategories = allCategories.filter { it.type == TransactionType.EXPENSE }
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            ListItem(
                headlineContent = { Text("小部件显示金额") },
                supportingContent = {
                    Text(if (widgetShowAmount) "桌面小组件展示具体金额" else "已隐藏金额，仅显示 ¥ •••")
                },
                trailingContent = {
                    Switch(checked = widgetShowAmount, onCheckedChange = { viewModel.setWidgetShowAmount(it) })
                }
            )
            Spacer(modifier = Modifier.height(0.5.dp))
            ListItem(
                headlineContent = { Text("快捷记账按钮") },
                supportingContent = { Text("自动显示最近使用的分类；无记录时使用下方勾选（最多 6 个）") }
            )
            expenseWidgetCategories.chunked(4).forEach { rowCats ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCats.forEach { cat ->
                        val selected = cat.name in widgetQuickCategories
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated =
                                    if (selected) widgetQuickCategories - cat.name
                                    else (widgetQuickCategories + cat.name).take(6)
                                viewModel.setWidgetQuickCategories(updated)
                            },
                            label = { Text("${cat.icon} ${cat.name}", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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