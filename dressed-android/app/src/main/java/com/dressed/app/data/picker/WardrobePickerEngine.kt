package com.dressed.app.data.picker

import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Rule-based outfit suggestions (Phase 1). Mirrors logic in iOS `WardrobePickerEngine.swift`.
 */
object WardrobePickerEngine {

    data class Occasion(
        val id: String,
        val label: String,
        /** Categories that get a score bonus for this vibe. */
        val preferredCategories: Set<String>,
    )

    val OCCASIONS: List<Occasion> = listOf(
        Occasion("casual", "Casual", setOf(WardrobeCategories.TOPS, WardrobeCategories.BOTTOMS, WardrobeCategories.SHOES)),
        Occasion("work", "Work", setOf(WardrobeCategories.TOPS, WardrobeCategories.BOTTOMS, WardrobeCategories.OUTERWEAR)),
        Occasion("date_night", "Date night", setOf(WardrobeCategories.DRESSES, WardrobeCategories.TOPS, WardrobeCategories.BOTTOMS)),
        Occasion("formal", "Formal", setOf(WardrobeCategories.DRESSES, WardrobeCategories.OUTERWEAR, WardrobeCategories.SHOES)),
        Occasion("gym", "Gym", setOf(WardrobeCategories.TOPS, WardrobeCategories.BOTTOMS, WardrobeCategories.SHOES)),
    )

    val WEATHER_TAGS: List<Pair<String, String>> = listOf(
        "sunny" to "Sunny",
        "rainy" to "Rainy",
        "cold" to "Cold",
        "hot" to "Hot",
        "mild" to "Mild",
    )

    val MOOD_TAGS: List<Pair<String, String>> = listOf(
        "bright" to "Bright",
        "minimal" to "Minimal",
        "cozy" to "Cozy",
        "bold" to "Bold",
    )

    data class PickerSuggestion(
        val title: String,
        val items: List<WardrobeItemEntity>,
        val score: Double,
    ) {
        val itemIds: List<String> get() = items.map { it.id }
    }

    /** Northern-hemisphere season from local month (simple heuristic). */
    fun currentSeasonKey(month1Based: Int): String = when (month1Based) {
        12, 1, 2 -> "winter"
        in 3..5 -> "spring"
        in 6..8 -> "summer"
        else -> "fall"
    }

    fun suggest(
        allItems: List<WardrobeItemEntity>,
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        seed: Long,
        maxOutfits: Int = 3,
    ): List<PickerSuggestion> {
        val occasion = OCCASIONS.find { it.id == occasionId } ?: OCCASIONS.first()
        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        val season = currentSeasonKey(month)
        val tagTerms = (weatherTagIds + moodTagIds).map { it.lowercase() }.toSet()

        val pool = allItems.filter { item ->
            item.seasons.isEmpty() || item.seasons.any { it.equals(season, ignoreCase = true) }
        }
        if (pool.size < 2) return emptyList()

        val byCat = pool.groupBy { it.category }
        val tops = byCat[WardrobeCategories.TOPS].orEmpty()
        val bottoms = byCat[WardrobeCategories.BOTTOMS].orEmpty()
        val dresses = byCat[WardrobeCategories.DRESSES].orEmpty()
        val shoes = byCat[WardrobeCategories.SHOES].orEmpty()
        val outer = byCat[WardrobeCategories.OUTERWEAR].orEmpty()
        val acc = byCat[WardrobeCategories.ACCESSORIES].orEmpty()

        val rnd = Random(seed)
        fun <T> List<T>.sample(n: Int): List<T> = shuffled(rnd).take(min(n, size))

        val rngTops = tops.sample(10)
        val rngBottoms = bottoms.sample(10)
        val rngDresses = dresses.sample(8)
        val rngShoes = shoes.sample(8)
        val rngOuter = outer.sample(6)
        val rngAcc = acc.sample(6)

        val candidates = mutableListOf<Pair<List<WardrobeItemEntity>, Double>>()

        fun addCandidate(pieces: List<WardrobeItemEntity>) {
            if (pieces.isEmpty()) return
            if (!sizesCoherent(pieces)) return
            val sc = scoreOutfit(pieces, occasion, tagTerms, season)
            candidates.add(pieces to sc)
        }

        // Dress-based looks
        for (d in rngDresses) {
            for (sh in listOf(null) + rngShoes) {
                for (ac in listOf(null) + rngAcc) {
                    val list = buildList {
                        add(d)
                        sh?.let { add(it) }
                        ac?.let { add(it) }
                    }
                    addCandidate(list)
                }
            }
        }

        // Top + bottom looks
        for (t in rngTops) {
            for (b in rngBottoms) {
                for (sh in listOf(null) + rngShoes) {
                    for (ow in listOf(null) + rngOuter) {
                        for (ac in listOf(null) + rngAcc) {
                            val list = buildList {
                                add(t)
                                add(b)
                                sh?.let { add(it) }
                                ow?.let { add(it) }
                                ac?.let { add(it) }
                            }.take(4)
                            addCandidate(list)
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return emptyList()

        // Diversity: greedy pick by score with limited overlap
        val sorted = candidates.sortedByDescending { it.second }
        val picked = mutableListOf<Pair<List<WardrobeItemEntity>, Double>>()
        for ((pair, sc) in sorted) {
            if (picked.size >= maxOutfits * 8) break
            val ids = pair.map { it.id }.toSet()
            val tooSimilar = picked.any { prev ->
                jaccard(ids, prev.first.map { it.id }.toSet()) > 0.55
            }
            if (!tooSimilar) picked.add(pair to sc)
            if (picked.size >= maxOutfits * 4) break
        }

        val topPick = if (picked.isNotEmpty()) picked else sorted.take(maxOutfits * 2)
        val final = topPick
            .distinctBy { it.first.map { e -> e.id }.sorted().joinToString() }
            .sortedByDescending { it.second }
            .take(maxOutfits)
            .mapIndexed { idx, (pieces, sc) ->
                PickerSuggestion(
                    title = "${occasion.label} · Look ${idx + 1}",
                    items = pieces.sortedBy { displayOrder(it.category) },
                    score = sc,
                )
            }

        return final.ifEmpty {
            sorted.take(1).mapIndexed { idx, (pieces, sc) ->
                PickerSuggestion(
                    title = "${occasion.label} · Look ${idx + 1}",
                    items = pieces.sortedBy { displayOrder(it.category) },
                    score = sc,
                )
            }
        }
    }

    private fun displayOrder(category: String): Int = when (category) {
        WardrobeCategories.TOPS -> 0
        WardrobeCategories.DRESSES -> 0
        WardrobeCategories.BOTTOMS -> 1
        WardrobeCategories.OUTERWEAR -> 2
        WardrobeCategories.SHOES -> 3
        WardrobeCategories.ACCESSORIES -> 4
        else -> 5
    }

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
            if (a.isEmpty() && b.isEmpty()) return 0.0
        val inter = (a intersect b).size
        val uni = (a union b).size
        return inter.toDouble() / uni.toDouble()
    }

    private fun sizesCoherent(pieces: List<WardrobeItemEntity>): Boolean {
        val sizes = pieces.map { it.sizeLabel.trim() }.filter { it.isNotEmpty() }
            .map { it.uppercase() }.toSet()
        return sizes.size <= 1
    }

    private fun scoreOutfit(
        pieces: List<WardrobeItemEntity>,
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
    ): Double {
        var s = 0.0
        for (p in pieces) {
            s += 72.0 / (1 + p.wornCount)
            if (p.seasons.isEmpty() || p.seasons.any { it.equals(seasonKey, ignoreCase = true) }) {
                s += 10
            }
            if (occasion.preferredCategories.contains(p.category)) s += 8
            val hay = "${p.name} ${p.colorName}".lowercase()
            for (t in tagTerms) {
                if (t.isNotEmpty() && hay.contains(t)) s += 5
            }
            if (tagTerms.contains("cold") && p.category == WardrobeCategories.OUTERWEAR) s += 12
            if (tagTerms.contains("rainy") && p.category == WardrobeCategories.OUTERWEAR) s += 10
            if (tagTerms.contains("cozy") && p.category == WardrobeCategories.OUTERWEAR) s += 8
            if (tagTerms.contains("bright") && isVividHex(p.colorHex)) s += 6
            if (tagTerms.contains("minimal") && isNeutralHex(p.colorHex)) s += 6
        }
        val mainPieces = pieces.filter {
            it.category == WardrobeCategories.TOPS ||
                it.category == WardrobeCategories.BOTTOMS ||
                it.category == WardrobeCategories.DRESSES
        }
        if (mainPieces.size >= 2) {
            val c0 = parseRgb(mainPieces[0].colorHex)
            val c1 = parseRgb(mainPieces[1].colorHex)
            if (c0 != null && c1 != null) {
                val lumDiff = abs(luminance(c0) - luminance(c1))
                when {
                    lumDiff in 0.10..0.52 -> s += 16
                    lumDiff < 0.07 -> s -= 8
                }
            }
        }
        // Variety bonus: more categories represented
        s += pieces.map { it.category }.distinct().size * 1.5
        return s
    }

    private data class Rgb(val r: Int, val g: Int, val b: Int)

    private fun luminance(c: Rgb): Double {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun parseRgb(hex: String): Rgb? {
        val h = hex.trim().removePrefix("#")
        if (h.length != 6) return null
        val v = h.toLongOrNull(16) ?: return null
        val r = ((v shr 16) and 0xFF).toInt()
        val g = ((v shr 8) and 0xFF).toInt()
        val b = (v and 0xFF).toInt()
        return Rgb(r, g, b)
    }

    private fun isNeutralHex(hex: String): Boolean {
        val n = hex.lowercase()
        return n.contains("f5f5") || n.contains("2c2c") || n.contains("a0a0") || n.contains("8b6e") ||
            n.contains("c4a88") || n.contains("808080")
    }

    private fun isVividHex(hex: String): Boolean {
        val c = parseRgb(hex) ?: return false
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val maxC = max(max(r, g), b)
        val minC = min(min(r, g), b)
        val sat = if (maxC <= 1e-6) 0.0 else (maxC - minC) / maxC
        return sat > 0.35 && maxC > 0.45
    }
}
