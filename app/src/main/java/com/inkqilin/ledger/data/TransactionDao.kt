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
}
