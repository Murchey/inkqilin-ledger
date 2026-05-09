package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.util.ExcelExporter
import com.inkqilin.ledger.util.ExcelImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.runtime.*
import com.inkqilin.ledger.util.ThemeMode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun SettingsScreen(viewModel: TransactionViewModel, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by viewModel.themeMode.collectAsState()
    val incomeColorHex by viewModel.incomeColor.collectAsState()
    val expenseColorHex by viewModel.expenseColor.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val transactions = viewModel.allTransactions.first()
                    val success = ExcelExporter.exportTransactionsToUri(context, it, transactions)
                    if (success) {
                        Toast.makeText(context, "导出成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val templateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val success = ExcelExporter.exportTemplateToUri(context, it)
                    if (success) {
                        Toast.makeText(context, "模板下载成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "模板下载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                viewModel.importTransactions(context, it)
                Toast.makeText(context, "导入请求已提交，正在后台处理...", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(text = "显示设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("深浅色模式") },
                    supportingContent = {
                        Text(when (themeMode) {
                            ThemeMode.AUTO -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色模式"
                            ThemeMode.DARK -> "深色模式"
                        })
                    },
                    trailingContent = {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text("切换")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("跟随系统") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.AUTO); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("浅色模式") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("深色模式") },
                                    onClick = { viewModel.setThemeMode(ThemeMode.DARK); expanded = false }
                                )
                            }
                        }
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("收入展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = incomeColorHex,
                            onColorSelected = { viewModel.setIncomeColor(it) }
                        )
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("支出展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = expenseColorHex,
                            onColorSelected = { viewModel.setExpenseColor(it) }
                        )
                    }
                )
            }
        }

        Text(text = "分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("账单标签（类别）管理") },
                supportingContent = { Text("添加、修改或删除收支分类") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("category_management") }
            )
        }

        Text(text = "数据管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("导出账单为 Excel") },
                    supportingContent = { Text("选择位置并保存所有记账记录") },
                    leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingContent = {
                        Button(onClick = {
                            scope.launch {
                                val transactions = viewModel.allTransactions.first()
                                if (transactions.isNotEmpty()) {
                                    val fileName = "墨麒麟记账_${System.currentTimeMillis()}.xlsx"
                                    exportLauncher.launch(fileName)
                                } else {
                                    Toast.makeText(context, "暂无数据可导出", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("导出")
                        }
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("下载账单模板") },
                    supportingContent = { Text("导出 Excel 模板，填写后可导入") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = {
                            templateLauncher.launch("墨麒麟账单模板.xlsx")
                        }) {
                            Text("下载")
                        }
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("导入账单") },
                    supportingContent = { Text("从填写好的 Excel 模板导入记录") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = {
                            importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        }) {
                            Text("选择文件")
                        }
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("关于 墨麒麟记账") },
                    supportingContent = { Text("版本 1.1.0") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun ColorPickerButton(selectedColor: String, onColorSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val colors = listOf("#715CFF", "#51B4FF", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#E91E63", "#00BCD4", "#000000", "#795548")

    Box {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(selectedColor)))
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.take(5).forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .clickable { onColorSelected(colorHex); expanded = false }
                    )
                }
            }
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colors.drop(5).forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .clickable { onColorSelected(colorHex); expanded = false }
                    )
                }
            }
        }
    }
}
