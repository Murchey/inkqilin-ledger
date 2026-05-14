package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkqilin.ledger.data.*
import com.inkqilin.ledger.ui.RenQingViewModel
import com.inkqilin.ledger.ui.TransactionViewModel
import com.inkqilin.ledger.ui.theme.InkQilinLedgerTheme
import com.inkqilin.ledger.ui.motion.*
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: TransactionViewModel,
    renQingViewModel: RenQingViewModel
) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
    val incomeCategories = categories.filter { it.type == TransactionType.INCOME }
    val renQingTags by renQingViewModel.allTags.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<RenQingTag?>(null) }

    if (showAddDialog) {
        CategoryEditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { viewModel.addCategory(it.name, it.icon, it.type, it.color); showAddDialog = false }
        )
    }

    editingCategory?.let { category ->
        CategoryEditDialog(
            category = category,
            onDismiss = { editingCategory = null },
            onConfirm = { viewModel.updateCategory(it); editingCategory = null }
        )
    }

    if (showAddTagDialog) {
        RenQingTagEditDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { renQingViewModel.addTag(it); showAddTagDialog = false }
        )
    }

    editingTag?.let { tag ->
        RenQingTagEditDialog(
            tag = tag,
            onDismiss = { editingTag = null },
            onConfirm = { renQingViewModel.updateTag(it); editingTag = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("支出类别", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加")
                }
            }
        }
        items(expenseCategories) { category ->
            CategoryItem(
                category = category,
                onEdit = { editingCategory = it },
                onDelete = { viewModel.deleteCategory(it) }
            )
        }
        item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
        item {
            Text("收入类别", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(incomeCategories) { category ->
            CategoryItem(
                category = category,
                onEdit = { editingCategory = it },
                onDelete = { viewModel.deleteCategory(it) }
            )
        }
        item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("人情标签", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddTagDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加")
                }
            }
        }
        items(renQingTags) { tag ->
            RenQingTagItem(
                tag = tag,
                onEdit = { editingTag = it },
                onDelete = { renQingViewModel.deleteTag(it) }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${category.name}」类别吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(category); showDeleteDialog = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource) // iOS-style interactive feedback
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onEdit(category) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RenQingTagItem(
    tag: RenQingTag,
    onEdit: (RenQingTag) -> Unit,
    onDelete: (RenQingTag) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val tagColor = try {
        Color(android.graphics.Color.parseColor(tag.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${tag.name}」标签吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(tag); showDeleteDialog = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource) // iOS-style interactive feedback
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tagColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(tag.icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onEdit(tag) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    val isEdit = category != null
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "") }
    var type by remember { mutableStateOf(category?.type ?: TransactionType.EXPENSE) }
    var color by remember { mutableStateOf(category?.color ?: "#715CFF") }
    val emojiList = listOf(
        "\uD83D\uDCE6", "\uD83C\uDF7D\uFE0F", "\uD83D\uDE97", "\uD83D\uDCBB", "\uD83C\uDF93",
        "\uD83C\uDFE0", "\uD83D\uDC8D", "\uD83D\uDC57", "\uD83D\uDED2", "\u2764\uFE0F",
        "\uD83C\uDF81", "\uD83C\uDF89", "\uD83C\uDFA8", "\uD83C\uDFB5", "\uD83D\uDCDA",
        "\uD83C\uDFB8", "\u2B50", "\uD83C\uDF1F", "\uD83C\uDFC6", "\uD83D\uDC8E",
        "\uD83D\uDCB0", "\uD83D\uDCB3", "\uD83D\uDCA1", "\u2708\uFE0F", "\uD83C\uDFAB",
        "\uD83C\uDF88", "\uD83C\uDF70", "\uD83C\uDF7A", "\uD83D\uDCDA", "\u2615",
        "\uD83C\uDF54", "\uD83C\uDF55", "\uD83C\uDF63", "\uD83C\uDF66", "\u26BD",
        "\uD83C\uDFC0", "\uD83C\uDFBF", "\uD83E\uDD3E", "\uD83D\uDCF7", "\uD83D\uDCAC",
        "\u2702\uFE0F", "\uD83D\uDD27", "\uD83D\uDCBA", "\uD83D\uDCF1", "\u2328\uFE0F",
        "\uD83D\uDCE6", "\uD83C\uDFD4\uFE0F", "\uD83D\uDEEB", "\uD83D\uDEE9\uFE0F", "\u2693",
        "\uD83C\uDFA4", "\uD83C\uDFA7", "\uD83C\uDFB6", "\uD83C\uDFBC"
    )
    val colorOptions = listOf("#715CFF", "#E91E63", "#F44336", "#FF9800", "#FFC107", "#4CAF50", "#00BCD4", "#2196F3", "#9C27B0", "#607D8B", "#795548", "#FF5722")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑类别" else "添加类别") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("类别名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("类型", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(if (t == TransactionType.EXPENSE) "支出" else "收入") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("图标", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("自定义emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojiList) { emoji ->
                        FilterChip(
                            selected = icon == emoji,
                            onClick = { icon = emoji },
                            label = { Text(emoji, fontSize = 20.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("颜色", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { c ->
                        val cColor = try { Color(android.graphics.Color.parseColor(c)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(cColor))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && icon.isNotBlank()) {
                        onConfirm(
                            Category(
                                id = category?.id ?: 0,
                                name = name.trim(),
                                icon = icon.trim(),
                                type = type,
                                color = color
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && icon.isNotBlank()
            ) { Text(if (isEdit) "保存" else "添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun CategoryManagementPreview() {
    InkQilinLedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val cats = listOf(
                Category(1, "餐饮", "🍜", TransactionType.EXPENSE, "#F44336"),
                Category(2, "交通", "🚌", TransactionType.EXPENSE, "#FF9800"),
                Category(3, "购物", "🛒", TransactionType.EXPENSE, "#E91E63"),
                Category(4, "工资", "💰", TransactionType.INCOME, "#4CAF50"),
                Category(5, "奖金", "🎁", TransactionType.INCOME, "#00BCD4")
            )
            val expenseCats = cats.filter { it.type == TransactionType.EXPENSE }
            val incomeCats = cats.filter { it.type == TransactionType.INCOME }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("支出类别", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(expenseCats) { category -> CategoryPreviewItem(category) }
                item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                item { Text("收入类别", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(incomeCats) { category -> CategoryPreviewItem(category) }
            }
        }
    }
}

@Composable
private fun CategoryPreviewItem(category: Category) {
    val categoryColor = try { Color(android.graphics.Color.parseColor(category.color)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(categoryColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text(category.icon, fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenQingTagEditDialog(
    tag: RenQingTag? = null,
    onDismiss: () -> Unit,
    onConfirm: (RenQingTag) -> Unit
) {
    val isEdit = tag != null
    var name by remember { mutableStateOf(tag?.name ?: "") }
    var icon by remember { mutableStateOf(tag?.icon ?: "\uD83C\uDF81") }
    var color by remember { mutableStateOf(tag?.color ?: "#715CFF") }
    val emojiList = listOf(
        "\uD83D\uDC92", "\uD83D\uDE4F", "\uD83C\uDF82", "\uD83C\uDFE0", "\uD83C\uDF93",
        "\uD83D\uDC76", "\uD83C\uDF81", "\uD83C\uDF89", "\u2764\uFE0F", "\uD83D\uDC8D",
        "\uD83D\uDC57", "\uD83D\uDEAA", "\uD83C\uDF7D\uFE0F", "\uD83D\uDCDA", "\uD83D\uDCE6",
        "\u2B50", "\uD83C\uDF1F", "\uD83C\uDFC6", "\uD83D\uDC8E", "\uD83D\uDCB0",
        "\uD83C\uDFA8", "\uD83C\uDFB5", "\uD83C\uDFA4", "\uD83C\uDFA7", "\uD83C\uDFB6",
        "\uD83D\uDE97", "\u2708\uFE0F", "\uD83D\uDED2", "\uD83D\uDCF7", "\u26BD",
        "\uD83C\uDFC0", "\uD83C\uDFBF", "\uD83E\uDD3E", "\uD83C\uDFAB", "\uD83C\uDF88",
        "\uD83C\uDF70", "\uD83C\uDF7A", "\u2615", "\uD83C\uDF54", "\uD83C\uDF55",
        "\uD83C\uDF63", "\uD83C\uDF66"
    )
    val colorOptions = listOf("#715CFF", "#E91E63", "#F44336", "#FF9800", "#FFC107", "#4CAF50", "#00BCD4", "#2196F3", "#9C27B0", "#607D8B", "#795548", "#FF5722")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑标签" else "添加标签") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("标签名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("图标", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("自定义emoji") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojiList) { emoji ->
                        FilterChip(
                            selected = icon == emoji,
                            onClick = { icon = emoji },
                            label = { Text(emoji, fontSize = 20.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("颜色", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { c ->
                        val cColor = try { Color(android.graphics.Color.parseColor(c)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(cColor))
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            RenQingTag(
                                id = tag?.id ?: 0,
                                name = name.trim(),
                                icon = icon.trim(),
                                color = color
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text(if (isEdit) "保存" else "添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
