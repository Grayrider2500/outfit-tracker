import SwiftData
import SwiftUI

enum MainRoute: Hashable {
    case wardrobe
    case search
    case outfits
    case picker
    case libraries
    case borrowedLibraryDetail(String)
    /// Pushed on top of Search or Wardrobe; one `NavigationStack` only (nested stacks break on some simulators).
    case itemDetail(String)
    case outfitDetail(String)
}

struct RootView: View {
    @State private var path = NavigationPath()
    @Environment(\.modelContext) private var modelContext
    @State private var openURLToast: String?
    @State private var libraryImportError = ""
    @State private var showLibraryImportError = false

    var body: some View {
        NavigationStack(path: $path) {
            LandingView(
                onMyWardrobe: { path.append(MainRoute.wardrobe) },
                onSearch: { path.append(MainRoute.search) },
                onOutfits: { path.append(MainRoute.outfits) },
                onSuggestOutfits: { path.append(MainRoute.picker) },
                onLibraries: { path.append(MainRoute.libraries) },
            )
            .navigationDestination(for: MainRoute.self) { route in
                switch route {
                case .wardrobe:
                    WardrobeListView(
                        onNavigateHome: { path.removeLast() },
                        onSelectItem: { itemId in
                            path.append(MainRoute.itemDetail(itemId))
                        },
                    )
                case .search:
                    WardrobeSearchView(
                        onNavigateHome: { path.removeLast() },
                        onSelectItem: { itemId in
                            path.append(MainRoute.itemDetail(itemId))
                        },
                    )
                case .outfits:
                    OutfitsListView(
                        onNavigateHome: { path.removeLast() },
                        onSelectOutfit: { path.append(MainRoute.outfitDetail($0)) }
                    )
                case .picker:
                    PickerView(onNavigateHome: { path.removeLast() })
                case .libraries:
                    LibrariesListView(
                        onNavigateHome: { path.removeLast() },
                        onSelectLibrary: { path.append(MainRoute.borrowedLibraryDetail($0)) },
                    )
                case .borrowedLibraryDetail(let libraryId):
                    BorrowedLibraryDetailView(libraryId: libraryId) {
                        path.removeLast()
                    }
                case .itemDetail(let id):
                    WardrobeItemDetailView(itemId: id) {
                        path.removeLast()
                    }
                case .outfitDetail(let outfitId):
                    OutfitDetailView(outfitId: outfitId)
                }
            }
        }
        .onOpenURL { url in
            importSharedLibraryIfPresented(url: url)
        }
        .alert("Library import", isPresented: $showLibraryImportError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(libraryImportError)
        }
        .overlay(alignment: .bottom) {
            if let message = openURLToast {
                Text(message)
                    .font(.subheadline.weight(.medium))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.thinMaterial, in: Capsule())
                    .padding(.bottom, 32)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation { openURLToast = nil }
                        }
                    }
            }
        }
    }

    private func importSharedLibraryIfPresented(url: URL) {
        let p = url.path.lowercased()
        guard p.hasSuffix(".dressed-library") else { return }
        let accessed = url.startAccessingSecurityScopedResource()
        defer {
            if accessed { url.stopAccessingSecurityScopedResource() }
        }
        do {
            try DressedLibraryShare.importFromZip(url: url, modelContext: modelContext)
            withAnimation { openURLToast = "Imported shared library" }
        } catch {
            libraryImportError = error.localizedDescription
            showLibraryImportError = true
        }
    }
}

#Preview {
    RootView()
}
