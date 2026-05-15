package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.ui.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()

    var tempApiKey by remember { mutableStateOf(aiApiKey) }
    var tempBaseUrl by remember { mutableStateOf(aiBaseUrl) }
    var tempModel by remember { mutableStateOf(aiModel) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI API 配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.setAiApiKey(tempApiKey)
                        viewModel.setAiBaseUrl(tempBaseUrl)
                        viewModel.setAiModel(tempModel)
                        onBack()
                    }) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            Text(
                text = "OCR AI API 设置指南",
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
                    GuideItem(number = "1", text = "建议使用国内 API 平台的模型（例如硅基流动、智谱清言、阿里云百炼等）")
                    GuideItem(number = "2", text = "模型必须为视觉模型或包含视觉输入（模型名称内包含 omni 或 VL 字段）")
                    GuideItem(number = "3", text = "链接使用 OpenAI 格式的请求链接")
                    GuideItem(number = "4", text = "识别数据不全、错误问题，大部分由模型造成，建议更换能力更好的模型")
                    GuideItem(number = "5", text = "本软件只负责上传图片数据到 API 平台，接受返回数据")
                }
            }
        }
    }
}

@Composable
private fun GuideItem(number: String, text: String) {
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
