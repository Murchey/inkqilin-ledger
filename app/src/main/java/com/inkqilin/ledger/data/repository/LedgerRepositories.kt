package com.inkqilin.ledger.data.repository

import android.content.Context
import com.inkqilin.ledger.data.AppDatabase

/**
 * 应用内统一仓库入口。
 * ViewModel / Service / Widget / Worker 均经此取仓库，避免散落直连 Dao。
 */
class LedgerRepositories private constructor(db: AppDatabase) {
    val transactions = TransactionRepository(db.transactionDao())
    val categories = CategoryRepository(db.categoryDao())
    val currencyAssets = CurrencyAssetRepository(db.currencyAssetDao())
    val assets = AssetRepository(db.userAssetDao(), db.assetFlowDao())
    val album = AlbumRepository(db.albumPhotoDao())
    val keywordCategories = KeywordCategoryRepository(db.keywordCategoryDao())
    val cycleBills = CycleBillRepository(db.cycleBillDao(), db.notificationLogDao())
    val renQing = RenQingRepository(db.renQingContactDao(), db.renQingEventDao(), db.renQingTagDao())

    companion object {
        @Volatile
        private var INSTANCE: LedgerRepositories? = null

        fun get(context: Context): LedgerRepositories {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LedgerRepositories(AppDatabase.getDatabase(context.applicationContext)).also {
                    INSTANCE = it
                }
            }
        }

        /** 云恢复替换库文件后丢弃仓库缓存，下次 get 重建。 */
        fun clear() {
            synchronized(this) { INSTANCE = null }
        }
    }
}
