import SwiftData
import SwiftUI

private let outfitSortOptions: [(key: String, label: String)] = [
    (key: "newest", label: "Newest"),
    (key: "worn", label: "Most Worn"),
    (key: "name", label: "A–Z"),
]

private let outfitSeasonFilters: [(key: String, label: String)] = [
    (key: "all", label: "All seasons"),
    (key: "spring", label: "Spring"),
    (key: "summer", label: "Summer"),
    (key: "fall", label: "Autumn"),
    (key: "winter", label: "Winter"),
]

private let outfitSizeFilters: [(key: String, label: String)] = [
    (key: "any", label: "Any size"),
    (key: "1", label: "Solo"),
    (key: "2-3", label: "2–3 pcs"),
    (key: "4+", label: "4+ pcs"),
]

/// Outfits grid + create flow, aligned with Android `OutfitsNav` / `OutfitsListScreen`.
struct OutfitsListView: View {
    var onNavigateHome: () -> Void
    var onSelectOutfit: (String) -> Void

    @Query(sort: \Outfit.createdAtEpochMs, order: .reverse) private var outfits: [Outfit]
    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var showCreate = false
    @State private var sortMode = "newest"
    @State private var seasonFilter = "all"
    @State private var sizeFilter = "any"

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var itemsById: [String: WardrobeItem] {
        Dictionary(uniqueKeysWithValues: allItems.map { ($0.id, $0) })
    }

    private var displayedOutfits: [Outfit] {
        var list = outfits

        if seasonFilter != "all" {
            list = list.filter { outfit in
                outfit.itemIdList.contains { id in
                    itemsById[id]?.seasonsList.contains(seasonFilter) == true
                }
            }
        }

        switch sizeFilter {
        case "1": list = list.filter { $0.itemIdList.count == 1 }
        case "2-3": list = list.filter { (2 ... 3).contains($0.itemIdList.count) }
        case "4+": list = list.filter { $0.itemIdList.count >= 4 }
        default: break
        }

        switch sortMode {
        case "worn": return list.sorted(by: { $0.wornCount > $1.wornCount })
        case "name": return list.sorted(by: { $0.name.localizedLowercase < $1.name.localizedLowercase })
        default: return list.sorted(by: { $0.createdAtEpochMs > $1.createdAtEpochMs })
        }
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
                VStack(spacing: 0) {
                    filterBar
                    if displayedOutfits.isEmpty {
                        noResultsState
                    } else {
                        ScrollView {
                            LazyVGrid(columns: columns, spacing: 14) {
                                ForEach(displayedOutfits, id: \.id) { outfit in
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

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(outfitSortOptions, id: \.key) { opt in
                    FilterChip(label: opt.label, selected: sortMode == opt.key) {
                        sortMode = opt.key
                    }
                }
                Divider()
                    .frame(height: 20)
                ForEach(outfitSeasonFilters, id: \.key) { opt in
                    FilterChip(label: opt.label, selected: seasonFilter == opt.key) {
                        seasonFilter = opt.key
                    }
                }
                Divider()
                    .frame(height: 20)
                ForEach(outfitSizeFilters, id: \.key) { opt in
                    FilterChip(label: opt.label, selected: sizeFilter == opt.key) {
                        sizeFilter = opt.key
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .background(Color(.systemGroupedBackground))
    }

    private var noResultsState: some View {
        VStack(spacing: 8) {
            Text("No outfits match")
                .font(.title3.weight(.semibold))
            Text("Try adjusting the sort or filter.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
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
                CachedLocalPhotoImage(
                    photoPath: item.photoPath,
                    categoryKey: item.category,
                    emojiSize: 28,
                )
            } else {
                Color(.secondarySystemGroupedBackground).opacity(0.55)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

private struct FilterChip: View {
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(selected ? Color(red: 0.42, green: 0.29, blue: 0.68) : Color(.secondarySystemGroupedBackground))
                .foregroundStyle(selected ? .white : .primary)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(selected ? Color.clear : Color(.separator), lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        OutfitsListView(onNavigateHome: {}, onSelectOutfit: { _ in })
    }
    .modelContainer(for: [WardrobeItem.self, Outfit.self], inMemory: true)
}
