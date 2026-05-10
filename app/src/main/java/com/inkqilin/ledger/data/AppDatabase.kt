package com.inkqilin.ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Category::class, RenQingContact::class, RenQingEvent::class, RenQingTag::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun renQingContactDao(): RenQingContactDao
    abstract fun renQingEventDao(): RenQingEventDao
    abstract fun renQingTagDao(): RenQingTagDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
