import Foundation
import SwiftData

/// Deterministic dev wardrobe for one professional woman. Debug builds only from UI.
enum DevTestDataSeeder {
    static let itemIdPrefix = "devseed-item-"
    static let outfitIdPrefix = "devseed-outfit-"

    private struct SeedItemSpec {
        let name: String
        let category: String
        let sizeLabel: String
        let colorHex: String
        let colorName: String
        let seasons: [String]
    }

    private static let seedItemSpecs: [SeedItemSpec] = [
        SeedItemSpec(name: "White Button-Down Shirt", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#F7F7F2", colorName: "Soft White", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Ivory Silk Blouse", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#F3EBDD", colorName: "Ivory", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Black Short-Sleeve Knit Top", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#1F1F1F", colorName: "Black", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Navy Crewneck Shell", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#223A5E", colorName: "Navy", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Dusty Blush Blouse", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#D8A7A1", colorName: "Blush", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Camel Lightweight Sweater", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#B88A5A", colorName: "Camel", seasons: ["fall", "winter", "spring"]),
        SeedItemSpec(name: "Charcoal Fine-Gauge Turtleneck", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#4A4A4A", colorName: "Charcoal", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Cobalt Wrap Blouse", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#2F5DCC", colorName: "Cobalt", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Soft Gray Mock-Neck Top", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#B8BCC2", colorName: "Gray", seasons: ["fall", "winter", "spring"]),
        SeedItemSpec(name: "Olive Utility Blouse", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#66724D", colorName: "Olive", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Burgundy Knit Shell", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#6E2233", colorName: "Burgundy", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Cream Ribbed Sweater", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#EADFCF", colorName: "Cream", seasons: ["fall", "winter", "spring"]),
        SeedItemSpec(name: "Light Blue Oxford Shirt", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#AFCBE3", colorName: "Light Blue", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Black Sleeveless Mock-Neck Top", category: WardrobeCatalog.tops, sizeLabel: "M", colorHex: "#181818", colorName: "Black", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Black Tailored Trousers", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#1D1D1D", colorName: "Black", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Navy Ankle Pants", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#223A5E", colorName: "Navy", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Charcoal Slim Trousers", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#4D5259", colorName: "Charcoal", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Camel Wide-Leg Pants", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#BC8E5F", colorName: "Camel", seasons: ["fall", "spring"]),
        SeedItemSpec(name: "Olive Cropped Trousers", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#6B7655", colorName: "Olive", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Ivory Straight-Leg Pants", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#F1E7D8", colorName: "Ivory", seasons: ["spring", "summer"]),
        SeedItemSpec(name: "Dark Wash Straight Jeans", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#2F4560", colorName: "Dark Denim", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Black Pencil Skirt", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#202020", colorName: "Black", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Navy Midi Skirt", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#2A3F63", colorName: "Navy", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Soft Gray Wide-Leg Trousers", category: WardrobeCatalog.bottoms, sizeLabel: "M", colorHex: "#B5B9BF", colorName: "Gray", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Black Sheath Dress", category: WardrobeCatalog.dresses, sizeLabel: "M", colorHex: "#1C1C1C", colorName: "Black", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Navy Wrap Dress", category: WardrobeCatalog.dresses, sizeLabel: "M", colorHex: "#243B63", colorName: "Navy", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Burgundy Midi Dress", category: WardrobeCatalog.dresses, sizeLabel: "M", colorHex: "#6D2336", colorName: "Burgundy", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Cream Sweater Dress", category: WardrobeCatalog.dresses, sizeLabel: "M", colorHex: "#E8DDCC", colorName: "Cream", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Slate Blue Shirt Dress", category: WardrobeCatalog.dresses, sizeLabel: "M", colorHex: "#6E859B", colorName: "Slate Blue", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Blush Belted Dress", category: WardrobeCatalog.dresses, sizeLabel: "M", colorHex: "#D5A5A5", colorName: "Blush", seasons: ["spring", "summer"]),
        SeedItemSpec(name: "Black Structured Blazer", category: WardrobeCatalog.outerwear, sizeLabel: "M", colorHex: "#1B1B1B", colorName: "Black", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Navy Blazer", category: WardrobeCatalog.outerwear, sizeLabel: "M", colorHex: "#22385C", colorName: "Navy", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Camel Longline Coat", category: WardrobeCatalog.outerwear, sizeLabel: "M", colorHex: "#B98858", colorName: "Camel", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Charcoal Wool Coat", category: WardrobeCatalog.outerwear, sizeLabel: "M", colorHex: "#4B4E54", colorName: "Charcoal", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Ivory Cropped Cardigan", category: WardrobeCatalog.outerwear, sizeLabel: "M", colorHex: "#EFE4D4", colorName: "Ivory", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Olive Utility Jacket", category: WardrobeCatalog.outerwear, sizeLabel: "M", colorHex: "#65724F", colorName: "Olive", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Black Pointed-Toe Flats", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#1A1A1A", colorName: "Black", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Nude Leather Pumps", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#C79B7A", colorName: "Nude", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Black Block-Heel Pumps", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#202020", colorName: "Black", seasons: ["spring", "fall", "winter"]),
        SeedItemSpec(name: "Brown Loafers", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#6B4A34", colorName: "Brown", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "White Leather Sneakers", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#F5F5F3", colorName: "White", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Black Ankle Boots", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#1E1E1E", colorName: "Black", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Cognac Heeled Boots", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#9A623D", colorName: "Cognac", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Navy Suede Flats", category: WardrobeCatalog.shoes, sizeLabel: "", colorHex: "#263A59", colorName: "Navy", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Black Leather Tote", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#1F1F1F", colorName: "Black", seasons: ["spring", "summer", "fall", "winter"]),
        SeedItemSpec(name: "Tan Structured Tote", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#BE8D60", colorName: "Tan", seasons: ["spring", "summer", "fall"]),
        SeedItemSpec(name: "Pearl Stud Earrings", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#F2ECE3", colorName: "Pearl", seasons: ["spring", "summer", "fall", "winter"]),
        SeedItemSpec(name: "Gold Hoop Earrings", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#C9A03A", colorName: "Gold", seasons: ["spring", "summer", "fall", "winter"]),
        SeedItemSpec(name: "Silk Navy Scarf", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#23395B", colorName: "Navy", seasons: ["spring", "fall"]),
        SeedItemSpec(name: "Burgundy Leather Belt", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#702437", colorName: "Burgundy", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Black Leather Belt", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#1F1F1F", colorName: "Black", seasons: ["spring", "summer", "fall", "winter"]),
        SeedItemSpec(name: "Camel Cashmere Scarf", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#B88958", colorName: "Camel", seasons: ["fall", "winter"]),
        SeedItemSpec(name: "Silver Watch", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#BFC5CC", colorName: "Silver", seasons: ["spring", "summer", "fall", "winter"]),
        SeedItemSpec(name: "Black Structured Satchel", category: WardrobeCatalog.accessories, sizeLabel: "", colorHex: "#222222", colorName: "Black", seasons: ["spring", "summer", "fall", "winter"]),
    ]

    private static let seededOutfits: [(String, [Int])] = [
        ("Seed · Office core", [0, 14, 30, 37, 44]),
        ("Seed · Navy polish", [3, 19, 31, 37, 46]),
        ("Seed · Creative Friday", [7, 20, 32, 39, 47]),
        ("Seed · Cozy winter", [11, 14, 33, 41, 51]),
        ("Seed · Utility day", [9, 15, 35, 40, 45]),
        ("Seed · Black dress", [24, 30, 38, 46, 53]),
        ("Seed · Burgundy evening", [26, 32, 42, 47, 49]),
        ("Seed · Soft layers", [4, 23, 34, 43, 52]),
        ("Seed · Minimal commute", [13, 22, 36, 53, 50]),
    ]

    static func run(modelContext: ModelContext, targetItemCount: Int = seedItemSpecs.count) throws -> String {
#if DEBUG
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        var existingItemIds = Set(try modelContext.fetch(FetchDescriptor<WardrobeItem>()).map(\.id))
        var existingOutfitIds = Set(try modelContext.fetch(FetchDescriptor<Outfit>()).map(\.id))

        var itemsAdded = 0
        var itemsSkipped = 0
        var outfitsAdded = 0
        var outfitsSkipped = 0

        for (index, spec) in seedItemSpecs.prefix(min(targetItemCount, seedItemSpecs.count)).enumerated() {
            let item = buildItem(index: index, spec: spec, nowMs: now)
            if existingItemIds.contains(item.id) {
                itemsSkipped += 1
            } else {
                modelContext.insert(item)
                existingItemIds.insert(item.id)
                itemsAdded += 1
            }
        }

        for (index, tuple) in seededOutfits.enumerated() {
            let outfit = buildOutfit(index: index, name: tuple.0, itemIndexes: tuple.1, nowMs: now)
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

    private static func buildItem(index: Int, spec: SeedItemSpec, nowMs: Int64) -> WardrobeItem {
        let worn = (index * 5 + 2) % 14
        return WardrobeItem(
            id: itemId(index),
            name: spec.name,
            category: spec.category,
            sizeLabel: spec.sizeLabel,
            colorHex: spec.colorHex,
            colorName: spec.colorName,
            seasonsJoined: WardrobeItem.joinSeasons(spec.seasons),
            photoPath: nil,
            wornCount: worn,
            lastWornAtEpochMs: worn == 0 ? nil : nowMs - Int64((index % 28) + 1) * 86_400_000,
            addedAtEpochMs: nowMs - Int64(seedItemSpecs.count - index) * 60_000
        )
    }

    private static func buildOutfit(index: Int, name: String, itemIndexes: [Int], nowMs: Int64) -> Outfit {
        Outfit(
            id: outfitId(index),
            name: name,
            itemIdsJoined: Outfit.joinItemIds(itemIndexes.map(itemId)),
            wornCount: index % 4,
            createdAtEpochMs: nowMs - Int64(seededOutfits.count - index) * 120_000
        )
    }
#endif
}
