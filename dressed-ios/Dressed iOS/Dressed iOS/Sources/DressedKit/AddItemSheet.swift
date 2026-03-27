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
    @State private var selectedColor = Color(.sRGB, red: 0.55, green: 0.38, blue: 0.83)
    @State private var colorName = ""
    @State private var seasons: Set<String> = []
    @State private var photoPickerItem: PhotosPickerItem?
    @State private var photoData: Data?
    @State private var showCamera = false
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

                ColorPicker(selection: $selectedColor, supportsOpacity: false) {
                    HStack {
                        fieldLabel("Color")
                        Spacer()
                        Text(WardrobeColorMath.hexFromColor(selectedColor))
                            .font(.system(.caption, design: .monospaced))
                            .foregroundStyle(.secondary)
                    }
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
        .onAppear { syncColorName() }
        .onChange(of: selectedColor) { _, _ in syncColorName() }
        .onChange(of: photoPickerItem) { _, new in
            Task {
                photoData = try? await new?.loadTransferable(type: Data.self)
            }
        }
        .navigationTitle("Add a New Piece")
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

            // Preview area
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
                            Text("Add a photo below")
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

            // Gallery + Camera buttons
            HStack(spacing: 12) {
                PhotosPicker(selection: $photoPickerItem, matching: .images) {
                    Label("Gallery", systemImage: "photo.on.rectangle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(navPurple)

                if UIImagePickerController.isSourceTypeAvailable(.camera) {
                    Button {
                        showCamera = true
                    } label: {
                        Label("Camera", systemImage: "camera")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .tint(navPurple)
                }
            }
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker { image in
                photoData = image.jpegData(compressionQuality: 0.85)
            }
            .ignoresSafeArea()
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

    private func syncColorName() {
        let hex = WardrobeColorMath.hexFromColor(selectedColor)
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
        let hex = WardrobeColorMath.hexFromColor(selectedColor)
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

// MARK: - Camera picker (wraps UIImagePickerController)

private struct CameraPicker: UIViewControllerRepresentable {
    var onImageCaptured: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPicker
        init(_ parent: CameraPicker) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.onImageCaptured(image)
            }
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
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
