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
    /** Occasion tags, e.g. ["date_night", "work"]. Stored as comma-joined string via [Converters]. */
    val occasions: List<String> = emptyList(),
    val photoPath: String?,
    val wornCount: Int,
    /** Set when the user marks the item worn (`Wear today` / Mark worn); used for picker rotation and reasons. */
    val lastWornAtEpochMs: Long? = null,
    val addedAtEpochMs: Long,
    /** When true, item can be included in a `.dressed-library` share pack. */
    val lendable: Boolean = false,
)
