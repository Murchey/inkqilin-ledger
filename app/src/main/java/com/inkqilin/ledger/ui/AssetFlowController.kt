package com.inkqilin.ledger.ui

import com.inkqilin.ledger.data.AssetFlow
import com.inkqilin.ledger.data.repository.AssetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** 资产流转写库并同步资产现值。 */
class AssetFlowController(
    private val assets: AssetRepository,
    private val scope: CoroutineScope,
) {
    val allAssetFlows: StateFlow<List<AssetFlow>> = assets.getAllFlows()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun GetFlows(assetId: Long): Flow<List<AssetFlow>> = assets.getFlowsByAssetId(assetId)

    fun Add(flow: AssetFlow) {
        scope.launch {
            val f = if (flow.uuid == null) flow.copy(uuid = UUID.randomUUID().toString()) else flow
            assets.insertFlow(f)
            syncAssetValue(f)
        }
    }

    suspend fun ImportSkipDuplicates(flow: AssetFlow): Boolean {
        if (flow.uuid != null) {
            val count = assets.countByUuid(flow.uuid!!)
            if (count == 0) {
                assets.insertFlowIgnore(flow)
                syncAssetValue(flow)
                return true
            }
            return false
        } else {
            val f = flow.copy(uuid = UUID.randomUUID().toString())
            assets.insertFlow(f)
            syncAssetValue(f)
            return true
        }
    }

    fun BackfillUuids() {
        scope.launch(Dispatchers.IO) {
            val nulls = assets.getFlowsWithoutUuid()
            if (nulls.isNotEmpty()) {
                assets.updateFlows(nulls.map { it.copy(uuid = UUID.randomUUID().toString()) })
            }
        }
    }

    fun Update(flow: AssetFlow) {
        scope.launch {
            assets.updateFlow(flow)
            syncAssetValue(flow)
        }
    }

    fun Delete(flow: AssetFlow) {
        scope.launch {
            assets.deleteFlow(flow)
            val latestFlow = assets.getLatestFlowByAssetId(flow.assetId)
            assets.getAssetById(flow.assetId)?.let { asset ->
                assets.updateAsset(
                    asset.copy(
                        currentValue = latestFlow?.newValue ?: asset.currentValue,
                        lastUpdated = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    private suspend fun syncAssetValue(flow: AssetFlow) {
        assets.getAssetById(flow.assetId)?.let { asset ->
            assets.updateAsset(
                asset.copy(
                    currentValue = flow.newValue,
                    lastUpdated = System.currentTimeMillis(),
                )
            )
        }
    }
}
