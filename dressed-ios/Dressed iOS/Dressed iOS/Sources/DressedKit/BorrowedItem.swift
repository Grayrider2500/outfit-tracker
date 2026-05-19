import Foundation
import SwiftData

/// Read-only borrowed piece (mirrors wardrobe item fields; not in user’s wardrobe).
@Model
final class BorrowedItem {
    @Attribute(.unique) var id: String
    var name: String
    var category: String
    var sizeLabel: String
    var colorHex: String
    var colorName: String
    var seasonsJoined: String
    var occasionsJoined: String
    var photoPath: String?
    var wornCount: Int
    var lastWornAtEpochMs: Int64?
    var addedAtEpochMs: Int64
    var library: BorrowedLibrary?

    init(
        id: String,
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
        addedAtEpochMs: Int64,
        library: BorrowedLibrary? = nil,
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
        self.library = library
    }
}

extension BorrowedItem {
    var seasonsList: [String] {
        seasonsJoined.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    var occasionsList: [String] {
        occasionsJoined.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }
}
