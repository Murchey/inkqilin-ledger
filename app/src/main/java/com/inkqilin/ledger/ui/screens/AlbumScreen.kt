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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    viewModel: TransactionViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val photos by viewModel.allAlbumPhotos.collectAsState()

    var selectedPhoto by remember { mutableStateOf<AlbumPhoto?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<AlbumPhoto?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    var showPolaroid by remember { mutableStateOf(false) }
    var polaroidBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

    var showFlash by remember { mutableStateOf(false) }

    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequestedThisDrag by remember { mutableStateOf(false) }

    val expansionProgress = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(isDragging, expansionProgress.value) {
        viewModel.setAlbumInteracting(isDragging || expansionProgress.value > 0.01f)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
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
                    onImageCaptureReady = { imageCapture = it }
                )
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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

        if (showFlash) {
            val flashAlpha = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                flashAlpha.animateTo(0f, animationSpec = tween(200))
                showFlash = false
                showPolaroid = true
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = flashAlpha.value }
                    .background(Color.White)
            )
        }

        if (showPolaroid && polaroidBitmap != null) {
            PolaroidPrintAnimation(
                bitmap = polaroidBitmap!!,
                onComplete = {
                    showPolaroid = false
                    capturedUri?.let { viewModel.addAlbumPhoto(it.toString()) }
                    polaroidBitmap = null
                    capturedUri = null
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { galleryPicker.launch("image/*") },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "从相册选择")
            }
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
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("删除照片") },
                text = { Text("确定要删除这张照片吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAlbumPhoto(photo)
                            showDeleteConfirm = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
                }
            )
        }

        if (showBatchDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                title = { Text("批量删除") },
                text = { Text("确定要删除选中的 ${selectedIds.size} 张照片吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedIds.forEach { id ->
                                photos.find { it.id == id }?.let { viewModel.deleteAlbumPhoto(it) }
                            }
                            selectedIds = emptySet()
                            isSelectionMode = false
                            showBatchDeleteConfirm = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("取消") }
                }
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
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val collapsedWidth = 150.dp
    val expandedWidth = 340.dp
    val collapsedHeight = 40.dp
    val expandedHeight = 260.dp

    val currentWidth = collapsedWidth + (expandedWidth - collapsedWidth) * expansionProgress.value
    val currentHeight = collapsedHeight + (expandedHeight - collapsedHeight) * expansionProgress.value
    val cornerRadius = 50.dp - (50.dp - 24.dp) * expansionProgress.value
    val shadowElevation = 4.dp + 12.dp * expansionProgress.value

    val shouldBindCamera = expansionProgress.value > 0.15f

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    LaunchedEffect(shouldBindCamera) {
        if (shouldBindCamera && hasPermission) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
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
            } catch (_: Exception) {}
        } else if (!shouldBindCamera) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
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
            if (shouldBindCamera && hasPermission) {
                val cameraPreviewView = remember { mutableStateOf<PreviewView?>(null) }

                LaunchedEffect(cameraPreviewView.value) {
                    val pv = cameraPreviewView.value ?: return@LaunchedEffect
                    try {
                        val cameraProvider = cameraProviderFuture.get()
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
                    } catch (_: Exception) {}
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
    val expandProgress = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            expandProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = CubicBezierEasing(0.2f, 0.0f, 0.4f, 1.0f)
                )
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 350f,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = CubicBezierEasing(0.2f, 0.0f, 0.4f, 1.0f)
                )
            )
        }

        kotlinx.coroutines.delay(1400)

        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(300)
        )
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .graphicsLayer {
                    this.alpha = alpha.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleY = expandProgress.value
                }
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
            }
        } catch (_: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
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
@OptIn(ExperimentalMaterial3Api::class)
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
            }
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
                    Divider()
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .navigationBarsPadding()
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
        AlertDialog(
            onDismissRequest = { showNoteEditor = false },
            title = { Text("编辑备注") },
            text = {
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    placeholder = { Text("输入备注") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    currentPhoto = currentPhoto.copy(note = editNote)
                    onUpdate(currentPhoto)
                    showNoteEditor = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    editNote = currentPhoto.note
                    showNoteEditor = false
                }) { Text("取消") }
            }
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

        DatePickerDialog(
            onDismissRequest = { showTimeEditor = false },
            confirmButton = {
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
            },
            dismissButton = {
                TextButton(onClick = { showTimeEditor = false }) { Text("取消") }
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DatePicker(state = datePickerState)
                Spacer(modifier = Modifier.height(12.dp))
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
        }
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
