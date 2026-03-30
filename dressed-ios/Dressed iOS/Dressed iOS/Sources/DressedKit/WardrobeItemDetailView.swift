import SwiftData
import SwiftUI
import UIKit

/// Read-only item detail from Search (Android `ItemDetailScreen` parity is partial — view-only for now).
struct WardrobeItemDetailView: View {
    let itemId: String
    var onBack: () -> Void

    @Query(sort: \WardrobeItem.addedAtEpochMs) private var allItems: [WardrobeItem]

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var item: WardrobeItem? {
        allItems.first { $0.id == itemId }
    }

    var body: some View {
        Group {
            if let item {
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        hero(for: item)
                        Text(item.name)
                            .font(.title2.weight(.bold))
                        detailRows(for: item)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .background(Color(.systemGroupedBackground))
            } else {
                ContentUnavailableView(
                    "Item not found",
                    systemImage: "tshirt.fill",
                    description: Text("It may have been removed.")
                )
            }
        }
        .navigationTitle("")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("← Back") {
                    onBack()
                }
                .foregroundStyle(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Piece")
                    .font(.headline)
                    .foregroundStyle(.white)
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private func hero(for item: WardrobeItem) -> some View {
        ZStack {
            Rectangle()
                .fill(Color(.secondarySystemGroupedBackground))
            if let path = item.photoPath, FileManager.default.fileExists(atPath: path),
               let ui = UIImage(contentsOfFile: path) {
                Image(uiImage: ui)
                    .resizable()
                    .scaledToFill()
            } else {
                Text(WardrobeCatalog.emoji(forCategoryKey: item.category))
                    .font(.system(size: 72))
            }
        }
        .aspectRatio(3.0 / 4.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .frame(maxWidth: .infinity)
    }

    private func detailRows(for item: WardrobeItem) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            labeled("Category", WardrobeCatalog.label(forCategoryKey: item.category))
            if !item.sizeLabel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                labeled("Size", item.sizeLabel)
            }
            labeled("Color", item.colorName)
            labeled("Seasons", seasonsLine(item))
            labeled("Times worn", "\(item.wornCount)")
            labeled("Added", Self.formatAdded(item.addedAtEpochMs))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func labeled(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title.uppercased())
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.secondary)
            Text(value)
                .font(.body)
        }
    }

    private func seasonsLine(_ item: WardrobeItem) -> String {
        let list = item.seasonsList
        if list.isEmpty { return "All seasons" }
        return list.map { key -> String in
            if key == "fall" { return "Autumn" }
            return WardrobeCatalog.seasons.first { $0.key == key }?.label ?? key
        }
        .joined(separator: ", ")
    }

    private static func formatAdded(_ epochMs: Int64) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        let d = Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000)
        return formatter.string(from: d)
    }
}

#Preview {
    NavigationStack {
        WardrobeItemDetailView(itemId: "preview") {}
    }
}
