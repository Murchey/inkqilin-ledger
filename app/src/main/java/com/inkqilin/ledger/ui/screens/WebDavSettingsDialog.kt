package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.util.WebDavClient
import com.inkqilin.ledger.util.WebDavConfig
import kotlinx.coroutines.launch

@Composable
internal fun WebDavSettingsDialog(
    initial: WebDavConfig,
    onDismiss: () -> Unit,
    onSave: (WebDavConfig) -> Unit
) {
    var endpoint by remember { mutableStateOf(initial.endpoint) }
    var username by remember { mutableStateOf(initial.username) }
    var password by remember { mutableStateOf(initial.password) }
    var prefix by remember { mutableStateOf(initial.prefix) }
    var status by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val config = WebDavConfig(
        endpoint = endpoint.trim(),
        username = username.trim(),
        password = password,
        prefix = prefix.trim().ifBlank { "backups/v1" }
    )
    val canUse = config.isConfigured

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("WebDAV 设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "仅支持 HTTPS。HTTP 仅允许 localhost、127.0.0.1 或 ::1，用于本机联调。",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it; status = "" },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://dav.example.com/remote.php/dav/files/user") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; status = "" },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; status = "" },
                    label = { Text("密码或应用密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it; status = "" },
                    label = { Text("备份目录") },
                    supportingText = { Text("目录不存在时，测试将尝试创建并验证目录。") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (status.isNotBlank()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.startsWith("连接成功")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !working && canUse,
                    onClick = {
                        scope.launch {
                            working = true
                            status = "正在测试连接…"
                            try {
                                WebDavClient.testConnection(config)
                                status = "连接成功，备份目录可用"
                            } catch (error: Exception) {
                                status = error.message ?: "连接失败"
                            } finally {
                                working = false
                            }
                        }
                    }
                ) { Text(if (working) "测试中…" else "测试连接") }
                Button(
                    enabled = !working && canUse,
                    onClick = { onSave(config) }
                ) { Text("保存") }
            }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text("取消") }
        }
    )
}
