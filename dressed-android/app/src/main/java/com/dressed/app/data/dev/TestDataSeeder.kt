package com.dressed.app.data.dev

import androidx.room.withTransaction
import com.dressed.app.data.local.DressedDatabase
import com.dressed.app.data.local.OutfitEntity
import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import com.dressed.app.data.model.WardrobeColors

/**
 * Deterministic dev/test wardrobe + outfits for exercising search and the Automatic Wardrobe Picker.
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

    private val categories = listOf(
        WardrobeCategories.TOPS,
        WardrobeCategories.BOTTOMS,
        WardrobeCategories.DRESSES,
        WardrobeCategories.SHOES,
        WardrobeCategories.OUTERWEAR,
        WardrobeCategories.ACCESSORIES,
    )

    private val topNames = listOf("Merino Crewneck", "Oxford Shirt", "Linen Tee", "Cashmere Sweater", "Poplin Shirt", "Henley")
    private val bottomNames = listOf("Slim Chinos", "Dark Jeans", "Pleated Trousers", "Cotton Shorts", "Wool Slacks", "Cargo Pants")
    private val dressNames = listOf("Midi Wrap Dress", "Shirt Dress", "Knit Dress", "Slip Dress", "Sweater Dress")
    private val shoeNames = listOf("Leather Sneakers", "Loafers", "Ankle Boots", "Running Trainers", "Chelsea Boots", "Sandals")
    private val outerNames = listOf("Wool Overcoat", "Denim Jacket", "Puffer Vest", "Trench Coat", "Bomber Jacket", "Field Jacket")
    private val accNames = listOf("Leather Belt", "Canvas Tote", "Wool Scarf", "Beanie", "Structured Tote", "Watch")

    private val styleTags = listOf("work", "casual", "weekend", "date", "gym", "travel", "evening")

    private val sizes = listOf("XS", "S", "M", "L", "XL", "8", "9", "10", "32", "34")

    /**
     * @param targetItemCount use 100 for the standard stress test.
     */
    suspend fun run(database: DressedDatabase, targetItemCount: Int = 100): SeedSummary {
        val wardrobeDao = database.wardrobeDao()
        val outfitDao = database.outfitDao()
        val items = buildItems(targetItemCount)
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

        return SeedSummary(
            itemsAdded = itemsAdded,
            itemsSkipped = itemsSkipped,
            outfitsAdded = outfitsAdded,
            outfitsSkipped = outfitsSkipped,
        )
    }

    private fun buildItems(count: Int): List<WardrobeItemEntity> {
        val palette = WardrobeColors.PALETTE
        val now = System.currentTimeMillis()
        return List(count) { i ->
            val category = categories[i % categories.size]
            val tag = styleTags[i % styleTags.size]
            val baseName = when (category) {
                WardrobeCategories.TOPS -> topNames[i % topNames.size]
                WardrobeCategories.BOTTOMS -> bottomNames[i % bottomNames.size]
                WardrobeCategories.DRESSES -> dressNames[i % dressNames.size]
                WardrobeCategories.SHOES -> shoeNames[i % shoeNames.size]
                WardrobeCategories.OUTERWEAR -> outerNames[i % outerNames.size]
                WardrobeCategories.ACCESSORIES -> accNames[i % accNames.size]
                else -> "Piece $i"
            }
            val name = "Seed $baseName ($tag) #$i"
            val seasons = seasonsForIndex(i)
            val swatch = palette[i % palette.size]
            val worn = (i * 3 + i % 7) % 18
            val lastWorn = if (worn > 0) {
                now - (i % 45) * 86_400_000L
            } else {
                null
            }
            WardrobeItemEntity(
                id = itemId(i),
                name = name,
                category = category,
                sizeLabel = sizes[i % sizes.size],
                colorHex = swatch.hex,
                colorName = swatch.name,
                seasons = seasons,
                photoPath = null,
                wornCount = worn,
                lastWornAtEpochMs = lastWorn,
                addedAtEpochMs = now - (count - i) * 60_000L,
            )
        }
    }

    private fun seasonsForIndex(i: Int): List<String> = when (i % 7) {
        0 -> listOf("spring", "summer")
        1 -> listOf("fall", "winter")
        2 -> listOf("spring")
        3 -> listOf("summer")
        4 -> listOf("fall")
        5 -> listOf("winter")
        else -> listOf("spring", "summer", "fall", "winter")
    }

    private fun itemId(index: Int) = "$ITEM_ID_PREFIX${index.toString().padStart(3, '0')}"

    private fun outfitId(index: Int) = "$OUTFIT_ID_PREFIX$index"

    /** Outfits reference seeded item ids (indices 0–~35) so they resolve after items exist. */
    private fun buildOutfits(): List<OutfitEntity> {
        val now = System.currentTimeMillis()
        val combos = listOf(
            listOf(0, 1, 12, 18),
            listOf(2, 7, 13, 19),
            listOf(4, 5, 14, 22),
            listOf(6, 8, 15, 24),
            listOf(3, 9, 16, 20),
            listOf(10, 11, 17, 21),
            listOf(23, 25, 26, 27),
            listOf(28, 29, 30, 31),
            listOf(0, 6, 12, 18, 24),
        )
        val names = listOf(
            "Seed · Office core",
            "Seed · Weekend casual",
            "Seed · Date night",
            "Seed · Gym run",
            "Seed · Travel layer",
            "Seed · Creative meet",
            "Seed · Autumn layers",
            "Seed · Summer easy",
            "Seed · City walk",
        )
        return combos.mapIndexed { idx, indices ->
            val ids = indices.map { itemId(it) }
            OutfitEntity(
                id = outfitId(idx),
                name = names[idx],
                itemIds = ids,
                wornCount = idx % 5,
                createdAtEpochMs = now - (combos.size - idx) * 120_000L,
            )
        }
    }
}
