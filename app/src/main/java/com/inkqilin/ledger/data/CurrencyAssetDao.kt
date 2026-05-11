package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyAssetDao {
    @Query("SELECT * FROM currency_assets ORDER BY isDefault DESC, id ASC")
    fun getAllAssets(): Flow<List<CurrencyAsset>>

    @Query("SELECT * FROM currency_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): CurrencyAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: CurrencyAsset)

    @Update
    suspend fun updateAsset(asset: CurrencyAsset)

    @Delete
    suspend fun deleteAsset(asset: CurrencyAsset)

    @Query("SELECT COUNT(*) FROM currency_assets")
    suspend fun getCount(): Int
}
