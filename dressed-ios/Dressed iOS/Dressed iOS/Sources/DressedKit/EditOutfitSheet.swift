import SwiftData
import SwiftUI
import UIKit

/// Edit an existing outfit's name and piece selection.
struct EditOutfitSheet: View {
    let outfit: Outfit

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @Query(sort: \WardrobeItem.addedAtEpochMs, order: .reverse) private var allItems: [WardrobeItem]

    @State private var outfitName = ""
    @State private var selectedIds: Set<String> = []
    @State private var didLoad = false

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    private var trimmedName: String {
        outfitName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var canSave: Bool {
        !trimmedName.isEmpty && !selectedIds.isEmpty
    }

    var body: some View {
        VStack(spacing: 0) {
            TextField("Outfit name", text: $outfitName)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.words)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)

            Text("Pick pieces (\(selectedIds.count) selected)")
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.bottom, 4)

            if allItems.isEmpty {
                ContentUnavailableView(
                    "No wardrobe pieces",
                    systemImage: "tshirt",
                    description: Text("Add pieces in My Wardrobe first.")
                )
                .frame(maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(allItems, id: \.id) { item in
                            itemPickerRow(item: item, selected: selectedIds.contains(item.id)) {
                                toggle(item.id)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
            }

            Button(action: save) {
                Text("Save Changes")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(navPurple)
            .disabled(!canSave)
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
        }
        .onAppear { loadOutfit() }
        .navigationTitle("Edit Outfit")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private func loadOutfit() {
        guard !didLoad else { return }
        didLoad = true
        outfitName = outfit.name
        selectedIds = Set(outfit.itemIdList)
    }

    private func toggle(_ id: String) {
        if selectedIds.contains(id) {
            selectedIds.remove(id)
        } else {
            selectedIds.insert(id)
        }
    }

    private func save() {
        guard canSave else { return }
        let orderedIds = allItems.map(\.id).filter { selectedIds.contains($0) }
        outfit.name = trimmedName
        outfit.itemIdsJoined = Outfit.joinItemIds(orderedIds)
        try? modelContext.save()
        dismiss()
    }

    private func itemPickerRow(item: WardrobeItem, selected: Bool, onToggle: @escaping () -> Void) -> some View {
        let border: Color = selected ? navPurple : Color.secondary.opacity(0.3)
        let bg: Color = selected ? navPurple.opacity(0.12) : Color(.systemBackground)

        return Button(action: onToggle) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color(.secondarySystemGroupedBackground))
                    if let path = item.photoPath, FileManager.default.fileExists(atPath: path),
                       let ui = UIImage(contentsOfFile: path) {
                        Image(uiImage: ui)
                            .resizable()
                            .scaledToFill()
                    } else {
                        Text(WardrobeCatalog.emoji(forCategoryKey: item.category))
                            .font(.title2)
                    }
                }
                .frame(width: 52, height: 52)
                .clipped()

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                    Text(WardrobeCatalog.label(forCategoryKey: item.category))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if selected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(navPurple)
                }
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(bg)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        EditOutfitSheet(outfit: Outfit(name: "Preview Outfit", itemIdsJoined: ""))
    }
    .modelContainer(for: [WardrobeItem.self, Outfit.self], inMemory: true)
}
