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
fun CurrencyManagementScreen(
    viewModel: TransactionViewModel
) {
    val allAssets by viewModel.allAssets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<CurrencyAsset?>(null) }

    if (showAddDialog) {
        CurrencyEditDialog(
            asset = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { asset ->
                viewModel.addCurrencyAsset(asset)
                showAddDialog = false
            }
        )
    }

    if (editingAsset != null) {
        CurrencyEditDialog(
            asset = editingAsset,
            onDismiss = { editingAsset = null },
            onConfirm = { asset ->
                viewModel.updateCurrencyAsset(asset)
                editingAsset = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "币种卡片管理",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    elevation = appButtonElevation()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加币种")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "管理您在首页展示的币种金额卡片，每张卡片代表一种货币的资产。点击心形图标可切换默认币种。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(allAssets, key = { it.id }) { asset ->
            val isDark = MaterialTheme.colorScheme.background.let { it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f } < 0.5f
            val resolvedColor = resolveCardColor(asset, isDark)
            val animatedCardColor by animateColorAsState(
                targetValue = resolvedColor,
                animationSpec = MotionSprings.interactive(), // iOS-like bouncy card color
                label = "cardColor_${asset.id}"
            )
            val assetInteractionSource = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(assetInteractionSource), // iOS-style interactive feedback
                shape = com.inkqilin.ledger.ui.theme.Corners.Md,
                colors = CardDefaults.cardColors(containerColor = animatedCardColor),
                interactionSource = assetInteractionSource,
                onClick = {}
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${asset.symbol} ${asset.name}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = asset.code + if (asset.isDefault) " · 默认" else "",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!asset.isDefault) {
                            TextButton(onClick = {
                                val currentDefault = allAssets.firstOrNull { it.isDefault }
                                if (currentDefault != null) {
                                    viewModel.updateCurrencyAsset(currentDefault.copy(isDefault = false))
                                }
                                viewModel.updateCurrencyAsset(asset.copy(isDefault = true))
                            }) {
                                Text("设为默认", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { editingAsset = asset }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = Color.White)
                        }
                        if (!asset.isDefault) {
                            IconButton(onClick = { viewModel.deleteCurrencyAsset(asset) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (allAssets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无币种卡片", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("点击上方按钮添加", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyEditDialog(
    asset: CurrencyAsset?,
    onDismiss: () -> Unit,
    onConfirm: (CurrencyAsset) -> Unit
) {
    var code by remember { mutableStateOf(asset?.code ?: "") }
    var symbol by remember { mutableStateOf(asset?.symbol ?: "") }
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var cardColor by remember { mutableStateOf(asset?.cardColor ?: "#1E6FFF") }
    var cardColorLight by remember { mutableStateOf(asset?.cardColorLight ?: asset?.cardColor ?: "#5B87FF") }
    var colorInput by remember { mutableStateOf(asset?.cardColor ?: "#1E6FFF") }
    val isEdit = asset != null
    val isDark = MaterialTheme.colorScheme.background.let { it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f } < 0.5f

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = if (isEdit) "编辑币种" else "添加币种",
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("币种代码（如 CNY）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEdit
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("符号（如 ¥）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（如 人民币）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("卡片颜色", style = MaterialTheme.typography.labelMedium)
                var showCurrencyColorPicker by remember { mutableStateOf(false) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CardColorPresets.take(CardColorPresets.size - 1)) { preset ->
                        val displayHex = if (isDark) preset.dark else preset.light
                        val c = try {
                            Color(android.graphics.Color.parseColor(displayHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        val isSelected = cardColor == preset.dark
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable {
                                    cardColor = preset.dark
                                    cardColorLight = preset.light
                                    colorInput = preset.dark
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                                .clickable { showCurrencyColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "自定义颜色",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (showCurrencyColorPicker) {
                    ColorPickerDialog(
                        initialColor = cardColor,
                        onColorSelected = {
                            cardColor = it
                            cardColorLight = it
                            colorInput = it
                            showCurrencyColorPicker = false
                        },
                        onDismiss = { showCurrencyColorPicker = false }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val previewColor = try {
                        Color(android.graphics.Color.parseColor(colorInput))
                    } catch (_: Exception) {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                    )
                    OutlinedTextField(
                        value = colorInput,
                        onValueChange = { newVal ->
                            colorInput = newVal
                            if (newVal.matches(Regex("^#[0-9A-Fa-f]{6,8}$"))) {
                                cardColor = newVal
                            }
                        },
                        label = { Text("自定义颜色代码") },
                        placeholder = { Text("#RRGGBB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        buttons = listOf(
            AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL, onDismiss),
            AppleDialogButton(if (isEdit) "保存" else "添加", AppleDialogButtonStyle.DEFAULT) {
                if (code.isNotBlank() && symbol.isNotBlank() && name.isNotBlank()) {
                    onConfirm(
                        (asset ?: CurrencyAsset(code = code, symbol = symbol, name = name, cardColor = cardColor, cardColorLight = cardColorLight)).copy(
                            code = code,
                            symbol = symbol,
                            name = name,
                            cardColor = cardColor,
                            cardColorLight = cardColorLight
                        )
                    )
                }
            }
        )
    )
}
