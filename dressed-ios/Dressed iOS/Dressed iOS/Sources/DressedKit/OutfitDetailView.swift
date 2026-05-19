import SwiftData
import SwiftUI

/// Read-only outfit detail: hero collage + full piece list (including missing ids after restore).
struct OutfitDetailView: View {
    let outfitId: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    @Query private var outfitMatch: [Outfit]
    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var showDeleteConfirm = false
    @State private var showEditSheet = false

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    init(outfitId: String) {
        self.outfitId = outfitId
        _outfitMatch = Query(filter: #Predicate<Outfit> { $0.id == outfitId })
    }

    private var itemsById: [String: WardrobeItem] {
        Dictionary(uniqueKeysWithValues: allItems.map { ($0.id, $0) })
    }

    var body: some View {
        Group {
            if outfitId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                ContentUnavailableView(
                    "Outfit not found",
                    systemImage: "rectangle.stack",
                    description: Text("Invalid or empty outfit link.")
                )
            } else if let outfit = outfitMatch.first {
                detailScroll(outfit: outfit)
            } else {
                ContentUnavailableView(
                    "Outfit not found",
                    systemImage: "rectangle.stack",
                    description: Text("It may have been deleted.")
                )
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Back") { dismiss() }
                    }
                }
            }
        }
    }

    private func detailScroll(outfit: Outfit) -> some View {
        let orderedIds = outfit.itemIdList
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let resolved = orderedIds.compactMap { itemsById[$0] }
        let displayName = outfit.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? "Untitled outfit" : outfit.name

        return ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                heroCollage(resolved: resolved, orderedIds: orderedIds)
                    .aspectRatio(1, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 8) {
                    Text(displayName)
                        .font(.title2.weight(.semibold))
                    HStack(spacing: 8) {
                        let n = orderedIds.count
                        Text("\(n) piece" + (n == 1 ? "" : "s"))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        if outfit.wornCount > 0 {
                            Text("· Worn \(outfit.wornCount)×")
                                .font(.subheadline)
                                .foregroundStyle(navPurple)
                        }
                    }
                }
                .padding(.horizontal, 16)

                Text("Pieces")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 16)

                if orderedIds.isEmpty {
                    Text("No pieces are linked to this outfit.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 16)
                }

                LazyVStack(spacing: 10) {
                    ForEach(orderedIds, id: \.self) { pid in
                        if let item = itemsById[pid] {
                            pieceRow(item: item)
                        } else {
                            missingPieceRow(id: pid)
                        }
                    }
                }
                .padding(.horizontal, 16)

                // MARK: - Action buttons
                VStack(spacing: 12) {
                    Button {
                        markWorn(outfit)
                    } label: {
                        Text("+ Mark as Worn Today")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(navPurple)

                    Button {
                        showEditSheet = true
                    } label: {
                        Label("Edit Outfit", systemImage: "pencil")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(navPurple)

                    Button(role: .destructive) {
                        showDeleteConfirm = true
                    } label: {
                        Text("Delete Outfit")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .padding(.vertical, 16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("Outfit")
                    .font(.headline)
                    .foregroundStyle(.white)
            }
        }
        .confirmationDialog("Delete this outfit?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Delete", role: .destructive) {
                deleteOutfit(outfit)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This cannot be undone.")
        }
        .sheet(isPresented: $showEditSheet) {
            NavigationStack {
                EditOutfitSheet(outfit: outfit)
            }
        }
    }

    // MARK: - Actions

    private func markWorn(_ outfit: Outfit) {
        outfit.wornCount += 1
        try? modelContext.save()
    }

    private func deleteOutfit(_ outfit: Outfit) {
        modelContext.delete(outfit)
        try? modelContext.save()
        dismiss()
    }

    // MARK: - Hero (same layout rules as `OutfitCollageCard`; 👗 only when no ids and nothing resolved)

    private func heroCollage(resolved: [WardrobeItem], orderedIds: [String]) -> some View {
        let collageItems = Array(resolved.prefix(4))
        let dressOnly = resolved.isEmpty && orderedIds.isEmpty

        return GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            Group {
                if dressOnly {
                    emptyDressCell
                        .frame(width: w, height: h)
                } else if collageItems.isEmpty {
                    fourEmptyCells(w: w, h: h)
                } else if collageItems.count == 1 {
                    heroCell(item: collageItems[0])
                        .frame(width: w, height: h)
                } else {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            heroCell(item: collageItems[0])
                                .frame(width: w / 2, height: h / 2)
                            heroCell(item: collageItems.count > 1 ? collageItems[1] : nil)
                                .frame(width: w / 2, height: h / 2)
                        }
                        HStack(spacing: 0) {
                            heroCell(item: collageItems.count > 2 ? collageItems[2] : nil)
                                .frame(width: w / 2, height: h / 2)
                            heroCell(item: collageItems.count > 3 ? collageItems[3] : nil)
                                .frame(width: w / 2, height: h / 2)
                        }
                    }
                }
            }
        }
        .background(Color(.secondarySystemGroupedBackground))
    }

    private func fourEmptyCells(w: CGFloat, h: CGFloat) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                heroCell(item: nil)
                    .frame(width: w / 2, height: h / 2)
                heroCell(item: nil)
                    .frame(width: w / 2, height: h / 2)
            }
            HStack(spacing: 0) {
                heroCell(item: nil)
                    .frame(width: w / 2, height: h / 2)
                heroCell(item: nil)
                    .frame(width: w / 2, height: h / 2)
            }
        }
    }

    private var emptyDressCell: some View {
        Color(.secondarySystemGroupedBackground)
            .overlay {
                Text("👗")
                    .font(.system(size: 44))
            }
    }

    @ViewBuilder
    private func heroCell(item: WardrobeItem?) -> some View {
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

    // MARK: - Piece rows

    private func pieceRow(item: WardrobeItem) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
                CachedLocalPhotoImage(
                    photoPath: item.photoPath,
                    categoryKey: item.category,
                    emojiSize: 26,
                )
            }
            .frame(width: 52, height: 52)
            .clipped()

            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.subheadline.weight(.semibold))
                Text(WardrobeCatalog.label(forCategoryKey: item.category))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(10)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func missingPieceRow(id: String) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
                Image(systemName: "questionmark")
                    .foregroundStyle(.secondary)
            }
            .frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: 2) {
                Text("Missing piece")
                    .font(.subheadline.weight(.semibold))
                Text("Not in wardrobe · \(id)")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer()
        }
        .padding(10)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        OutfitDetailView(outfitId: "preview-id")
    }
    .modelContainer(for: [WardrobeItem.self, Outfit.self], inMemory: true)
}
