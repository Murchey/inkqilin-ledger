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
internal fun SettingsDisplaySection(
    viewModel: TransactionViewModel,
    themeMode: ThemeMode,
    incomeColorHex: String,
    expenseColorHex: String,
    customPrimaryColorHex: String?,
    homeCardColorHex: String?,
    onShowHomeBgSheet: () -> Unit,
) {
    var sectionExpanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
        // region 2. 显示设置
        var displaySettingsExpanded by rememberSaveable { mutableStateOf(false) }
        SettingsSectionHeader("显示设置", when (themeMode) {
            ThemeMode.AUTO -> "跟随系统"
            ThemeMode.LIGHT -> "浅色模式"
            ThemeMode.DARK -> "深色模式"
        }, sectionExpanded) { sectionExpanded = !sectionExpanded }
        AnimatedVisibility(visible = sectionExpanded) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
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
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("收入展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = incomeColorHex,
                            onColorSelected = { viewModel.setIncomeColor(it) }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("支出展示颜色") },
                    trailingContent = {
                        ColorPickerButton(
                            selectedColor = expenseColorHex,
                            onColorSelected = { viewModel.setExpenseColor(it) }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(0.5.dp))
                ListItem(
                    headlineContent = { Text("主题色") },
                    supportingContent = { Text(if (customPrimaryColorHex != null) "自定义" else "默认靛蓝") },
                    trailingContent = {
                        Icon(
                            if (displaySettingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.clickable { displaySettingsExpanded = !displaySettingsExpanded }
                        )
                    },
                    modifier = Modifier.clickable { displaySettingsExpanded = !displaySettingsExpanded }
                )

                AnimatedVisibility(
                    visible = displaySettingsExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(MotionDurations.MEDIUM)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(MotionDurations.SHORT)
                    ) + fadeOut(
                        animationSpec = tween(MotionDurations.FAST)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Spacer(modifier = Modifier.height(0.5.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        val currentPrimary = MaterialTheme.colorScheme.primary
                        val currentHex = (customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX).lowercase()
                        var showThemeColorPicker by remember { mutableStateOf(false) }

                        @Composable
                        fun PresetSwatchRow(
                            label: String,
                            presets: List<com.inkqilin.ledger.util.ThemeColorPreset>,
                            trailing: (@Composable () -> Unit)? = null
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(presets) { preset ->
                                    val parsed = try {
                                        Color(android.graphics.Color.parseColor(preset.hex))
                                    } catch (_: Exception) {
                                        currentPrimary
                                    }
                                    val isSelected = currentHex == preset.hex.lowercase()
                                    val checkTint = if (parsed.luminance() > 0.55f) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.White
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(parsed)
                                            .clickable { viewModel.setCustomPrimaryColor(preset.hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = preset.name,
                                                tint = checkTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                if (trailing != null) {
                                    item { trailing() }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        PresetSwatchRow("亮色", com.inkqilin.ledger.util.BrightThemePresets) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        )
                                    )
                                    .clickable { showThemeColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "自定义颜色",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        PresetSwatchRow("暗色（低饱和）", com.inkqilin.ledger.util.DarkThemePresets)

                        if (showThemeColorPicker) {
                            ColorPickerDialog(
                                initialColor = customPrimaryColorHex ?: DEFAULT_PRIMARY_COLOR_HEX,
                                onColorSelected = {
                                    viewModel.setCustomPrimaryColor(it)
                                    showThemeColorPicker = false
                                },
                                onDismiss = { showThemeColorPicker = false }
                            )
                        }

                        if (customPrimaryColorHex != null && !currentHex.equals(DEFAULT_PRIMARY_COLOR_HEX, ignoreCase = true)) {
                            TextButton(onClick = { viewModel.setCustomPrimaryColor(null) }) {
                                Text("恢复默认主题色")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 主页主币种卡片颜色
                        Text(
                            "主页主币种卡片颜色",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var showHomeCardColorPicker by remember { mutableStateOf(false) }

                        @Composable
                        fun HomeCardSwatchRow(
                            label: String,
                            hexes: List<String>,
                            trailing: (@Composable () -> Unit)? = null
                        ) {
                            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(hexes) { hex ->
                                    val c = try {
                                        Color(android.graphics.Color.parseColor(hex))
                                    } catch (_: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    val selectedHex = homeCardColorHex ?: DEFAULT_HOME_CARD_COLOR_HEX
                                    val isSelected = selectedHex.equals(hex, true)
                                    val checkTint = if (c.luminance() > 0.55f) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.White
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .clickable { viewModel.setHomeCardColor(hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = checkTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (trailing != null) {
                                    item { trailing() }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        HomeCardSwatchRow("亮色", com.inkqilin.ledger.ui.theme.BrightHomeCardPresets) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        )
                                    )
                                    .clickable { showHomeCardColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "自定义颜色",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HomeCardSwatchRow("暗色（低饱和）", com.inkqilin.ledger.ui.theme.DarkHomeCardPresets)

                        if (homeCardColorHex != null) {
                            TextButton(onClick = { viewModel.setHomeCardColor(null) }) {
                                Text("恢复默认")
                            }
                        }

                        if (showHomeCardColorPicker) {
                            ColorPickerDialog(
                                initialColor = homeCardColorHex ?: DEFAULT_HOME_CARD_COLOR_HEX,
                                onColorSelected = {
                                    viewModel.setHomeCardColor(it)
                                    showHomeCardColorPicker = false
                                },
                                onDismiss = { showHomeCardColorPicker = false }
                            )
                        }
                    }
                }

                // 显示设置一级项：首页背景图（不放在主题色展开区内）
                Spacer(modifier = Modifier.height(0.5.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                val homeBgPathForRow by viewModel.homeBgImagePath.collectAsState()
                val homeBgOpacityForRow by viewModel.homeBgOpacity.collectAsState()
                ListItem(
                    headlineContent = { Text("首页背景图") },
                    supportingContent = {
                        Text(
                            if (homeBgPathForRow.isNullOrBlank()) "未设置 · 点击选择图片"
                            else "已设置 · 不透明度 ${(homeBgOpacityForRow * 100).toInt()}%"
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                    modifier = Modifier.clickable { onShowHomeBgSheet() }
                )
            }
        }

        }
}
