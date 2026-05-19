package com.dressed.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrowed_libraries")
data class BorrowedLibraryEntity(
    @PrimaryKey val id: String,
    val sharerName: String,
    val importedAtEpochMs: Long,
)
