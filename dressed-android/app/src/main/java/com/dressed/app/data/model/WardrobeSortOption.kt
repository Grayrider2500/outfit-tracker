package com.dressed.app.data.model

import com.dressed.app.data.local.WardrobeItemEntity

enum class WardrobeSortOption(val label: String, val shortLabel: String) {
    DATE_ADDED_DESC("Newest first", "Newest"),
    DATE_ADDED_ASC("Oldest first", "Oldest"),
    NAME_ASC("Name A–Z", "A–Z"),
    NAME_DESC("Name Z–A", "Z–A"),
    COLOR("Color", "Color"),
    SIZE("Size", "Size"),
    WORN_DESC("Most worn", "Most worn"),
    WORN_ASC("Least worn", "Least worn"),
    CATEGORY_THEN_NAME("Category", "Category"),
}

fun List<WardrobeItemEntity>.sortedForDisplay(option: WardrobeSortOption): List<WardrobeItemEntity> =
    when (option) {
        WardrobeSortOption.DATE_ADDED_DESC -> sortedByDescending { it.addedAtEpochMs }
        WardrobeSortOption.DATE_ADDED_ASC -> sortedBy { it.addedAtEpochMs }
        WardrobeSortOption.NAME_ASC -> sortedBy { it.name.lowercase() }
        WardrobeSortOption.NAME_DESC -> sortedByDescending { it.name.lowercase() }
        WardrobeSortOption.COLOR ->
            sortedWith(compareBy({ it.colorName.lowercase() }, { it.name.lowercase() }))
        WardrobeSortOption.SIZE ->
            sortedWith(
                compareBy({ sortKeyForSize(it.sizeLabel) }, { it.name.lowercase() }),
            )
        WardrobeSortOption.WORN_DESC -> sortedByDescending { it.wornCount }
        WardrobeSortOption.WORN_ASC -> sortedBy { it.wornCount }
        WardrobeSortOption.CATEGORY_THEN_NAME ->
            sortedWith(compareBy({ it.category }, { it.name.lowercase() }))
    }

/** Blank sizes sort last. */
private fun sortKeyForSize(label: String): String =
    if (label.isBlank()) "\uFFFF" else label.lowercase()
