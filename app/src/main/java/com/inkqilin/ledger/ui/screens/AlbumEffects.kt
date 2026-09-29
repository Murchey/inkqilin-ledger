@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.inkqilin.ledger.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.inkqilin.ledger.data.AlbumPhoto
import com.inkqilin.ledger.util.DeviceCompat
import com.inkqilin.ledger.ui.TransactionViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
internal fun DynamicIslandCapsule(
    modifier: Modifier,
    expansionProgress: Animatable<Float, AnimationVector1D>,
    isDragging: Boolean,
    showFlash: Boolean,
    isPrinting: Boolean
) {
    val collapsedWidth = 150.dp
    val expandedWidth = 340.dp
    val printWidth = 184.dp
    val collapsedHeight = 40.dp
    val expandedHeight = 260.dp

    val currentWidth = if (isPrinting) {
        printWidth
    } else {
        collapsedWidth + (expandedWidth - collapsedWidth) * expansionProgress.value
    }
    val currentHeight = if (isPrinting) {
        collapsedHeight
    } else {
        collapsedHeight + (expandedHeight - collapsedHeight) * expansionProgress.value
    }
    val cornerRadius = if (isPrinting) {
        24.dp
    } else {
        50.dp - (50.dp - 24.dp) * expansionProgress.value
    }
    // 常量 elevation：阴影在展开动画中变化会在低端机上明显掉帧
    val shadowElevation = 8.dp

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(currentWidth)
                .height(currentHeight)
                .shadow(shadowElevation, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color(0xFF1C1C1C))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 纯系统相机：胶囊内不嵌 CameraX 预览，避免卓易通 native abort
            if (expansionProgress.value > 0.35f) {
                if (!(expansionProgress.value > 0.5f && isDragging)) {
                    Text(
                        "松手打开相机",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = ((expansionProgress.value - 0.35f) / 0.3f).coerceIn(0f, 1f)
                        }
                    )
                }
            }

            if (expansionProgress.value < 0.1f) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expansionProgress.value > 0.5f && isDragging) {
                Text(
                    "松手打开相机",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .graphicsLayer { alpha = ((expansionProgress.value - 0.5f) / 0.4f).coerceIn(0f, 1f) }
                )
            }

            if (showFlash) {
                val flashAlpha = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    flashAlpha.animateTo(0f, animationSpec = tween(300))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = flashAlpha.value }
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
internal fun PolaroidPrintAnimation(
    bitmap: Bitmap,
    onComplete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color.White else Color.Black
    val revealOffset = remember { Animatable(-700f) }
    val moveY = remember { Animatable(0f) }
    val shrinkScale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    val printEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val settleEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    LaunchedEffect(Unit) {
        launch {
            revealOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1400, easing = printEasing)
            )
        }
        launch {
            moveY.animateTo(
                targetValue = 80f,
                animationSpec = tween(durationMillis = 1400, easing = printEasing)
            )
        }
        kotlinx.coroutines.delay(1300)
        launch {
            shrinkScale.animateTo(
                targetValue = 0.15f,
                animationSpec = tween(durationMillis = 600, easing = settleEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
            )
        }
        kotlinx.coroutines.delay(700)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, moveY.value.roundToInt()) }
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = shrinkScale.value
                    scaleY = shrinkScale.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .width(180.dp)
                .height(240.dp)
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, revealOffset.value.roundToInt()) }
                    .width(180.dp)
                    .height(240.dp)
                    .border(3.dp, borderColor, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(12.dp, RoundedCornerShape(6.dp)),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp, 10.dp, 10.dp, 32.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(2.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
