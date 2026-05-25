package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.AlbumPhoto
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrBatchRecognitionScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val albumEnabled by viewModel.albumEnabled.collectAsState()
    val albumPhotos by viewModel.allAlbumPhotos.collectAsState()

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var recognizedTransactions by remember { mutableStateOf<List<RecognizedTransaction>>(emptyList()) }
    var isRecognizing by remember { mutableStateOf(false) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    var albumSelectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = uris
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (recognizedTransactions.isEmpty()) {
            // Image Selection State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedImages.isEmpty()) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("点击上传账单图片", style = MaterialTheme.typography.bodyLarge)
                    Text("支持批量上传，建议图片清晰", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (albumEnabled && albumPhotos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = { showAlbumPicker = true }) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从记账相册选择")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedImages) { uri ->
                            ImageThumbnail(uri)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { selectedImages = emptyList() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重置")
                        }
                        Button(
                            onClick = {
                                if (aiApiKey.isEmpty()) {
                                    Toast.makeText(context, "请先在设置中配置 AI API Key", Toast.LENGTH_LONG).show()
                                } else {
                                    scope.launch {
                                        isRecognizing = true
                                        val results = performOcr(context, selectedImages, aiApiKey, aiBaseUrl, aiModel)
                                        recognizedTransactions = results
                                        isRecognizing = false
                                        if (results.isEmpty()) {
                                            Toast.makeText(context, "未能从图片中识别出有效交易，请确保图片清晰或检查 API 配置", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isRecognizing
                        ) {
                            if (isRecognizing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("识别中...")
                            } else {
                                Text("开始识别")
                            }
                        }
                    }
                }
            }
        } else {
            // Recognition Results State
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("识别结果 (${recognizedTransactions.size})", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recognizedTransactions) { item ->
                        RecognizedItemCard(item, onRemove = {
                            recognizedTransactions = recognizedTransactions.filter { it != item }
                        })
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            recognizedTransactions.forEach { recognized ->
                                viewModel.addTransaction(
                                    Transaction(
                                        amount = recognized.amount,
                                        note = recognized.note,
                                        category = recognized.category,
                                        type = recognized.type,
                                        date = recognized.date.time,
                                        currency = "CNY"
                                    )
                                )
                            }
                            Toast.makeText(context, "已成功导入 ${recognizedTransactions.size} 条账单", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认并导入")
                }
            }
        }
    }

    if (showAlbumPicker) {
        AlertDialog(
            onDismissRequest = { showAlbumPicker = false; albumSelectedIds = emptySet() },
            title = { Text("选择记账相册照片") },
            text = {
                Column {
                    Text(
                        "已选择 ${albumSelectedIds.size} 张",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(albumPhotos, key = { it.id }) { photo ->
                            val isSelected = albumSelectedIds.contains(photo.id)
                            AlbumPickerItem(
                                photo = photo,
                                isSelected = isSelected,
                                onClick = {
                                    albumSelectedIds = if (isSelected) {
                                        albumSelectedIds - photo.id
                                    } else {
                                        albumSelectedIds + photo.id
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uris = albumPhotos
                            .filter { albumSelectedIds.contains(it.id) }
                            .map { Uri.parse(it.uri) }
                        selectedImages = uris
                        showAlbumPicker = false
                        albumSelectedIds = emptySet()
                    },
                    enabled = albumSelectedIds.isNotEmpty()
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAlbumPicker = false; albumSelectedIds = emptySet() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AlbumPickerItem(
    photo: AlbumPhoto,
    isSelected: Boolean,
    onClick: () -> Unit
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

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ImageThumbnail(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = uri.lastPathSegment ?: "图片",
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun RecognizedItemCard(item: RecognizedTransaction, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.type == TransactionType.EXPENSE) "-${item.amount}" else "+${item.amount}",
                        color = if (item.type == TransactionType.EXPENSE) Color(0xFFFF5252) else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(text = item.note, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(item.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

data class RecognizedTransaction(
    val date: Date,
    val amount: Double,
    val category: String,
    val note: String,
    val type: TransactionType
)

private suspend fun performOcr(
    context: Context,
    uris: List<Uri>,
    apiKey: String,
    baseUrl: String,
    model: String
): List<RecognizedTransaction> = withContext(Dispatchers.IO) {
    val results = mutableListOf<RecognizedTransaction>()
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val jsonRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.MULTILINE)

    for (uri in uris) {
        try {
            val base64Image = uriToBase64(context, uri) ?: continue

            val json = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", "你是一个极其严谨的账单识别专家。你的任务是逐行扫描图片，识别并提取图中的【每一笔】交易记录，严禁遗漏任何一条可见的账单。即使图片包含长列表、多栏数据或微小的文字，也必须全部识别。对于每一笔交易，请严格返回如下格式的 JSON 数组（不要包含任何额外说明或 markdown 块）：\n[{\"date\": \"YYYY-MM-DD\", \"amount\": 0.0, \"category\": \"分类名称\", \"note\": \"具体商品或服务名称\", \"type\": \"EXPENSE\"或\"INCOME\"}]。\n\n规则：\n1. 必须识别【所有】可见的交易，不得有选择性地忽略。\n2. 如果图片中没有日期，请推测或使用当前日期 yyyy-MM-dd。\n3. 分类名称请归纳为：餐饮, 交通, 购物, 娱乐, 居住, 医疗, 教育, 人情, 投资, 收入, 其他。\n4. 备注请填入具体的交易内容，如“瑞幸咖啡”或“美团外卖”。")
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        })
                    })
                })
                put("temperature", 0.0)
            }

            val request = Request.Builder()
                .url("${baseUrl.removeSuffix("/")}/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val responseJson = JSONObject(body)
                val content = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val match = jsonRegex.find(content)
                val jsonString = if (match != null) {
                    match.groupValues[1].trim()
                } else {
                    content.trim()
                }

                if (jsonString.isBlank() || !jsonString.startsWith("[")) continue

                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (!obj.has("amount")) continue

                    val amount = obj.optDouble("amount", -1.0)
                    if (amount < 0) continue

                    val dateStr = obj.optString("date", "")
                    val date = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: Date()
                    } catch (_: Exception) {
                        Date()
                    }

                    results.add(RecognizedTransaction(
                        date = date,
                        amount = amount,
                        category = obj.optString("category", "其他").ifBlank { "其他" },
                        note = obj.optString("note", ""),
                        type = if (obj.optString("type", "EXPENSE") == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    results
}

private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}
