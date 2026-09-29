package com.inkqilin.ledger.ui.screens

import android.content.Context
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.util.NotificationHelper
import com.inkqilin.ledger.ui.screens.AppleDatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleBillSettingsPanel(
    globalEnabled: Boolean, onGlobalEnabledChanged: (Boolean) -> Unit,
    defaultAdvanceMinutes: Int, onDefaultAdvanceMinutesChanged: (Int) -> Unit,
    onViewRecycleBin: () -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
            Text("周期账单设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("总开关", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = globalEnabled, onCheckedChange = onGlobalEnabledChanged)
            }
            Spacer(Modifier.height(16.dp))

            Text("默认提前提醒时间 (新建时预填)", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            AdvanceTimePicker(selectedMinutes = defaultAdvanceMinutes, onSelect = onDefaultAdvanceMinutesChanged)
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        (context as? Activity)?.requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                        )
                    } else {
                        NotificationHelper.showTestNotification(context)
                        android.widget.Toast.makeText(context, "测试通知已发送", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("测试系统通知")
            }
            Spacer(Modifier.height(16.dp))

            Button(onClick = onViewRecycleBin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("查看回收站")
            }
            Spacer(Modifier.height(16.dp))

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(),
                   colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Text("完成")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdvanceTimePicker(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    val options = arrayOf(15, 30, 60, 120, 720, 1440)
    val optionTexts = arrayOf("15分钟", "30分钟", "1小时", "2小时", "半天", "一天")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { index, minutes ->
            val isSelected = selectedMinutes == minutes
            Surface(modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null) {
                Text(optionTexts[index],
                     modifier = Modifier.fillMaxWidth().clickable { onSelect(minutes) }.padding(vertical = 8.dp),
                     style = MaterialTheme.typography.labelSmall,
                     color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                     textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
