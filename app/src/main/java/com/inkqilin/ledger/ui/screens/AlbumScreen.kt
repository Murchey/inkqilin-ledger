package com.inkqilin.ledger.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.inkqilin.ledger.data.AlbumPhoto
import com.inkqilin.ledger.ui.TransactionViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
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

    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequestedThisDrag by remember { mutableStateOf(false) }

    var isDragging by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraXFailed by remember { mutableStateOf(false) }
    var tempSystemCameraUri by remember { mutableStateOf<Uri?>(null) }

    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempSystemCameraUri?.let { uri ->
                try {
                    val bitmap = BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri)
                    )
                    if (bitmap != null) {
                        saveToSystemGallery(context, bitmap)
                        polaroidBitmap = bitmap
                        capturedUri = uri
                        viewModel.addAlbumPhoto(uri.toString())
                        showFlash = true
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(isDragging, expansionProgress.value, selectedPhoto) {
        viewModel.setAlbumInteracting(selectedPhoto != null || isDragging || expansionProgress.value > 0.01f)
    }

    LaunchedEffect(isDragging) {
        if (!isDragging && expansionProgress.value > 0.01f) {
            permissionRequestedThisDrag = false
            kotlinx.coroutines.delay(150)
            if (isDragging) return@LaunchedEffect
            if (expansionProgress.value > 0.6f && imageCapture != null) {
                val capture = imageCapture!!
                val imageDir = File(context.filesDir, "album_photos")
                if (!imageDir.exists()) imageDir.mkdirs()
                val photoFile = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val uri = Uri.fromFile(photoFile)
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap != null) {
                                saveToSystemGallery(context, bitmap)
                                polaroidBitmap = bitmap
                                capturedUri = uri
                                // 立刻入库，避免动画未完成时退出相册导致丢失
                                viewModel.addAlbumPhoto(uri.toString())
                                showFlash = true
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
                        }
                        override fun onError(exception: ImageCaptureException) {
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
                )
            } else if (expansionProgress.value > 0.6f && cameraXFailed) {
                // CameraX 不可用时使用系统相机
                val imageDir = File(context.filesDir, "album_photos")
                if (!imageDir.exists()) imageDir.mkdirs()
                val photoFile = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                tempSystemCameraUri = Uri.fromFile(photoFile)
                scope.launch {
                    expansionProgress.animateTo(
                        0f,
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                }
                systemCameraLauncher.launch(contentUri)
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission.value = granted
        if (!granted) {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
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
                                if (!hasCameraPermission.value && newValue > 0.3f && !permissionRequestedThisDrag) {
                                    permissionRequestedThisDrag = true
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
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
                    hasPermission = hasCameraPermission.value,
                    showFlash = showFlash,
                    isPrinting = isPrinting,
                    onImageCaptureReady = { imageCapture = it },
                    onCameraInitFailed = { cameraXFailed = true }
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
            PhotoViewerScreen(
                photo = photo,
                onUpdate = { updated -> viewModel.updateAlbumPhoto(updated) },
                onDelete = {
                    showDeleteConfirm = photo
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

@Composable
private fun DynamicIslandCapsule(
    modifier: Modifier,
    expansionProgress: Animatable<Float, AnimationVector1D>,
    isDragging: Boolean,
    hasPermission: Boolean,
    showFlash: Boolean,
    isPrinting: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onCameraInitFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
    val shadowElevation = 4.dp + 12.dp * (if (isPrinting) 0f else expansionProgress.value)

    val shouldBindCamera = expansionProgress.value > 0.15f
    var cameraInitFailed by remember { mutableStateOf(false) }

    // 摄像头解绑清理
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider.unbindAll()
            } catch (_: Throwable) {}
        }
    }

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
            if (shouldBindCamera && hasPermission && !cameraInitFailed) {
                val cameraPreviewView = remember { mutableStateOf<PreviewView?>(null) }

                LaunchedEffect(cameraPreviewView.value) {
                    val pv = cameraPreviewView.value ?: return@LaunchedEffect
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(pv.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        onImageCaptureReady(capture)
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (_: Throwable) {
                        cameraInitFailed = true
                        onCameraInitFailed()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(cornerRadius))
                        .graphicsLayer { alpha = ((expansionProgress.value - 0.15f) / 0.4f).coerceIn(0f, 1f) }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }.also { cameraPreviewView.value = it }
                        },
                        modifier = Modifier.fillMaxSize()
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

            if (expansionProgress.value > 0.6f && isDragging) {
                Text(
                    "松手拍摄",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .graphicsLayer { alpha = ((expansionProgress.value - 0.6f) / 0.4f).coerceIn(0f, 1f) }
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
private fun PolaroidPrintAnimation(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumPhotoCard(
    photo: AlbumPhoto,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember(photo.id, photo.uri) {
        try {
            val uri = Uri.parse(photo.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.takeIf { it.width > 0 && it.height > 0 }
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = photo.note.ifEmpty { "照片" },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isSelectionMode && !isSelected) Modifier.graphicsLayer { alpha = 0.6f }
                            else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (photo.note.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = photo.note,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val sdf = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
            Text(
                text = sdf.format(Date(photo.createdAt)),
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun PhotoViewerScreen(
    photo: AlbumPhoto,
    onUpdate: (AlbumPhoto) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(photo.id, photo.uri) {
        try {
            val uri = Uri.parse(photo.uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.takeIf { it.width > 0 && it.height > 0 }
        } catch (_: Exception) {
            null
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var menuExpanded by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var showTimeEditor by remember { mutableStateOf(false) }

    var currentPhoto by remember { mutableStateOf(photo) }
    var editNote by remember { mutableStateOf(currentPhoto.note) }

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

    LaunchedEffect(photo) {
        currentPhoto = photo
        editNote = photo.note
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset + 48.dp, bottom = bottomInset + 48.dp)
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
                        onClick = { menuExpanded = false; onDelete() },
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

private fun copyUriToInternalStorage(context: Context, sourceUri: Uri): Uri? {
    return try {
        val imageDir = File(context.filesDir, "album_photos")
        if (!imageDir.exists()) imageDir.mkdirs()
        val destFile = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(destFile)
    } catch (_: Exception) {
        null
    }
}

private fun saveToSystemGallery(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/墨麒麟记账")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { targetUri ->
            resolver.openOutputStream(targetUri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }
        }
        uri
    } catch (_: Exception) {
        null
    }
}
