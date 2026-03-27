package com.dressed.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Ordered list of wardrobe item IDs, stored as a comma-joined string via [Converters]. */
    val itemIds: List<String>,
    val wornCount: Int,
    val createdAtEpochMs: Long,
)
