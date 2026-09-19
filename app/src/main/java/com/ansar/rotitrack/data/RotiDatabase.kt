package com.ansar.rotitrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(
    entities = [OrderEntry::class, Rate::class, Payment::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RotiDatabase : RoomDatabase() {

    abstract fun rotiDao(): RotiDao

    companion object {
        private const val DEFAULT_RATE_PAISE = 1000L // Rs 10.00, editable from the Edit Rates screen
        private const val NOON_SECONDS = 43_200

        @Volatile
        private var instance: RotiDatabase? = null

        fun get(context: Context): RotiDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        /**
         * Version 1 stored a single total per day. Version 2 stores one row per order placed, so
         * lunch and dinner stay apart. Each old daily total becomes a single entry timed at noon.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_entry` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`item` TEXT NOT NULL, " +
                        "`count` INTEGER NOT NULL, " +
                        "`recordedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_entry_date` ON `order_entry` (`date`)")
                db.execSQL(
                    "INSERT INTO `order_entry` (`date`, `item`, `count`, `recordedAt`) " +
                        "SELECT `date`, `item`, `count`, $NOON_SECONDS FROM `daily_order`"
                )
                db.execSQL("DROP TABLE `daily_order`")
            }
        }

        private fun build(context: Context): RotiDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                RotiDatabase::class.java,
                "roti_track.db"
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Start with a placeholder rate so the first order has a value.
                        val today = LocalDate.now().toEpochDay()
                        ItemType.entries.forEach { item ->
                            db.execSQL(
                                "INSERT INTO rate (item, pricePaise, effectiveFrom) VALUES (?, ?, ?)",
                                arrayOf<Any>(item.name, DEFAULT_RATE_PAISE, today)
                            )
                        }
                    }
                })
                .build()
    }
}
