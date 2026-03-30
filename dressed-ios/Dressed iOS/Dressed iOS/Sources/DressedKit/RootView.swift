import SwiftUI

enum MainRoute: Hashable {
    case wardrobe
    case search
    case outfits
    /// Pushed on top of Search or Wardrobe; one `NavigationStack` only (nested stacks break on some simulators).
    case itemDetail(String)
}

struct RootView: View {
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            LandingView(
                onMyWardrobe: { path.append(MainRoute.wardrobe) },
                onSearch: { path.append(MainRoute.search) },
                onOutfits: { path.append(MainRoute.outfits) },
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
                    OutfitsPlaceholderView()
                case .itemDetail(let id):
                    WardrobeItemDetailView(itemId: id) {
                        path.removeLast()
                    }
                }
            }
        }
    }
}

#Preview {
    RootView()
}
