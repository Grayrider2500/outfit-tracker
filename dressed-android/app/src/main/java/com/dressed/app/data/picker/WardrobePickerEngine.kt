package com.dressed.app.data.picker

import android.util.Log
import com.dressed.app.BuildConfig
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

    /** One item per category per outfit (prevents two jackets / two bags from enrichment). */
    private val SLOT_CAPPED_CATEGORIES: Set<String> = setOf(
        WardrobeCategories.TOPS,
        WardrobeCategories.BOTTOMS,
        WardrobeCategories.DRESSES,
        WardrobeCategories.SHOES,
        WardrobeCategories.OUTERWEAR,
        WardrobeCategories.ACCESSORIES,
    )

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

    private const val LOG_TAG = "WardrobePickerEng"

    private fun pickerLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, message)
    }

    /** Minimum pieces after expand; target after enrich (both ≤ 5). */
    private data class SamplerTier(val minPieces: Int, val enrichMin: Int)

    /** Random complete outfits to score per sampler pass (not cross-products). */
    private const val SAMPLER_TRIALS_STRICT = 72
    private const val SAMPLER_TRIALS_STRICT_BOOST = 96
    private const val SAMPLER_TRIALS_RELAXED = 56

    private const val CAP_TB_MAIN = 22
    private const val CAP_DRESS = 14
    private const val CAP_SHOE = 14
    private const val CAP_OUTER = 10
    private const val CAP_ACC = 10
    private const val CAP_BOOST_TB = 28
    private const val CAP_BOOST_DRESS = 18
    private const val CAP_BOOST_SHOE = 18

    /**
     * True iff some triple of items from the pool is a valid 3-piece outfit under the same rules
     * as generation ([isValidDistinctPieces], [sizesCoherent], distinct ids, three items).
     */
    private fun poolSupportsThreePieceOutfit(
        tops: List<WardrobeItemEntity>,
        bottoms: List<WardrobeItemEntity>,
        dresses: List<WardrobeItemEntity>,
        shoes: List<WardrobeItemEntity>,
        outer: List<WardrobeItemEntity>,
        acc: List<WardrobeItemEntity>,
        nowEpochMs: Long,
    ): Boolean {
        val capMain = 18
        val capTert = 14

        fun isValidThreePieceCombo(p: List<WardrobeItemEntity>): Boolean =
            p.size == 3 &&
                p.map { it.id }.toSet().size == 3 &&
                isValidDistinctPieces(p) &&
                sizesCoherent(p)

        if (tops.isNotEmpty() && bottoms.isNotEmpty() && listOf(shoes, outer, acc).any { it.isNotEmpty() }) {
            val tt = if (tops.size <= capMain) tops else tops.sortedBy { rotationKey(it, nowEpochMs) }.take(capMain)
            val bb = if (bottoms.size <= capMain) bottoms else bottoms.sortedBy { rotationKey(it, nowEpochMs) }.take(capMain)
            for (t in tt) {
                for (b in bb) {
                    for (bucket in listOf(shoes, outer, acc)) {
                        if (bucket.isEmpty()) continue
                        val slice = if (bucket.size <= capTert) bucket else bucket.sortedBy { rotationKey(it, nowEpochMs) }.take(capTert)
                        for (x in slice) {
                            if (isValidThreePieceCombo(listOf(t, b, x))) return true
                        }
                    }
                }
            }
        }

        val dressPairs = listOf(
            shoes to outer,
            shoes to acc,
            outer to acc,
        ).filter { it.first.isNotEmpty() && it.second.isNotEmpty() }
        if (dresses.isNotEmpty() && dressPairs.isNotEmpty()) {
            val dd = if (dresses.size <= capMain) dresses else dresses.sortedBy { rotationKey(it, nowEpochMs) }.take(capMain)
            for (d in dd) {
                for ((bucketA, bucketB) in dressPairs) {
                    val aa = if (bucketA.size <= capTert) bucketA else bucketA.sortedBy { rotationKey(it, nowEpochMs) }.take(capTert)
                    val bb = if (bucketB.size <= capTert) bucketB else bucketB.sortedBy { rotationKey(it, nowEpochMs) }.take(capTert)
                    for (a in aa) {
                        for (b in bb) {
                            if (isValidThreePieceCombo(listOf(d, a, b))) return true
                        }
                    }
                }
            }
        }

        return false
    }

    /** Shoes first, then outerwear, then accessories; within each bucket by [rotationKey] (stable). */
    private fun orderedFillCandidates(
        shoes: List<WardrobeItemEntity>,
        outer: List<WardrobeItemEntity>,
        acc: List<WardrobeItemEntity>,
        nowEpochMs: Long,
    ): List<WardrobeItemEntity> {
        fun rank(list: List<WardrobeItemEntity>): List<WardrobeItemEntity> =
            list.sortedWith(
                compareBy<WardrobeItemEntity>({ rotationKey(it, nowEpochMs) }).thenBy { it.id },
            )
        return rank(shoes) + rank(outer) + rank(acc)
    }

    private fun outfitSignature(pieces: List<WardrobeItemEntity>): String =
        pieces.map { it.id }.sorted().joinToString(",")

    private fun finalizeScoredOutfit(
        raw: List<WardrobeItemEntity>,
        tertiaryOrdered: List<WardrobeItemEntity>,
        tier: SamplerTier,
        occasion: Occasion,
        tagTerms: Set<String>,
        season: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Long,
    ): Pair<List<WardrobeItemEntity>, Double>? {
        var pieces = raw.distinctBy { it.id }.toMutableList()
        if (pieces.isEmpty()) return null
        if (!isValidDistinctPieces(pieces)) return null
        if (pieces.size < tier.minPieces) {
            val expanded = expandIncompleteOutfit(
                pieces,
                tertiaryOrdered,
                minPieces = tier.minPieces,
            ) ?: return null
            pieces = expanded.distinctBy { it.id }.toMutableList()
        }
        if (!isValidDistinctPieces(pieces)) return null
        if (pieces.size < tier.minPieces || pieces.size > 5) return null
        if (!sizesCoherent(pieces)) return null
        enrichToTargetPieceCount(
            pieces,
            tertiaryOrdered,
            minTarget = tier.enrichMin,
            maxPieces = 5,
        )
        if (!isValidDistinctPieces(pieces)) return null
        if (pieces.size < tier.minPieces || pieces.size > 5 || !sizesCoherent(pieces)) return null
        val sc = scoreOutfit(
            pieces,
            occasion,
            tagTerms,
            season,
            weatherTagIds,
            moodTagIds,
            nowEpochMs,
        )
        return pieces.toList() to sc
    }

    /**
     * One stochastic outfit skeleton (2–5 items) before expand/enrich.
     * Uses only capped pools passed in (already stratified).
     */
    private fun sampleOutfitRaw(
        rt: List<WardrobeItemEntity>,
        rb: List<WardrobeItemEntity>,
        rd: List<WardrobeItemEntity>,
        rsh: List<WardrobeItemEntity>,
        row: List<WardrobeItemEntity>,
        rac: List<WardrobeItemEntity>,
        rnd: Random,
        dressBias: Float,
    ): List<WardrobeItemEntity>? {
        val canDress = rd.isNotEmpty()
        val canTb = rt.isNotEmpty() && rb.isNotEmpty()
        val pickDress = when {
            !canDress -> false
            !canTb -> true
            else -> rnd.nextFloat() < dressBias
        }
        if (pickDress) {
            val out = mutableListOf(rd[rnd.nextInt(rd.size)])
            if (rsh.isNotEmpty() && rnd.nextFloat() < 0.9f) {
                val s = rsh[rnd.nextInt(rsh.size)]
                if (canAddCategoryToOutfit(out, s.category)) out.add(s)
            }
            if (row.isNotEmpty() && rnd.nextFloat() < 0.48f) {
                val o = row[rnd.nextInt(row.size)]
                if (canAddCategoryToOutfit(out, o.category)) out.add(o)
            }
            if (rac.isNotEmpty() && rnd.nextFloat() < 0.4f) {
                val a = rac[rnd.nextInt(rac.size)]
                if (canAddCategoryToOutfit(out, a.category)) out.add(a)
            }
            return if (isValidDistinctPieces(out)) out else null
        }
        if (!canTb) return null
        val out = mutableListOf(
            rt[rnd.nextInt(rt.size)],
            rb[rnd.nextInt(rb.size)],
        )
        if (rsh.isNotEmpty() && rnd.nextFloat() < 0.86f) {
            val s = rsh[rnd.nextInt(rsh.size)]
            if (canAddCategoryToOutfit(out, s.category)) out.add(s)
        }
        if (row.isNotEmpty() && rnd.nextFloat() < 0.44f) {
            val o = row[rnd.nextInt(row.size)]
            if (canAddCategoryToOutfit(out, o.category)) out.add(o)
        }
        if (rac.isNotEmpty() && rnd.nextFloat() < 0.38f) {
            val a = rac[rnd.nextInt(rac.size)]
            if (canAddCategoryToOutfit(out, a.category)) out.add(a)
        }
        return if (isValidDistinctPieces(out)) out else null
    }

    private fun runBoundedSampler(
        rt: List<WardrobeItemEntity>,
        rb: List<WardrobeItemEntity>,
        rd: List<WardrobeItemEntity>,
        rsh: List<WardrobeItemEntity>,
        row: List<WardrobeItemEntity>,
        rac: List<WardrobeItemEntity>,
        tertiaryOrdered: List<WardrobeItemEntity>,
        tier: SamplerTier,
        trials: Int,
        rnd: Random,
        dressBias: Float,
        occasion: Occasion,
        tagTerms: Set<String>,
        season: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Long,
    ): MutableList<Pair<List<WardrobeItemEntity>, Double>> {
        val candidates = mutableListOf<Pair<List<WardrobeItemEntity>, Double>>()
        val seen = mutableSetOf<String>()
        repeat(trials) {
            val raw = sampleOutfitRaw(rt, rb, rd, rsh, row, rac, rnd, dressBias) ?: return@repeat
            val done = finalizeScoredOutfit(
                raw,
                tertiaryOrdered,
                tier,
                occasion,
                tagTerms,
                season,
                weatherTagIds,
                moodTagIds,
                nowEpochMs,
            ) ?: return@repeat
            val sig = outfitSignature(done.first)
            if (sig in seen) return@repeat
            seen.add(sig)
            candidates.add(done)
        }
        return candidates
    }

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
        pickerLog("stage=in season_filter pool=${pool.size} allItems=${allItems.size} occasion=$occasionId")
        if (pool.size < 2) {
            pickerLog("stage=exit early pool<2")
            return emptyList()
        }

        val blendedSeed = blendPickerSeed(seed, occasionId, weatherTagIds, moodTagIds, nowEpochMs)
        val byCat = pool.groupBy { it.category }
        val tops = byCat[WardrobeCategories.TOPS].orEmpty()
        val bottoms = byCat[WardrobeCategories.BOTTOMS].orEmpty()
        val dresses = byCat[WardrobeCategories.DRESSES].orEmpty()
        val shoes = byCat[WardrobeCategories.SHOES].orEmpty()
        val outer = byCat[WardrobeCategories.OUTERWEAR].orEmpty()
        val acc = byCat[WardrobeCategories.ACCESSORIES].orEmpty()
        /** Built once: expand/enrich used to re-sort these pools on every candidate (very hot path). */
        val tertiaryOrdered = orderedFillCandidates(shoes, outer, acc, nowEpochMs)

        val rnd = Random(blendedSeed)
        val rnd2 = Random(blendedSeed xor XOR_MASK)

        fun stratified(items: List<WardrobeItemEntity>, n: Int): List<WardrobeItemEntity> {
            if (items.size <= n) return items.shuffled(rnd)
            val half = (n + 1) / 2
            val freshFirst = items.sortedBy { rotationKey(it, nowEpochMs) }
            val fromFresh = freshFirst.take(half)
            val remaining = items.filter { it.id !in fromFresh.map { x -> x.id }.toSet() }
            val fromRand = (if (remaining.isNotEmpty()) remaining else items).shuffled(rnd2).take(n - fromFresh.size)
            return (fromFresh + fromRand).distinctBy { it.id }.take(n).shuffled(rnd)
        }

        fun cappedPools(
            tbCap: Int,
            dressCap: Int,
            shoeCap: Int,
            outerCap: Int,
            accCap: Int,
        ): Array<List<WardrobeItemEntity>> = arrayOf(
            stratified(tops, tbCap),
            stratified(bottoms, tbCap),
            stratified(dresses, dressCap),
            stratified(shoes, shoeCap),
            stratified(outer, outerCap),
            stratified(acc, accCap),
        )

        var poolCaps = cappedPools(CAP_TB_MAIN, CAP_DRESS, CAP_SHOE, CAP_OUTER, CAP_ACC)
        var rt = poolCaps[0]
        var rb = poolCaps[1]
        var rd = poolCaps[2]
        var rsh = poolCaps[3]
        var row = poolCaps[4]
        var rac = poolCaps[5]

        val threeFeasible = poolSupportsThreePieceOutfit(tops, bottoms, dresses, shoes, outer, acc, nowEpochMs)
        pickerLog("stage=pool_three_feasible=$threeFeasible")

        val dressBias = when (occasionId) {
            "date_night", "formal" -> 0.44f
            else -> 0.30f
        }

        val strictTier = SamplerTier(minPieces = 3, enrichMin = 3)
        val relaxedTier = SamplerTier(minPieces = 2, enrichMin = 2)

        var candidates = runBoundedSampler(
            rt, rb, rd, rsh, row, rac,
            tertiaryOrdered,
            strictTier,
            SAMPLER_TRIALS_STRICT,
            rnd,
            dressBias,
            occasion,
            tagTerms,
            season,
            weatherTagIds,
            moodTagIds,
            nowEpochMs,
        )
        pickerLog("stage=sampler_strict count=${candidates.size}")

        if (candidates.isEmpty() && threeFeasible) {
            pickerLog("stage=sampler_strict_boost wider_pools")
            poolCaps = cappedPools(CAP_BOOST_TB, CAP_BOOST_DRESS, CAP_BOOST_SHOE, 14, 12)
            rt = poolCaps[0]
            rb = poolCaps[1]
            rd = poolCaps[2]
            rsh = poolCaps[3]
            row = poolCaps[4]
            rac = poolCaps[5]
            candidates = runBoundedSampler(
                rt, rb, rd, rsh, row, rac,
                tertiaryOrdered,
                strictTier,
                SAMPLER_TRIALS_STRICT_BOOST,
                rnd,
                dressBias,
                occasion,
                tagTerms,
                season,
                weatherTagIds,
                moodTagIds,
                nowEpochMs,
            )
            pickerLog("stage=sampler_strict_boost count=${candidates.size}")
        }

        var usedRelaxedTier = false
        if (candidates.isEmpty() && !threeFeasible) {
            poolCaps = cappedPools(CAP_TB_MAIN, CAP_DRESS, CAP_SHOE, CAP_OUTER, CAP_ACC)
            rt = poolCaps[0]
            rb = poolCaps[1]
            rd = poolCaps[2]
            rsh = poolCaps[3]
            row = poolCaps[4]
            rac = poolCaps[5]
            candidates = runBoundedSampler(
                rt, rb, rd, rsh, row, rac,
                tertiaryOrdered,
                relaxedTier,
                SAMPLER_TRIALS_RELAXED,
                rnd,
                dressBias,
                occasion,
                tagTerms,
                season,
                weatherTagIds,
                moodTagIds,
                nowEpochMs,
            )
            usedRelaxedTier = candidates.isNotEmpty()
            pickerLog("stage=sampler_relaxed count=${candidates.size}")
        } else if (candidates.isEmpty()) {
            pickerLog("stage=strict_empty_despite_three_feasible")
        }

        if (candidates.isEmpty()) {
            pickerLog("stage=exit no_raw_candidates")
            return emptyList()
        }

        // One row per unique item set (keep best score) so duplicate loops don’t flood the shortlist.
        val sorted = candidates
            .groupBy { it.first.map { e -> e.id }.sorted().joinToString(",") }
            .values
            .map { rows -> rows.maxBy { it.second } }
            .sortedByDescending { it.second }
        pickerLog("stage=deduped_unique_sets count=${sorted.size}")

        val picked = mutableListOf<Pair<List<WardrobeItemEntity>, Double>>()
        for ((pair, sc) in sorted) {
            if (picked.size >= maxOutfits * 24) break
            val ids = pair.map { it.id }.toSet()
            val tooSimilar = picked.any { prev ->
                val o = prev.first.map { it.id }.toSet()
                val shared = (ids intersect o).size
                shared >= 2 || sameTopBottomSilhouette(pair, prev.first)
            }
            if (!tooSimilar) picked.add(pair to sc)
            if (picked.size >= maxOutfits * 16) break
        }
        pickerLog("stage=diversity_picked count=${picked.size} (reject_if_2plus_shared_items)")

        val topPick = if (picked.isNotEmpty()) picked else sorted.take(maxOutfits * 3)
        pickerLog("stage=top_pick source=${if (picked.isNotEmpty()) "picked" else "sorted_slice"} count=${topPick.size}")

        val finalPairs = topPick
            .distinctBy { it.first.map { e -> e.id }.sorted().joinToString() }
            .sortedByDescending { it.second }
            .take(maxOutfits)

        val final = finalPairs.mapIndexed { idx, (pieces, sc) ->
            val ordered = pieces.sortedBy { displayOrder(it.category) }
            PickerSuggestion(
                title = "${occasion.label} · Look ${idx + 1}",
                items = ordered,
                score = sc,
                reason = formatSuggestionReason(
                    ordered,
                    weatherTagIds,
                    moodTagIds,
                    nowEpochMs,
                    simplerLooks = ordered.size < 3,
                ),
            )
        }
        pickerLog("stage=final_outfits count=${final.size} relaxedTier=$usedRelaxedTier")

        return final.ifEmpty {
            val minOk = if (usedRelaxedTier) 2 else 3
            val fallbackRow = sorted.firstOrNull { it.first.size >= minOk && isValidDistinctPieces(it.first) }
                ?: sorted.firstOrNull()?.let { (pieces, _) ->
                    val expanded = expandIncompleteOutfit(
                        pieces,
                        tertiaryOrdered,
                        minPieces = minOk,
                    )
                    if (expanded != null &&
                        expanded.size in minOk..5 &&
                        isValidDistinctPieces(expanded) &&
                        sizesCoherent(expanded)
                    ) {
                        expanded to scoreOutfit(expanded, occasion, tagTerms, season, weatherTagIds, moodTagIds, nowEpochMs)
                    } else null
                }
            if (fallbackRow == null) {
                pickerLog("stage=exit fallback_exhausted")
                return@ifEmpty emptyList()
            }
            val (pieces, sc) = fallbackRow
            val ordered = pieces.sortedBy { displayOrder(it.category) }
            pickerLog("stage=fallback single size=${ordered.size}")
            listOf(
                PickerSuggestion(
                    title = "${occasion.label} · Look 1",
                    items = ordered,
                    score = sc,
                    reason = formatSuggestionReason(
                        ordered,
                        weatherTagIds,
                        moodTagIds,
                        nowEpochMs,
                        simplerLooks = ordered.size < 3,
                    ),
                ),
            )
        }
    }

    /** Distinct item ids, one piece per capped slot, and no dress mixed with top/bottom. */
    private fun isValidDistinctPieces(pieces: List<WardrobeItemEntity>): Boolean {
        if (pieces.map { it.id }.toSet().size != pieces.size) return false
        val counts = pieces.groupingBy { it.category }.eachCount()
        for ((cat, n) in counts) {
            if (cat in SLOT_CAPPED_CATEGORIES && n > 1) return false
        }
        val hasDress = pieces.any { it.category == WardrobeCategories.DRESSES }
        val hasTop = pieces.any { it.category == WardrobeCategories.TOPS }
        val hasBottom = pieces.any { it.category == WardrobeCategories.BOTTOMS }
        if (hasDress && (hasTop || hasBottom)) return false
        return true
    }

    private fun canAddCategoryToOutfit(pieces: List<WardrobeItemEntity>, category: String): Boolean {
        if (category in SLOT_CAPPED_CATEGORIES && pieces.any { it.category == category }) return false
        val hasDress = pieces.any { it.category == WardrobeCategories.DRESSES }
        if (hasDress && (category == WardrobeCategories.TOPS || category == WardrobeCategories.BOTTOMS)) {
            return false
        }
        val hasTop = pieces.any { it.category == WardrobeCategories.TOPS }
        val hasBottom = pieces.any { it.category == WardrobeCategories.BOTTOMS }
        if (category == WardrobeCategories.DRESSES && (hasTop || hasBottom)) return false
        return true
    }

    /** Incomplete combos get extra pieces until at least [minPieces] items when pool allows. */
    private fun expandIncompleteOutfit(
        pieces: List<WardrobeItemEntity>,
        tertiaryOrdered: List<WardrobeItemEntity>,
        minPieces: Int = 3,
    ): List<WardrobeItemEntity>? {
        if (pieces.isEmpty()) return null
        val out = pieces.distinctBy { it.id }.toMutableList()
        val used = out.map { it.id }.toMutableSet()
        while (out.size < minPieces) {
            val extras = tertiaryOrdered
                .filter { it.id !in used && canAddCategoryToOutfit(out, it.category) }
            var added = false
            for (pick in extras) {
                out.add(pick)
                used.add(pick.id)
                if (sizesCoherent(out) && isValidDistinctPieces(out)) {
                    added = true
                    break
                }
                out.removeAt(out.lastIndex)
                used.remove(pick.id)
            }
            if (!added) break
        }
        return if (out.size >= minPieces && isValidDistinctPieces(out)) out.take(5) else null
    }

    /** Adds shoes / outer / accessories until at least [minTarget] pieces (when pool allows), max [maxPieces]. */
    private fun enrichToTargetPieceCount(
        pieces: MutableList<WardrobeItemEntity>,
        tertiaryOrdered: List<WardrobeItemEntity>,
        minTarget: Int,
        maxPieces: Int,
    ) {
        val used = pieces.map { it.id }.toMutableSet()
        while (pieces.size < minTarget && pieces.size < maxPieces) {
            val extras = tertiaryOrdered
                .filter { it.id !in used && canAddCategoryToOutfit(pieces, it.category) }
            var added = false
            for (pick in extras) {
                pieces.add(pick)
                used.add(pick.id)
                if (sizesCoherent(pieces) && isValidDistinctPieces(pieces)) {
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

    private fun sizesCoherent(pieces: List<WardrobeItemEntity>): Boolean {
        val labeled = pieces.filter { it.sizeLabel.trim().isNotEmpty() }
        if (labeled.size < 2) return true
        val distinct = labeled.map { it.sizeLabel.trim().uppercase() }.toSet()
        return distinct.size <= 1
    }

    private fun formatSuggestionReason(
        pieces: List<WardrobeItemEntity>,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Long,
        simplerLooks: Boolean,
    ): String {
        val base = buildReason(pieces, weatherTagIds, moodTagIds, nowEpochMs)
        return if (simplerLooks) {
            "Limited wardrobe - showing simpler looks · $base"
        } else {
            base
        }
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
