package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.appButtonElevation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRConfigScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val ocrApiKey by viewModel.ocrApiKey.collectAsState()
    val ocrBaseUrl by viewModel.ocrBaseUrl.collectAsState()
    val ocrModel by viewModel.ocrModel.collectAsState()

    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()

    var tempApiKey by remember { mutableStateOf(ocrApiKey) }
    var tempBaseUrl by remember { mutableStateOf(ocrBaseUrl) }
    var tempModel by remember { mutableStateOf(ocrModel) }

    LaunchedEffect(ocrApiKey, ocrBaseUrl, ocrModel) {
        tempApiKey = ocrApiKey
        tempBaseUrl = ocrBaseUrl
        tempModel = ocrModel
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "OCR 识别 API 配置",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = tempApiKey,
            onValueChange = { tempApiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入您的 AI API Key") }
        )

        OutlinedTextField(
            value = tempBaseUrl,
            onValueChange = { tempBaseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如: https://api.openai.com/v1") }
        )

        OutlinedTextField(
            value = tempModel,
            onValueChange = { tempModel = it },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如: gpt-4o 或 qwen-vl-plus") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                tempApiKey = aiApiKey
                tempBaseUrl = aiBaseUrl
                tempModel = aiModel
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("从收支分析 API 一键同步配置")
        }

        Divider()

        Text(
            text = "OCR API 设置指南",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OCRGuideItem(number = "1", text = "建议使用国内 API 平台的模型（例如硅基流动、智谱清言、阿里云百炼等）")
                OCRGuideItem(number = "2", text = "OCR 功能需要视觉模型（名称包含 omni 或 VL 字段）")
                OCRGuideItem(number = "3", text = "支持 OpenAI 格式的 API 接口")
                OCRGuideItem(number = "4", text = "本软件只负责上传账单图片到 API 平台，不上传个人信息")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.setOcrApiKey(tempApiKey)
                viewModel.setOcrBaseUrl(tempBaseUrl)
                viewModel.setOcrModel(tempModel)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = appButtonElevation()
        ) {
            Text("保存配置", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OCRGuideItem(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = 1.dp)
        ) {
            Text(
                text = number,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}
