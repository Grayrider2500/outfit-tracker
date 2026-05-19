import SwiftData
import SwiftUI
import UIKit
import UniformTypeIdentifiers

private struct PendingRestore {
    var items: [WardrobeItemDTO]
    var outfits: [OutfitDTO]
    var extractedPhotoPaths: [String: String]
}

/// Hub screen aligned with Android `LandingScreen` / HTML mockup landing tiles.
struct LandingView: View {
    var onMyWardrobe: () -> Void
    var onSearch: () -> Void
    var onOutfits: () -> Void
    var onSuggestOutfits: () -> Void
    var onLibraries: () -> Void

    @Query(sort: \WardrobeItem.addedAtEpochMs) private var allItems: [WardrobeItem]
    @Query(sort: \Outfit.createdAtEpochMs) private var allOutfits: [Outfit]
    @Environment(\.modelContext) private var modelContext

    private var pieceCount: Int { allItems.count }
    private var totalWears: Int { allItems.reduce(0) { $0 + $1.wornCount } }
    private var outfitCount: Int { allOutfits.count }

    // Backup/restore state
    @State private var showingDocumentPicker = false
    @State private var pendingRestore: PendingRestore?
    @State private var showingRestoreModeChoice = false
    @State private var showingReplaceConfirmation = false
    @State private var toastMessage: String?
    @State private var showingErrorAlert = false
    @State private var errorMessage = ""
    @State private var shareURL: URL?
    @State private var showingShareSheet = false

    @AppStorage("library_explainer_seen") private var libraryExplainerSeen = false
    @State private var showLibraryExplainer = false
    @State private var libraryExplainerDontShowAgain = false
    @State private var pendingNavigateLibrariesAfterExplainer = false
    @State private var showingLibraryImporter = false

    private let gradient = LinearGradient(
        colors: [
            Color(red: 0.29, green: 0.20, blue: 0.44),
            Color(red: 0.42, green: 0.29, blue: 0.68),
            Color(red: 0.55, green: 0.38, blue: 0.83),
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing,
    )

    var body: some View {
        ZStack {
            gradient.ignoresSafeArea()
            VStack(spacing: 0) {
                // Brand mark — matches mockup frosted rounded square with dress emoji
                Text("👗")
                    .font(.system(size: 64))
                    .frame(width: 148, height: 148)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 40, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 40, style: .continuous)
                            .stroke(Color.white.opacity(0.25), lineWidth: 1)
                    )
                    .padding(.top, 48)
                    .padding(.bottom, 24)

                Text("Dressed")
                    .font(.system(size: 48, weight: .light))
                    .foregroundStyle(Color(red: 0.94, green: 0.90, blue: 1.0))
                    .padding(.bottom, 12)

                Text("YOUR PERSONAL WARDROBE")
                    .font(.system(size: 11, weight: .regular))
                    .tracking(2.8)
                    .foregroundStyle(.white.opacity(0.55))

                statsCard
                    .padding(.top, 20)
                    .padding(.horizontal, 24)

                // Nav buttons
                VStack(spacing: 14) {
                    hubButton(title: "My Wardrobe", icon: "tshirt.fill", subtitle: "Browse & manage pieces", action: onMyWardrobe)
                    hubButton(title: "Search & Filter", icon: "magnifyingglass", subtitle: "Find by name, category & season", action: onSearch)
                    hubButton(title: "Outfits", icon: "rectangle.stack.fill", subtitle: "Put looks together", action: onOutfits)
                    hubButton(title: "Borrowed libraries", icon: "books.vertical", subtitle: "Pieces friends shared with you") {
                        if !libraryExplainerSeen {
                            pendingNavigateLibrariesAfterExplainer = true
                            libraryExplainerDontShowAgain = false
                            showLibraryExplainer = true
                        } else {
                            onLibraries()
                        }
                    }
                    hubButton(title: "Suggest outfits", icon: "sparkles", subtitle: "Occasion, weather & mood picks", action: onSuggestOutfits)
                }
                .padding(.top, 12)
                .padding(.horizontal, 24)

                Spacer(minLength: 0)
            }

            // Menu button (top-right, matches mockup "⋯")
            VStack {
                HStack {
                    Spacer()
                    menuButton
                        .padding(.top, 12)
                        .padding(.trailing, 20)
                }
                Spacer()
            }
        }
        .sheet(isPresented: $showingDocumentPicker) {
            BackupDocumentPicker { result in
                handlePickedFile(result)
            }
        }
        .confirmationDialog("Restore backup", isPresented: $showingRestoreModeChoice, titleVisibility: .visible) {
            Button("Merge") { performMerge() }
            Button("Replace all", role: .destructive) { showingReplaceConfirmation = true }
            Button("Cancel", role: .cancel) { pendingRestore = nil }
        } message: {
            Text(
                "Merge adds new items and outfits and skips duplicates by id. " +
                    "Replace all clears your wardrobe and outfits, then loads the backup. " +
                    "Both options import photos from the file when adding new pieces."
            )
        }
        .alert("Replace all data?", isPresented: $showingReplaceConfirmation) {
            Button("Replace all", role: .destructive) { performReplace() }
            Button("Cancel", role: .cancel) { pendingRestore = nil }
        } message: {
            Text(
                "This will delete all existing wardrobe items and outfits, replacing them with the backup. " +
                    "Your current photos will be removed. This cannot be undone."
            )
        }
        .alert("Restore Error", isPresented: $showingErrorAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .overlay(alignment: .bottom) {
            if let message = toastMessage {
                Text(message)
                    .font(.subheadline.weight(.medium))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.thinMaterial, in: Capsule())
                    .padding(.bottom, 32)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation { toastMessage = nil }
                        }
                    }
            }
        }
        .sheet(isPresented: $showingShareSheet, onDismiss: {
            if let url = shareURL {
                try? FileManager.default.removeItem(at: url)
                shareURL = nil
            }
        }) {
            if let url = shareURL {
                ShareSheet(activityItems: [url])
            }
        }
        .sheet(isPresented: $showLibraryExplainer) {
            LibraryExplainerSheet(
                isPresented: $showLibraryExplainer,
                dontShowAgain: $libraryExplainerDontShowAgain,
                onCancel: {
                    pendingNavigateLibrariesAfterExplainer = false
                },
                onContinue: {
                    if libraryExplainerDontShowAgain {
                        libraryExplainerSeen = true
                    }
                    let goLibraries = pendingNavigateLibrariesAfterExplainer
                    pendingNavigateLibrariesAfterExplainer = false
                    if goLibraries {
                        onLibraries()
                    }
                },
            )
        }
        .fileImporter(
            isPresented: $showingLibraryImporter,
            allowedContentTypes: libraryImporterContentTypes,
            allowsMultipleSelection: false,
        ) { result in
            handleLibraryFileImport(result)
        }
    }

    private var libraryImporterContentTypes: [UTType] {
        var types: [UTType] = [.zip, .data]
        if let dressedLib = UTType(filenameExtension: "dressed-library") {
            types.insert(dressedLib, at: 0)
        }
        return types
    }

    private func handleLibraryFileImport(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            let accessed = url.startAccessingSecurityScopedResource()
            defer {
                if accessed { url.stopAccessingSecurityScopedResource() }
            }
            do {
                try DressedLibraryShare.importFromZip(url: url, modelContext: modelContext)
                withAnimation { toastMessage = "Imported shared library" }
            } catch {
                errorMessage = error.localizedDescription
                showingErrorAlert = true
            }
        case .failure(let error):
            errorMessage = error.localizedDescription
            showingErrorAlert = true
        }
    }

    private var statsCard: some View {
        HStack(spacing: 0) {
            statCell(value: pieceCount, label: "PIECES")
            Divider()
                .frame(height: 28)
                .background(Color.white.opacity(0.2))
            statCell(value: totalWears, label: "TOTAL WEARS")
            Divider()
                .frame(height: 28)
                .background(Color.white.opacity(0.2))
            statCell(value: outfitCount, label: "OUTFITS")
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(.ultraThinMaterial.opacity(0.6), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color.white.opacity(0.18), lineWidth: 1),
        )
    }

    private func statCell(value: Int, label: String) -> some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(Color(red: 0.94, green: 0.90, blue: 1.0))
            Text(label)
                .font(.system(size: 9, weight: .regular))
                .tracking(1.5)
                .foregroundStyle(.white.opacity(0.55))
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Menu

    private var menuButton: some View {
        Menu {
            Button {
                exportZipAndShare()
            } label: {
                Label("Backup to file…", systemImage: "square.and.arrow.up")
            }
            Button {
                showingDocumentPicker = true
            } label: {
                Label("Restore from file…", systemImage: "square.and.arrow.down")
            }
            Button {
                showingLibraryImporter = true
            } label: {
                Label("Import shared library…", systemImage: "tray.and.arrow.down")
            }
            Button {
                libraryExplainerSeen = false
                pendingNavigateLibrariesAfterExplainer = false
                libraryExplainerDontShowAgain = false
                showLibraryExplainer = true
            } label: {
                Label("About sharing libraries", systemImage: "info.circle")
            }
            #if DEBUG
            Button {
                seedDebugTestData()
            } label: {
                Label("Seed professional closet test data", systemImage: "leaf.fill")
            }
            #endif
        } label: {
            Image(systemName: "ellipsis")
                .font(.title3)
                .foregroundStyle(.white.opacity(0.85))
                .frame(width: 38, height: 38)
                .background(.ultraThinMaterial, in: Circle())
                .overlay(Circle().stroke(Color.white.opacity(0.22), lineWidth: 1))
        }
    }

    // MARK: - Debug seed

    #if DEBUG
    private func seedDebugTestData() {
        toastMessage = "Seeding test data…"
        do {
            let summary = try DevTestDataSeeder.run(modelContext: modelContext)
            toastMessage = summary
        } catch {
            toastMessage = nil
            errorMessage = "Seed failed: \(error.localizedDescription)"
            showingErrorAlert = true
        }
    }
    #endif

    // MARK: - Backup / restore

    private func exportZipAndShare() {
        do {
            let url = try DressedBackup.exportBackupZipFile(items: allItems, outfits: allOutfits)
            shareURL = url
            showingShareSheet = true
        } catch {
            errorMessage = "Could not create backup: \(error.localizedDescription)"
            showingErrorAlert = true
        }
    }

    private func handlePickedFile(_ result: Result<URL, Error>) {
        switch result {
        case .success(let url):
            guard url.startAccessingSecurityScopedResource() else {
                errorMessage = "Unable to access the selected file."
                showingErrorAlert = true
                return
            }
            defer { url.stopAccessingSecurityScopedResource() }
            do {
                let parsed = try DressedBackup.importBackup(from: url)
                pendingRestore = PendingRestore(
                    items: parsed.items,
                    outfits: parsed.outfits,
                    extractedPhotoPaths: parsed.extractedPhotoPaths
                )
                showingRestoreModeChoice = true
            } catch {
                errorMessage = "The selected file is not a valid Dressed backup.\n\(error.localizedDescription)"
                showingErrorAlert = true
            }
        case .failure(let error):
            errorMessage = "Failed to open file: \(error.localizedDescription)"
            showingErrorAlert = true
        }
    }

    private func performMerge() {
        guard let pending = pendingRestore else { return }
        do {
            let counts = try DressedBackup.restoreMerge(
                items: pending.items,
                outfits: pending.outfits,
                extractedPhotoPaths: pending.extractedPhotoPaths,
                modelContext: modelContext
            )
            let parts = [
                counts.newItems > 0 ? "\(counts.newItems) item\(counts.newItems == 1 ? "" : "s")" : nil,
                counts.newOutfits > 0 ? "\(counts.newOutfits) outfit\(counts.newOutfits == 1 ? "" : "s")" : nil,
            ].compactMap { $0 }
            let summary = parts.isEmpty ? "No new data to merge" : "Merged \(parts.joined(separator: ", "))"
            withAnimation { toastMessage = summary }
        } catch {
            errorMessage = "Failed to merge: \(error.localizedDescription)"
            showingErrorAlert = true
        }
        pendingRestore = nil
    }

    private func performReplace() {
        guard let pending = pendingRestore else { return }
        do {
            try DressedBackup.restoreReplace(
                items: pending.items,
                outfits: pending.outfits,
                extractedPhotoPaths: pending.extractedPhotoPaths,
                modelContext: modelContext
            )
            let count = pending.items.count + pending.outfits.count
            withAnimation { toastMessage = "Restored \(count) record\(count == 1 ? "" : "s")" }
        } catch {
            errorMessage = "Failed to restore: \(error.localizedDescription)"
            showingErrorAlert = true
        }
        pendingRestore = nil
    }

    // MARK: - Hub button

    private func hubButton(title: String, icon: String, subtitle: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title2)
                    .frame(width: 44, height: 44)
                    .foregroundStyle(Color(red: 0.29, green: 0.20, blue: 0.44))
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundStyle(Color(red: 0.18, green: 0.12, blue: 0.26))
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
            }
            .padding(16)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Document Picker

private struct BackupDocumentPicker: UIViewControllerRepresentable {
    var onPick: (Result<URL, Error>) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIDocumentPickerViewController {
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.zip, .json])
        picker.allowsMultipleSelection = false
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIDocumentPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    class Coordinator: NSObject, UIDocumentPickerDelegate {
        let parent: BackupDocumentPicker
        init(_ parent: BackupDocumentPicker) { self.parent = parent }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            if let url = urls.first {
                parent.onPick(.success(url))
            }
            parent.dismiss()
        }

        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            parent.dismiss()
        }
    }
}

// MARK: - Share sheet

private struct ShareSheet: UIViewControllerRepresentable {
    var activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

#Preview {
    LandingView(onMyWardrobe: {}, onSearch: {}, onOutfits: {}, onSuggestOutfits: {}, onLibraries: {})
}
