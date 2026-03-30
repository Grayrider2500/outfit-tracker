import SwiftData
import SwiftUI
import UIKit

/// Search + filters + sort (matches Android `WardrobeSearchScreen`).
struct WardrobeSearchView: View {
    var onNavigateHome: () -> Void
    var onSelectItem: (String) -> Void

    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var nameQuery = ""
    @State private var filterKey = WardrobeCatalog.allKey
    @State private var seasonKey = WardrobeCatalog.allKey
    @State private var sortMode: WardrobeSortMode = .recent

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var afterCategory: [WardrobeItem] {
        if filterKey == WardrobeCatalog.allKey {
            allItems
        } else {
            allItems.filter { $0.category == filterKey }
        }
    }

    private var displayed: [WardrobeItem] {
        var list = afterCategory
        let q = nameQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        if !q.isEmpty {
            list = list.filter { $0.name.localizedCaseInsensitiveContains(q) }
        }
        if seasonKey != WardrobeCatalog.allKey {
            list = list.filter { $0.seasonsList.contains(seasonKey) }
        }
        return list.sortedForDisplay(sortMode)
    }

    private var filtersExcludeAll: Bool {
        !allItems.isEmpty && !afterCategory.isEmpty && displayed.isEmpty
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                TextField("Search pieces…", text: $nameQuery)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)

                sectionHeader("CATEGORY")
                chipGrid {
                    ForEach(WardrobeCatalog.filters, id: \.key) { entry in
                        filterChip(title: entry.label, selected: filterKey == entry.key) {
                            filterKey = entry.key
                        }
                    }
                }

                sectionHeader("SEASON")
                    .padding(.top, 16)
                chipGrid {
                    ForEach(WardrobeCatalog.searchSeasonFilters, id: \.key) { entry in
                        filterChip(title: entry.label, selected: seasonKey == entry.key) {
                            seasonKey = entry.key
                        }
                    }
                }

                chipGrid {
                    ForEach(WardrobeSortMode.allCases, id: \.self) { mode in
                        filterChip(title: mode.label, selected: sortMode == mode) {
                            sortMode = mode
                        }
                    }
                }
                .padding(.top, 12)

                if displayed.isEmpty {
                    searchEmptyState
                        .frame(maxWidth: .infinity)
                        .padding(.top, 24)
                        .padding(.horizontal, 24)
                } else {
                    LazyVStack(spacing: 10) {
                        ForEach(displayed, id: \.id) { item in
                            Button {
                                onSelectItem(item.id)
                            } label: {
                                WardrobeSearchResultRow(item: item)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
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
                    Text("Search")
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text("Filter & sort")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.75))
                }
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private var searchEmptyState: some View {
        let hasItems = !allItems.isEmpty
        let nameNoMatch = hasItems && !nameQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty

        let title: String = {
            if nameNoMatch { return "No name matches" }
            if !hasItems { return "Your wardrobe awaits" }
            if filtersExcludeAll { return "No pieces match these filters" }
            return "Nothing in this category"
        }()

        let subtitle: String = {
            if nameNoMatch { return "Try a different spelling or clear the search box." }
            if !hasItems { return "Add pieces from My Wardrobe, then search here." }
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
    }

    private func sectionHeader(_ text: String) -> some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .tracking(1.2)
            .foregroundStyle(.secondary)
            .padding(.horizontal, 16)
            .padding(.top, 4)
            .padding(.bottom, 10)
    }

    private func chipGrid<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 72), spacing: 8)],
            alignment: .leading,
            spacing: 8
        ) {
            content()
        }
        .padding(.horizontal, 16)
    }

    private func filterChip(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
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
}

// MARK: - Result row

private struct WardrobeSearchResultRow: View {
    let item: WardrobeItem

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
                if let path = item.photoPath, FileManager.default.fileExists(atPath: path),
                   let ui = UIImage(contentsOfFile: path) {
                    Image(uiImage: ui)
                        .resizable()
                        .scaledToFill()
                } else {
                    Text(WardrobeCatalog.emoji(forCategoryKey: item.category))
                        .font(.system(size: 28))
                }
            }
            .frame(width: 52, height: 52)
            .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                Text(item.wardrobeSubtitleLine)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
    }
}

#Preview {
    NavigationStack {
        WardrobeSearchView(onNavigateHome: {}, onSelectItem: { _ in })
    }
}
