import Foundation
import SwiftData

/// Mirrors Android `OutfitEntity` / Room `outfits`.
@Model
final class Outfit {
    @Attribute(.unique) var id: String
    var name: String
    /// Comma-separated `WardrobeItem.id` values (matches Android `Converters` for itemIds).
    var itemIdsJoined: String
    var wornCount: Int
    var createdAtEpochMs: Int64

    init(
        id: String = UUID().uuidString,
        name: String,
        itemIdsJoined: String = "",
        wornCount: Int = 0,
        createdAtEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
    ) {
        self.id = id
        self.name = name
        self.itemIdsJoined = itemIdsJoined
        self.wornCount = wornCount
        self.createdAtEpochMs = createdAtEpochMs
    }
}

extension Outfit {
    var itemIdList: [String] {
        itemIdsJoined.split(separator: ",").map { String($0).trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    static func joinItemIds(_ ids: [String]) -> String {
        ids.map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }.joined(separator: ",")
    }
}
