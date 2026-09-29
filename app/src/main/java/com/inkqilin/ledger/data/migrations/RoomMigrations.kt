package com.inkqilin.ledger.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Room 迁移集中定义。结构变更必须新增 Migration，禁止 destructive 升级。 */
object RoomMigrations {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN color TEXT NOT NULL DEFAULT '#715CFF'")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `renqing_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `relationship` TEXT NOT NULL DEFAULT 'RELATIVE',
                        `phone` TEXT NOT NULL DEFAULT '',
                        `birthday` INTEGER,
                        `note` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent()
            )
            db.execSQL(
                """
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
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `renqing_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL DEFAULT '🎁',
                        `color` TEXT NOT NULL DEFAULT '#715CFF'
                    )
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE renqing_events ADD COLUMN `tagId` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE renqing_events ADD COLUMN `tagName` TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'CNY'")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `currency_assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `code` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `cardColor` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
            )
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
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `album_photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `keyword_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `keyword` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL
                    )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
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
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
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
                """.trimIndent()
            )
            db.execSQL(
                """
                    CREATE TABLE `user_assets_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `currentValue` REAL NOT NULL DEFAULT 0,
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `lastUpdated` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
            )
            db.execSQL(
                """
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
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `user_assets`")
            db.execSQL("ALTER TABLE `user_assets_new` RENAME TO `user_assets`")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
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
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
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
                """.trimIndent()
            )
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
            db.execSQL("ALTER TABLE transactions ADD COLUMN cycleBillId INTEGER DEFAULT NULL")
            db.execSQL(
                """
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
                """.trimIndent()
            )
            db.execSQL(
                """
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
                """.trimIndent()
            )
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `notification_log` (
                        `cycleBillId` INTEGER NOT NULL,
                        `logDate` TEXT NOT NULL,
                        PRIMARY KEY(`cycleBillId`, `logDate`)
                    )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_log_cycleBillId_logDate` ON `notification_log` (`cycleBillId`, `logDate`)")
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE user_assets ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'CNY'")
            db.execSQL("ALTER TABLE asset_flows ADD COLUMN `currency` TEXT NOT NULL DEFAULT 'CNY'")
            db.execSQL(
                """
                    UPDATE asset_flows SET currency = COALESCE(
                        (SELECT user_assets.currency FROM user_assets WHERE user_assets.id = asset_flows.assetId),
                        'CNY'
                    )
                """.trimIndent()
            )
        }
    }

    /** 仅建索引，不改表结构、不删数据。索引名与 Entity `indices` 声明保持一致。 */
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_currency` ON `transactions` (`currency`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_uuid` ON `transactions` (`uuid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_cycleBillId` ON `transactions` (`cycleBillId`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_type` ON `categories` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_sortOrder` ON `categories` (`sortOrder`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_assets_type` ON `user_assets` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_flows_assetId` ON `asset_flows` (`assetId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_flows_uuid` ON `asset_flows` (`uuid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_flows_date` ON `asset_flows` (`date`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cycle_bills_enabled` ON `cycle_bills` (`enabled`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_cycle_bills_nextTriggerDate` ON `cycle_bills` (`nextTriggerDate`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_keyword_categories_keyword` ON `keyword_categories` (`keyword`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_renqing_events_contactId` ON `renqing_events` (`contactId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_renqing_events_date` ON `renqing_events` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_photos_createdAt` ON `album_photos` (`createdAt`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
    )
}
