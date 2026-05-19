import SwiftData
import SwiftUI
import UIKit

/// Wardrobe grid + category chips + total count (matches Android `WardrobeListScreen`).
struct WardrobeListView: View {
    var onNavigateHome: () -> Void
    var onSelectItem: (String) -> Void

    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var filterKey = WardrobeCatalog.allKey
    @State private var showAdd = false
    @AppStorage("library_explainer_seen") private var libraryExplainerSeen = false
    @State private var showLibraryExplainer = false
    @State private var libraryExplainerDontShowAgain = false
    @State private var explainerLeadsToExport = true
    @State private var showSharerNameAlert = false
    @State private var sharerNameInput = ""
    @State private var shareLibraryURL: URL?
    @State private var showingLibraryShareSheet = false
    @State private var exportErrorAlert = false
    @State private var exportErrorMessage = ""

    private var afterCategory: [WardrobeItem] {
        if filterKey == WardrobeCatalog.allKey {
            allItems
        } else {
            allItems.filter { $0.category == filterKey }
        }
    }

    private var displayed: [WardrobeItem] {
        afterCategory.sortedForDisplay(.recent)
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
                            Button {
                                onSelectItem(item.id)
                            } label: {
                                WardrobeItemCard(item: item)
                            }
                            .buttonStyle(.plain)
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
                HStack(spacing: 4) {
                    Menu {
                        Button {
                            beginExportSharedLibrary()
                        } label: {
                            Label("Export shared library…", systemImage: "square.and.arrow.up")
                        }
                        Button {
                            libraryExplainerSeen = false
                            explainerLeadsToExport = false
                            libraryExplainerDontShowAgain = false
                            showLibraryExplainer = true
                        } label: {
                            Label("About sharing libraries", systemImage: "info.circle")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel("More options")
                    Button {
                        showAdd = true
                    } label: {
                        Image(systemName: "plus")
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel("Add piece")
                }
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
        .sheet(isPresented: $showLibraryExplainer) {
            LibraryExplainerSheet(
                isPresented: $showLibraryExplainer,
                dontShowAgain: $libraryExplainerDontShowAgain,
                onContinue: {
                    if libraryExplainerDontShowAgain {
                        libraryExplainerSeen = true
                    }
                    showLibraryExplainer = false
                    if explainerLeadsToExport {
                        proceedExportAfterExplainer()
                    }
                },
            )
        }
        .sheet(isPresented: $showingLibraryShareSheet, onDismiss: {
            if let url = shareLibraryURL {
                try? FileManager.default.removeItem(at: url)
                shareLibraryURL = nil
            }
        }) {
            if let url = shareLibraryURL {
                ShareSheet(activityItems: [url])
            }
        }
        .alert("Your name on the library file", isPresented: $showSharerNameAlert) {
            TextField("e.g. Chris", text: $sharerNameInput)
            Button("Cancel", role: .cancel) {}
            Button("Continue") {
                let trimmed = sharerNameInput.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !trimmed.isEmpty else { return }
                UserDefaults.standard.set(trimmed, forKey: DressedLibraryShare.sharerNameDefaultsKey)
                showSharerNameAlert = false
                exportAndShareLibrary(sharerName: trimmed)
            }
        } message: {
            Text("This name appears on the file so friends know who shared it.")
        }
        .alert("Could not export library", isPresented: $exportErrorAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(exportErrorMessage)
        }
    }

    private func storedSharerName() -> String {
        UserDefaults.standard.string(forKey: DressedLibraryShare.sharerNameDefaultsKey)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    private func beginExportSharedLibrary() {
        explainerLeadsToExport = true
        if !libraryExplainerSeen {
            libraryExplainerDontShowAgain = false
            showLibraryExplainer = true
        } else {
            proceedExportAfterExplainer()
        }
    }

    private func proceedExportAfterExplainer() {
        let name = storedSharerName()
        if name.isEmpty {
            sharerNameInput = ""
            showSharerNameAlert = true
        } else {
            exportAndShareLibrary(sharerName: name)
        }
    }

    private func exportAndShareLibrary(sharerName: String) {
        do {
            let url = try DressedLibraryShare.exportZipFile(items: allItems, sharerName: sharerName)
            shareLibraryURL = url
            showingLibraryShareSheet = true
        } catch {
            exportErrorMessage = error.localizedDescription
            exportErrorAlert = true
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

private struct ShareSheet: UIViewControllerRepresentable {
    var activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Card

private struct WardrobeItemCard: View {
    let item: WardrobeItem

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                Rectangle()
                    .fill(Color(.secondarySystemGroupedBackground))
                CachedLocalPhotoImage(
                    photoPath: item.photoPath,
                    categoryKey: item.category,
                    emojiSize: 44,
                )
            }
            // Floating-point ratio required — `3 / 4` is integer 0 in Swift and collapses the image.
            .aspectRatio(3.0 / 4.0, contentMode: .fit)
            .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                Text(item.wardrobeSubtitleLine)
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
}

#Preview {
    NavigationStack {
        WardrobeListView(onNavigateHome: {}, onSelectItem: { _ in })
    }
}
