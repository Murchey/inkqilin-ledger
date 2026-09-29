package com.inkqilin.ledger.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import com.inkqilin.ledger.data.Category
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.repository.CategoryRepository
import com.inkqilin.ledger.data.repository.TransactionRepository
import com.inkqilin.ledger.util.ExcelImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Excel 账单导入：进度流 + 写库。 */
class ExcelImportController(
    private val scope: CoroutineScope,
    private val categoryDao: CategoryRepository,
    private val transactionDao: TransactionRepository,
    private val onImported: () -> Unit,
) {
    private val _excelProgress = MutableStateFlow<Pair<Float, String>?>(null)
    val excelProgress: StateFlow<Pair<Float, String>?> = _excelProgress.asStateFlow()

    fun ImportTransactions(
        context: Context,
        uri: Uri,
        existingCategories: suspend () -> List<Category>,
        onDone: ((imported: Int, newCats: Int) -> Unit)? = null,
    ) {
        scope.launch {
            _excelProgress.value = 0f to "开始导入…"
            try {
                val categories = existingCategories()
                val result = withContext(Dispatchers.IO) {
                    ExcelImporter.importTransactionsFromUri(context, uri, categories) { fraction, msg ->
                        _excelProgress.value = fraction.coerceIn(0f, 1f) to msg
                    }
                }
                _excelProgress.value = 0.96f to "写入数据库…"
                result.newCategories.forEach { category ->
                    categoryDao.insertCategory(category)
                }
                result.transactions.forEach { transaction: Transaction ->
                    transactionDao.insertTransaction(transaction)
                }
                onImported()
                _excelProgress.value = 1f to "导入完成"
                onDone?.invoke(result.transactions.size, result.newCategories.size)
                kotlinx.coroutines.delay(400)
            } catch (e: Exception) {
                Log.e("TransactionVM", "import failed", e)
                onDone?.invoke(0, 0)
            } finally {
                _excelProgress.value = null
            }
        }
    }
}
