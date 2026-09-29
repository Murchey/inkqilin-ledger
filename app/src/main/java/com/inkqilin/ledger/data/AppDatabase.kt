package com.inkqilin.ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inkqilin.ledger.data.migrations.RoomMigrations

@Database(
    entities = [
        Transaction::class,
        Category::class,
        RenQingContact::class,
        RenQingEvent::class,
        RenQingTag::class,
        CurrencyAsset::class,
        AlbumPhoto::class,
        KeywordCategory::class,
        UserAsset::class,
        AssetFlow::class,
        CycleBill::class,
        RecycledCycleBill::class,
        NotificationLog::class,
    ],
    version = 19,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun renQingContactDao(): RenQingContactDao
    abstract fun renQingEventDao(): RenQingEventDao
    abstract fun renQingTagDao(): RenQingTagDao
    abstract fun currencyAssetDao(): CurrencyAssetDao
    abstract fun albumPhotoDao(): AlbumPhotoDao
    abstract fun keywordCategoryDao(): KeywordCategoryDao
    abstract fun userAssetDao(): UserAssetDao
    abstract fun assetFlowDao(): AssetFlowDao
    abstract fun cycleBillDao(): CycleBillDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_database"
                )
                    .addMigrations(*RoomMigrations.ALL)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    // DELETE 时用零覆盖行内容，降低从 db 文件残留页恢复账单的可能
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            runCatching { db.execSQL("PRAGMA secure_delete = ON") }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /** 云恢复前关闭并丢弃单例，避免覆盖文件后仍使用旧连接 */
        fun closeAndClear() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {
                }
                INSTANCE = null
            }
        }
    }
}
