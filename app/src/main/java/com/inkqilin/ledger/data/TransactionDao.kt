package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND currency = :currency")
    fun getTotalIncomeByCurrency(currency: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND currency = :currency")
    fun getTotalExpenseByCurrency(currency: String): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE currency = :currency ORDER BY date DESC")
    fun getTransactionsByCurrency(currency: String): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE uuid = :uuid AND uuid IS NOT NULL")
    suspend fun countByUuid(uuid: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionIgnore(transaction: Transaction): Long

    @Query("SELECT * FROM transactions WHERE uuid IS NULL")
    suspend fun getTransactionsWithoutUuid(): List<Transaction>

    @Update
    suspend fun updateTransactions(transactions: List<Transaction>)

    // ── AppWidget 同步聚合查询（供桌面小组件 goAsync 渲染使用）──
    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type = 'INCOME' AND date BETWEEN :start AND :end")
    suspend fun getIncomeSumByRangeSync(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end")
    suspend fun getExpenseSumByRangeSync(start: Long, end: Long): Double

    @Query("SELECT category, COALESCE(SUM(amount),0) AS total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end GROUP BY category ORDER BY total DESC LIMIT :limit")
    suspend fun getTopExpenseCategoriesSync(start: Long, end: Long, limit: Int): List<CategoryTotal>

    // ── AppWidget 用：最近使用的支出分类（按最近一次记账时间排）──
    @Query("""SELECT category, MAX(date) AS last_used, COUNT(*) AS use_count FROM transactions
               WHERE type = 'EXPENSE' AND date >= :since
               GROUP BY category ORDER BY last_used DESC, use_count DESC LIMIT :limit""")
    suspend fun getRecentExpenseCategoriesSync(since: Long, limit: Int): List<CategoryUsage>

    // ── 搜索聚合：一次 SQL 返回支出/收入合计（供搜索结果头部金额展示）──
    @Query("""SELECT
                COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount END), 0) AS expenseTotal,
                COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS incomeTotal
              FROM transactions
              WHERE note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'""")
    fun searchSummary(query: String): Flow<SearchSummary>

    /** 关键词 + 日期范围搜索（query 为空表示只按日期筛选） */
    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :start AND :end
          AND (:query = '' OR note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY date DESC
    """)
    fun searchTransactionsFiltered(query: String, start: Long, end: Long): Flow<List<Transaction>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount END), 0) AS expenseTotal,
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS incomeTotal
        FROM transactions
        WHERE date BETWEEN :start AND :end
          AND (:query = '' OR note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
    """)
    fun searchSummaryFiltered(query: String, start: Long, end: Long): Flow<SearchSummary>
}

data class SearchSummary(
    val expenseTotal: Double,
    val incomeTotal: Double
)

data class CategoryUsage(
    val category: String,
    val last_used: Long,
    val use_count: Int
)

data class CategoryTotal(
    val category: String,
    val total: Double
)
