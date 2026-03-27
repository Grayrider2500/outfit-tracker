import SwiftData
import SwiftUI
import UIKit

/// Wardrobe grid + category chips + total count (matches Android `WardrobeListScreen`).
struct WardrobeListView: View {
    var onNavigateHome: () -> Void

    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var filterKey = WardrobeCatalog.allKey
    @State private var showAdd = false

    private var afterCategory: [WardrobeItem] {
        if filterKey == WardrobeCatalog.allKey {
            allItems
        } else {
            allItems.filter { $0.category == filterKey }
        }
    }

    private var displayed: [WardrobeItem] {
        afterCategory.sorted { $0.addedAtEpochMs > $1.addedAtEpochMs }
    }

    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14),
    ]

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    var body: some View {
        Group {
            if displayed.isEmpty {
                emptyState
            } else {
                ScrollView {
                    chipRow
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(displayed, id: \.id) { item in
                            WardrobeItemCard(item: item)
                        }
                    }
                    .padding(.horizontal, 16)
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
                    Text("Wardrobe")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text(itemCountLabel(allItems.count))
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.75))
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showAdd = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel("Add piece")
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .sheet(isPresented: $showAdd) {
            NavigationStack {
                AddItemSheet {
                    showAdd = false
                }
            }
        }
    }

    private var chipRow: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 56), spacing: 8)], alignment: .leading, spacing: 8) {
            ForEach(WardrobeCatalog.filters, id: \.key) { entry in
                chip(title: entry.label, selected: filterKey == entry.key) {
                    filterKey = entry.key
                }
            }
        }
    }

    private func chip(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.caption.weight(.medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selected ? navPurple : Color(.secondarySystemGroupedBackground))
                .foregroundStyle(selected ? Color.white : Color.primary)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var emptyState: some View {
        let hasItems = !allItems.isEmpty
        let filtersExcludeAll = hasItems && afterCategory.isEmpty
        let title: String = {
            if !hasItems { return "Your wardrobe awaits" }
            if filtersExcludeAll { return "No pieces match these filters" }
            return "Nothing in this category"
        }()
        let subtitle: String = {
            if !hasItems { return "Tap + to add your first piece." }
            if filtersExcludeAll { return "Try clearing filters or widening your search." }
            return "Try another category or add something new."
        }()
        return VStack(spacing: 8) {
            Text(title)
                .font(.title3.weight(.semibold))
            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }

    private func itemCountLabel(_ count: Int) -> String {
        "\(count) item" + (count == 1 ? "" : "s")
    }
}

// MARK: - Card

private struct WardrobeItemCard: View {
    let item: WardrobeItem

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
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
                        .font(.system(size: 44))
                }
            }
            // Floating-point ratio required — `3 / 4` is integer 0 in Swift and collapses the image.
            .aspectRatio(3.0 / 4.0, contentMode: .fit)
            .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                Text(tagsLine)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            .padding(10)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
    }

    private var tagsLine: String {
        let cat = WardrobeCatalog.label(forCategoryKey: item.category)
        let seasons: String = {
            let list = item.seasonsList
            if list.isEmpty { return "All seasons" }
            return list.map { key -> String in
                if key == "fall" { return "Autumn" }
                return WardrobeCatalog.seasons.first { $0.key == key }?.label ?? key
            }
            .joined(separator: ", ")
        }()
        return "\(cat) · \(item.colorName) · \(seasons)"
    }
}

#Preview {
    NavigationStack {
        WardrobeListView(onNavigateHome: {})
    }
}
