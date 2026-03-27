import Foundation
import SwiftData
import UniformTypeIdentifiers

// MARK: - Codable DTOs (plain structs for JSON, not SwiftData)

struct DressedBackupFile: Codable {
    var version: Int = 2
    var exportedAtEpochMs: Int64
    var items: [WardrobeItemDTO]
    var outfits: [OutfitDTO]?
}

struct WardrobeItemDTO: Codable {
    var id: String
    var name: String
    var category: String
    var sizeLabel: String
    var colorHex: String
    var colorName: String
    var seasons: [String]
    var wornCount: Int
    var addedAtEpochMs: Int64
    var photoBase64: String?
}

struct OutfitDTO: Codable {
    var id: String
    var name: String
    var itemIds: [String]
    var wornCount: Int
    var createdAtEpochMs: Int64
}

// MARK: - Export

enum DressedBackup {

    static func backupFileName() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return "dressed-backup-\(formatter.string(from: Date())).json"
    }

    static func exportBackup(items: [WardrobeItem], outfits: [Outfit]) throws -> Data {
        let itemDTOs = items.map { item in
            WardrobeItemDTO(
                id: item.id,
                name: item.name,
                category: item.category,
                sizeLabel: item.sizeLabel,
                colorHex: item.colorHex,
                colorName: item.colorName,
                seasons: item.seasonsList,
                wornCount: item.wornCount,
                addedAtEpochMs: item.addedAtEpochMs,
                photoBase64: encodePhoto(item.photoPath)
            )
        }
        let outfitDTOs = outfits.map { outfit in
            OutfitDTO(
                id: outfit.id,
                name: outfit.name,
                itemIds: outfit.itemIdList,
                wornCount: outfit.wornCount,
                createdAtEpochMs: outfit.createdAtEpochMs
            )
        }
        let backup = DressedBackupFile(
            exportedAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000),
            items: itemDTOs,
            outfits: outfitDTOs
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(backup)
    }

    private static func encodePhoto(_ path: String?) -> String? {
        guard let path, let data = PhotoStorage.readJPEGData(at: path) else { return nil }
        return data.base64EncodedString()
    }

    // MARK: - Import

    static func importBackup(from data: Data) throws -> (items: [WardrobeItemDTO], outfits: [OutfitDTO]) {
        let decoder = JSONDecoder()
        let backup = try decoder.decode(DressedBackupFile.self, from: data)
        guard backup.version <= 2 else {
            throw BackupError.unsupportedVersion(backup.version)
        }
        return (backup.items, backup.outfits ?? [])
    }

    // MARK: - Restore (Replace All)

    static func restoreReplace(
        items: [WardrobeItemDTO],
        outfits: [OutfitDTO],
        modelContext: ModelContext
    ) throws {
        // Delete existing photos from disk
        let existingItems = try modelContext.fetch(FetchDescriptor<WardrobeItem>())
        for item in existingItems {
            if let path = item.photoPath {
                try? FileManager.default.removeItem(atPath: path)
            }
        }

        // Delete all existing data
        try modelContext.delete(model: WardrobeItem.self)
        try modelContext.delete(model: Outfit.self)

        // Insert new items
        for dto in items {
            let photoPath = decodePhoto(dto.photoBase64)
            let item = WardrobeItem(
                id: dto.id,
                name: dto.name,
                category: dto.category,
                sizeLabel: dto.sizeLabel,
                colorHex: dto.colorHex,
                colorName: dto.colorName,
                seasonsJoined: WardrobeItem.joinSeasons(dto.seasons),
                photoPath: photoPath,
                wornCount: dto.wornCount,
                addedAtEpochMs: dto.addedAtEpochMs
            )
            modelContext.insert(item)
        }

        // Insert new outfits
        for dto in outfits {
            let outfit = Outfit(
                id: dto.id,
                name: dto.name,
                itemIdsJoined: Outfit.joinItemIds(dto.itemIds),
                wornCount: dto.wornCount,
                createdAtEpochMs: dto.createdAtEpochMs
            )
            modelContext.insert(outfit)
        }

        try modelContext.save()
    }

    // MARK: - Restore (Merge — add new, skip duplicates)

    static func restoreMerge(
        items: [WardrobeItemDTO],
        outfits: [OutfitDTO],
        modelContext: ModelContext
    ) throws -> (newItems: Int, newOutfits: Int) {
        let existingItems = try modelContext.fetch(FetchDescriptor<WardrobeItem>())
        let existingItemIDs = Set(existingItems.map(\.id))

        var newItemCount = 0
        for dto in items where !existingItemIDs.contains(dto.id) {
            let photoPath = decodePhoto(dto.photoBase64)
            let item = WardrobeItem(
                id: dto.id,
                name: dto.name,
                category: dto.category,
                sizeLabel: dto.sizeLabel,
                colorHex: dto.colorHex,
                colorName: dto.colorName,
                seasonsJoined: WardrobeItem.joinSeasons(dto.seasons),
                photoPath: photoPath,
                wornCount: dto.wornCount,
                addedAtEpochMs: dto.addedAtEpochMs
            )
            modelContext.insert(item)
            newItemCount += 1
        }

        let existingOutfits = try modelContext.fetch(FetchDescriptor<Outfit>())
        let existingOutfitIDs = Set(existingOutfits.map(\.id))

        var newOutfitCount = 0
        for dto in outfits where !existingOutfitIDs.contains(dto.id) {
            let outfit = Outfit(
                id: dto.id,
                name: dto.name,
                itemIdsJoined: Outfit.joinItemIds(dto.itemIds),
                wornCount: dto.wornCount,
                createdAtEpochMs: dto.createdAtEpochMs
            )
            modelContext.insert(outfit)
            newOutfitCount += 1
        }

        try modelContext.save()
        return (newItemCount, newOutfitCount)
    }

    private static func decodePhoto(_ base64: String?) -> String? {
        guard let base64, !base64.isEmpty,
              let data = Data(base64Encoded: base64) else { return nil }
        return try? PhotoStorage.saveJPEGData(data)
    }

    enum BackupError: LocalizedError {
        case unsupportedVersion(Int)

        var errorDescription: String? {
            switch self {
            case .unsupportedVersion(let v):
                return "This backup uses version \(v), which is newer than this app supports."
            }
        }
    }
}
