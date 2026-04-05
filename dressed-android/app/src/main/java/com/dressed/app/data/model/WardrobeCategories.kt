package com.dressed.app.data.model

object WardrobeCategories {

    const val ALL = "all"
    const val TOPS = "tops"
    const val BOTTOMS = "bottoms"
    const val DRESSES = "dresses"
    const val SHOES = "shoes"
    const val OUTERWEAR = "outerwear"
    const val ACCESSORIES = "accessories"

    val FILTERS = listOf(
        ALL to "All",
        TOPS to "Tops",
        BOTTOMS to "Bottoms",
        DRESSES to "Dresses",
        SHOES to "Shoes",
        OUTERWEAR to "Outerwear",
        ACCESSORIES to "Accessories",
    )

    val ADD_PICKER = listOf(
        TOPS to "Tops",
        BOTTOMS to "Bottoms",
        DRESSES to "Dresses",
        SHOES to "Shoes",
        OUTERWEAR to "Outerwear",
        ACCESSORIES to "Accessories",
    )

    fun label(key: String): String =
        FILTERS.firstOrNull { it.first == key }?.second ?: key

    fun emoji(categoryKey: String): String = when (categoryKey) {
        TOPS -> "👕"
        BOTTOMS -> "👖"
        DRESSES -> "👗"
        SHOES -> "👠"
        OUTERWEAR -> "🧥"
        ACCESSORIES -> "👜"
        else -> "🧷"
    }
}

object WardrobeColors {
    data class Swatch(val hex: String, val name: String)

    val PALETTE = listOf(
        Swatch("#F5F5F0", "White"),
        Swatch("#2C2C2C", "Black"),
        Swatch("#8B6E5A", "Brown"),
        Swatch("#C4A882", "Tan"),
        Swatch("#6B7FA3", "Blue"),
        Swatch("#8FA68C", "Green"),
        Swatch("#C4788A", "Pink"),
        Swatch("#B05C3A", "Orange"),
        Swatch("#7A5C8A", "Purple"),
        Swatch("#C4B820", "Yellow"),
        Swatch("#A0A0A0", "Gray"),
        Swatch("#C42B2B", "Red"),
    )
}

object WardrobeSeasons {
    val ALL = listOf(
        "spring" to "Spring",
        "summer" to "Summer",
        "fall" to "Fall",
        "winter" to "Winter",
    )
}
