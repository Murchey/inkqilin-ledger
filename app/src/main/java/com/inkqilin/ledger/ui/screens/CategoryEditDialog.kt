@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.BackHandler
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun CategoryEditDialog(
    category: com.inkqilin.ledger.data.Category? = null,
    @Suppress("UNUSED_PARAMETER") type: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "📦") }
    var color by remember { mutableStateOf(category?.color ?: "#715CFF") }

    val emojiList = listOf(
        "🍜", "🚗", "🛒", "🎮", "🏠", "📦", "💰", "🎁", "📈", "💼",
        "💳", "🚌", "✈️", "🏥", "📚", "🎵", "🎬", "⚽", "🐱", "🐶",
        "☕", "🍺", "🛍️", "💄", "💇", "🔧", "📱", "💻", "🎓", "🎉",
        "🌿", "🏋️", "🍕", "🍰", "🧋", "🚕", "⛽", "🏡", "🏢", "🏦",
        "👶", "🧹", "💊", "📌", "💡", "🔥", "⭐", "❤️", "✅", "🆕"
    )

    val bgColor = MaterialTheme.colorScheme.background
    val isDark = (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f

    AppleAlertDialog(
        onDismissRequest = onDismiss,
        title = if (category == null) "添加分类" else "修改分类",
        content = {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF007AFF),
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                focusedLabelColor = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF007AFF),
                unfocusedLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6E6E73),
                cursorColor = if (isDark) Color.White else Color(0xFF1D1D1F),
                focusedTextColor = if (isDark) Color.White else Color(0xFF1D1D1F),
                unfocusedTextColor = if (isDark) Color.White else Color(0xFF1D1D1F)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )
                Text(
                    "选择图标",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6E6E73)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(emojiList) { emoji ->
                        val selected = icon == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color(0xFF007AFF).copy(alpha = 0.12f)
                                    else if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color.Black.copy(alpha = 0.05f)
                                )
                                .clickable { icon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("或手动输入 Emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors
                )
                Text(
                    "选择颜色",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6E6E73)
                )
                val presetColors = listOf("#715CFF", "#51B4FF", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#E91E63")
                var showCategoryColorPicker by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                .clickable { color = colorHex }
                                .then(
                                    if (color == colorHex) Modifier.border(2.dp, Color.White, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == colorHex) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                    // Custom color button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                )
                            )
                            .clickable { showCategoryColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "自定义颜色",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (showCategoryColorPicker) {
                    ColorPickerDialog(
                        initialColor = color,
                        onColorSelected = {
                            color = it
                            showCategoryColorPicker = false
                        },
                        onDismiss = { showCategoryColorPicker = false }
                    )
                }
            }
        },
        buttons = listOf(
            AppleDialogButton(
                text = "取消",
                style = AppleDialogButtonStyle.CANCEL,
                onClick = onDismiss
            ),
            AppleDialogButton(
                text = "确定",
                style = AppleDialogButtonStyle.DEFAULT,
                onClick = { if (name.isNotBlank()) onConfirm(name, icon, color) }
            )
        )
    )
}
