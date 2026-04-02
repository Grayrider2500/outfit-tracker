import Foundation
import SwiftData

/// Deterministic dev wardrobe + outfits (matches Android `TestDataSeeder`). Debug builds only from UI.
enum DevTestDataSeeder {
    static let itemIdPrefix = "devseed-item-"
    static let outfitIdPrefix = "devseed-outfit-"

    private struct Swatch {
        let hex: String
        let name: String
    }

    private static let palette: [Swatch] = [
        Swatch(hex: "#F5F5F0", name: "White"),
        Swatch(hex: "#2C2C2C", name: "Black"),
        Swatch(hex: "#8B6E5A", name: "Brown"),
        Swatch(hex: "#C4A882", name: "Tan"),
        Swatch(hex: "#6B7FA3", name: "Blue"),
        Swatch(hex: "#8FA68C", name: "Green"),
        Swatch(hex: "#C4788A", name: "Pink"),
        Swatch(hex: "#B05C3A", name: "Orange"),
        Swatch(hex: "#7A5C8A", name: "Purple"),
        Swatch(hex: "#C4B820", name: "Yellow"),
        Swatch(hex: "#A0A0A0", name: "Gray"),
        Swatch(hex: "#C42B2B", name: "Red"),
    ]

    private static let categories = [
        WardrobeCatalog.tops,
        WardrobeCatalog.bottoms,
        WardrobeCatalog.dresses,
        WardrobeCatalog.shoes,
        WardrobeCatalog.outerwear,
        WardrobeCatalog.accessories,
    ]

    private static let topNames = ["Merino Crewneck", "Oxford Shirt", "Linen Tee", "Cashmere Sweater", "Poplin Shirt", "Henley"]
    private static let bottomNames = ["Slim Chinos", "Dark Jeans", "Pleated Trousers", "Cotton Shorts", "Wool Slacks", "Cargo Pants"]
    private static let dressNames = ["Midi Wrap Dress", "Shirt Dress", "Knit Dress", "Slip Dress", "Sweater Dress"]
    private static let shoeNames = ["Leather Sneakers", "Loafers", "Ankle Boots", "Running Trainers", "Chelsea Boots", "Sandals"]
    private static let outerNames = ["Wool Overcoat", "Denim Jacket", "Puffer Vest", "Trench Coat", "Bomber Jacket", "Field Jacket"]
    private static let accNames = ["Leather Belt", "Canvas Tote", "Wool Scarf", "Beanie", "Structured Tote", "Watch"]

    private static let styleTags = ["work", "casual", "weekend", "date", "gym", "travel", "evening"]
    private static let sizes = ["XS", "S", "M", "L", "XL", "8", "9", "10", "32", "34"]

    static func run(modelContext: ModelContext, targetItemCount: Int = 100) throws -> String {
#if DEBUG
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        var existingItemIds = Set(
            try modelContext.fetch(FetchDescriptor<WardrobeItem>()).map(\.id),
        )
        var existingOutfitIds = Set(
            try modelContext.fetch(FetchDescriptor<Outfit>()).map(\.id),
        )

        var itemsAdded = 0
        var itemsSkipped = 0
        var outfitsAdded = 0
        var outfitsSkipped = 0

        for i in 0 ..< targetItemCount {
            let item = buildItem(i: i, nowMs: now, count: targetItemCount)
            if existingItemIds.contains(item.id) {
                itemsSkipped += 1
            } else {
                modelContext.insert(item)
                existingItemIds.insert(item.id)
                itemsAdded += 1
            }
        }

        let outfits = buildOutfits(nowMs: now)
        for outfit in outfits {
            if existingOutfitIds.contains(outfit.id) {
                outfitsSkipped += 1
            } else {
                modelContext.insert(outfit)
                existingOutfitIds.insert(outfit.id)
                outfitsAdded += 1
            }
        }

        try modelContext.save()
        return "Added \(itemsAdded) items, \(outfitsAdded) outfits. Skipped \(itemsSkipped) items, \(outfitsSkipped) outfits (already in database)."
#else
        return "Not available in release builds."
#endif
    }

#if DEBUG
    private static func itemId(_ index: Int) -> String {
        String(format: "%@%03d", itemIdPrefix, index)
    }

    private static func outfitId(_ index: Int) -> String {
        "\(outfitIdPrefix)\(index)"
    }

    private static func seasonsForIndex(_ i: Int) -> [String] {
        switch i % 7 {
        case 0: return ["spring", "summer"]
        case 1: return ["fall", "winter"]
        case 2: return ["spring"]
        case 3: return ["summer"]
        case 4: return ["fall"]
        case 5: return ["winter"]
        default: return ["spring", "summer", "fall", "winter"]
        }
    }

    private static func buildItem(i: Int, nowMs: Int64, count: Int) -> WardrobeItem {
        let category = categories[i % categories.count]
        let tag = styleTags[i % styleTags.count]
        let baseName: String = {
            switch category {
            case WardrobeCatalog.tops: return topNames[i % topNames.count]
            case WardrobeCatalog.bottoms: return bottomNames[i % bottomNames.count]
            case WardrobeCatalog.dresses: return dressNames[i % dressNames.count]
            case WardrobeCatalog.shoes: return shoeNames[i % shoeNames.count]
            case WardrobeCatalog.outerwear: return outerNames[i % outerNames.count]
            case WardrobeCatalog.accessories: return accNames[i % accNames.count]
            default: return "Piece \(i)"
            }
        }()
        let name = "Seed \(baseName) (\(tag)) #\(i)"
        let seasons = WardrobeItem.joinSeasons(seasonsForIndex(i))
        let swatch = palette[i % palette.count]
        let worn = (i * 3 + i % 7) % 18
        let lastWorn: Int64? = worn > 0
            ? nowMs - Int64(i % 45) * 86_400_000
            : nil
        return WardrobeItem(
            id: itemId(i),
            name: name,
            category: category,
            sizeLabel: sizes[i % sizes.count],
            colorHex: swatch.hex,
            colorName: swatch.name,
            seasonsJoined: seasons,
            photoPath: nil,
            wornCount: worn,
            lastWornAtEpochMs: lastWorn,
            addedAtEpochMs: nowMs - Int64(count - i) * 60_000,
        )
    }

    private static func buildOutfits(nowMs: Int64) -> [Outfit] {
        let combos: [[Int]] = [
            [0, 1, 12, 18],
            [2, 7, 13, 19],
            [4, 5, 14, 22],
            [6, 8, 15, 24],
            [3, 9, 16, 20],
            [10, 11, 17, 21],
            [23, 25, 26, 27],
            [28, 29, 30, 31],
            [0, 6, 12, 18, 24],
        ]
        let names = [
            "Seed · Office core",
            "Seed · Weekend casual",
            "Seed · Date night",
            "Seed · Gym run",
            "Seed · Travel layer",
            "Seed · Creative meet",
            "Seed · Autumn layers",
            "Seed · Summer easy",
            "Seed · City walk",
        ]
        return combos.enumerated().map { idx, indices in
            let ids = indices.map { itemId($0) }
            return Outfit(
                id: outfitId(idx),
                name: names[idx],
                itemIdsJoined: Outfit.joinItemIds(ids),
                wornCount: idx % 5,
                createdAtEpochMs: nowMs - Int64(combos.count - idx) * 120_000,
            )
        }
    }
#endif
}
