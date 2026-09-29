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
internal fun SettingsFeatureSection(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToKeywordCategoryManagement: () -> Unit,
    onNavigateToContactManagement: () -> Unit,
    onNavigateToCurrencyManagement: () -> Unit,
    onNavigateToAIConfig: () -> Unit,
    onNavigateToOCRConfig: () -> Unit,
    onSendTestNotification: () -> Unit,
    ocrApiKey: String,
    harmonyCompatMode: Boolean,
) {
    val context = LocalContext.current
    var categorySectionExpanded by rememberSaveable { mutableStateOf(false) }
    var featureSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var currencySectionExpanded by rememberSaveable { mutableStateOf(false) }
    var updateSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var labSectionExpanded by rememberSaveable { mutableStateOf(false) }
    val renQingEnabled by renQingViewModel.renQingEnabled.collectAsState()
    val autoRecordEnabled by viewModel.autoRecordEnabled.collectAsState()
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()
    val albumEnabled by viewModel.albumEnabled.collectAsState()
        SettingsSectionHeader("功能开关", if (renQingEnabled) "人情账本已启用" else "按需开启页面功能", featureSectionExpanded) { featureSectionExpanded = !featureSectionExpanded }
        AnimatedVisibility(visible = featureSectionExpanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text("人情账本") },
                        supportingContent = {
                            Text(if (renQingEnabled) "已启用，底部导航栏显示人情页面" else "未启用；首次使用可按需开启")
                        },
                        trailingContent = {
                            Switch(checked = renQingEnabled, onCheckedChange = { renQingViewModel.setRenQingEnabled(it) })
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("记账相册") },
                        supportingContent = {
                            Text(if (albumEnabled) "已启用，底部导航栏显示相册页面" else "未启用；需要时再开启")
                        },
                        trailingContent = {
                            Switch(checked = albumEnabled, onCheckedChange = { viewModel.setAlbumEnabled(it) })
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("自动记账") },
                        supportingContent = {
                            Text(
                                when {
                                    harmonyCompatMode -> "鸿蒙/兼容环境不支持自动记账"
                                    autoRecordEnabled -> "已启用；需要通知监听权限"
                                    else -> "未启用；需要时再开启"
                                }
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = autoRecordEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !isNotificationServiceEnabled(context)) {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        Toast.makeText(context, "请先开启通知监听权限", Toast.LENGTH_LONG).show()
                                    }
                                    viewModel.setAutoRecordEnabled(enabled)
                                }
                            )
                        }
                    )
                }
            }
        }
        val multiCurrencyEnabled by viewModel.multiCurrencyEnabled.collectAsState()
        SettingsSectionHeader("多币种管理", if (multiCurrencyEnabled) "已启用" else "未启用", currencySectionExpanded) { currencySectionExpanded = !currencySectionExpanded }
        AnimatedVisibility(visible = currencySectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("多币种资金管理") },
                    supportingContent = { Text(if (multiCurrencyEnabled) "已启用，首页显示多币种卡片" else "未启用") },
                    trailingContent = {
                        Switch(checked = multiCurrencyEnabled, onCheckedChange = { viewModel.setMultiCurrencyEnabled(it) })
                    }
                )
                if (multiCurrencyEnabled) {
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("币种卡片管理") },
                        supportingContent = { Text("添加、编辑或删除币种金额卡片") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToCurrencyManagement() }
                    )
                }
            }
        }

        }
        val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsState()
        val updateProxyUrl by viewModel.updateProxyUrl.collectAsState()
        val updateRepo by viewModel.updateRepo.collectAsState()
        val githubRepo by viewModel.githubRepo.collectAsState()
        val proxyOptions = com.inkqilin.ledger.util.PROXY_SOURCES + "自定义"
        var showProxyDropdown by remember { mutableStateOf(false) }
        var showCustomProxyInput by remember { mutableStateOf(false) }
        var customProxyUrl by remember { mutableStateOf("") }
        var showUpdateRepoDialog by remember { mutableStateOf(false) }
        var updateRepoInput by remember { mutableStateOf(updateRepo) }
        var showGithubRepoDialog by remember { mutableStateOf(false) }
        var githubRepoInput by remember { mutableStateOf(githubRepo) }

        SettingsSectionHeader("更新检测", if (checkUpdateEnabled) "启动时自动检查" else "已关闭", updateSectionExpanded) { updateSectionExpanded = !updateSectionExpanded }
        AnimatedVisibility(visible = updateSectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("启动时检测新版本") },
                    supportingContent = { Text(if (checkUpdateEnabled) "已启用，启动时自动检测 Gitee 新版本" else "已关闭") },
                    trailingContent = {
                        Switch(checked = checkUpdateEnabled, onCheckedChange = { viewModel.setCheckUpdateEnabled(it) })
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("更新检测仓库") },
                    supportingContent = { Text("Gitee: $updateRepo", maxLines = 2, fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        updateRepoInput = updateRepo
                        showUpdateRepoDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("GitHub 下载仓库") },
                    supportingContent = { Text("GitHub: $githubRepo", maxLines = 2, fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        githubRepoInput = githubRepo
                        showGithubRepoDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("检查更新") },
                    supportingContent = { Text("立即检测是否有新版本，有则弹出更新") },
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    modifier = Modifier.clickable { viewModel.triggerManualUpdateCheck() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // 代理源选择
                Box {
                    ListItem(
                        headlineContent = { Text("使用代理源") },
                        supportingContent = {
                            val displayText = if (updateProxyUrl in com.inkqilin.ledger.util.PROXY_SOURCES) {
                                val idx = com.inkqilin.ledger.util.PROXY_SOURCES.indexOf(updateProxyUrl)
                                "代理 ${idx + 1}: ${com.inkqilin.ledger.util.PROXY_SOURCES[idx]}"
                            } else {
                                "自定义: $updateProxyUrl"
                            }
                            Text(displayText, maxLines = 1, fontSize = 12.sp)
                        },
                        modifier = Modifier.clickable { showProxyDropdown = true }
                    )
                    DropdownMenu(
                        expanded = showProxyDropdown,
                        onDismissRequest = { showProxyDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        proxyOptions.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label, maxLines = 1, fontSize = 13.sp) },
                                onClick = {
                                    if (label == "自定义") {
                                        showCustomProxyInput = true
                                    } else {
                                        viewModel.setUpdateProxyUrl(label)
                                    }
                                    showProxyDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        }
        // 自定义代理源输入对话框
        if (showCustomProxyInput) {
            AlertDialog(
                onDismissRequest = { showCustomProxyInput = false },
                title = { Text("自定义代理源") },
                text = {
                    Column {
                        Text("请输入代理源 URL 前缀", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customProxyUrl,
                            onValueChange = { customProxyUrl = it },
                            placeholder = { Text("https://example.com/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val url = customProxyUrl.trim()
                        if (url.isNotBlank()) {
                            viewModel.setUpdateProxyUrl(url)
                        }
                        showCustomProxyInput = false
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomProxyInput = false }) { Text("取消") }
                }
            )
        }

        // 更新检测仓库对话框
        if (showUpdateRepoDialog) {
            AlertDialog(
                onDismissRequest = { showUpdateRepoDialog = false },
                title = { Text("更新检测仓库") },
                text = {
                    Column {
                        Text(
                            "填写 Gitee 仓库路径，用于检测新版本。支持 owner/repo 或完整地址。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "默认: ${com.inkqilin.ledger.util.DEFAULT_UPDATE_REPO}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = updateRepoInput,
                            onValueChange = { updateRepoInput = it },
                            placeholder = { Text("Murchey/inkqinlin-ledger") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val raw = updateRepoInput.trim()
                        val normalized = com.inkqilin.ledger.util.normalizeGiteeRepo(raw)
                        if (normalized.isBlank()) {
                            Toast.makeText(context, "路径不能为空", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setUpdateRepo(normalized)
                            Toast.makeText(context, "已保存: $normalized", Toast.LENGTH_SHORT).show()
                            showUpdateRepoDialog = false
                        }
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdateRepoDialog = false }) { Text("取消") }
                }
            )
        }

        // GitHub 下载仓库对话框
        if (showGithubRepoDialog) {
            AlertDialog(
                onDismissRequest = { showGithubRepoDialog = false },
                title = { Text("GitHub 下载仓库") },
                text = {
                    Column {
                        Text(
                            "填写 GitHub 仓库路径，用于「GitHub 仓库」和「代理」下载源。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "默认: ${com.inkqilin.ledger.util.DEFAULT_GITHUB_REPO}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = githubRepoInput,
                            onValueChange = { githubRepoInput = it },
                            placeholder = { Text("Murchey/inkqilin-ledger") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val raw = githubRepoInput.trim()
                        val normalized = com.inkqilin.ledger.util.normalizeGithubRepo(raw)
                        if (normalized.isBlank()) {
                            Toast.makeText(context, "路径不能为空", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setGithubRepo(normalized)
                            Toast.makeText(context, "已保存: $normalized", Toast.LENGTH_SHORT).show()
                            showGithubRepoDialog = false
                        }
                    }) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { showGithubRepoDialog = false }) { Text("取消") }
                }
            )
        }

        var labExpanded by rememberSaveable { mutableStateOf(false) }
        SettingsSectionHeader(
            title = "实验室功能",
            summary = if (autoRecordEnabled || ocrEnabled || albumEnabled) "部分功能已启用" else "未启用实验室功能",
            expanded = labExpanded,
            onClick = { labExpanded = !labExpanded }
        )
        AnimatedVisibility(visible = labExpanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Column {
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("测试系统通知") },
                    supportingContent = { Text("发送一条测试通知，确认系统通知权限和声音正常") },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    trailingContent = {
                        TextButton(onClick = { onSendTestNotification() }) { Text("测试") }
                    }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("OCR账单识别") },
                    supportingContent = { Text("通过 AI 识别图片账单并批量导入") },
                    trailingContent = {
                        Switch(
                            checked = ocrEnabled,
                            onCheckedChange = { viewModel.setOcrEnabled(it) }
                        )
                    }
                )
                if (ocrEnabled) {
                    Spacer(modifier = Modifier.height(0.5.dp))
                    ListItem(
                        headlineContent = { Text("OCR 识别 API 配置") },
                        supportingContent = { Text(if (ocrApiKey.isEmpty()) "点击配置 API Key" else "已配置 API Key") },
                        trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToOCRConfig() }
                    )
                }
            }
        }
    }


}
