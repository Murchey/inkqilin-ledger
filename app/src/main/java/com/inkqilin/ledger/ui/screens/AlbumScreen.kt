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
fun AlbumScreen(
    viewModel: TransactionViewModel,
    isActive: Boolean = true,
    fabTrigger: Boolean = false,
    onFabTriggered: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val photos by viewModel.allAlbumPhotos.collectAsState()

    // Reset capsule when page becomes active
    val expansionProgress = remember { Animatable(0f) }
    LaunchedEffect(isActive) {
        if (!isActive) {
            expansionProgress.snapTo(0f)
        }
    }

    var selectedPhoto by remember { mutableStateOf<AlbumPhoto?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AlbumPhoto?>(null) }

    // 离开相册（Tab 切走/导航/销毁）时必须复位，否则底栏永久隐藏
    DisposableEffect(Unit) {
        onDispose { viewModel.setAlbumInteracting(false) }
    }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    var showPolaroid by remember { mutableStateOf(false) }
    var polaroidBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

    var showFlash by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }

    LaunchedEffect(showFlash) {
        if (showFlash) {
            kotlinx.coroutines.delay(300)
            showFlash = false
            showPolaroid = true
            isPrinting = true
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    // 系统相机返回后进程可能被回收：URI 必须可恢复，否则 success 也会丢照
    var tempSystemCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempSystemCameraUri
                ?: latestAlbumPhotoFile(context)?.let { Uri.fromFile(it) }
            if (uri != null) {
                try {
                    val photoFile = uri.path?.let { File(it) }
                    val bitmap = photoFile?.let { DeviceCompat.decodeBitmapSampled(it) }
                        ?: DeviceCompat.decodeBitmapSampled(context, uri)
                    if (bitmap != null) {
                        saveToSystemGallery(context, bitmap)
                        polaroidBitmap = bitmap
                        capturedUri = uri
                        viewModel.addAlbumPhoto(uri.toString())
                        showFlash = true
                    } else {
                        Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Throwable) {
                    Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "未找到照片", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 取消：清理半截文件并收回胶囊
            runCatching { tempSystemCameraUri?.path?.let { File(it).delete() } }
            tempSystemCameraUri = null
            scope.launch {
                expansionProgress.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
            }
        }
    }

    LaunchedEffect(isDragging, expansionProgress.value, selectedPhoto) {
        viewModel.setAlbumInteracting(selectedPhoto != null || isDragging || expansionProgress.value > 0.01f)
    }

    // 系统相机启动（唯一拍照路径：避免卓易通上 CameraX native abort）
    val launchSystemCamera: () -> Unit = {
        val imageDir = File(context.filesDir, "album_photos")
        if (!imageDir.exists()) imageDir.mkdirs()
        val photoFile = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
        tempSystemCameraUri = Uri.fromFile(photoFile)
        val contentUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
        } catch (_: Throwable) {
            Toast.makeText(context, "无法打开相机（存储不可用）", Toast.LENGTH_SHORT).show()
            null
        }
        scope.launch {
            expansionProgress.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
        }
        if (contentUri != null) {
            try {
                systemCameraLauncher.launch(contentUri)
            } catch (_: Throwable) {
                Toast.makeText(context, "无法打开系统相机", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isDragging) {
        if (!isDragging && expansionProgress.value > 0.01f) {
            kotlinx.coroutines.delay(150)
            if (isDragging) return@LaunchedEffect
            if (expansionProgress.value > 0.6f) {
                launchSystemCamera()
            } else {
                expansionProgress.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedUri = copyUriToInternalStorage(context, it)
            if (savedUri != null) {
                viewModel.addAlbumPhoto(savedUri.toString())
            }
        }
    }

    // Trigger gallery picker from nav bar FAB
    LaunchedEffect(fabTrigger) {
        if (fabTrigger) {
            galleryPicker.launch("image/*")
            onFabTriggered()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isActive, selectedPhoto) {
                if (!isActive || selectedPhoto != null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var lastY = down.position.y
                        isDragging = true

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                isDragging = false
                                break
                            }
                            val currentY = change.position.y
                            val deltaY = currentY - lastY
                            lastY = currentY
                            if (deltaY > 0f) {
                                val dragThresholdPx = with(density) { 250.dp.toPx() }
                                val newValue = (expansionProgress.value + deltaY / dragThresholdPx)
                                    .coerceIn(0f, 1f)
                                scope.launch { expansionProgress.snapTo(newValue) }
                            }
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedPhoto == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicIslandCapsule(
                    modifier = Modifier,
                    expansionProgress = expansionProgress,
                    isDragging = isDragging,
                    showFlash = showFlash,
                    isPrinting = isPrinting
                )
            }
            }

            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isSelectionMode = false
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                    Text(
                        "已选 ${selectedIds.size} 项",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        selectedIds = if (selectedIds.size == photos.size) {
                            emptySet()
                        } else {
                            photos.map { it.id }.toSet()
                        }
                    }) {
                        Text(if (selectedIds.size == photos.size) "取消全选" else "全选")
                    }
                    IconButton(
                        onClick = { showBatchDeleteConfirm = true },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            if (photos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有照片",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "下拉页面拍照，或点击右下角从相册选择",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = navBarBottomPadding + 76.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        AlbumPhotoCard(
                            photo = photo,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedIds.contains(photo.id),
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (selectedIds.contains(photo.id)) {
                                        selectedIds - photo.id
                                    } else {
                                        selectedIds + photo.id
                                    }
                                    if (selectedIds.isEmpty()) isSelectionMode = false
                                } else {
                                    selectedPhoto = photo
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedIds = setOf(photo.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showPolaroid && polaroidBitmap != null) {
            PolaroidPrintAnimation(
                bitmap = polaroidBitmap!!,
                onComplete = {
                    showPolaroid = false
                    isPrinting = false
                    polaroidBitmap = null
                    capturedUri = null
                    // 已在拍照成功时入库，这里不再重复插入
                }
            )
        }

        selectedPhoto?.let { photo ->
            val startIndex = photos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
            PhotoViewerScreen(
                photos = photos,
                initialIndex = startIndex,
                onUpdate = { updated -> viewModel.updateAlbumPhoto(updated) },
                onDelete = { toDelete ->
                    showDeleteConfirm = toDelete
                    selectedPhoto = null
                },
                onBack = { selectedPhoto = null }
            )
        }

        showDeleteConfirm?.let { photo ->
            AppleAlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = "删除照片",
                message = "确定要删除这张照片吗？",
                buttons = listOf(
                    AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { showDeleteConfirm = null },
                    AppleDialogButton("删除", AppleDialogButtonStyle.DESTRUCTIVE) {
                        viewModel.deleteAlbumPhoto(photo)
                        showDeleteConfirm = null
                    }
                )
            )
        }

        if (showBatchDeleteConfirm) {
            AppleAlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                title = "批量删除",
                message = "确定要删除选中的 ${selectedIds.size} 张照片吗？",
                buttons = listOf(
                    AppleDialogButton("取消", AppleDialogButtonStyle.CANCEL) { showBatchDeleteConfirm = false },
                    AppleDialogButton("删除", AppleDialogButtonStyle.DESTRUCTIVE) {
                        selectedIds.forEach { id ->
                            photos.find { it.id == id }?.let { viewModel.deleteAlbumPhoto(it) }
                        }
                        selectedIds = emptySet()
                        isSelectionMode = false
                        showBatchDeleteConfirm = false
                    }
                )
            )
        }
    }
}
