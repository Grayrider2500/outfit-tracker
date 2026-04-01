import SwiftData
import SwiftUI
import UIKit

/// Outfits grid + create flow, aligned with Android `OutfitsNav` / `OutfitsListScreen`.
struct OutfitsListView: View {
    var onNavigateHome: () -> Void
    var onSelectOutfit: (String) -> Void

    @Query(sort: \Outfit.createdAtEpochMs, order: .reverse) private var outfits: [Outfit]
    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var showCreate = false

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var itemsById: [String: WardrobeItem] {
        Dictionary(uniqueKeysWithValues: allItems.map { ($0.id, $0) })
    }

    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14),
    ]

    var body: some View {
        Group {
            if outfits.isEmpty {
                emptyState
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(outfits, id: \.id) { outfit in
                            let resolved = outfit.itemIdList.compactMap { itemsById[$0] }
                            Button {
                                guard !outfit.id.isEmpty else { return }
                                onSelectOutfit(outfit.id)
                            } label: {
                                OutfitCollageCard(outfit: outfit, items: resolved)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("← Home") {
                    onNavigateHome()
                }
                .foregroundStyle(.white)
            }
            ToolbarItem(placement: .principal) {
                VStack(spacing: 2) {
                    Text("Outfits")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text(outfitCountLabel(outfits.count))
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.75))
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showCreate = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Create outfit")
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .sheet(isPresented: $showCreate) {
            NavigationStack {
                CreateOutfitSheet {
                    showCreate = false
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text("No outfits yet")
                .font(.title3.weight(.semibold))
            Text("Tap + to build your first look from your wardrobe pieces.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }

    private func outfitCountLabel(_ count: Int) -> String {
        "\(count) outfit" + (count == 1 ? "" : "s")
    }
}

// MARK: - Collage card (matches Android `OutfitCollageCard`)

private struct OutfitCollageCard: View {
    let outfit: Outfit
    let items: [WardrobeItem]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            collage
                .aspectRatio(1, contentMode: .fit)
                .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(outfit.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                HStack(spacing: 4) {
                    let n = outfit.itemIdList.count
                    Text("\(n) piece" + (n == 1 ? "" : "s"))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    if outfit.wornCount > 0 {
                        Text("· worn \(outfit.wornCount)×")
                            .font(.caption2)
                            .foregroundStyle(Color(red: 0.42, green: 0.29, blue: 0.68))
                    }
                }
            }
            .padding(10)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
    }

    private var collage: some View {
        let collageItems = Array(items.prefix(4))
        return GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            Group {
                if collageItems.isEmpty {
                    emptyCollageCell
                        .frame(width: w, height: h)
                } else if collageItems.count == 1 {
                    collageCell(item: collageItems[0])
                        .frame(width: w, height: h)
                } else {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            collageCell(item: collageItems[0])
                                .frame(width: w / 2, height: h / 2)
                            collageCell(item: collageItems.count > 1 ? collageItems[1] : nil)
                                .frame(width: w / 2, height: h / 2)
                        }
                        HStack(spacing: 0) {
                            collageCell(item: collageItems.count > 2 ? collageItems[2] : nil)
                                .frame(width: w / 2, height: h / 2)
                            collageCell(item: collageItems.count > 3 ? collageItems[3] : nil)
                                .frame(width: w / 2, height: h / 2)
                        }
                    }
                }
            }
        }
        .background(Color(.secondarySystemGroupedBackground))
    }

    private var emptyCollageCell: some View {
        Color(.secondarySystemGroupedBackground)
            .overlay {
                Text("👗")
                    .font(.system(size: 44))
            }
    }

    @ViewBuilder
    private func collageCell(item: WardrobeItem?) -> some View {
        Group {
            if let item {
                if let path = item.photoPath, FileManager.default.fileExists(atPath: path),
                   let ui = UIImage(contentsOfFile: path) {
                    Image(uiImage: ui)
                        .resizable()
                        .scaledToFill()
                } else {
                    Text(WardrobeCatalog.emoji(forCategoryKey: item.category))
                        .font(.system(size: 28))
                }
            } else {
                Color(.secondarySystemGroupedBackground).opacity(0.55)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

#Preview {
    NavigationStack {
        OutfitsListView(onNavigateHome: {}, onSelectOutfit: { _ in })
    }
    .modelContainer(for: [WardrobeItem.self, Outfit.self], inMemory: true)
}
