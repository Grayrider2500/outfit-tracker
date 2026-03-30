import SwiftUI

enum MainRoute: Hashable {
    case wardrobe
    case search
    case outfits
    /// Pushed on top of search; keep navigation in one `NavigationStack` (nested stacks break Search on some simulators).
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
                    WardrobeListView(onNavigateHome: { path.removeLast() })
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
