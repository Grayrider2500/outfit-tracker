import SwiftData
import SwiftUI
import UIKit

/// Piece detail (matches Android `ItemDetailScreen`: layout, mark worn, delete).
struct WardrobeItemDetailView: View {
    let itemId: String
    var onBack: () -> Void

    @Environment(\.modelContext) private var modelContext
    @Query(sort: \WardrobeItem.addedAtEpochMs) private var allItems: [WardrobeItem]
    @Query(sort: \Outfit.createdAtEpochMs) private var allOutfits: [Outfit]

    @State private var showDeleteConfirm = false

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var item: WardrobeItem? {
        allItems.first { $0.id == itemId }
    }

    private var outfitCount: Int {
        allOutfits.filter { $0.itemIdList.contains(itemId) }.count
    }

    var body: some View {
        Group {
            if let item {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        hero(for: item)
                            .padding(.horizontal, 20)
                            .padding(.top, 8)

                        VStack(alignment: .leading, spacing: 0) {
                            Text(item.name)
                                .font(.title2.weight(.semibold))
                                .foregroundStyle(.primary)

                            Text(item.sizeLabel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                                 ? "Size —"
                                 : "Size \(item.sizeLabel)")
                                .font(.title3)
                                .foregroundStyle(navPurple)
                                .padding(.top, 6)

                            HStack(spacing: 8) {
                                Circle()
                                    .fill(Self.swatchColor(item.colorHex))
                                    .frame(width: 18, height: 18)
                                Text(item.colorName)
                                    .font(.body)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.top, 10)

                            if let seasons = seasonsTagline(item) {
                                Text(seasons)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(navPurple)
                                    .padding(.top, 8)
                            }

                            Divider()
                                .padding(.vertical, 20)

                            HStack {
                                Spacer()
                                statBlock(value: "\(item.wornCount)", label: "Times Worn")
                                Spacer()
                                statBlock(value: "\(outfitCount)", label: "Outfits")
                                Spacer()
                            }

                            Button {
                                markWorn(item)
                            } label: {
                                Text("+ Mark as Worn Today")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(navPurple)
                            .padding(.top, 24)

                            Button(role: .destructive) {
                                showDeleteConfirm = true
                            } label: {
                                Text("Remove from Wardrobe")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.bordered)
                            .padding(.top, 12)
                        }
                        .padding(20)
                    }
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
                Text(item.map { WardrobeCatalog.label(forCategoryKey: $0.category).uppercased() } ?? "Piece")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .confirmationDialog("Remove this piece?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Remove", role: .destructive) {
                if let item { deleteItem(item) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This cannot be undone.")
        }
    }

    private func markWorn(_ item: WardrobeItem) {
        item.wornCount += 1
        try? modelContext.save()
    }

    private func deleteItem(_ item: WardrobeItem) {
        PhotoStorage.deleteFileIfExists(at: item.photoPath)
        modelContext.delete(item)
        try? modelContext.save()
        onBack()
    }

    private func hero(for item: WardrobeItem) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground).opacity(0.6))
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
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .frame(maxWidth: .infinity)
    }

    private func statBlock(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2.weight(.semibold))
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private func seasonsTagline(_ item: WardrobeItem) -> String? {
        let list = item.seasonsList
        if list.isEmpty { return nil }
        return list.map { key -> String in
            if key == "fall" { return "Autumn" }
            return WardrobeCatalog.seasons.first { $0.key == key }?.label ?? key.capitalized
        }
        .joined(separator: " · ")
    }

    private static func swatchColor(_ hex: String) -> Color {
        let t = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        guard t.hasPrefix("#"), t.count >= 7 else { return Color.secondary }
        let start = t.index(t.startIndex, offsetBy: 1)
        let hexDigits = String(t[start...].prefix(6))
        guard hexDigits.count == 6, let n = UInt32(hexDigits, radix: 16) else { return Color.secondary }
        let r = Double((n >> 16) & 0xFF) / 255
        let g = Double((n >> 8) & 0xFF) / 255
        let b = Double(n & 0xFF) / 255
        return Color(red: r, green: g, blue: b)
    }
}

#Preview {
    NavigationStack {
        WardrobeItemDetailView(itemId: "preview") {}
    }
}
