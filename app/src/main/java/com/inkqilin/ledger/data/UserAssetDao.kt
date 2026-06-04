package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAssetDao {
    @Query("SELECT * FROM user_assets ORDER BY type ASC, id ASC")
    fun getAllAssets(): Flow<List<UserAsset>>

    @Query("SELECT * FROM user_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): UserAsset?

    @Query("SELECT * FROM user_assets WHERE type = :type ORDER BY id ASC")
    fun getAssetsByType(type: UserAssetType): Flow<List<UserAsset>>

    @Query("SELECT SUM(currentValue) FROM user_assets")
    fun getTotalValue(): Flow<Double?>

    @Query("SELECT SUM(currentValue) FROM user_assets WHERE type = :type")
    fun getTotalValueByType(type: UserAssetType): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: UserAsset)

    @Update
    suspend fun updateAsset(asset: UserAsset)

    @Delete
    suspend fun deleteAsset(asset: UserAsset)

    @Query("SELECT COUNT(*) FROM user_assets")
    suspend fun getCount(): Int
}
