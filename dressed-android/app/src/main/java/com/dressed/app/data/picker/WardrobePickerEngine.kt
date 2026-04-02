package com.dressed.app.data.picker

import com.dressed.app.data.local.WardrobeItemEntity
import com.dressed.app.data.model.WardrobeCategories
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Rule-based outfit suggestions (Phase 2). Kept in sync with iOS `WardrobePickerEngine.swift`.
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
        /** Short line explaining why this outfit was suggested. */
        val reason: String,
    ) {
        val itemIds: List<String> get() = items.map { it.id }
    }

    private const val DAY_MS = 86400000L
    /** Secondary RNG stream; must fit signed `Long` (Fibonacci-hash style). */
    private const val XOR_MASK = 0x51F4A94F51F4A94FL
    /** Bit pattern 0xC6BC279689FAD6A5 (ULong literal would overflow signed `const Long`). */
    private const val XOR_MASK_ENRICH = -4128403253811843803L

    /** Mix user entropy so occasion/tags/time change shuffle and “Surprise me” rerolls actually diverge. */
    private fun blendPickerSeed(
        seed: Long,
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Long,
    ): Long {
        var h = seed
        h = h xor (occasionId.hashCode().toLong() * -7046029254386353131L)
        val w = weatherTagIds.sorted().joinToString(",")
        h = h xor ((w.hashCode().toLong() and 0xFFFF_FFFFL) shl 9)
        h = h xor ((w.hashCode().toLong() shr 23) and 0xFFFF_FFFFL)
        val m = moodTagIds.sorted().joinToString(",")
        h = h xor ((m.hashCode().toLong()) shl 13)
        h = h xor nowEpochMs
        h = h xor java.lang.Long.reverse(nowEpochMs) xor (nowEpochMs ushr 33)
        if (h == 0L) h = -7046029254386353131L // 0x9E3779B97F4A7C15
        return h
    }

    /** Northern-hemisphere season from local month (simple heuristic). */
    fun currentSeasonKey(month1Based: Int): String = when (month1Based) {
        12, 1, 2 -> "winter"
        in 3..5 -> "spring"
        in 6..8 -> "summer"
        else -> "fall"
    }

    /** Calendar season plus weather hints so pool is not only “this month”. */
    fun expandedSeasonKeys(calendarSeason: String, weatherTagIds: Set<String>): Set<String> {
        val w = weatherTagIds.map { it.lowercase() }.toSet()
        val out = mutableSetOf(calendarSeason.lowercase())
        if ("cold" in w) {
            out.add("winter")
            out.add("fall")
        }
        if ("hot" in w) {
            out.add("summer")
            out.add("spring")
        }
        if ("mild" in w) {
            out.add("spring")
            out.add("fall")
        }
        return out
    }

    fun suggest(
        allItems: List<WardrobeItemEntity>,
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        seed: Long,
        maxOutfits: Int = 3,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<PickerSuggestion> {
        val occasion = OCCASIONS.find { it.id == occasionId } ?: OCCASIONS.first()
        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        val season = currentSeasonKey(month)
        val seasonPool = expandedSeasonKeys(season, weatherTagIds)
        val tagTerms = (weatherTagIds + moodTagIds).map { it.lowercase() }.toSet()

        val pool = allItems.filter { item ->
            item.seasons.isEmpty() || item.seasons.any { it.lowercase() in seasonPool }
        }
        if (pool.size < 2) return emptyList()

        val blendedSeed = blendPickerSeed(seed, occasionId, weatherTagIds, moodTagIds, nowEpochMs)
        val byCat = pool.groupBy { it.category }
        val tops = byCat[WardrobeCategories.TOPS].orEmpty()
        val bottoms = byCat[WardrobeCategories.BOTTOMS].orEmpty()
        val dresses = byCat[WardrobeCategories.DRESSES].orEmpty()
        val shoes = byCat[WardrobeCategories.SHOES].orEmpty()
        val outer = byCat[WardrobeCategories.OUTERWEAR].orEmpty()
        val acc = byCat[WardrobeCategories.ACCESSORIES].orEmpty()

        val rnd = Random(blendedSeed)
        val rnd2 = Random(blendedSeed xor XOR_MASK)
        val rndEnrich = Random(blendedSeed xor XOR_MASK_ENRICH)

        fun stratified(items: List<WardrobeItemEntity>, n: Int): List<WardrobeItemEntity> {
            if (items.size <= n) return items.shuffled(rnd)
            val half = (n + 1) / 2
            val freshFirst = items.sortedBy { rotationKey(it, nowEpochMs) }
            val fromFresh = freshFirst.take(half)
            val remaining = items.filter { it.id !in fromFresh.map { x -> x.id }.toSet() }
            val fromRand = (if (remaining.isNotEmpty()) remaining else items).shuffled(rnd2).take(n - fromFresh.size)
            return (fromFresh + fromRand).distinctBy { it.id }.take(n).shuffled(rnd)
        }

        val rngTops = stratified(tops, 12)
        val rngBottoms = stratified(bottoms, 12)
        val rngDresses = stratified(dresses, 9)
        val rngShoes = stratified(shoes, 9)
        val rngOuter = stratified(outer, 7)
        val rngAcc = stratified(acc, 7)

        val candidates = mutableListOf<Pair<List<WardrobeItemEntity>, Double>>()

        fun addCandidate(raw: List<WardrobeItemEntity>) {
            var pieces = raw.distinctBy { it.id }.toMutableList()
            if (pieces.isEmpty()) return
            if (pieces.size < 2) {
                val expanded = expandIncompleteOutfit(pieces, rngShoes, rngOuter, rngAcc, rnd) ?: return
                pieces = expanded.distinctBy { it.id }.toMutableList()
            }
            if (pieces.size < 2 || pieces.size > 5) return
            if (!sizesCoherent(pieces)) return
            enrichToTargetPieceCount(pieces, rngShoes, rngOuter, rngAcc, rndEnrich, minTarget = 3, maxPieces = 5)
            if (pieces.size < 2 || pieces.size > 5 || !sizesCoherent(pieces)) return
            val sc = scoreOutfit(pieces, occasion, tagTerms, season, weatherTagIds, moodTagIds, nowEpochMs)
            candidates.add(pieces.toList() to sc)
        }

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
                            }.take(5)
                            addCandidate(list)
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return emptyList()

        // One row per unique item set (keep best score) so duplicate loops don’t flood the shortlist.
        val sorted = candidates
            .groupBy { it.first.map { e -> e.id }.sorted().joinToString(",") }
            .values
            .map { rows -> rows.maxBy { it.second } }
            .sortedByDescending { it.second }
        val picked = mutableListOf<Pair<List<WardrobeItemEntity>, Double>>()
        val similarityCap = 0.36
        for ((pair, sc) in sorted) {
            if (picked.size >= maxOutfits * 12) break
            val ids = pair.map { it.id }.toSet()
            val tooSimilar = picked.any { prev ->
                val o = prev.first.map { it.id }.toSet()
                jaccard(ids, o) > similarityCap ||
                    sameTopBottomSilhouette(pair, prev.first)
            }
            if (!tooSimilar) picked.add(pair to sc)
            if (picked.size >= maxOutfits * 8) break
        }

        val topPick = if (picked.isNotEmpty()) picked else sorted.take(maxOutfits * 3)
        val final = topPick
            .distinctBy { it.first.map { e -> e.id }.sorted().joinToString() }
            .sortedByDescending { it.second }
            .take(maxOutfits)
            .mapIndexed { idx, (pieces, sc) ->
                val ordered = pieces.sortedBy { displayOrder(it.category) }
                PickerSuggestion(
                    title = "${occasion.label} · Look ${idx + 1}",
                    items = ordered,
                    score = sc,
                    reason = buildReason(ordered, weatherTagIds, moodTagIds, nowEpochMs),
                )
            }

        return final.ifEmpty {
            val fallbackRow = sorted.firstOrNull { it.first.size >= 2 }
                ?: sorted.firstOrNull()?.let { (pieces, sc) ->
                    val expanded = expandIncompleteOutfit(pieces, rngShoes, rngOuter, rngAcc, rndEnrich)
                    if (expanded != null && expanded.size in 2..5 && sizesCoherent(expanded)) {
                        expanded to scoreOutfit(expanded, occasion, tagTerms, season, weatherTagIds, moodTagIds, nowEpochMs)
                    } else null
                }
            if (fallbackRow == null) return@ifEmpty emptyList()
            val (pieces, sc) = fallbackRow
            val ordered = pieces.sortedBy { displayOrder(it.category) }
            listOf(
                PickerSuggestion(
                    title = "${occasion.label} · Look 1",
                    items = ordered,
                    score = sc,
                    reason = buildReason(ordered, weatherTagIds, moodTagIds, nowEpochMs),
                ),
            )
        }
    }

    /** Dress-only and similar incomplete combos get extra pieces so every outfit has 2–5 items when pool allows. */
    private fun expandIncompleteOutfit(
        pieces: List<WardrobeItemEntity>,
        shoes: List<WardrobeItemEntity>,
        outer: List<WardrobeItemEntity>,
        acc: List<WardrobeItemEntity>,
        rnd: Random,
    ): List<WardrobeItemEntity>? {
        if (pieces.isEmpty()) return null
        val out = pieces.distinctBy { it.id }.toMutableList()
        val used = out.map { it.id }.toMutableSet()
        while (out.size < 2) {
            val extras = (shoes + outer + acc).filter { it.id !in used }.shuffled(rnd)
            var added = false
            for (pick in extras) {
                out.add(pick)
                used.add(pick.id)
                if (sizesCoherent(out)) {
                    added = true
                    break
                }
                out.removeAt(out.lastIndex)
                used.remove(pick.id)
            }
            if (!added) break
        }
        return if (out.size >= 2) out.take(5) else null
    }

    /** Adds shoes / outer / accessories until at least [minTarget] pieces (when pool allows), max [maxPieces]. */
    private fun enrichToTargetPieceCount(
        pieces: MutableList<WardrobeItemEntity>,
        shoes: List<WardrobeItemEntity>,
        outer: List<WardrobeItemEntity>,
        acc: List<WardrobeItemEntity>,
        rnd: Random,
        minTarget: Int,
        maxPieces: Int,
    ) {
        val used = pieces.map { it.id }.toMutableSet()
        while (pieces.size < minTarget && pieces.size < maxPieces) {
            val extras = (shoes + outer + acc).filter { it.id !in used }.shuffled(rnd)
            var added = false
            for (pick in extras) {
                pieces.add(pick)
                used.add(pick.id)
                if (sizesCoherent(pieces)) {
                    added = true
                    break
                }
                pieces.removeAt(pieces.lastIndex)
                used.remove(pick.id)
            }
            if (!added) break
        }
    }

    /** True when both outfits use the exact same top set and bottom set (prevents three “casual” cards that are the same shirt+jeans). */
    private fun sameTopBottomSilhouette(a: List<WardrobeItemEntity>, b: List<WardrobeItemEntity>): Boolean {
        val ta = a.filter { it.category == WardrobeCategories.TOPS }.map { it.id }.toSet()
        val ba = a.filter { it.category == WardrobeCategories.BOTTOMS }.map { it.id }.toSet()
        val tb = b.filter { it.category == WardrobeCategories.TOPS }.map { it.id }.toSet()
        val bb = b.filter { it.category == WardrobeCategories.BOTTOMS }.map { it.id }.toSet()
        if (ta.isEmpty() || ba.isEmpty() || tb.isEmpty() || bb.isEmpty()) return false
        return ta == tb && ba == bb
    }

    private fun rotationKey(item: WardrobeItemEntity, now: Long): Long {
        if (item.wornCount == 0) return Long.MIN_VALUE
        val lw = item.lastWornAtEpochMs ?: return now - 60L * DAY_MS
        return lw
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
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Long,
    ): Double {
        var s = 0.0
        for (p in pieces) {
            s += 78.0 / (1 + p.wornCount)
            if (p.seasons.isEmpty() || p.seasons.any { it.equals(seasonKey, ignoreCase = true) }) {
                s += 10
            }
            if (occasion.preferredCategories.contains(p.category)) s += 8

            val hay = "${p.name} ${p.colorName}".lowercase()
            for (t in tagTerms) {
                if (t.isNotEmpty() && hay.contains(t)) s += 5
            }
            s += tagKeywordBonus(p, weatherTagIds, moodTagIds)

            val lw = p.lastWornAtEpochMs
            if (p.wornCount == 0) {
                s += 14
            } else if (lw != null) {
                val days = (nowEpochMs - lw) / DAY_MS
                if (days >= 21) s += 18
                else if (days >= 8) s += 12
                else if (days >= 1) s += 4
            }

            if (p.wornCount in 3..9) s += 5

            if (tagTerms.contains("cold") && p.category == WardrobeCategories.OUTERWEAR) s += 12
            if (tagTerms.contains("rainy") && p.category == WardrobeCategories.OUTERWEAR) s += 10
            if ((moodTagIds.map { it.lowercase() }.toSet()).contains("cozy") && p.category == WardrobeCategories.OUTERWEAR) s += 8
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
                s += hueHarmonyBonus(c0, c1)
            }
        }
        s += pieces.map { it.category }.distinct().size * 2.0
        return s
    }

    private fun tagKeywordBonus(
        p: WardrobeItemEntity,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
    ): Double {
        var b = 0.0
        val hay = "${p.name} ${p.colorName}".lowercase()
        val w = weatherTagIds.map { it.lowercase() }.toSet()
        val m = moodTagIds.map { it.lowercase() }.toSet()
        if ("sunny" in w && (hay.contains("linen") || hay.contains("light") || hay.contains("breath"))) b += 4
        if ("rainy" in w && (hay.contains("nylon") || hay.contains("water") || hay.contains("shell"))) b += 4
        if ("hot" in w && (hay.contains("linen") || hay.contains("cotton") || hay.contains("mesh") || hay.contains("tank"))) b += 4
        if ("cold" in w && (hay.contains("wool") || hay.contains("fleece") || hay.contains("down") || hay.contains("thermal"))) b += 4
        if ("cozy" in m && (hay.contains("knit") || hay.contains("wool") || hay.contains("fleece"))) b += 3
        return b
    }

    private fun buildReason(
        pieces: List<WardrobeItemEntity>,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Long,
    ): String {
        val parts = mutableListOf<String>()
        val unworn = pieces.count { it.wornCount == 0 }
        if (unworn > 0) {
            parts += if (unworn == 1) "One piece never logged as worn here" else "$unworn pieces never logged as worn"
        }
        val daySpans = pieces.mapNotNull { it.lastWornAtEpochMs?.let { lw -> (nowEpochMs - lw) / DAY_MS } }
        if (daySpans.isNotEmpty()) {
            val minD = daySpans.minOrNull()!!
            val maxD = daySpans.maxOrNull()!!
            when {
                minD >= 14 -> parts += "Rested rotation (up to $maxD days since last wear)"
                minD >= 7 -> parts += "Haven’t been worn in at least a week"
                minD >= 1 -> parts += "Mix of recently worn and fresh picks"
            }
        } else if (unworn == 0 && pieces.all { it.wornCount > 0 && it.lastWornAtEpochMs == null }) {
            parts += "Uses staples (add “Wear today” to track last-worn dates)"
        }
        val cats = pieces.map { it.category }.distinct().size
        if (cats >= 3) parts += "Good category variety"
        parts += harmonyLabel(pieces)

        val w = weatherTagIds.map { it.lowercase() }.toSet()
        if (w.isNotEmpty()) parts += "Weather-aware wardrobe filter"

        val trimmed = parts.filter { it.isNotBlank() }.distinct().take(2)
        return trimmed.joinToString(" · ").ifEmpty { "Balanced for this occasion and season" }
    }

    private fun harmonyLabel(pieces: List<WardrobeItemEntity>): String {
        val mains = pieces.filter {
            it.category == WardrobeCategories.TOPS ||
                it.category == WardrobeCategories.BOTTOMS ||
                it.category == WardrobeCategories.DRESSES
        }
        if (mains.size < 2) return "Cohesive look"
        val c0 = parseRgb(mains[0].colorHex) ?: return "Cohesive look"
        val c1 = parseRgb(mains[1].colorHex) ?: return "Cohesive look"
        val lumDiff = abs(luminance(c0) - luminance(c1))
        val hb = hueHarmonyBonus(c0, c1)
        return when {
            hb >= 8.0 && lumDiff >= 0.08 -> "Strong color harmony (contrast + hue)"
            hb >= 6.0 -> "Complementary-style colors"
            lumDiff in 0.10..0.52 -> "Pleasing light–dark balance"
            lumDiff < 0.07 -> "Tonal / monochrome lean"
            else -> "Balanced palette"
        }
    }

    private data class Rgb(val r: Int, val g: Int, val b: Int)

    private fun luminance(c: Rgb): Double {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun hueDegrees(c: Rgb): Double? {
        val r = c.r / 255.0
        val g = c.g / 255.0
        val b = c.b / 255.0
        val maxC = max(max(r, g), b)
        val minC = min(min(r, g), b)
        val d = maxC - minC
        if (d < 1e-3) return null
        val h = when (maxC) {
            r -> ((g - b) / d) % 6
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        }
        var deg = h * 60
        if (deg < 0) deg += 360
        return deg
    }

    private fun hueHarmonyBonus(c0: Rgb, c1: Rgb): Double {
        val h0 = hueDegrees(c0) ?: return 0.0
        val h1 = hueDegrees(c1) ?: return 0.0
        var diff = abs(h0 - h1)
        if (diff > 180) diff = 360 - diff
        return when {
            diff < 28 -> 5.0
            diff in 75.0..108.0 -> 11.0
            diff in 140.0..180.0 -> 7.0
            else -> 0.0
        }
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
