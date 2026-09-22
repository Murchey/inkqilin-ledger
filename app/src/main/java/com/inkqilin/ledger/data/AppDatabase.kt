package com.inkqilin.ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Category::class, RenQingContact::class, RenQingEvent::class, RenQingTag::class, CurrencyAsset::class, AlbumPhoto::class, KeywordCategory::class, UserAsset::class, AssetFlow::class, CycleBill::class, RecycledCycleBill::class, NotificationLog::class],
    version = 17,
    exportSchema = false
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN color TEXT NOT NULL DEFAULT '#715CFF'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `renqing_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `relationship` TEXT NOT NULL DEFAULT 'RELATIVE',
                        `phone` TEXT NOT NULL DEFAULT '',
                        `birthday` INTEGER,
                        `note` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `renqing_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contactId` INTEGER NOT NULL DEFAULT 0,
                        `contactName` TEXT NOT NULL DEFAULT '',
                        `eventType` TEXT NOT NULL DEFAULT 'OTHER',
                        `direction` TEXT NOT NULL DEFAULT 'GIVEN',
                        `amount` REAL NOT NULL DEFAULT 0,
                        `giftDescription` TEXT NOT NULL DEFAULT '',
                        `date` INTEGER NOT NULL DEFAULT 0,
                        `location` TEXT NOT NULL DEFAULT '',
                        `note` TEXT NOT NULL DEFAULT '',
                        `photoUri` TEXT
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `renqing_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL DEFAULT '🎁',
                        `color` TEXT NOT NULL DEFAULT '#715CFF'
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE renqing_events ADD COLUMN `tagId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE renqing_events ADD COLUMN `tagName` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'CNY'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `currency_assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `code` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `cardColor` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO `currency_assets` (`code`, `symbol`, `name`, `cardColor`, `isDefault`) VALUES ('CNY', '¥', '人民币', '#43A047', 1)")
                db.execSQL("INSERT INTO `currency_assets` (`code`, `symbol`, `name`, `cardColor`, `isDefault`) VALUES ('USD', '${"$"}', '美元', '#1565C0', 0)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE currency_assets ADD COLUMN `cardColorLight` TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `album_photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `keyword_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `keyword` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `currentValue` REAL NOT NULL DEFAULT 0,
                        `purchasePrice` REAL NOT NULL DEFAULT 0,
                        `note` TEXT NOT NULL DEFAULT '',
                        `purchaseDate` INTEGER NOT NULL DEFAULT 0,
                        `lastUpdated` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建 asset_flows 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `asset_flows` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `assetName` TEXT NOT NULL,
                        `flowType` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `newValue` REAL NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `date` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 2. 重建 user_assets 表：移除 purchasePrice/purchaseDate，添加 createdAt
                // 旧枚举: REAL_ESTATE=0, STOCK=1, FUND=2, BOND=3, DEPOSIT=4, INSURANCE=5, CRYPTO=6, OTHER=7
                // 新枚举: REAL_ESTATE=0, VEHICLE=1, DEPOSIT=2, INSURANCE=3, JEWELRY=4, COLLECTION=5, DIGITAL=6, OTHER=7
                db.execSQL("""
                    CREATE TABLE `user_assets_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `currentValue` REAL NOT NULL DEFAULT 0,
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `lastUpdated` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `user_assets_new` (`id`, `name`, `type`, `currentValue`, `note`, `createdAt`, `lastUpdated`)
                    SELECT `id`, `name`,
                        CASE CAST(`type` AS INTEGER)
                            WHEN 0 THEN 0
                            WHEN 4 THEN 2
                            WHEN 5 THEN 3
                            WHEN 7 THEN 7
                            ELSE 7
                        END,
                        `currentValue`, `note`, `purchaseDate`, `lastUpdated`
                    FROM `user_assets`
                """.trimIndent())
                db.execSQL("DROP TABLE `user_assets`")
                db.execSQL("ALTER TABLE `user_assets_new` RENAME TO `user_assets`")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 资产类型枚举变更：
                // 旧: REAL_ESTATE, VEHICLE, DEPOSIT, INSURANCE, JEWELRY, COLLECTION, DIGITAL, OTHER
                // 新: REAL_ESTATE, VEHICLE, STOCK, FUND, INSURANCE, DEPOSIT, DIGITAL, OTHER
                // Room 对 TEXT 列使用枚举名存储，必须输出枚举名而非数字序号
                // 同时兼容旧值可能已是数字字符串的情况
                db.execSQL("""
                    UPDATE `user_assets` SET `type` = CASE
                        WHEN `type` = 'REAL_ESTATE' THEN 'REAL_ESTATE'
                        WHEN `type` = 'VEHICLE' THEN 'VEHICLE'
                        WHEN `type` = 'DEPOSIT' THEN 'DEPOSIT'
                        WHEN `type` = 'INSURANCE' THEN 'INSURANCE'
                        WHEN `type` = 'JEWELRY' THEN 'OTHER'
                        WHEN `type` = 'COLLECTION' THEN 'OTHER'
                        WHEN `type` = 'DIGITAL' THEN 'DIGITAL'
                        WHEN `type` = 'OTHER' THEN 'OTHER'
                        WHEN `type` = '0' THEN 'REAL_ESTATE'
                        WHEN `type` = '1' THEN 'VEHICLE'
                        WHEN `type` = '2' THEN 'DEPOSIT'
                        WHEN `type` = '3' THEN 'INSURANCE'
                        WHEN `type` = '4' THEN 'OTHER'
                        WHEN `type` = '5' THEN 'OTHER'
                        WHEN `type` = '6' THEN 'DIGITAL'
                        WHEN `type` = '7' THEN 'OTHER'
                        ELSE 'OTHER'
                    END
                """.trimIndent())
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 修复 v12 中可能因错误迁移产生的数字字符串枚举值
                // 如果值已经是合法枚举名（如 'REAL_ESTATE'），CASE 不会命中并保持原值
                db.execSQL("""
                    UPDATE `user_assets` SET `type` = CASE
                        WHEN `type` = '0' THEN 'REAL_ESTATE'
                        WHEN `type` = '1' THEN 'VEHICLE'
                        WHEN `type` = '2' THEN 'STOCK'
                        WHEN `type` = '3' THEN 'FUND'
                        WHEN `type` = '4' THEN 'INSURANCE'
                        WHEN `type` = '5' THEN 'DEPOSIT'
                        WHEN `type` = '6' THEN 'DIGITAL'
                        WHEN `type` = '7' THEN 'OTHER'
                        ELSE `type`
                    END
                """.trimIndent())
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN uuid TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE asset_flows ADD COLUMN uuid TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add cycleBillId to transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN cycleBillId INTEGER DEFAULT NULL")

                // 2. Create cycle_bills table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cycle_bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `currency` TEXT NOT NULL DEFAULT 'CNY',
                        `cycleType` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `reminderEnabled` INTEGER NOT NULL DEFAULT 1,
                        `advanceMinutes` INTEGER NOT NULL DEFAULT 60,
                        `generationMode` TEXT NOT NULL DEFAULT 'AUTO_BEFORE',
                        `note` TEXT NOT NULL DEFAULT '',
                        `colorHex` INTEGER DEFAULT NULL,
                        `currentCycleStart` INTEGER NOT NULL DEFAULT 0,
                        `currentCycleEnd` INTEGER NOT NULL DEFAULT 0,
                        `lastGeneratedDate` INTEGER DEFAULT NULL,
                        `nextTriggerDate` INTEGER NOT NULL DEFAULT 0,
                        `overdue` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 3. Create recycled_cycle_bills table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recycled_cycle_bills` (
                        `originalId` INTEGER NOT NULL,
                        `recycleTime` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `currency` TEXT NOT NULL DEFAULT 'CNY',
                        `cycleType` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `reminderEnabled` INTEGER NOT NULL DEFAULT 1,
                        `advanceMinutes` INTEGER NOT NULL DEFAULT 60,
                        `generationMode` TEXT NOT NULL DEFAULT 'AUTO_BEFORE',
                        `note` TEXT NOT NULL DEFAULT '',
                        `colorHex` INTEGER DEFAULT NULL,
                        PRIMARY KEY(`originalId`)
                    )
                """.trimIndent())

                // 4. Create notification_log table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notification_log` (
                        `cycleBillId` INTEGER NOT NULL,
                        `logDate` TEXT NOT NULL,
                        PRIMARY KEY(`cycleBillId`, `logDate`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_log_cycleBillId_logDate` ON `notification_log` (`cycleBillId`, `logDate`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
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
