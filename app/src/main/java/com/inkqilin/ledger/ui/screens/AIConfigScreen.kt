package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.inkqilin.ledger.util.AiDataRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val aiDataRange by viewModel.aiDataRange.collectAsState()

    val ocrApiKey by viewModel.ocrApiKey.collectAsState()
    val ocrBaseUrl by viewModel.ocrBaseUrl.collectAsState()
    val ocrModel by viewModel.ocrModel.collectAsState()

    var tempApiKey by remember { mutableStateOf(aiApiKey) }
    var tempBaseUrl by remember { mutableStateOf(aiBaseUrl) }
    var tempModel by remember { mutableStateOf(aiModel) }
    var dataRangeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(aiApiKey, aiBaseUrl, aiModel) {
        tempApiKey = aiApiKey
        tempBaseUrl = aiBaseUrl
        tempModel = aiModel
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "分析数据范围",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "每次分析时上传的账单数据时间范围",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box {
                ListItem(
                    headlineContent = { Text("数据范围") },
                    supportingContent = { Text(aiDataRange.label) },
                    trailingContent = {
                        Icon(
                            if (dataRangeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { dataRangeExpanded = !dataRangeExpanded }
                )
                DropdownMenu(
                    expanded = dataRangeExpanded,
                    onDismissRequest = { dataRangeExpanded = false }
                ) {
                    AiDataRange.entries.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.label) },
                            onClick = {
                                viewModel.setAiDataRange(range)
                                dataRangeExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Divider()

        Text(
            text = "AI API 配置",
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

        TextButton(
            onClick = {
                tempApiKey = ocrApiKey
                tempBaseUrl = ocrBaseUrl
                tempModel = ocrModel
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("从 OCR 识别 API 一键同步配置")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "收支分析 API 设置指南",
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
                GuideItem(number = "2", text = "支持 OpenAI 格式的 API 接口，使用文本模型即可")
                GuideItem(number = "3", text = "软件打开后每日自动分析一次，分析失败会在相关卡片显示红色感叹号")
                GuideItem(number = "4", text = "本软件只负责上传账单统计数据到 API 平台，不上传个人信息")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.setAiApiKey(tempApiKey)
                viewModel.setAiBaseUrl(tempBaseUrl)
                viewModel.setAiModel(tempModel)
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
