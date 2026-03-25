package com.dressed.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WardrobeItemEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DressedDatabase : RoomDatabase() {
    abstract fun wardrobeDao(): WardrobeDao
}
