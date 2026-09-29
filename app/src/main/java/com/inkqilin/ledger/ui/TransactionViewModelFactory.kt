package com.inkqilin.ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.inkqilin.ledger.data.repository.*
import com.inkqilin.ledger.util.ThemeManager

class TransactionViewModelFactory(
    private val transactionDao: TransactionRepository,
    private val categoryDao: CategoryRepository,
    private val currencyAssetDao: CurrencyAssetRepository,
    private val albumPhotoDao: AlbumRepository,
    private val keywordCategoryDao: KeywordCategoryRepository,
    private val assets: AssetRepository,
    private val themeManager: ThemeManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(transactionDao, categoryDao, currencyAssetDao, albumPhotoDao, keywordCategoryDao, assets, themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
