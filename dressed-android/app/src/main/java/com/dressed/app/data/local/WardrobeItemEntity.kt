package com.dressed.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wardrobe_items")
data class WardrobeItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    /** User-entered or chip-selected (e.g. "M", "8", "9.5"). */
    val sizeLabel: String,
    val colorHex: String,
    val colorName: String,
    val seasons: List<String>,
    val photoPath: String?,
    val wornCount: Int,
    val addedAtEpochMs: Long,
)
