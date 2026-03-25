package com.dressed.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WardrobeItemEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DressedDatabase : RoomDatabase() {
    abstract fun wardrobeDao(): WardrobeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wardrobe_items ADD COLUMN sizeLabel TEXT NOT NULL DEFAULT ''",
                )
            }
        }
    }
}
