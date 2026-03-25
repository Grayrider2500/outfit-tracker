package com.dressed.app.data.model

/**
 * Common size shortcuts per category. Users can still type any custom size.
 */
object WardrobeSizes {

    private val letterRun = listOf("XS", "S", "M", "L", "XL", "XXL", "2XL", "3XL")

    private val womenNumeric = listOf("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20")

    private val waistInches = listOf("24", "26", "28", "30", "32", "34", "36", "38", "40", "42")

    private val shoeUsWomens = listOf(
        "5", "5.5", "6", "6.5", "7", "7.5", "8", "8.5", "9", "9.5",
        "10", "10.5", "11", "11.5", "12", "12.5", "13",
    )

    fun suggestionsFor(categoryKey: String): List<String> = when (categoryKey) {
        WardrobeCategories.TOPS,
        WardrobeCategories.OUTERWEAR,
        -> letterRun

        WardrobeCategories.BOTTOMS -> letterRun + waistInches

        WardrobeCategories.DRESSES -> letterRun + womenNumeric

        WardrobeCategories.SHOES -> shoeUsWomens

        WardrobeCategories.ACCESSORIES -> listOf(
            "One size",
            "OS",
            "XS",
            "S",
            "M",
            "L",
            "XL",
            "Adjustable",
        )

        else -> emptyList()
    }
}
