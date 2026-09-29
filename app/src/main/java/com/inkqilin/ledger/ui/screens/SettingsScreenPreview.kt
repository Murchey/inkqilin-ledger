@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.*
import com.inkqilin.ledger.ui.motion.*
import com.inkqilin.ledger.ui.theme.*
import com.inkqilin.ledger.util.*
import com.inkqilin.ledger.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun SettingsScreenPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("显示设置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column {
                        ListItem(headlineContent = { Text("深浅色模式") }, supportingContent = { Text("跟随系统") }, trailingContent = { TextButton(onClick = {}) { Text("切换") } })
                        Spacer(modifier = Modifier.height(0.5.dp))
                        ListItem(headlineContent = { Text("收入展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF34C759))) })
                        Spacer(modifier = Modifier.height(0.5.dp))
                        ListItem(headlineContent = { Text("支出展示颜色") }, trailingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFF3B30))) })
                    }
                }
                Text("分类管理", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(headlineContent = { Text("账单标签（类别）管理") }, supportingContent = { Text("添加、修改或删除收支分类及人情标签") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
                Text("更新检测", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(
                        headlineContent = { Text("启动时检测新版本") },
                        supportingContent = { Text("已启用，启动时自动检测 Gitee 新版本") },
                        trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                    )
                }
                Text("关于", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    ListItem(headlineContent = { Text("关于 墨麒麟记账") }, supportingContent = { Text("版本 1.3.0 · GitHub 仓库") }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable {})
                }
            }
        }
    }
}
