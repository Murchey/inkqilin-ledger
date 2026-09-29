package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.CurrencyAsset
import com.inkqilin.ledger.data.CurrencyAssetDao
import kotlinx.coroutines.flow.Flow

class CurrencyAssetRepository(private val dao: CurrencyAssetDao) {
    fun getAllAssets(): Flow<List<CurrencyAsset>> = dao.getAllAssets()
    suspend fun getAssetById(id: Long): CurrencyAsset? = dao.getAssetById(id)
    suspend fun insertAsset(asset: CurrencyAsset) = dao.insertAsset(asset)
    suspend fun updateAsset(asset: CurrencyAsset) = dao.updateAsset(asset)
    suspend fun deleteAsset(asset: CurrencyAsset) = dao.deleteAsset(asset)
    suspend fun getCount(): Int = dao.getCount()
}
