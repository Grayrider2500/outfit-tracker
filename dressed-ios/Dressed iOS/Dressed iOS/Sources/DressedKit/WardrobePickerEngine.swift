import Foundation

/// Rule-based outfit suggestions (Phase 2). Kept in sync with Android `WardrobePickerEngine`.
enum WardrobePickerEngine {

    struct Occasion: Identifiable, Hashable {
        var id: String
        var label: String
        var preferredCategories: Set<String>
    }

    static let occasions: [Occasion] = [
        Occasion(id: "casual", label: "Casual", preferredCategories: [WardrobeCatalog.tops, WardrobeCatalog.bottoms, WardrobeCatalog.shoes]),
        Occasion(id: "work", label: "Work", preferredCategories: [WardrobeCatalog.tops, WardrobeCatalog.bottoms, WardrobeCatalog.outerwear]),
        Occasion(id: "date_night", label: "Date night", preferredCategories: [WardrobeCatalog.dresses, WardrobeCatalog.tops, WardrobeCatalog.bottoms]),
        Occasion(id: "formal", label: "Formal", preferredCategories: [WardrobeCatalog.dresses, WardrobeCatalog.outerwear, WardrobeCatalog.shoes]),
        Occasion(id: "gym", label: "Gym", preferredCategories: [WardrobeCatalog.tops, WardrobeCatalog.bottoms, WardrobeCatalog.shoes]),
    ]

    static let weatherTags: [(id: String, label: String)] = [
        ("sunny", "Sunny"), ("rainy", "Rainy"), ("cold", "Cold"), ("hot", "Hot"), ("mild", "Mild"),
    ]

    static let moodTags: [(id: String, label: String)] = [
        ("bright", "Bright"), ("minimal", "Minimal"), ("cozy", "Cozy"), ("bold", "Bold"),
    ]

    struct PickerSuggestion: Identifiable {
        var id: String { itemIds.sorted().joined(separator: "|") }
        var title: String
        var items: [WardrobeItem]
        var score: Double
        var reason: String
        var itemIds: [String] { items.map(\.id) }
    }

    /// Result of rule-based picking: only includes outfits with **≥ 3 distinct items** when `suggestions` is non-empty.
    struct PickerSuggestOutcome {
        let suggestions: [PickerSuggestion]
        /// Closet cannot assemble outfits, **or** fewer than requested diverse looks were found this run.
        let shouldShowInsufficientVarietyMessage: Bool
    }

    private static let xorMaskPick: Int64 = 0x6C07896543210AB0
    /// Inner-loop tries per slot (overlap/silhouette filters need headroom after rank penalties).
    private static let pickerAttemptsPerOutfitSlot = 96
    private static let dayMs: Int64 = 86_400_000

    static func currentSeasonKey(month1Based: Int) -> String {
        switch month1Based {
        case 12, 1, 2: return "winter"
        case 3...5: return "spring"
        case 6...8: return "summer"
        default: return "fall"
        }
    }

    static func expandedSeasonKeys(calendarSeason: String, weatherTagIds: Set<String>) -> Set<String> {
        let w = Set(weatherTagIds.map { $0.lowercased() })
        var out: Set<String> = [calendarSeason.lowercased()]
        if w.contains("cold") {
            out.formUnion(["winter", "fall"])
        }
        if w.contains("hot") {
            out.formUnion(["summer", "spring"])
        }
        if w.contains("mild") {
            out.formUnion(["spring", "fall"])
        }
        return out
    }

    private static func blendPickerSeed(
        seed: Int64,
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
    ) -> Int64 {
        var h = UInt64(bitPattern: seed)
        h ^= UInt64(bitPattern: Int64(occasionId.hashValue)) &* 0x9E3779B97F4A7C15
        for (i, u) in occasionId.utf8.enumerated() {
            h ^= UInt64(u) &<< UInt64((i * 7 + 3) % 56)
            h &+= UInt64(u) &* UInt64(i &+ 1)
        }
        let wx = weatherTagIds.sorted().joined(separator: ",")
        h ^= UInt64(UInt32(truncatingIfNeeded: wx.hashValue)) &<< 9
        h ^= UInt64(truncatingIfNeeded: wx.hashValue) &<< 17
        h ^= UInt64(wx.unicodeScalars.reduce(0) { $0 &+ UInt64($1.value) })
        let mx = moodTagIds.sorted().joined(separator: ",")
        h ^= UInt64(bitPattern: Int64(mx.hashValue)) &<< 13
        h ^= UInt64(mx.unicodeScalars.reduce(0) { $0 &+ UInt64($1.value) &* 31 })
        let now = UInt64(bitPattern: nowEpochMs)
        h ^= now
        h ^= reverseBitsUInt64(now)
        h ^= now >> 33
        h ^= UInt64(bitPattern: seed &* 0x5851_F42D_4C95_7F1D)
        var out = Int64(bitPattern: h)
        if out == 0 { out = -7_046_029_254_386_353_131 } // 0x9E3779B97F4A7C15
        return out
    }

    #if DEBUG
    private static func pickerDebugLog(_ msg: String) {
        print("[WardrobePicker] \(msg)")
    }
    #else
    private static func pickerDebugLog(_ msg: String) {}
    #endif

    /// Per-slot RNG so each selected look uses an independent stream (tap + slot).
    private static func streamSeed(blended: Int64, userSeed: Int64, slot: Int, salt: Int64) -> Int64 {
        var x = UInt64(bitPattern: blended)
        x ^= UInt64(bitPattern: userSeed) &* 0x9E37_79B9_7F4A_7C15
        x ^= UInt64(UInt32(truncatingIfNeeded: slot)) &* 0x85EB_CA6B
        x ^= UInt64(bitPattern: salt)
        x &+= UInt64(slot &+ 1) &<< 21
        var o = Int64(bitPattern: x)
        if o == 0 { o = -0x51F4_A94F_51F4_A94F }
        return o
    }

    private static func reverseBitsUInt64(_ x: UInt64) -> UInt64 {
        var x = x
        var r: UInt64 = 0
        for _ in 0 ..< 64 {
            r = (r << 1) | (x & 1)
            x >>= 1
        }
        return r
    }

    /// ≥3 distinct ids, 3–5 pieces; **no** size-label gating (real closets rarely track sizes consistently).
    private static func sanitizeDistinctOutfitPieces(_ pieces: [WardrobeItem]) -> [WardrobeItem]? {
        let u = pieces.uniqueStableById()
        guard u.count >= 3, u.count <= 5 else { return nil }
        guard Set(u.map(\.id)).count == u.count else { return nil }
        return u
    }

    /// True if the pool can form at least one dress-based or top+bottom+shoe silhouette with a third piece where needed.
    private static func poolHasFeasibleThreePieceOutfit(pool: [WardrobeItem]) -> Bool {
        var byCat: [String: [WardrobeItem]] = [:]
        for item in pool {
            byCat[item.category, default: []].append(item)
        }
        let tops = byCat[WardrobeCatalog.tops] ?? []
        let bottoms = byCat[WardrobeCatalog.bottoms] ?? []
        let dresses = byCat[WardrobeCatalog.dresses] ?? []
        let shoes = byCat[WardrobeCatalog.shoes] ?? []
        let outer = byCat[WardrobeCatalog.outerwear] ?? []
        let acc = byCat[WardrobeCatalog.accessories] ?? []

        if !dresses.isEmpty, !shoes.isEmpty, !(outer.isEmpty && acc.isEmpty) {
            return true
        }
        if !tops.isEmpty, !bottoms.isEmpty, !shoes.isEmpty {
            return true
        }
        return false
    }

    /// Strong ranking for occasion / season / tags / rotation; used for ordering and display score.
    private static func itemPickerRank(
        _ item: WardrobeItem,
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
        categoryTotalsInPool: [String: Int],
    ) -> Double {
        var r = 0.0
        if occasion.preferredCategories.contains(item.category) { r += 200 }
        let seasons = item.seasonsList
        if seasons.isEmpty || seasons.contains(where: { $0.caseInsensitiveCompare(seasonKey) == .orderedSame }) {
            r += 78
        }
        let hay = "\(item.name) \(item.colorName)".lowercased()
        for t in tagTerms where !t.isEmpty {
            if hay.contains(t) { r += 58 }
        }
        for w in weatherTagIds where !w.isEmpty {
            let low = w.lowercased()
            if hay.contains(low) { r += 96 }
        }
        for m in moodTagIds where !m.isEmpty {
            let low = m.lowercased()
            if hay.contains(low) { r += 96 }
        }
        r += tagKeywordBonus(item, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds)
        r += 72.0 / Double(1 + item.wornCount)
        if item.wornCount == 0 { r += 22 }
        else if let lw = item.lastWornAtEpochMs {
            let days = (nowEpochMs - lw) / dayMs
            if days >= 21 { r += 28 }
            else if days >= 8 { r += 16 }
            else if days >= 1 { r += 6 }
        }
        if let n = categoryTotalsInPool[item.category], n <= 3 { r += 14 }
        if tagTerms.contains("cold"), item.category == WardrobeCatalog.outerwear { r += 38 }
        if tagTerms.contains("rainy"), item.category == WardrobeCatalog.outerwear { r += 32 }
        let moodL = Set(moodTagIds.map { $0.lowercased() })
        if moodL.contains("cozy"), item.category == WardrobeCatalog.outerwear { r += 26 }
        if tagTerms.contains("bright"), isVividHex(item.colorHex) { r += 18 }
        if tagTerms.contains("minimal"), isNeutralHex(item.colorHex) { r += 18 }
        return r
    }

    /// Per id: number of **prior** suggested outfits in this run that already include the piece (drives diversity).
    private static func priorOutfitReuseCounts(_ priorOutfits: [[WardrobeItem]]) -> [String: Int] {
        var m: [String: Int] = [:]
        for outfit in priorOutfits {
            for id in Set(outfit.map(\.id)) {
                m[id, default: 0] += 1
            }
        }
        return m
    }

    private static func adjustedPickRank(
        _ item: WardrobeItem,
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
        categoryTotalsInPool: [String: Int],
        priorOutfitReuseCounts: [String: Int],
    ) -> Double {
        let base = itemPickerRank(item, occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool)
        let prior = priorOutfitReuseCounts[item.id, default: 0]
        var r = base - Double(prior) * 250
        // Strong stick after each prior outfit: deprioritize reuse even when multiplier alone would still leave “devseed” elites winning every slot.
        if prior > 0 { r -= 200 }
        return r
    }

    /// Full Fisher–Yates on the entire buffer (used for the top‑N ranked slice only).
    private static func fisherYatesShuffle(_ items: inout [WardrobeItem], gen: inout SeededGenerator) {
        let n = items.count
        guard n > 1 else { return }
        var i = n - 1
        while i > 0 {
            let j = Int(gen.next() % UInt64(i + 1))
            items.swapAt(i, j)
            i -= 1
        }
    }

    private static func pickRandomFromTopN(
        _ items: [WardrobeItem],
        excluding: Set<String>,
        topN: Int,
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
        categoryTotalsInPool: [String: Int],
        priorOutfitReuseCounts: [String: Int],
        gen: inout SeededGenerator,
    ) -> WardrobeItem? {
        let filtered = items.filter { !excluding.contains($0.id) }
        guard !filtered.isEmpty else { return nil }
        let ranked = filtered.sorted { a, b in
            let ra = adjustedPickRank(a, occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts)
            let rb = adjustedPickRank(b, occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts)
            if ra != rb { return ra > rb }
            return a.id < b.id
        }
        let take = min(topN, ranked.count)
        guard take > 0 else { return nil }
        var elite = Array(ranked.prefix(take))
        fisherYatesShuffle(&elite, gen: &gen)
        let idx = Int(gen.next() % UInt64(take))
        return elite[idx]
    }

    /// Reject near-duplicate outfits (≥3 shared pieces). Two shared items alone is too strict for coherent
    /// closets where the same shoes/belt/bag legitimately complete different top+bottom looks.
    private static func overlapAtLeastThreeWithAnyPrior(_ ids: Set<String>, _ prior: [[WardrobeItem]]) -> Bool {
        for p in prior {
            if ids.intersection(Set(p.map(\.id))).count >= 3 { return true }
        }
        return false
    }

    /// Same shirt(s) + bottom(s) as a prior look (Android `sameTopBottomSilhouette`); still block lazy repeats.
    private static func sameTopBottomSilhouette(_ a: [WardrobeItem], _ b: [WardrobeItem]) -> Bool {
        let ta = Set(a.filter { $0.category == WardrobeCatalog.tops }.map(\.id))
        let ba = Set(a.filter { $0.category == WardrobeCatalog.bottoms }.map(\.id))
        let tb = Set(b.filter { $0.category == WardrobeCatalog.tops }.map(\.id))
        let bb = Set(b.filter { $0.category == WardrobeCatalog.bottoms }.map(\.id))
        if ta.isEmpty || ba.isEmpty || tb.isEmpty || bb.isEmpty { return false }
        return ta == tb && ba == bb
    }

    private static func piecesHaveDistinctIds(_ pieces: [WardrobeItem]) -> Bool {
        pieces.count == Set(pieces.map(\.id)).count
    }

    private static func tryBuildDressSilhouette(
        dresses: [WardrobeItem],
        shoes: [WardrobeItem],
        outer: [WardrobeItem],
        acc: [WardrobeItem],
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
        categoryTotalsInPool: [String: Int],
        priorOutfitReuseCounts: [String: Int],
        gen: inout SeededGenerator,
    ) -> [WardrobeItem]? {
        guard let dress = pickRandomFromTopN(
            dresses, excluding: [], topN: 14,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) else { return nil }
        var usedIds = Set([dress.id])
        guard let shoe = pickRandomFromTopN(
            shoes, excluding: usedIds, topN: 14,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) else { return nil }
        usedIds.insert(shoe.id)
        let thirdPool = outer + acc
        guard let extra = pickRandomFromTopN(
            thirdPool, excluding: usedIds, topN: 14,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) else { return nil }
        usedIds.insert(extra.id)
        var pieces = [dress, shoe, extra]
        let roll = Int(gen.next() % 100)
        if pieces.count < 5 {
            if roll < 38, let o = pickRandomFromTopN(
                outer, excluding: usedIds, topN: 10,
                occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
                nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
            ) {
                pieces.append(o)
                usedIds.insert(o.id)
            } else if roll < 76, let a = pickRandomFromTopN(
                acc, excluding: usedIds, topN: 10,
                occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
                nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
            ) {
                pieces.append(a)
                usedIds.insert(a.id)
            }
        }
        guard piecesHaveDistinctIds(pieces) else { return nil }
        return pieces
    }

    private static func tryBuildTopBottomSilhouette(
        tops: [WardrobeItem],
        bottoms: [WardrobeItem],
        shoes: [WardrobeItem],
        outer: [WardrobeItem],
        acc: [WardrobeItem],
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
        categoryTotalsInPool: [String: Int],
        priorOutfitReuseCounts: [String: Int],
        gen: inout SeededGenerator,
    ) -> [WardrobeItem]? {
        guard let top = pickRandomFromTopN(
            tops, excluding: [], topN: 14,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) else { return nil }
        var usedIds = Set([top.id])
        guard let bottom = pickRandomFromTopN(
            bottoms, excluding: usedIds, topN: 14,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) else { return nil }
        usedIds.insert(bottom.id)
        guard let shoe = pickRandomFromTopN(
            shoes, excluding: usedIds, topN: 14,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) else { return nil }
        usedIds.insert(shoe.id)
        var pieces = [top, bottom, shoe]
        if pieces.count < 5, Int(gen.next() % 100) < 44, let o = pickRandomFromTopN(
            outer, excluding: usedIds, topN: 10,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) {
            pieces.append(o)
            usedIds.insert(o.id)
        }
        if pieces.count < 5, Int(gen.next() % 100) < 44, let a = pickRandomFromTopN(
            acc, excluding: usedIds, topN: 10,
            occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool, priorOutfitReuseCounts: priorOutfitReuseCounts, gen: &gen,
        ) {
            pieces.append(a)
            usedIds.insert(a.id)
        }
        guard piecesHaveDistinctIds(pieces) else { return nil }
        return pieces
    }

    private static func outfitHeuristicScore(
        _ pieces: [WardrobeItem],
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
        categoryTotalsInPool: [String: Int],
    ) -> Double {
        var s = pieces.map {
            itemPickerRank($0, occasion: occasion, tagTerms: tagTerms, seasonKey: seasonKey, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs, categoryTotalsInPool: categoryTotalsInPool)
        }.reduce(0, +)
        s += Double(Set(pieces.map(\.category)).count) * 5
        let mains = pieces.filter {
            $0.category == WardrobeCatalog.tops ||
                $0.category == WardrobeCatalog.bottoms ||
                $0.category == WardrobeCatalog.dresses
        }
        if mains.count >= 2,
           let c0 = parseRgb(mains[0].colorHex),
           let c1 = parseRgb(mains[1].colorHex) {
            let lumDiff = abs(c0.luminance - c1.luminance)
            if (0.10...0.52).contains(lumDiff) { s += 14 }
            s += hueHarmonyBonus(c0, c1)
        }
        return s
    }

    static func suggest(
        allItems: [WardrobeItem],
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        seed: Int64,
        maxOutfits: Int = 3,
        nowEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
    ) -> PickerSuggestOutcome {
        let occasion = occasions.first { $0.id == occasionId } ?? occasions[0]
        let month = Calendar.current.component(.month, from: Date())
        let season = currentSeasonKey(month1Based: month)
        let seasonPool = expandedSeasonKeys(calendarSeason: season, weatherTagIds: weatherTagIds)
        let tagTerms = Set((weatherTagIds.union(moodTagIds)).map { $0.lowercased() })

        let pool = allItems.filter { item in
            let seasons = item.seasonsList
            return seasons.isEmpty || seasons.contains { seasonPool.contains($0.lowercased()) }
        }

        func outcomeInsufficientOnly() -> PickerSuggestOutcome {
            let feasibleInPool = pool.count >= 3 && poolHasFeasibleThreePieceOutfit(pool: pool)
            let o = PickerSuggestOutcome(
                suggestions: [],
                shouldShowInsufficientVarietyMessage: !allItems.isEmpty && (pool.count < 3 || !feasibleInPool),
            )
            pickerDebugLog("Final suggestions count: \(o.suggestions.count), insufficient message: \(o.shouldShowInsufficientVarietyMessage)")
            return o
        }

        guard pool.count >= 3 else { return outcomeInsufficientOnly() }
        pickerDebugLog("Initial pool size: \(pool.count)")

        let blended = blendPickerSeed(
            seed: seed,
            occasionId: occasionId,
            weatherTagIds: weatherTagIds,
            moodTagIds: moodTagIds,
            nowEpochMs: nowEpochMs,
        )
        var byCat: [String: [WardrobeItem]] = [:]
        for item in pool {
            byCat[item.category, default: []].append(item)
        }

        let tops = byCat[WardrobeCatalog.tops] ?? []
        let bottoms = byCat[WardrobeCatalog.bottoms] ?? []
        let dresses = byCat[WardrobeCatalog.dresses] ?? []
        let shoes = byCat[WardrobeCatalog.shoes] ?? []
        let outer = byCat[WardrobeCatalog.outerwear] ?? []
        let acc = byCat[WardrobeCatalog.accessories] ?? []

        var categoryTotalsInPool: [String: Int] = [:]
        for item in pool {
            categoryTotalsInPool[item.category, default: 0] += 1
        }

        let canDressSilhouette = !dresses.isEmpty && !shoes.isEmpty && (!outer.isEmpty || !acc.isEmpty)
        let canTopBottomSilhouette = !tops.isEmpty && !bottoms.isEmpty && !shoes.isEmpty
        guard canDressSilhouette || canTopBottomSilhouette else {
            return outcomeInsufficientOnly()
        }

        let dressRouteBiasBase: Double = {
            switch occasion.id {
            case "formal", "date_night": return 0.58
            case "gym": return 0.07
            default: return 0.36
            }
        }()
        let weatherL = Set(weatherTagIds.map { $0.lowercased() })
        let moodL = Set(moodTagIds.map { $0.lowercased() })
        var dressRouteBias = dressRouteBiasBase
        if weatherL.contains("cold") || weatherL.contains("rainy") || weatherL.contains("windy") {
            dressRouteBias -= 0.10
        }
        if moodL.contains("cozy") || moodL.contains("relaxed") {
            dressRouteBias -= 0.08
        }
        if moodL.contains("bold") || moodL.contains("polished") {
            dressRouteBias += 0.10
        }
        if !weatherL.isEmpty || !moodL.isEmpty {
            dressRouteBias += 0.05
        }
        dressRouteBias = min(max(dressRouteBias, 0.05), 0.82)

        var suggestions: [PickerSuggestion] = []
        var builtPieces: [[WardrobeItem]] = []
        var usedOutfitKeys = Set<String>()

        // Diverse outfit selection: `priorOutfitReuseCounts(builtPieces)` + `adjustedPickRank` penalize items from prior looks (this run).
        for slot in 0 ..< maxOutfits {
            let reuseForPicks = priorOutfitReuseCounts(builtPieces)
            var slotGen = SeededGenerator(seed: streamSeed(blended: blended, userSeed: seed, slot: slot, salt: xorMaskPick))
            var outfit: [WardrobeItem]?
            var rejOverlap = 0
            var rejSilhouette = 0
            var rejDupKey = 0
            var rejSanitize = 0
            var rejPostSanDup = 0
            for _ in 0 ..< pickerAttemptsPerOutfitSlot {
                let useDress: Bool
                if canDressSilhouette && canTopBottomSilhouette {
                    let r = Double(Int64(bitPattern: slotGen.next()) % 10_000) / 10_000.0
                    useDress = r < dressRouteBias
                } else {
                    useDress = canDressSilhouette
                }
                let candidate: [WardrobeItem]?
                if useDress {
                    candidate = tryBuildDressSilhouette(
                        dresses: dresses,
                        shoes: shoes,
                        outer: outer,
                        acc: acc,
                        occasion: occasion,
                        tagTerms: tagTerms,
                        seasonKey: season,
                        weatherTagIds: weatherTagIds,
                        moodTagIds: moodTagIds,
                        nowEpochMs: nowEpochMs,
                        categoryTotalsInPool: categoryTotalsInPool,
                        priorOutfitReuseCounts: reuseForPicks,
                        gen: &slotGen,
                    )
                } else {
                    candidate = tryBuildTopBottomSilhouette(
                        tops: tops,
                        bottoms: bottoms,
                        shoes: shoes,
                        outer: outer,
                        acc: acc,
                        occasion: occasion,
                        tagTerms: tagTerms,
                        seasonKey: season,
                        weatherTagIds: weatherTagIds,
                        moodTagIds: moodTagIds,
                        nowEpochMs: nowEpochMs,
                        categoryTotalsInPool: categoryTotalsInPool,
                        priorOutfitReuseCounts: reuseForPicks,
                        gen: &slotGen,
                    )
                }
                guard let raw = candidate,
                      let sanitized = sanitizeDistinctOutfitPieces(raw) else {
                    rejSanitize += 1
                    continue
                }
                let sanIds = sanitized.map(\.id)
                guard Set(sanIds).count == sanIds.count else {
                    rejPostSanDup += 1
                    pickerDebugLog("Reject: internal duplicate ids post-sanitize, ids=\(sanIds)")
                    continue
                }
                let key = sanIds.sorted().joined(separator: ",")
                guard !usedOutfitKeys.contains(key) else {
                    rejDupKey += 1
                    continue
                }
                let idSet = Set(sanIds)
                if overlapAtLeastThreeWithAnyPrior(idSet, builtPieces) {
                    rejOverlap += 1
                    continue
                }
                if builtPieces.contains(where: { sameTopBottomSilhouette(sanitized, $0) }) {
                    rejSilhouette += 1
                    continue
                }
                outfit = sanitized
                break
            }
            guard let finalPieces = outfit else {
                pickerDebugLog(
                    "Slot \(slot + 1): no outfit after \(pickerAttemptsPerOutfitSlot) attempts (prior=\(builtPieces.count)); rejects — overlap≥3: \(rejOverlap), same top+bottom: \(rejSilhouette), dup outfit key: \(rejDupKey), no valid build/sanitize: \(rejSanitize), internal dup post-sanitize: \(rejPostSanDup)",
                )
                break
            }
            let key = finalPieces.map(\.id).sorted().joined(separator: ",")
            usedOutfitKeys.insert(key)
            builtPieces.append(finalPieces)
            let selIds = finalPieces.map(\.id).sorted()
            pickerDebugLog("Selected outfit #\(suggestions.count + 1): ids = \(selIds), pieces = \(finalPieces.count)")
            let display = finalPieces.sorted { displayOrder($0.category) < displayOrder($1.category) }
            let score = outfitHeuristicScore(
                display,
                occasion: occasion,
                tagTerms: tagTerms,
                seasonKey: season,
                weatherTagIds: weatherTagIds,
                moodTagIds: moodTagIds,
                nowEpochMs: nowEpochMs,
                categoryTotalsInPool: categoryTotalsInPool,
            )
            suggestions.append(
                PickerSuggestion(
                    title: "\(occasion.label) · Look \(suggestions.count + 1)",
                    items: display,
                    score: score,
                    reason: buildReason(display, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs),
                ),
            )
        }

        if suggestions.isEmpty {
            pickerDebugLog("simple suggest: no outfits built (pool=\(pool.count))")
            return outcomeInsufficientOnly()
        }

        let finalSets = builtPieces.map { "[" + $0.map(\.id).sorted().joined(separator: ", ") + "]" }.joined(separator: ", ")
        pickerDebugLog("Final selected outfits: [\(finalSets)]")

        let insufficientVariety = false
        pickerDebugLog("Final suggestions count: \(suggestions.count), insufficient message: \(insufficientVariety)")
        return PickerSuggestOutcome(suggestions: suggestions, shouldShowInsufficientVarietyMessage: insufficientVariety)
    }

    private static func displayOrder(_ category: String) -> Int {
        switch category {
        case WardrobeCatalog.tops, WardrobeCatalog.dresses: return 0
        case WardrobeCatalog.bottoms: return 1
        case WardrobeCatalog.outerwear: return 2
        case WardrobeCatalog.shoes: return 3
        case WardrobeCatalog.accessories: return 4
        default: return 5
        }
    }

    private static func tagKeywordBonus(_ p: WardrobeItem, weatherTagIds: Set<String>, moodTagIds: Set<String>) -> Double {
        var b = 0.0
        let hay = "\(p.name) \(p.colorName)".lowercased()
        let w = Set(weatherTagIds.map { $0.lowercased() })
        let m = Set(moodTagIds.map { $0.lowercased() })
        if w.contains("sunny"), hay.contains("linen") || hay.contains("light") || hay.contains("breath") { b += 8 }
        if w.contains("rainy"), hay.contains("nylon") || hay.contains("water") || hay.contains("shell") { b += 8 }
        if w.contains("hot"), hay.contains("linen") || hay.contains("cotton") || hay.contains("mesh") || hay.contains("tank") { b += 8 }
        if w.contains("cold"), hay.contains("wool") || hay.contains("fleece") || hay.contains("down") || hay.contains("thermal") { b += 8 }
        if m.contains("cozy"), hay.contains("knit") || hay.contains("wool") || hay.contains("fleece") { b += 7 }
        return b
    }

    private static func buildReason(
        _ pieces: [WardrobeItem],
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
    ) -> String {
        var parts: [String] = []
        let unworn = pieces.filter { $0.wornCount == 0 }.count
        if unworn > 0 {
            parts.append(unworn == 1 ? "One piece never logged as worn here" : "\(unworn) pieces never logged as worn")
        }
        let daySpans = pieces.compactMap { p -> Int64? in
            guard let lw = p.lastWornAtEpochMs else { return nil }
            return (nowEpochMs - lw) / dayMs
        }
        if !daySpans.isEmpty {
            let minD = daySpans.min()!
            let maxD = daySpans.max()!
            if minD >= 14 {
                parts.append("Rested rotation (up to \(maxD) days since last wear)")
            } else if minD >= 7 {
                parts.append("Hasn’t been worn in at least a week")
            } else if minD >= 1 {
                parts.append("Mix of recently worn and fresh picks")
            }
        } else if unworn == 0, pieces.allSatisfy({ $0.wornCount > 0 && $0.lastWornAtEpochMs == nil }) {
            parts.append("Uses staples (use Wear today to track last-worn dates)")
        }
        let cats = Set(pieces.map(\.category)).count
        if cats >= 3 { parts.append("Good category variety") }
        parts.append(harmonyLabel(pieces))
        let w = Set(weatherTagIds.map { $0.lowercased() })
        if !w.isEmpty { parts.append("Weather-aware wardrobe filter") }
        let trimmed = Array(Set(parts.filter { !$0.isEmpty })).prefix(2)
        return trimmed.joined(separator: " · ").isEmpty ? "Balanced for this occasion and season" : trimmed.joined(separator: " · ")
    }

    private static func harmonyLabel(_ pieces: [WardrobeItem]) -> String {
        let mains = pieces.filter {
            $0.category == WardrobeCatalog.tops ||
                $0.category == WardrobeCatalog.bottoms ||
                $0.category == WardrobeCatalog.dresses
        }
        guard mains.count >= 2,
              let c0 = parseRgb(mains[0].colorHex),
              let c1 = parseRgb(mains[1].colorHex) else { return "Cohesive look" }
        let lumDiff = abs(c0.luminance - c1.luminance)
        let hb = hueHarmonyBonus(c0, c1)
        if hb >= 8 && lumDiff >= 0.08 { return "Strong color harmony (contrast + hue)" }
        if hb >= 6 { return "Complementary-style colors" }
        if (0.10...0.52).contains(lumDiff) { return "Pleasing light–dark balance" }
        if lumDiff < 0.07 { return "Tonal / monochrome lean" }
        return "Balanced palette"
    }

    private struct RGB {
        let r: Int, g: Int, b: Int
        var luminance: Double {
            let rr = Double(r) / 255, gg = Double(g) / 255, bb = Double(b) / 255
            return 0.2126 * rr + 0.7152 * gg + 0.0722 * bb
        }
    }

    private static func hueDegrees(_ c: RGB) -> Double? {
        let r = Double(c.r) / 255
        let g = Double(c.g) / 255
        let b = Double(c.b) / 255
        let maxC = max(r, g, b)
        let minC = min(r, g, b)
        let d = maxC - minC
        if d < 1e-3 { return nil }
        let h: Double = {
            if maxC == r { return ((g - b) / d).truncatingRemainder(dividingBy: 6) }
            if maxC == g { return (b - r) / d + 2 }
            return (r - g) / d + 4
        }()
        var deg = h * 60
        if deg < 0 { deg += 360 }
        return deg
    }

    private static func hueHarmonyBonus(_ c0: RGB, _ c1: RGB) -> Double {
        guard let h0 = hueDegrees(c0), let h1 = hueDegrees(c1) else { return 0 }
        var diff = abs(h0 - h1)
        if diff > 180 { diff = 360 - diff }
        if diff < 28 { return 5 }
        if (75...108).contains(diff) { return 11 }
        if (140...180).contains(diff) { return 7 }
        return 0
    }

    private static func parseRgb(_ hex: String) -> RGB? {
        var h = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if h.hasPrefix("#") { h.removeFirst() }
        guard h.count == 6, let v = UInt64(h, radix: 16) else { return nil }
        let r = Int((v >> 16) & 0xFF)
        let g = Int((v >> 8) & 0xFF)
        let b = Int(v & 0xFF)
        return RGB(r: r, g: g, b: b)
    }

    private static func isNeutralHex(_ hex: String) -> Bool {
        let n = hex.lowercased()
        return n.contains("f5f5") || n.contains("2c2c") || n.contains("a0a0") || n.contains("8b6e")
            || n.contains("c4a88") || n.contains("808080")
    }

    private static func isVividHex(_ hex: String) -> Bool {
        guard let c = parseRgb(hex) else { return false }
        let r = Double(c.r) / 255, g = Double(c.g) / 255, b = Double(c.b) / 255
        let maxC = max(r, g, b)
        let minC = min(r, g, b)
        let sat = maxC <= 1e-6 ? 0 : (maxC - minC) / maxC
        return sat > 0.35 && maxC > 0.45
    }

    private struct SeededGenerator: RandomNumberGenerator {
        private var state: UInt64
        init(seed: Int64) {
            state = UInt64(bitPattern: seed)
            if state == 0 { state = 0xDEADBEEF }
        }
        mutating func next() -> UInt64 {
            state = state &* 6_364_136_223_846_793_005 &+ 1
            return state
        }
    }
}

private extension Array where Element == WardrobeItem {
    func uniqueStableById() -> [WardrobeItem] {
        var seen = Set<String>()
        var r: [WardrobeItem] = []
        for p in self {
            if seen.insert(p.id).inserted { r.append(p) }
        }
        return r
    }
}
