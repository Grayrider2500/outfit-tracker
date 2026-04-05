import Foundation
import SwiftData
import ZIPFoundation

// MARK: - Manifest (cross-platform with Android `LibraryManifestDto`)

private struct LibraryManifestFile: Codable {
    var type: String
    var version: Int
    var sharerName: String
    var exportedAtEpochMs: Int64
    var items: [WardrobeItemDTO]
}

enum DressedLibraryShare {
    private static let zipMetadata = "metadata.json"
    private static let photosPrefix = "photos/"
    private static let metadataMaxBytes = 16 * 1024 * 1024

    static let sharerNameDefaultsKey = "library_sharer_name"

    /// Writes `.dressed-library` zip (only [items] in manifest; photos/ same as backup).
    static func exportZipFile(items: [WardrobeItem], sharerName: String) throws -> URL {
        let trimmed = sharerName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            throw LibraryShareError.missingSharerName
        }
        let lendableItems = items.filter(\.lendable)
        guard !lendableItems.isEmpty else {
            throw LibraryShareError.noLendableItems
        }

        let itemDTOs = lendableItems.map { toDtoV3($0) }
        let backup = LibraryManifestFile(
            type: "library",
            version: 1,
            sharerName: trimmed,
            exportedAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000),
            items: itemDTOs,
        )
        let encoder = JSONEncoder()
        let metaData = try encoder.encode(backup)

        let zipURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("dressed-library-\(UUID().uuidString).dressed-library", isDirectory: false)
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
            },
        )

        for item in lendableItems {
            guard let rel = itemDTOs.first(where: { $0.id == item.id })?.photoEntry,
                  let photoPath = item.photoPath, !photoPath.isEmpty,
                  FileManager.default.isReadableFile(atPath: photoPath) else { continue }
            let fileURL = URL(fileURLWithPath: photoPath)
            try archive.addEntry(with: rel, fileURL: fileURL, compressionMethod: .none)
        }

        return zipURL
    }

    private static func toDtoV3(_ item: WardrobeItem) -> WardrobeItemDTO {
        let entry: String? = {
            guard let p = item.photoPath, !p.isEmpty, FileManager.default.isReadableFile(atPath: p) else {
                return nil
            }
            return "\(photosPrefix)\(Self.safePhotoFileStem(item.id)).jpg"
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
            photoEntry: entry,
        )
    }

    static func importFromZip(url: URL, modelContext: ModelContext) throws {
        let archive = try Archive(url: url, accessMode: .read)

        let photosDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            .appendingPathComponent("photos", isDirectory: true)
        try FileManager.default.createDirectory(at: photosDir, withIntermediateDirectories: true)

        var metadataJson: String?
        var extractedPaths: [String: String] = [:]

        for entry in archive {
            let name = try Self.normalizeZipPath(entry.path)
            if name.caseInsensitiveCompare(zipMetadata) == .orderedSame {
                var collected = Data()
                var cap = 0
                let _: CRC32 = try archive.extract(entry, consumer: { chunk in
                    cap += chunk.count
                    if cap > metadataMaxBytes { throw LibraryShareError.metadataTooLarge }
                    collected.append(chunk)
                })
                metadataJson = String(data: collected, encoding: .utf8)
                continue
            }
            if entry.type == .file,
               name.lowercased().hasPrefix(photosPrefix),
               name.lowercased().hasSuffix(".jpg") {
                let dest = photosDir.appendingPathComponent(UUID().uuidString + ".jpg", isDirectory: false)
                let _: CRC32 = try archive.extract(entry, to: dest)
                extractedPaths[name.lowercased()] = dest.path
            }
        }

        guard let raw = metadataJson, !raw.isEmpty else {
            throw LibraryShareError.missingMetadata
        }
        let decoder = JSONDecoder()
        let file = try decoder.decode(LibraryManifestFile.self, from: Data(raw.utf8))
        guard file.type == "library" else { throw LibraryShareError.notALibrary }
        guard file.version == 1 else { throw LibraryShareError.unsupportedVersion(file.version) }

        let sharer = file.sharerName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sharer.isEmpty else { throw LibraryShareError.missingSharerName }

        try modelContext.transaction {
            let fd = FetchDescriptor<BorrowedLibrary>()
            let existingAll = try modelContext.fetch(fd)
            if let old = existingAll.first(where: { $0.sharerName == sharer }) {
                modelContext.delete(old)
            }

            let lib = BorrowedLibrary(sharerName: sharer, importedAtEpochMs: Int64(Date().timeIntervalSince1970 * 1000))
            modelContext.insert(lib)

            let uuid = lib.id
            for dto in file.items {
                let photoPath = resolvePhoto(dto: dto, extracted: extractedPaths)
                let row = BorrowedItem(
                    id: "\(uuid)|\(dto.id)",
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
                    addedAtEpochMs: dto.addedAtEpochMs,
                    library: lib,
                )
                modelContext.insert(row)
            }
        }
    }

    private static func resolvePhoto(dto: WardrobeItemDTO, extracted: [String: String]) -> String? {
        if let pe = dto.photoEntry?.trimmingCharacters(in: .whitespacesAndNewlines), !pe.isEmpty,
           let p = extracted[pe.lowercased()] {
            return p
        }
        guard let base64 = dto.photoBase64, !base64.isEmpty,
              let data = Data(base64Encoded: base64) else { return nil }
        return try? PhotoStorage.saveJPEGData(data)
    }

    private static func normalizeZipPath(_ raw: String) throws -> String {
        var t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasPrefix("/") { t.removeFirst() }
        t = t.replacingOccurrences(of: "\\", with: "/")
        if t.contains("..") { throw LibraryShareError.invalidPath }
        return t
    }

    private static func safePhotoFileStem(_ id: String) -> String {
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-")
        let replaced = id.unicodeScalars.map { allowed.contains($0) ? Character($0) : "_" }
        let s = String(replaced).trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        let stem = s.isEmpty ? "item" : s
        return String(stem.prefix(120))
    }

    enum LibraryShareError: LocalizedError {
        case missingSharerName
        case noLendableItems
        case missingMetadata
        case notALibrary
        case unsupportedVersion(Int)
        case metadataTooLarge
        case invalidPath

        var errorDescription: String? {
            switch self {
            case .missingSharerName: return "Add the sharer name before exporting."
            case .noLendableItems: return "Mark at least one piece as “Available to lend” first."
            case .missingMetadata: return "This file is missing library metadata."
            case .notALibrary: return "This is not a Dressed library file."
            case .unsupportedVersion(let v): return "Unsupported library format version \(v)."
            case .metadataTooLarge: return "Library metadata is too large."
            case .invalidPath: return "Invalid path inside archive."
            }
        }
    }
}
