package com.dressed.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "borrowed_items",
    foreignKeys = [
        ForeignKey(
            entity = BorrowedLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("libraryId")],
)
data class BorrowedItemEntity(
    @PrimaryKey val id: String,
    val libraryId: String,
    val name: String,
    val category: String,
    val sizeLabel: String,
    val colorHex: String,
    val colorName: String,
    val seasons: List<String>,
    val occasions: List<String> = emptyList(),
    val photoPath: String?,
    val wornCount: Int,
    val lastWornAtEpochMs: Long? = null,
    val addedAtEpochMs: Long,
)
