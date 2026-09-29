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
internal fun PhotoViewerScreen(
    photos: List<AlbumPhoto>,
    initialIndex: Int,
    onUpdate: (AlbumPhoto) -> Unit,
    onDelete: (AlbumPhoto) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 避免 composable 早期 return（部分编译链路会生成 NON_LOCAL_RETURN 导致 D8 失败）
    if (photos.isEmpty()) {
        onBack()
    }
    val startIndex = initialIndex.coerceIn(0, photos.lastIndex)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = startIndex) {
        photos.size
    }
    val currentPage = pagerState.currentPage.coerceIn(0, photos.lastIndex)
    var currentPhoto by remember { mutableStateOf(photos[startIndex]) }
    var editNote by remember { mutableStateOf(currentPhoto.note) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showTimeEditor by remember { mutableStateOf(false) }

    LaunchedEffect(currentPage, photos) {
        photos.getOrNull(currentPage)?.let {
            currentPhoto = it
            editNote = it.note
        }
    }

    // 系统返回：先关弹层/查看器，避免 MainScreen 把返回劫去切 Tab 导致底栏卡死
    androidx.activity.compose.BackHandler {
        when {
            showTimeEditor -> showTimeEditor = false
            showNoteEditor -> showNoteEditor = false
            menuExpanded -> menuExpanded = false
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // 系统相册式：左右滑切换上一张/下一张
        // 注意：key 内避免 return@key，会生成 NON_LOCAL_RETURN 导致 D8 无法 dex
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset + 48.dp, bottom = bottomInset + 48.dp)
        ) { page ->
            key(page) {
                val pagePhoto = photos.getOrNull(page)
                if (pagePhoto != null) {
                    val bitmap = remember(pagePhoto.id, pagePhoto.uri) {
                        try {
                            val uri = Uri.parse(pagePhoto.uri)
                            DeviceCompat.decodeBitmapSampled(context, uri, maxDim = 2048)
                                ?.takeIf { it.width > 0 && it.height > 0 }
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var offsetY by remember { mutableFloatStateOf(0f) }
                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                        if (scale > 1f) {
                            offsetX += offsetChange.x
                            offsetY += offsetChange.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformState, lockRotationOnZoomPan = true)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
            }

            if (photos.size > 1) {
                Text(
                    text = "${currentPage + 1}/${photos.size}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.White)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑备注") },
                        onClick = { menuExpanded = false; showNoteEditor = true },
                        leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("修改时间") },
                        onClick = { menuExpanded = false; showTimeEditor = true },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete(currentPhoto) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }

        // Bottom info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(bottom = bottomInset + 8.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (currentPhoto.note.isNotEmpty()) {
                Text(
                    text = currentPhoto.note,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            val fullSdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            Text(
                text = fullSdf.format(Date(currentPhoto.createdAt)),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    if (showNoteEditor) {
        AppleAlertDialog(
            onDismissRequest = { showNoteEditor = false },
            title = "编辑备注",
            content = {
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    placeholder = { Text("输入备注") }
                )
            },
            buttons = listOf(
                AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) {
                    editNote = currentPhoto.note
                    showNoteEditor = false
                },
                AppleDialogButton("保存", AppleDialogButtonStyle.DEFAULT) {
                    currentPhoto = currentPhoto.copy(note = editNote)
                    onUpdate(currentPhoto)
                    showNoteEditor = false
                }
            )
        )
    }

    if (showTimeEditor) {
        val calendar = remember { Calendar.getInstance() }
        LaunchedEffect(currentPhoto) {
            calendar.timeInMillis = currentPhoto.createdAt
        }

        var hourText by remember(currentPhoto.createdAt) {
            mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'))
        }
        var minuteText by remember(currentPhoto.createdAt) {
            mutableStateOf(calendar.get(Calendar.MINUTE).toString().padStart(2, '0'))
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentPhoto.createdAt
        )

        val bgColor = MaterialTheme.colorScheme.background
        val isDark = (bgColor.red * 0.299f + bgColor.green * 0.587f + bgColor.blue * 0.114f) < 0.5f

        val view = LocalView.current
        Dialog(
            onDismissRequest = { showTimeEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LaunchedEffect(Unit) {
                (view.context as? android.app.Activity)?.window?.let { window ->
                    window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showTimeEditor = false }
                        )
                )
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = if (isDark) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.widthIn(min = 328.dp, max = 360.dp)
                ) {
                Column {
                    DatePicker(state = datePickerState)
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("时间", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = hourText,
                                onValueChange = { hourText = it.filter { c -> c.isDigit() }.take(2) },
                                modifier = Modifier.width(72.dp),
                                label = { Text("时") },
                                singleLine = true
                            )
                            Text(":", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = minuteText,
                                onValueChange = { minuteText = it.filter { c -> c.isDigit() }.take(2) },
                                modifier = Modifier.width(72.dp),
                                label = { Text("分") },
                                singleLine = true
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimeEditor = false }) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            val selectedDate = datePickerState.selectedDateMillis ?: currentPhoto.createdAt
                            val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 0
                            val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            currentPhoto = currentPhoto.copy(createdAt = cal.timeInMillis)
                            onUpdate(currentPhoto)
                            showTimeEditor = false
                        }) { Text("确定") }
                    }
                }
            } // Surface
            } // Box
        } // Popup
    }
}
