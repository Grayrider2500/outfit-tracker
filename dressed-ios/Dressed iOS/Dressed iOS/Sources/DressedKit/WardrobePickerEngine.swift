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

    private static let xorMask: Int64 = 0x51F4A94F51F4A94F
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

    static func suggest(
        allItems: [WardrobeItem],
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        seed: Int64,
        maxOutfits: Int = 3,
        nowEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
    ) -> [PickerSuggestion] {
        let occasion = occasions.first { $0.id == occasionId } ?? occasions[0]
        let month = Calendar.current.component(.month, from: Date())
        let season = currentSeasonKey(month1Based: month)
        let seasonPool = expandedSeasonKeys(calendarSeason: season, weatherTagIds: weatherTagIds)
        let tagTerms = Set((weatherTagIds.union(moodTagIds)).map { $0.lowercased() })

        let pool = allItems.filter { item in
            let seasons = item.seasonsList
            return seasons.isEmpty || seasons.contains { seasonPool.contains($0.lowercased()) }
        }
        guard pool.count >= 2 else { return [] }

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

        var gen = SeededGenerator(seed: seed)
        var gen2 = SeededGenerator(seed: seed ^ xorMask)

        func stratified(_ items: [WardrobeItem], _ n: Int) -> [WardrobeItem] {
            if items.count <= n { return items.shuffled(using: &gen) }
            let half = (n + 1) / 2
            let freshFirst = items.sorted { rotationKey($0, nowEpochMs) < rotationKey($1, nowEpochMs) }
            let fromFresh = Array(freshFirst.prefix(half))
            let freshIds = Set(fromFresh.map(\.id))
            var remaining = items.filter { !freshIds.contains($0.id) }
            if remaining.isEmpty { remaining = items }
            let need = max(0, n - fromFresh.count)
            let fromRand = Array(remaining.shuffled(using: &gen2).prefix(need))
            var combined = fromFresh + fromRand
            var seen = Set<String>()
            combined = combined.filter { seen.insert($0.id).inserted }
            return Array(combined.prefix(n)).shuffled(using: &gen)
        }

        let rngTops = stratified(tops, 12)
        let rngBottoms = stratified(bottoms, 12)
        let rngDresses = stratified(dresses, 9)
        let rngShoes = stratified(shoes, 9)
        let rngOuter = stratified(outer, 7)
        let rngAcc = stratified(acc, 7)

        var candidates: [(pieces: [WardrobeItem], score: Double)] = []

        func addCandidate(_ pieces: [WardrobeItem]) {
            guard !pieces.isEmpty, sizesCoherent(pieces) else { return }
            let sc = scoreOutfit(
                pieces,
                occasion: occasion,
                tagTerms: tagTerms,
                seasonKey: season,
                weatherTagIds: weatherTagIds,
                moodTagIds: moodTagIds,
                nowEpochMs: nowEpochMs,
            )
            candidates.append((pieces, sc))
        }

        for d in rngDresses {
            for sh in ([Optional<WardrobeItem>.none] + rngShoes.map { Optional($0) }) {
                for ac in ([Optional<WardrobeItem>.none] + rngAcc.map { Optional($0) }) {
                    var list = [d]
                    if let sh { list.append(sh) }
                    if let ac { list.append(ac) }
                    addCandidate(list)
                }
            }
        }

        for t in rngTops {
            for b in rngBottoms {
                for sh in ([Optional<WardrobeItem>.none] + rngShoes.map { Optional($0) }) {
                    for ow in ([Optional<WardrobeItem>.none] + rngOuter.map { Optional($0) }) {
                        for ac in ([Optional<WardrobeItem>.none] + rngAcc.map { Optional($0) }) {
                            var list = [t, b]
                            if let sh { list.append(sh) }
                            if let ow { list.append(ow) }
                            if let ac { list.append(ac) }
                            addCandidate(Array(list.prefix(4)))
                        }
                    }
                }
            }
        }

        guard !candidates.isEmpty else { return [] }

        let sorted = candidates.sorted { $0.score > $1.score }
        var picked: [(pieces: [WardrobeItem], score: Double)] = []
        let similarityCap = 0.48
        for pair in sorted {
            if picked.count >= maxOutfits * 10 { break }
            let ids = Set(pair.pieces.map(\.id))
            let tooSimilar = picked.contains { prev in
                jaccard(ids, Set(prev.pieces.map(\.id))) > similarityCap
            }
            if !tooSimilar { picked.append(pair) }
            if picked.count >= maxOutfits * 5 { break }
        }

        let topPick = picked.isEmpty ? Array(sorted.prefix(maxOutfits * 3)) : picked
        var unique: [(pieces: [WardrobeItem], score: Double)] = []
        var seen: Set<String> = []
        for p in topPick.sorted(by: { $0.score > $1.score }) {
            let key = p.pieces.map(\.id).sorted().joined(separator: ",")
            if seen.contains(key) { continue }
            seen.insert(key)
            unique.append(p)
            if unique.count >= maxOutfits { break }
        }

        let mapped: [PickerSuggestion] = unique.enumerated().map { idx, row in
            let ordered = row.pieces.sorted { displayOrder($0.category) < displayOrder($1.category) }
            return PickerSuggestion(
                title: "\(occasion.label) · Look \(idx + 1)",
                items: ordered,
                score: row.score,
                reason: buildReason(ordered, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs),
            )
        }

        if mapped.isEmpty, let first = sorted.first {
            let ordered = first.pieces.sorted { displayOrder($0.category) < displayOrder($1.category) }
            return [
                PickerSuggestion(
                    title: "\(occasion.label) · Look 1",
                    items: ordered,
                    score: first.score,
                    reason: buildReason(ordered, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds, nowEpochMs: nowEpochMs),
                ),
            ]
        }
        return mapped
    }

    private static func rotationKey(_ item: WardrobeItem, _ now: Int64) -> Int64 {
        if item.wornCount == 0 { return .min }
        guard let lw = item.lastWornAtEpochMs else { return now - 60 * dayMs }
        return lw
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

    private static func jaccard(_ a: Set<String>, _ b: Set<String>) -> Double {
        if a.isEmpty && b.isEmpty { return 0 }
        let inter = a.intersection(b).count
        let uni = a.union(b).count
        return Double(inter) / Double(uni)
    }

    private static func sizesCoherent(_ pieces: [WardrobeItem]) -> Bool {
        let sizes = Set(
            pieces.map { $0.sizeLabel.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
                .map { $0.uppercased() },
        )
        return sizes.count <= 1
    }

    private static func tagKeywordBonus(_ p: WardrobeItem, weatherTagIds: Set<String>, moodTagIds: Set<String>) -> Double {
        var b = 0.0
        let hay = "\(p.name) \(p.colorName)".lowercased()
        let w = Set(weatherTagIds.map { $0.lowercased() })
        let m = Set(moodTagIds.map { $0.lowercased() })
        if w.contains("sunny"), hay.contains("linen") || hay.contains("light") || hay.contains("breath") { b += 4 }
        if w.contains("rainy"), hay.contains("nylon") || hay.contains("water") || hay.contains("shell") { b += 4 }
        if w.contains("hot"), hay.contains("linen") || hay.contains("cotton") || hay.contains("mesh") || hay.contains("tank") { b += 4 }
        if w.contains("cold"), hay.contains("wool") || hay.contains("fleece") || hay.contains("down") || hay.contains("thermal") { b += 4 }
        if m.contains("cozy"), hay.contains("knit") || hay.contains("wool") || hay.contains("fleece") { b += 3 }
        return b
    }

    private static func scoreOutfit(
        _ pieces: [WardrobeItem],
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        nowEpochMs: Int64,
    ) -> Double {
        var s = 0.0
        let moodLower = Set(moodTagIds.map { $0.lowercased() })
        for p in pieces {
            s += 78.0 / Double(1 + p.wornCount)
            let seasons = p.seasonsList
            if seasons.isEmpty || seasons.contains(where: { $0.caseInsensitiveCompare(seasonKey) == .orderedSame }) {
                s += 10
            }
            if occasion.preferredCategories.contains(p.category) { s += 8 }
            let hay = "\(p.name) \(p.colorName)".lowercased()
            for t in tagTerms where !t.isEmpty {
                if hay.contains(t) { s += 5 }
            }
            s += tagKeywordBonus(p, weatherTagIds: weatherTagIds, moodTagIds: moodTagIds)
            if p.wornCount == 0 {
                s += 14
            } else if let lw = p.lastWornAtEpochMs {
                let days = (nowEpochMs - lw) / dayMs
                if days >= 21 { s += 18 }
                else if days >= 8 { s += 12 }
                else if days >= 1 { s += 4 }
            }
            if (3...9).contains(p.wornCount) { s += 5 }
            if tagTerms.contains("cold"), p.category == WardrobeCatalog.outerwear { s += 12 }
            if tagTerms.contains("rainy"), p.category == WardrobeCatalog.outerwear { s += 10 }
            if moodLower.contains("cozy"), p.category == WardrobeCatalog.outerwear { s += 8 }
            if tagTerms.contains("bright"), isVividHex(p.colorHex) { s += 6 }
            if tagTerms.contains("minimal"), isNeutralHex(p.colorHex) { s += 6 }
        }

        let mainPieces = pieces.filter {
            $0.category == WardrobeCatalog.tops ||
                $0.category == WardrobeCatalog.bottoms ||
                $0.category == WardrobeCatalog.dresses
        }
        if mainPieces.count >= 2,
           let c0 = parseRgb(mainPieces[0].colorHex),
           let c1 = parseRgb(mainPieces[1].colorHex) {
            let lumDiff = abs(c0.luminance - c1.luminance)
            if (0.10...0.52).contains(lumDiff) { s += 16 }
            if lumDiff < 0.07 { s -= 8 }
            s += hueHarmonyBonus(c0, c1)
        }

        s += Double(Set(pieces.map(\.category)).count) * 2.0
        return s
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
