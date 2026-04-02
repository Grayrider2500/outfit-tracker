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

    @Query(sort: \WardrobeItem.addedAtEpochMs) private var allItems: [WardrobeItem]
    @Query(sort: \Outfit.createdAtEpochMs) private var allOutfits: [Outfit]
    @Environment(\.modelContext) private var modelContext

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

                // Nav buttons
                VStack(spacing: 14) {
                    hubButton(title: "My Wardrobe", icon: "tshirt.fill", subtitle: "Browse & manage pieces", action: onMyWardrobe)
                    hubButton(title: "Search & Filter", icon: "magnifyingglass", subtitle: "Find by name, category & season", action: onSearch)
                    hubButton(title: "Suggest outfits", icon: "sparkles", subtitle: "Occasion, weather & mood picks", action: onSuggestOutfits)
                    hubButton(title: "Outfits", icon: "rectangle.stack.fill", subtitle: "Put looks together", action: onOutfits)
                }
                .padding(.top, 32)
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
            #if DEBUG
            Button {
                seedDebugTestData()
            } label: {
                Label("Seed test data (100 items)", systemImage: "leaf.fill")
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
    LandingView(onMyWardrobe: {}, onSearch: {}, onOutfits: {}, onSuggestOutfits: {})
}
