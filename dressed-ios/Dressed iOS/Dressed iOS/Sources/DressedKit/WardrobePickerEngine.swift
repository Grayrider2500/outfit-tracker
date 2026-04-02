import Foundation

/// Rule-based outfit suggestions (Phase 1). Kept in sync with Android `WardrobePickerEngine`.
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
        var itemIds: [String] { items.map(\.id) }
    }

    static func currentSeasonKey(month1Based: Int) -> String {
        switch month1Based {
        case 12, 1, 2: return "winter"
        case 3...5: return "spring"
        case 6...8: return "summer"
        default: return "fall"
        }
    }

    static func suggest(
        allItems: [WardrobeItem],
        occasionId: String,
        weatherTagIds: Set<String>,
        moodTagIds: Set<String>,
        seed: Int64,
        maxOutfits: Int = 3,
    ) -> [PickerSuggestion] {
        let occasion = occasions.first { $0.id == occasionId } ?? occasions[0]
        let month = Calendar.current.component(.month, from: Date())
        let season = currentSeasonKey(month1Based: month)
        let tagTerms = Set((weatherTagIds.union(moodTagIds)).map { $0.lowercased() })

        let pool = allItems.filter { item in
            let seasons = item.seasonsList
            return seasons.isEmpty || seasons.contains { $0.caseInsensitiveCompare(season) == .orderedSame }
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

        func sample<T>(_ arr: [T], _ n: Int) -> [T] { Array(arr.shuffled(using: &gen).prefix(n)) }

        let rngTops = sample(tops, 10)
        let rngBottoms = sample(bottoms, 10)
        let rngDresses = sample(dresses, 8)
        let rngShoes = sample(shoes, 8)
        let rngOuter = sample(outer, 6)
        let rngAcc = sample(acc, 6)

        var candidates: [(pieces: [WardrobeItem], score: Double)] = []

        func addCandidate(_ pieces: [WardrobeItem]) {
            guard !pieces.isEmpty, sizesCoherent(pieces) else { return }
            let sc = scoreOutfit(pieces, occasion: occasion, tagTerms: tagTerms, seasonKey: season)
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
        for pair in sorted {
            if picked.count >= maxOutfits * 8 { break }
            let ids = Set(pair.pieces.map(\.id))
            let tooSimilar = picked.contains { prev in
                jaccard(ids, Set(prev.pieces.map(\.id))) > 0.55
            }
            if !tooSimilar { picked.append(pair) }
            if picked.count >= maxOutfits * 4 { break }
        }

        let topPick = picked.isEmpty ? Array(sorted.prefix(maxOutfits * 2)) : picked
        var unique: [(pieces: [WardrobeItem], score: Double)] = []
        var seen: Set<String> = []
        for p in topPick.sorted(by: { $0.score > $1.score }) {
            let key = p.pieces.map(\.id).sorted().joined(separator: ",")
            if seen.contains(key) { continue }
            seen.insert(key)
            unique.append(p)
            if unique.count >= maxOutfits { break }
        }

        let result = unique.enumerated().map { idx, row in
            PickerSuggestion(
                title: "\(occasion.label) · Look \(idx + 1)",
                items: row.pieces.sorted { displayOrder($0.category) < displayOrder($1.category) },
                score: row.score,
            )
        }

        if result.isEmpty, let first = sorted.first {
            return [
                PickerSuggestion(
                    title: "\(occasion.label) · Look 1",
                    items: first.pieces.sorted { displayOrder($0.category) < displayOrder($1.category) },
                    score: first.score,
                ),
            ]
        }
        return result
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

    private static func scoreOutfit(
        _ pieces: [WardrobeItem],
        occasion: Occasion,
        tagTerms: Set<String>,
        seasonKey: String,
    ) -> Double {
        var s = 0.0
        for p in pieces {
            s += 72.0 / Double(1 + p.wornCount)
            let seasons = p.seasonsList
            if seasons.isEmpty || seasons.contains(where: { $0.caseInsensitiveCompare(seasonKey) == .orderedSame }) {
                s += 10
            }
            if occasion.preferredCategories.contains(p.category) { s += 8 }
            let hay = "\(p.name) \(p.colorName)".lowercased()
            for t in tagTerms where !t.isEmpty {
                if hay.contains(t) { s += 5 }
            }
            if tagTerms.contains("cold"), p.category == WardrobeCatalog.outerwear { s += 12 }
            if tagTerms.contains("rainy"), p.category == WardrobeCatalog.outerwear { s += 10 }
            if tagTerms.contains("cozy"), p.category == WardrobeCatalog.outerwear { s += 8 }
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
        }

        s += Double(Set(pieces.map(\.category)).count) * 1.5
        return s
    }

    private struct RGB { let r: Int, g: Int, b: Int; var luminance: Double {
        let rr = Double(r) / 255, gg = Double(g) / 255, bb = Double(b) / 255
        return 0.2126 * rr + 0.7152 * gg + 0.0722 * bb
    } }

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
            state = state &* 6364136223846793005 &+ 1
            return state
        }
    }
}
