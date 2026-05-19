import Foundation
import SwiftData
import ZIPFoundation

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
    var occasions: [String] = []
    var wornCount: Int
    var lastWornAtEpochMs: Int64?
    var addedAtEpochMs: Int64
    /// Legacy v1–v2 JSON backups.
    var photoBase64: String?
    /// v3 zip: path inside archive, e.g. `photos/{id}.jpg`.
    var photoEntry: String?
}

struct OutfitDTO: Codable {
    var id: String
    var name: String
    var itemIds: [String]
    var wornCount: Int
    var createdAtEpochMs: Int64
}

struct ParsedBackup {
    var items: [WardrobeItemDTO]
    var outfits: [OutfitDTO]
    /// Zip entry path (normalized) -> absolute photo path on disk after extract; empty for JSON imports.
    var extractedPhotoPaths: [String: String]
}

// MARK: - Export / import

enum DressedBackup {
    private static let zipMetadata = "metadata.json"
    private static let photosPrefix = "photos/"
    private static let metadataMaxBytes = 16 * 1024 * 1024

    static func backupFileName() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return "dressed-backup-\(formatter.string(from: Date())).zip"
    }

    /// v3 cross-platform `.zip`: `metadata.json` + `photos/*.jpg` (no base64 in metadata).
    static func exportBackupZipFile(items: [WardrobeItem], outfits: [Outfit]) throws -> URL {
        let itemDTOs = items.map { toDtoV3($0) }
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
            version: 3,
            exportedAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000),
            items: itemDTOs,
            outfits: outfitDTOs
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = []
        let metaData = try encoder.encode(backup)

        let zipURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("dressed-export-\(UUID().uuidString).zip", isDirectory: false)
        if FileManager.default.fileExists(atPath: zipURL.path) {
            try FileManager.default.removeItem(at: zipURL)
        }
        let archive = try Archive(url: zipURL, accessMode: .create)

        let metaLen = Int64(metaData.count)
        try archive.addEntry(
            with: zipMetadata,
            type: .file,
            uncompressedSize: metaLen,
            bufferSize: 32 * 1024,
            provider: { position, size -> Data in
                let start = Int(position)
                let end = min(start + Int(size), metaData.count)
                return metaData.subdata(in: start..<end)
            }
        )

        for item in items {
            guard let rel = itemDTOs.first(where: { $0.id == item.id })?.photoEntry,
                  let photoPath = item.photoPath, !photoPath.isEmpty
            else { continue }
            let fileURL = URL(fileURLWithPath: photoPath)
            guard FileManager.default.isReadableFile(atPath: photoPath) else { continue }
            try archive.addEntry(with: rel, fileURL: fileURL, compressionMethod: .none)
        }

        return zipURL
    }

    /// Peek zip (`PK`) vs JSON; zip photos are streamed to disk during import.
    static func importBackup(from url: URL) throws -> ParsedBackup {
        let handle = try FileHandle(forReadingFrom: url)
        defer { try? handle.close() }
        let prefix = try handle.read(upToCount: 2) ?? Data()
        if prefix.count == 2, prefix[0] == 0x50, prefix[1] == 0x4B {
            return try importZip(from: url)
        }
        let data = try Data(contentsOf: url)
        return try importJsonBackup(data: data)
    }

    private static func importJsonBackup(data: Data) throws -> ParsedBackup {
        let decoder = JSONDecoder()
        let backup = try decoder.decode(DressedBackupFile.self, from: data)
        guard backup.version <= 2 else {
            throw BackupError.unsupportedJsonVersion(backup.version)
        }
        return ParsedBackup(items: backup.items, outfits: backup.outfits ?? [], extractedPhotoPaths: [:])
    }

    private static func importZip(from url: URL) throws -> ParsedBackup {
        let archive = try Archive(url: url, accessMode: .read)

        let photosDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            .appendingPathComponent("photos", isDirectory: true)
        try FileManager.default.createDirectory(at: photosDir, withIntermediateDirectories: true)

        var metadataJson: String?
        var extractedPaths: [String: String] = [:]

        for entry in archive {
            let name = try normalizeZipPath(entry.path)
            if name.caseInsensitiveCompare(zipMetadata) == .orderedSame {
                var collected = Data()
                var cap = 0
                _ = try archive.extract(entry, consumer: { chunk in
                    cap += chunk.count
                    if cap > metadataMaxBytes { throw BackupError.metadataTooLarge }
                    collected.append(chunk)
                })
                metadataJson = String(data: collected, encoding: .utf8)
                continue
            }
            if entry.type == .file,
               name.lowercased().hasPrefix(photosPrefix),
               name.lowercased().hasSuffix(".jpg") {
                let dest = photosDir.appendingPathComponent(UUID().uuidString + ".jpg", isDirectory: false)
                _ = try archive.extract(entry, to: dest)
                extractedPaths[name.lowercased()] = dest.path
            }
        }

        guard let raw = metadataJson, !raw.isEmpty else {
            throw BackupError.missingMetadata
        }
        let decoder = JSONDecoder()
        let file = try decoder.decode(DressedBackupFile.self, from: Data(raw.utf8))
        guard file.version == 3 else {
            throw BackupError.unsupportedZipMetadataVersion(file.version)
        }
        return ParsedBackup(items: file.items, outfits: file.outfits ?? [], extractedPhotoPaths: extractedPaths)
    }

    private static func normalizeZipPath(_ raw: String) throws -> String {
        var t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasPrefix("/") { t.removeFirst() }
        t = t.replacingOccurrences(of: "\\", with: "/")
        if t.contains("..") { throw BackupError.invalidZipEntryPath }
        return t
    }

    private static func toDtoV3(_ item: WardrobeItem) -> WardrobeItemDTO {
        let entry: String? = {
            guard let p = item.photoPath, !p.isEmpty, FileManager.default.isReadableFile(atPath: p) else {
                return nil
            }
            return "\(photosPrefix)\(safePhotoFileStem(item.id)).jpg"
        }()
        return WardrobeItemDTO(
            id: item.id,
            name: item.name,
            category: item.category,
            sizeLabel: item.sizeLabel,
            colorHex: item.colorHex,
            colorName: item.colorName,
            seasons: item.seasonsList,
            occasions: item.occasionsList,
            wornCount: item.wornCount,
            lastWornAtEpochMs: item.lastWornAtEpochMs,
            addedAtEpochMs: item.addedAtEpochMs,
            photoBase64: nil,
            photoEntry: entry
        )
    }

    private static func safePhotoFileStem(_ id: String) -> String {
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-")
        let replaced = id.unicodeScalars.map { allowed.contains($0) ? Character($0) : "_" }
        let s = String(replaced).trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        let stem = s.isEmpty ? "item" : s
        return String(stem.prefix(120))
    }

    private static func resolvePhotoPath(dto: WardrobeItemDTO, extracted: [String: String]) -> String? {
        if let pe = dto.photoEntry?.trimmingCharacters(in: .whitespacesAndNewlines), !pe.isEmpty,
           let p = extracted[pe.lowercased()] {
            return p
        }
        return decodePhoto(dto.photoBase64)
    }

    // MARK: - Restore (Replace All)

    static func restoreReplace(
        items: [WardrobeItemDTO],
        outfits: [OutfitDTO],
        extractedPhotoPaths: [String: String],
        modelContext: ModelContext
    ) throws {
        let existingItems = try modelContext.fetch(FetchDescriptor<WardrobeItem>())
        let oldPhotoPaths = Set(
            existingItems.compactMap(\.photoPath).map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
        )
        let newPhotoPaths = Set(
            items.map { resolvePhotoPath(dto: $0, extracted: extractedPhotoPaths) }
                .compactMap { $0 }
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
        )

        try modelContext.transaction {
            for item in existingItems {
                modelContext.delete(item)
            }
            let existingOutfits = try modelContext.fetch(FetchDescriptor<Outfit>())
            for outfit in existingOutfits {
                modelContext.delete(outfit)
            }
            for dto in items {
                let photoPath = resolvePhotoPath(dto: dto, extracted: extractedPhotoPaths)
                let item = WardrobeItem(
                    id: dto.id,
                    name: dto.name,
                    category: dto.category,
                    sizeLabel: dto.sizeLabel,
                    colorHex: dto.colorHex,
                    colorName: dto.colorName,
                    seasonsJoined: WardrobeItem.joinSeasons(dto.seasons),
                    occasionsJoined: WardrobeItem.joinOccasions(dto.occasions),
                    photoPath: photoPath,
                    wornCount: dto.wornCount,
                    lastWornAtEpochMs: dto.lastWornAtEpochMs,
                    addedAtEpochMs: dto.addedAtEpochMs
                )
                modelContext.insert(item)
            }
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
        }

        for path in oldPhotoPaths.subtracting(newPhotoPaths) {
            try? FileManager.default.removeItem(atPath: path)
        }
    }

    // MARK: - Restore (Merge)

    static func restoreMerge(
        items: [WardrobeItemDTO],
        outfits: [OutfitDTO],
        extractedPhotoPaths: [String: String],
        modelContext: ModelContext
    ) throws -> (newItems: Int, newOutfits: Int) {
        let existingItems = try modelContext.fetch(FetchDescriptor<WardrobeItem>())
        let existingItemIDs = Set(existingItems.map(\.id))

        var newItemCount = 0
        for dto in items where !existingItemIDs.contains(dto.id) {
            let photoPath = resolvePhotoPath(dto: dto, extracted: extractedPhotoPaths)
            let item = WardrobeItem(
                id: dto.id,
                name: dto.name,
                category: dto.category,
                sizeLabel: dto.sizeLabel,
                colorHex: dto.colorHex,
                colorName: dto.colorName,
                seasonsJoined: WardrobeItem.joinSeasons(dto.seasons),
                occasionsJoined: WardrobeItem.joinOccasions(dto.occasions),
                photoPath: photoPath,
                wornCount: dto.wornCount,
                lastWornAtEpochMs: dto.lastWornAtEpochMs,
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
        case unsupportedJsonVersion(Int)
        case unsupportedZipMetadataVersion(Int)
        case missingMetadata
        case metadataTooLarge
        case invalidZipEntryPath

        var errorDescription: String? {
            switch self {
            case .unsupportedJsonVersion(let v):
                return "This JSON backup uses version \(v). Import a v1–v2 JSON file or a v3 zip backup."
            case .unsupportedZipMetadataVersion(let v):
                return "This zip backup uses metadata version \(v); expected 3."
            case .missingMetadata:
                return "Invalid backup: missing \(zipMetadata)."
            case .metadataTooLarge:
                return "Backup metadata is too large to import."
            case .invalidZipEntryPath:
                return "Invalid path inside backup archive."
            }
        }
    }
}
