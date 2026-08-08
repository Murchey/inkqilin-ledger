package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetFlowDao {
    @Query("SELECT * FROM asset_flows WHERE assetId = :assetId ORDER BY date DESC")
    fun getFlowsByAssetId(assetId: Long): Flow<List<AssetFlow>>

    @Query("SELECT * FROM asset_flows ORDER BY date DESC")
    fun getAllFlows(): Flow<List<AssetFlow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlow(flow: AssetFlow)

    @Update
    suspend fun updateFlow(flow: AssetFlow)

    @Delete
    suspend fun deleteFlow(flow: AssetFlow)

    @Query("SELECT * FROM asset_flows WHERE assetId = :assetId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestFlowByAssetId(assetId: Long): AssetFlow?

    @Query("SELECT COUNT(*) FROM asset_flows WHERE uuid = :uuid AND uuid IS NOT NULL")
    suspend fun countByUuid(uuid: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFlowIgnore(flow: AssetFlow): Long

    @Query("SELECT * FROM asset_flows WHERE uuid IS NULL")
    suspend fun getFlowsWithoutUuid(): List<AssetFlow>

    @Update
    suspend fun updateFlows(flows: List<AssetFlow>)
}
