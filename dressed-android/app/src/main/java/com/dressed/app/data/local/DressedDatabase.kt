package com.dressed.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WardrobeItemEntity::class, OutfitEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DressedDatabase : RoomDatabase() {
    abstract fun wardrobeDao(): WardrobeDao
    abstract fun outfitDao(): OutfitDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wardrobe_items ADD COLUMN sizeLabel TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS outfits (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        itemIds TEXT NOT NULL,
                        wornCount INTEGER NOT NULL DEFAULT 0,
                        createdAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
