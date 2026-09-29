package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.AssetFlowDao
import com.inkqilin.ledger.data.UserAsset
import com.inkqilin.ledger.data.UserAssetDao
import com.inkqilin.ledger.data.UserAssetType
import kotlinx.coroutines.flow.Flow

/** 资产与流转：流转写入常伴随资产现值更新，收敛在同一仓库。 */
class AssetRepository(
    private val assetDao: UserAssetDao,
    private val flowDao: AssetFlowDao,
) {
    fun getAllAssets(): Flow<List<UserAsset>> = assetDao.getAllAssets()
    suspend fun getAssetById(id: Long): UserAsset? = assetDao.getAssetById(id)
    fun getAssetsByType(type: UserAssetType): Flow<List<UserAsset>> = assetDao.getAssetsByType(type)
    fun getTotalValue(): Flow<Double?> = assetDao.getTotalValue()
    fun getTotalValueByType(type: UserAssetType): Flow<Double?> = assetDao.getTotalValueByType(type)
    suspend fun insertAsset(asset: UserAsset) = assetDao.insertAsset(asset)
    suspend fun updateAsset(asset: UserAsset) = assetDao.updateAsset(asset)
    suspend fun deleteAsset(asset: UserAsset) = assetDao.deleteAsset(asset)
    suspend fun getAssetCount(): Int = assetDao.getCount()

    fun getFlowsByAssetId(assetId: Long): Flow<List<AssetFlow>> = flowDao.getFlowsByAssetId(assetId)
    fun getAllFlows(): Flow<List<AssetFlow>> = flowDao.getAllFlows()
    suspend fun insertFlow(flow: AssetFlow) = flowDao.insertFlow(flow)
    suspend fun updateFlow(flow: AssetFlow) = flowDao.updateFlow(flow)
    suspend fun deleteFlow(flow: AssetFlow) = flowDao.deleteFlow(flow)
    suspend fun getLatestFlowByAssetId(assetId: Long): AssetFlow? = flowDao.getLatestFlowByAssetId(assetId)
    suspend fun countByUuid(uuid: String): Int = flowDao.countByUuid(uuid)
    suspend fun insertFlowIgnore(flow: AssetFlow): Long = flowDao.insertFlowIgnore(flow)
    suspend fun getFlowsWithoutUuid(): List<AssetFlow> = flowDao.getFlowsWithoutUuid()
    suspend fun updateFlows(flows: List<AssetFlow>) = flowDao.updateFlows(flows)
}
