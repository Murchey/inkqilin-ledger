package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.CategoryTotal
import com.inkqilin.ledger.data.CategoryUsage
import com.inkqilin.ledger.data.SearchSummary
import com.inkqilin.ledger.data.Transaction
import com.inkqilin.ledger.data.TransactionDao
import kotlinx.coroutines.flow.Flow

/** 账单读写入口。UI/ViewModel 不直接触达 Dao。 */
class TransactionRepository(private val dao: TransactionDao) {
    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        dao.getTransactionsByCategory(category)

    fun searchTransactions(query: String): Flow<List<Transaction>> = dao.searchTransactions(query)
    suspend fun insertTransaction(transaction: Transaction) = dao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = dao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) = dao.deleteTransaction(transaction)
    fun getTotalIncome(): Flow<Double?> = dao.getTotalIncome()
    fun getTotalExpense(): Flow<Double?> = dao.getTotalExpense()
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        dao.getTransactionsByDateRange(startTime, endTime)

    fun getTotalIncomeByCurrency(currency: String): Flow<Double?> =
        dao.getTotalIncomeByCurrency(currency)

    fun getTotalExpenseByCurrency(currency: String): Flow<Double?> =
        dao.getTotalExpenseByCurrency(currency)

    fun getTransactionsByCurrency(currency: String): Flow<List<Transaction>> =
        dao.getTransactionsByCurrency(currency)

    suspend fun countByUuid(uuid: String): Int = dao.countByUuid(uuid)
    suspend fun insertTransactionIgnore(transaction: Transaction): Long =
        dao.insertTransactionIgnore(transaction)

    suspend fun getTransactionsWithoutUuid(): List<Transaction> = dao.getTransactionsWithoutUuid()
    suspend fun updateTransactions(transactions: List<Transaction>) = dao.updateTransactions(transactions)
    suspend fun getIncomeSumByRangeSync(start: Long, end: Long): Double =
        dao.getIncomeSumByRangeSync(start, end)

    suspend fun getExpenseSumByRangeSync(start: Long, end: Long): Double =
        dao.getExpenseSumByRangeSync(start, end)

    suspend fun getTopExpenseCategoriesSync(start: Long, end: Long, limit: Int): List<CategoryTotal> =
        dao.getTopExpenseCategoriesSync(start, end, limit)

    suspend fun getRecentExpenseCategoriesSync(since: Long, limit: Int): List<CategoryUsage> =
        dao.getRecentExpenseCategoriesSync(since, limit)

    fun searchSummary(query: String): Flow<SearchSummary> = dao.searchSummary(query)
    fun searchTransactionsFiltered(query: String, start: Long, end: Long): Flow<List<Transaction>> =
        dao.searchTransactionsFiltered(query, start, end)

    fun searchSummaryFiltered(query: String, start: Long, end: Long): Flow<SearchSummary> =
        dao.searchSummaryFiltered(query, start, end)
}
