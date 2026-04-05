package com.dressed.app.data.dev

import androidx.room.withTransaction
import com.dressed.app.data.local.DressedDatabase
import com.dressed.app.data.local.OutfitEntity
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories

/**
 * Deterministic dev/test wardrobe for one professional woman.
 * Safe to run multiple times: skips rows whose ids already exist.
 */
object TestDataSeeder {

    const val ITEM_ID_PREFIX = "devseed-item-"
    const val OUTFIT_ID_PREFIX = "devseed-outfit-"

    data class SeedSummary(
        val itemsAdded: Int,
        val itemsSkipped: Int,
        val outfitsAdded: Int,
        val outfitsSkipped: Int,
    )

    private data class SeedItemSpec(
        val name: String,
        val category: String,
        val sizeLabel: String,
        val colorHex: String,
        val colorName: String,
        val seasons: List<String>,
    )

    private val seedItemSpecs = listOf(
        SeedItemSpec("White Button-Down Shirt", WardrobeCategories.TOPS, "M", "#F7F7F2", "Soft White", listOf("spring", "summer", "fall")),
        SeedItemSpec("Ivory Silk Blouse", WardrobeCategories.TOPS, "M", "#F3EBDD", "Ivory", listOf("spring", "fall")),
        SeedItemSpec("Black Short-Sleeve Knit Top", WardrobeCategories.TOPS, "M", "#1F1F1F", "Black", listOf("spring", "fall", "winter")),
        SeedItemSpec("Navy Crewneck Shell", WardrobeCategories.TOPS, "M", "#223A5E", "Navy", listOf("spring", "summer", "fall")),
        SeedItemSpec("Dusty Blush Blouse", WardrobeCategories.TOPS, "M", "#D8A7A1", "Blush", listOf("spring", "fall")),
        SeedItemSpec("Camel Lightweight Sweater", WardrobeCategories.TOPS, "M", "#B88A5A", "Camel", listOf("fall", "winter", "spring")),
        SeedItemSpec("Charcoal Fine-Gauge Turtleneck", WardrobeCategories.TOPS, "M", "#4A4A4A", "Charcoal", listOf("fall", "winter")),
        SeedItemSpec("Cobalt Wrap Blouse", WardrobeCategories.TOPS, "M", "#2F5DCC", "Cobalt", listOf("spring", "fall")),
        SeedItemSpec("Soft Gray Mock-Neck Top", WardrobeCategories.TOPS, "M", "#B8BCC2", "Gray", listOf("fall", "winter", "spring")),
        SeedItemSpec("Olive Utility Blouse", WardrobeCategories.TOPS, "M", "#66724D", "Olive", listOf("spring", "fall")),
        SeedItemSpec("Burgundy Knit Shell", WardrobeCategories.TOPS, "M", "#6E2233", "Burgundy", listOf("fall", "winter")),
        SeedItemSpec("Cream Ribbed Sweater", WardrobeCategories.TOPS, "M", "#EADFCF", "Cream", listOf("fall", "winter", "spring")),
        SeedItemSpec("Light Blue Oxford Shirt", WardrobeCategories.TOPS, "M", "#AFCBE3", "Light Blue", listOf("spring", "summer", "fall")),
        SeedItemSpec("Black Sleeveless Mock-Neck Top", WardrobeCategories.TOPS, "M", "#181818", "Black", listOf("spring", "summer", "fall")),
        SeedItemSpec("Black Tailored Trousers", WardrobeCategories.BOTTOMS, "M", "#1D1D1D", "Black", listOf("spring", "fall", "winter")),
        SeedItemSpec("Navy Ankle Pants", WardrobeCategories.BOTTOMS, "M", "#223A5E", "Navy", listOf("spring", "fall", "winter")),
        SeedItemSpec("Charcoal Slim Trousers", WardrobeCategories.BOTTOMS, "M", "#4D5259", "Charcoal", listOf("fall", "winter")),
        SeedItemSpec("Camel Wide-Leg Pants", WardrobeCategories.BOTTOMS, "M", "#BC8E5F", "Camel", listOf("fall", "spring")),
        SeedItemSpec("Olive Cropped Trousers", WardrobeCategories.BOTTOMS, "M", "#6B7655", "Olive", listOf("spring", "fall")),
        SeedItemSpec("Ivory Straight-Leg Pants", WardrobeCategories.BOTTOMS, "M", "#F1E7D8", "Ivory", listOf("spring", "summer")),
        SeedItemSpec("Dark Wash Straight Jeans", WardrobeCategories.BOTTOMS, "M", "#2F4560", "Dark Denim", listOf("spring", "fall", "winter")),
        SeedItemSpec("Black Pencil Skirt", WardrobeCategories.BOTTOMS, "M", "#202020", "Black", listOf("spring", "fall", "winter")),
        SeedItemSpec("Navy Midi Skirt", WardrobeCategories.BOTTOMS, "M", "#2A3F63", "Navy", listOf("spring", "fall")),
        SeedItemSpec("Soft Gray Wide-Leg Trousers", WardrobeCategories.BOTTOMS, "M", "#B5B9BF", "Gray", listOf("spring", "fall", "winter")),
        SeedItemSpec("Black Sheath Dress", WardrobeCategories.DRESSES, "M", "#1C1C1C", "Black", listOf("spring", "fall", "winter")),
        SeedItemSpec("Navy Wrap Dress", WardrobeCategories.DRESSES, "M", "#243B63", "Navy", listOf("spring", "fall")),
        SeedItemSpec("Burgundy Midi Dress", WardrobeCategories.DRESSES, "M", "#6D2336", "Burgundy", listOf("fall", "winter")),
        SeedItemSpec("Cream Sweater Dress", WardrobeCategories.DRESSES, "M", "#E8DDCC", "Cream", listOf("fall", "winter")),
        SeedItemSpec("Slate Blue Shirt Dress", WardrobeCategories.DRESSES, "M", "#6E859B", "Slate Blue", listOf("spring", "summer", "fall")),
        SeedItemSpec("Blush Belted Dress", WardrobeCategories.DRESSES, "M", "#D5A5A5", "Blush", listOf("spring", "summer")),
        SeedItemSpec("Black Structured Blazer", WardrobeCategories.OUTERWEAR, "M", "#1B1B1B", "Black", listOf("spring", "fall", "winter")),
        SeedItemSpec("Navy Blazer", WardrobeCategories.OUTERWEAR, "M", "#22385C", "Navy", listOf("spring", "fall", "winter")),
        SeedItemSpec("Camel Longline Coat", WardrobeCategories.OUTERWEAR, "M", "#B98858", "Camel", listOf("fall", "winter")),
        SeedItemSpec("Charcoal Wool Coat", WardrobeCategories.OUTERWEAR, "M", "#4B4E54", "Charcoal", listOf("fall", "winter")),
        SeedItemSpec("Ivory Cropped Cardigan", WardrobeCategories.OUTERWEAR, "M", "#EFE4D4", "Ivory", listOf("spring", "fall")),
        SeedItemSpec("Olive Utility Jacket", WardrobeCategories.OUTERWEAR, "M", "#65724F", "Olive", listOf("spring", "fall")),
        SeedItemSpec("Black Pointed-Toe Flats", WardrobeCategories.SHOES, "", "#1A1A1A", "Black", listOf("spring", "summer", "fall")),
        SeedItemSpec("Nude Leather Pumps", WardrobeCategories.SHOES, "", "#C79B7A", "Nude", listOf("spring", "summer", "fall")),
        SeedItemSpec("Black Block-Heel Pumps", WardrobeCategories.SHOES, "", "#202020", "Black", listOf("spring", "fall", "winter")),
        SeedItemSpec("Brown Loafers", WardrobeCategories.SHOES, "", "#6B4A34", "Brown", listOf("spring", "fall")),
        SeedItemSpec("White Leather Sneakers", WardrobeCategories.SHOES, "", "#F5F5F3", "White", listOf("spring", "summer", "fall")),
        SeedItemSpec("Black Ankle Boots", WardrobeCategories.SHOES, "", "#1E1E1E", "Black", listOf("fall", "winter")),
        SeedItemSpec("Cognac Heeled Boots", WardrobeCategories.SHOES, "", "#9A623D", "Cognac", listOf("fall", "winter")),
        SeedItemSpec("Navy Suede Flats", WardrobeCategories.SHOES, "", "#263A59", "Navy", listOf("spring", "fall")),
        SeedItemSpec("Black Leather Tote", WardrobeCategories.ACCESSORIES, "", "#1F1F1F", "Black", listOf("spring", "summer", "fall", "winter")),
        SeedItemSpec("Tan Structured Tote", WardrobeCategories.ACCESSORIES, "", "#BE8D60", "Tan", listOf("spring", "summer", "fall")),
        SeedItemSpec("Pearl Stud Earrings", WardrobeCategories.ACCESSORIES, "", "#F2ECE3", "Pearl", listOf("spring", "summer", "fall", "winter")),
        SeedItemSpec("Gold Hoop Earrings", WardrobeCategories.ACCESSORIES, "", "#C9A03A", "Gold", listOf("spring", "summer", "fall", "winter")),
        SeedItemSpec("Silk Navy Scarf", WardrobeCategories.ACCESSORIES, "", "#23395B", "Navy", listOf("spring", "fall")),
        SeedItemSpec("Burgundy Leather Belt", WardrobeCategories.ACCESSORIES, "", "#702437", "Burgundy", listOf("fall", "winter")),
        SeedItemSpec("Black Leather Belt", WardrobeCategories.ACCESSORIES, "", "#1F1F1F", "Black", listOf("spring", "summer", "fall", "winter")),
        SeedItemSpec("Camel Cashmere Scarf", WardrobeCategories.ACCESSORIES, "", "#B88958", "Camel", listOf("fall", "winter")),
        SeedItemSpec("Silver Watch", WardrobeCategories.ACCESSORIES, "", "#BFC5CC", "Silver", listOf("spring", "summer", "fall", "winter")),
        SeedItemSpec("Black Structured Satchel", WardrobeCategories.ACCESSORIES, "", "#222222", "Black", listOf("spring", "summer", "fall", "winter")),
    )

    private val seededOutfits = listOf(
        "Seed · Office core" to listOf(0, 14, 30, 37, 44),
        "Seed · Navy polish" to listOf(3, 19, 31, 37, 46),
        "Seed · Creative Friday" to listOf(7, 20, 32, 39, 47),
        "Seed · Cozy winter" to listOf(11, 14, 33, 41, 51),
        "Seed · Utility day" to listOf(9, 15, 35, 40, 45),
        "Seed · Black dress" to listOf(24, 30, 38, 46, 53),
        "Seed · Burgundy evening" to listOf(26, 32, 42, 47, 49),
        "Seed · Soft layers" to listOf(4, 23, 34, 43, 52),
        "Seed · Minimal commute" to listOf(13, 22, 36, 53, 50),
    )

    suspend fun run(database: DressedDatabase, targetItemCount: Int = seedItemSpecs.size): SeedSummary {
        val wardrobeDao = database.wardrobeDao()
        val outfitDao = database.outfitDao()
        val items = buildItems(targetItemCount.coerceAtMost(seedItemSpecs.size))
        val outfits = buildOutfits()

        var itemsAdded = 0
        var itemsSkipped = 0
        var outfitsAdded = 0
        var outfitsSkipped = 0

        database.withTransaction {
            for (item in items) {
                if (wardrobeDao.getById(item.id) == null) {
                    wardrobeDao.insert(item)
                    itemsAdded++
                } else {
                    itemsSkipped++
                }
            }
            val existingOutfitIds = outfitDao.getAllSnapshot().map { it.id }.toSet()
            for (outfit in outfits) {
                if (outfit.id !in existingOutfitIds) {
                    outfitDao.insert(outfit)
                    outfitsAdded++
                } else {
                    outfitsSkipped++
                }
            }
        }

        return SeedSummary(itemsAdded, itemsSkipped, outfitsAdded, outfitsSkipped)
    }

    private fun buildItems(count: Int): List<WardrobeItemEntity> {
        val now = System.currentTimeMillis()
        return seedItemSpecs.take(count).mapIndexed { index, spec ->
            val worn = (index * 5 + 2) % 14
            WardrobeItemEntity(
                id = itemId(index),
                name = spec.name,
                category = spec.category,
                sizeLabel = spec.sizeLabel,
                colorHex = spec.colorHex,
                colorName = spec.colorName,
                seasons = spec.seasons,
                occasions = emptyList(),
                photoPath = null,
                wornCount = worn,
                lastWornAtEpochMs = if (worn == 0) null else now - ((index % 28) + 1) * 86_400_000L,
                addedAtEpochMs = now - (seedItemSpecs.size - index) * 60_000L,
                lendable = false,
            )
        }
    }

    private fun buildOutfits(): List<OutfitEntity> {
        val now = System.currentTimeMillis()
        return seededOutfits.mapIndexed { index, (name, itemIndexes) ->
            OutfitEntity(
                id = outfitId(index),
                name = name,
                itemIds = itemIndexes.map(::itemId),
                wornCount = index % 4,
                createdAtEpochMs = now - (seededOutfits.size - index) * 120_000L,
            )
        }
    }

    private fun itemId(index: Int) = "$ITEM_ID_PREFIX${index.toString().padStart(3, '0')}"

    private fun outfitId(index: Int) = "$OUTFIT_ID_PREFIX$index"
}
