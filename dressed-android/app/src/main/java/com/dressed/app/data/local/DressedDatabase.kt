package com.dressed.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WardrobeItemEntity::class,
        OutfitEntity::class,
        BorrowedLibraryEntity::class,
        BorrowedItemEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DressedDatabase : RoomDatabase() {
    abstract fun wardrobeDao(): WardrobeDao
    abstract fun outfitDao(): OutfitDao
    abstract fun borrowedLibraryDao(): BorrowedLibraryDao
    abstract fun borrowedItemDao(): BorrowedItemDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wardrobe_items ADD COLUMN lastWornAtEpochMs INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wardrobe_items ADD COLUMN occasions TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wardrobe_items ADD COLUMN lendable INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS borrowed_libraries (
                        id TEXT NOT NULL PRIMARY KEY,
                        sharerName TEXT NOT NULL,
                        importedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS borrowed_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        libraryId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        sizeLabel TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        colorName TEXT NOT NULL,
                        seasons TEXT NOT NULL,
                        occasions TEXT NOT NULL DEFAULT '',
                        photoPath TEXT,
                        wornCount INTEGER NOT NULL,
                        lastWornAtEpochMs INTEGER,
                        addedAtEpochMs INTEGER NOT NULL,
                        FOREIGN KEY(libraryId) REFERENCES borrowed_libraries(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_borrowed_items_libraryId ON borrowed_items(libraryId)")
            }
        }
    }
}
