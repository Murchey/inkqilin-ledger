package com.inkqilin.ledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.TransactionType
import com.inkqilin.ledger.ui.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(viewModel: TransactionViewModel) {
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("支出分类", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("收入分类", modifier = Modifier.padding(16.dp))
            }
        }

        val filteredCategories = categories.filter { 
            if (selectedTab == 0) it.type == TransactionType.EXPENSE 
            else it.type == TransactionType.INCOME 
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCategories) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { categoryToEdit = it },
                    onDelete = { viewModel.deleteCategory(it) }
                )
            }
            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("添加分类")
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryEditDialog(
            type = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, icon, color ->
                viewModel.addCategory(name, icon, if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME, color)
                showAddDialog = false
            }
        )
    }

    categoryToEdit?.let { category ->
        CategoryEditDialog(
            category = category,
            type = category.type,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, icon, color ->
                viewModel.updateCategory(category.copy(name = name, icon = icon, color = color))
                categoryToEdit = null
            }
        )
    }
}

@Composable
fun CategoryItem(category: Category, onEdit: (Category) -> Unit, onDelete: (Category) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.width(16.dp))
            Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onEdit(category) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onDelete(category) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
