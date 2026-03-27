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
    var photoPath: String?
    var wornCount: Int
    var addedAtEpochMs: Int64

    init(
        id: String = UUID().uuidString,
        name: String,
        category: String,
        sizeLabel: String = "",
        colorHex: String,
        colorName: String,
        seasonsJoined: String = "",
        photoPath: String? = nil,
        wornCount: Int = 0,
        addedAtEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
    ) {
        self.id = id
        self.name = name
        self.category = category
        self.sizeLabel = sizeLabel
        self.colorHex = colorHex
        self.colorName = colorName
        self.seasonsJoined = seasonsJoined
        self.photoPath = photoPath
        self.wornCount = wornCount
        self.addedAtEpochMs = addedAtEpochMs
    }
}

extension WardrobeItem {
    var seasonsList: [String] {
        seasonsJoined.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    static func joinSeasons(_ values: [String]) -> String {
        values.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.joined(separator: ",")
    }
}
