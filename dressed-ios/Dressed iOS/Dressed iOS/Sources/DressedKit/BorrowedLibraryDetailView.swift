import SwiftData
import SwiftUI

/// Read-only grid of borrowed items (matches Android `BorrowedLibraryDetailScreen`).
struct BorrowedLibraryDetailView: View {
    let libraryId: String
    var onBack: () -> Void

    @Query(sort: \BorrowedLibrary.importedAtEpochMs, order: .reverse)
    private var allLibraries: [BorrowedLibrary]
    @Environment(\.modelContext) private var modelContext

    private let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14),
    ]

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var library: BorrowedLibrary? { allLibraries.first { $0.id == libraryId } }

    var body: some View {
        Group {
            if let lib = library {
                ScrollView {
                    Text("Read-only — borrowed pieces")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(lib.items.sorted(by: { $0.addedAtEpochMs > $1.addedAtEpochMs }), id: \.id) { item in
                            BorrowedPieceCard(item: item)
                        }
                    }
                    .padding(16)
                }
            } else {
                ContentUnavailableView("Library not found", systemImage: "books.vertical")
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("← Back") { onBack() }
                    .foregroundStyle(.white)
            }
            ToolbarItem(placement: .principal) {
                Text((library.map { "\($0.sharerName)'s library" }) ?? "Library")
                    .font(.headline)
                    .foregroundStyle(.white)
            }
            ToolbarItem(placement: .topBarTrailing) {
                if library != nil {
                    Menu {
                        Button("Remove library", role: .destructive) {
                            removeCurrentLibrary()
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .foregroundStyle(.white)
                    }
                    .accessibilityLabel("Library actions")
                }
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private func removeCurrentLibrary() {
        let id = libraryId
        onBack()
        Task { @MainActor in
            let predicate = #Predicate<BorrowedLibrary> { $0.id == id }
            var descriptor = FetchDescriptor<BorrowedLibrary>(predicate: predicate)
            descriptor.fetchLimit = 1
            if let lib = try? modelContext.fetch(descriptor).first {
                modelContext.delete(lib)
                try? modelContext.save()
            }
        }
    }
}

private struct BorrowedPieceCard: View {
    let item: BorrowedItem

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                Rectangle().fill(Color(.secondarySystemGroupedBackground))
                CachedLocalPhotoImage(
                    photoPath: item.photoPath,
                    categoryKey: item.category,
                    emojiSize: 44,
                )
            }
            .aspectRatio(3.0 / 4.0, contentMode: .fit)
            .clipped()

            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                Text(WardrobeCatalog.label(forCategoryKey: item.category))
                    .font(.caption2)
                    .foregroundStyle(Color(red: 0.42, green: 0.29, blue: 0.68))
            }
            .padding(10)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
    }
}
