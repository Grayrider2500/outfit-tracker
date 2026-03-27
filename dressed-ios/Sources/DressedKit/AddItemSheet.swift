import PhotosUI
import SwiftData
import SwiftUI
import UIKit

/// Add piece flow aligned with Android `AddItemScreen` (scroll form + PhotosPicker + HSV + seasons).
struct AddItemSheet: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    /// Called after a successful save (in addition to dismissing).
    var onSaved: () -> Void = {}

    @State private var name = ""
    @State private var category = ""
    @State private var sizeText = ""
    @State private var hueDegrees = 285.0
    @State private var saturation = 0.42
    @State private var brightness = 0.96
    @State private var colorName = ""
    @State private var seasons: Set<String> = []
    @State private var photoPickerItem: PhotosPickerItem?
    @State private var photoData: Data?
    @State private var errorHint: String?

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                photoSection

                fieldLabel("Name")
                TextField("e.g. White linen blouse", text: $name)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.sentences)

                fieldLabel("Category")
                FlowChipWrap(spacing: 8) {
                    ForEach(WardrobeCatalog.addPicker, id: \.key) { entry in
                        categoryChip(title: entry.label, selected: category == entry.key) {
                            category = entry.key
                        }
                    }
                }

                fieldLabel("Size (optional)")
                if !category.isEmpty {
                    let sug = WardrobeCatalog.sizeSuggestions(for: category)
                    if !sug.isEmpty {
                        FlowChipWrap(spacing: 8) {
                            ForEach(sug, id: \.self) { label in
                                categoryChip(title: label, selected: sizeText == label) {
                                    sizeText = label
                                }
                            }
                        }
                    }
                } else {
                    Text("Pick a category for common size shortcuts, or type your own below.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                TextField("e.g. M, 10, 9.5, or custom", text: $sizeText)
                    .textFieldStyle(.roundedBorder)

                fieldLabel("Color")
                Text("Adjust hue, saturation, and brightness — same idea as the Android color wheel.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                HStack {
                    Text(WardrobeColorMath.hsvToHex(hueDegrees: hueDegrees, saturation: saturation, brightness: brightness))
                        .font(.system(.body, design: .monospaced))
                    Spacer()
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(UIColor(hue: CGFloat(hueDegrees / 360), saturation: CGFloat(saturation), brightness: CGFloat(brightness), alpha: 1)))
                        .frame(width: 52, height: 36)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.black.opacity(0.1)))
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("Hue").font(.caption).foregroundStyle(.secondary)
                    Slider(value: $hueDegrees, in: 0 ... 360, step: 1)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("Saturation").font(.caption).foregroundStyle(.secondary)
                    Slider(value: $saturation, in: 0 ... 1, step: 0.01)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("Brightness").font(.caption).foregroundStyle(.secondary)
                    Slider(value: $brightness, in: 0 ... 1, step: 0.01)
                }

                fieldLabel("Color name")
                TextField("e.g. Lavender, Navy, Cream…", text: $colorName)
                    .textFieldStyle(.roundedBorder)
                Menu("Insert preset name") {
                    ForEach(orderedColorSuggestions, id: \.self) { suggestion in
                        Button(suggestion) { colorName = suggestion }
                    }
                }
                .font(.subheadline)

                fieldLabel("Season")
                FlowChipWrap(spacing: 8) {
                    ForEach(WardrobeCatalog.seasons, id: \.key) { entry in
                        let sel = seasons.contains(entry.key)
                        categoryChip(title: entry.label, selected: sel) {
                            if sel { seasons.remove(entry.key) } else { seasons.insert(entry.key) }
                        }
                    }
                }

                if let errorHint {
                    Text(errorHint)
                        .font(.caption)
                        .foregroundStyle(.red)
                }

                Button {
                    save()
                } label: {
                    Text("Save to Wardrobe")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(navPurple)
                .controlSize(.large)
            }
            .padding(20)
        }
        .onAppear { syncColorNameFromWheel() }
        .onChange(of: hueDegrees) { _, _ in syncColorNameFromWheel() }
        .onChange(of: saturation) { _, _ in syncColorNameFromWheel() }
        .onChange(of: brightness) { _, _ in syncColorNameFromWheel() }
        .onChange(of: photoPickerItem) { _, new in
            Task {
                photoData = try? await new?.loadTransferable(type: Data.self)
            }
        }
        .navigationTitle("Add a New Piece")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private var orderedColorSuggestions: [String] {
        let q = colorName.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if q.isEmpty { return WardrobeCatalog.colorNameSuggestions }
        let top = WardrobeCatalog.colorNameSuggestions.filter {
            $0.lowercased().hasPrefix(q) || $0.lowercased().contains(q)
        }
        let rest = WardrobeCatalog.colorNameSuggestions.filter { !top.contains($0) }
        return top + rest
    }

    private var photoSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            fieldLabel("Photo")
            PhotosPicker(selection: $photoPickerItem, matching: .images) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .strokeBorder(Color.secondary.opacity(0.3))
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color(.secondarySystemGroupedBackground)))
                    Group {
                        if let photoData, let ui = UIImage(data: photoData) {
                            Image(uiImage: ui)
                                .resizable()
                                .scaledToFill()
                        } else {
                            VStack(spacing: 8) {
                                Image(systemName: "photo")
                                    .font(.largeTitle)
                                    .foregroundStyle(.secondary)
                                Text("Tap to choose a photo")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 200)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .buttonStyle(.plain)
        }
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.subheadline.weight(.semibold))
    }

    private func categoryChip(title: String, selected: Bool, action: @escaping () -> Void) -> some View {
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

    private func syncColorNameFromWheel() {
        let hex = WardrobeColorMath.hsvToHex(hueDegrees: hueDegrees, saturation: saturation, brightness: brightness)
        colorName = WardrobeColorMath.labelForPickedColor(hex: hex)
    }

    private func save() {
        let n = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if n.isEmpty {
            errorHint = "Please enter a name"
            return
        }
        if category.isEmpty {
            errorHint = "Please select a category"
            return
        }
        errorHint = nil
        let hex = WardrobeColorMath.hsvToHex(hueDegrees: hueDegrees, saturation: saturation, brightness: brightness)
        let resolvedName = colorName.trimmingCharacters(in: .whitespacesAndNewlines)
        let finalColorName = resolvedName.isEmpty ? WardrobeColorMath.labelForPickedColor(hex: hex) : resolvedName
        let seasonOrder = ["spring", "summer", "fall", "winter"]
        let seasonsJoined = WardrobeItem.joinSeasons(seasonOrder.filter { seasons.contains($0) })

        var path: String?
        if let photoData {
            path = try? PhotoStorage.saveJPEGData(photoData)
        }

        let item = WardrobeItem(
            name: n,
            category: category,
            sizeLabel: sizeText.trimmingCharacters(in: .whitespacesAndNewlines),
            colorHex: hex,
            colorName: finalColorName,
            seasonsJoined: seasonsJoined,
            photoPath: path,
        )
        modelContext.insert(item)
        try? modelContext.save()
        onSaved()
        dismiss()
    }
}

// MARK: - Chip wrap without custom Layout (simpler for Add sheet)

private struct FlowChipWrap<Content: View>: View {
    var spacing: CGFloat = 8
    @ViewBuilder var content: () -> Content

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 76), spacing: spacing)],
            alignment: .leading,
            spacing: spacing,
        ) {
            content()
        }
    }
}
