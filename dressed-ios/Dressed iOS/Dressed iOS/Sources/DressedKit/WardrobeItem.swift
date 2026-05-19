import Foundation
import SwiftData

/// Mirrors Android `WardrobeItemEntity` / Room `wardrobe_items`.
@Model
final class WardrobeItem {
    @Attribute(.unique) var id: String
    var name: String
    var category: String
    var sizeLabel: String
    var colorHex: String
    var colorName: String
    /// Comma-separated season keys: spring, summer, fall, winter (matches Android `Converters`).
    var seasonsJoined: String
    /// Comma-separated occasion keys, e.g. "date_night,work" (matches Android `occasions` column).
    var occasionsJoined: String
    var photoPath: String?
    var wornCount: Int
    /// Mirrors Android `lastWornAtEpochMs`; set when marking an item worn.
    var lastWornAtEpochMs: Int64?
    var addedAtEpochMs: Int64
    /// When true, piece can be included in a `.dressed-library` share file.
    var lendable: Bool = false

    init(
        id: String = UUID().uuidString,
        name: String,
        category: String,
        sizeLabel: String = "",
        colorHex: String,
        colorName: String,
        seasonsJoined: String = "",
        occasionsJoined: String = "",
        photoPath: String? = nil,
        wornCount: Int = 0,
        lastWornAtEpochMs: Int64? = nil,
        addedAtEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        lendable: Bool = false,
    ) {
        self.id = id
        self.name = name
        self.category = category
        self.sizeLabel = sizeLabel
        self.colorHex = colorHex
        self.colorName = colorName
        self.seasonsJoined = seasonsJoined
        self.occasionsJoined = occasionsJoined
        self.photoPath = photoPath
        self.wornCount = wornCount
        self.lastWornAtEpochMs = lastWornAtEpochMs
        self.addedAtEpochMs = addedAtEpochMs
        self.lendable = lendable
    }
}

extension WardrobeItem {
    var seasonsList: [String] {
        seasonsJoined.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    static func joinSeasons(_ values: [String]) -> String {
        values.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.joined(separator: ",")
    }

    var occasionsList: [String] {
        occasionsJoined.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    static func joinOccasions(_ values: [String]) -> String {
        values.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.joined(separator: ",")
    }

    /// Category · color · seasons line (list card + search row; matches Android `searchResultTagsLine`).
    var wardrobeSubtitleLine: String {
        let cat = WardrobeCatalog.label(forCategoryKey: category)
        let seasons: String = {
            let list = seasonsList
            if list.isEmpty { return "All seasons" }
            return list.map { key -> String in
                if key == "fall" { return "Autumn" }
                return WardrobeCatalog.seasons.first { $0.key == key }?.label ?? key
            }
            .joined(separator: ", ")
        }()
        return "\(cat) · \(colorName) · \(seasons)"
    }
}

enum WardrobeSortMode: String, CaseIterable {
    case recent
    case worn
    case nameAZ

    var label: String {
        switch self {
        case .recent: return "Recently added"
        case .worn: return "Most worn"
        case .nameAZ: return "A → Z"
        }
    }
}

extension [WardrobeItem] {
    /// Mirrors Android `sortedForDisplay` for the three chips on Search / list.
    func sortedForDisplay(_ mode: WardrobeSortMode) -> [WardrobeItem] {
        switch mode {
        case .recent:
            sorted { $0.addedAtEpochMs > $1.addedAtEpochMs }
        case .worn:
            sorted { $0.wornCount > $1.wornCount }
        case .nameAZ:
            sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        }
    }
}
