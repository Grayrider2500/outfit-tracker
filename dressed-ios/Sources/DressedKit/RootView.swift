import SwiftUI

enum MainRoute: Hashable {
    case wardrobe
    case search
    case outfits
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
                    WardrobeListView(onNavigateHome: {
                        if !path.isEmpty { path.removeLast() }
                    })
                case .search:
                    SearchPlaceholderView()
                case .outfits:
                    OutfitsPlaceholderView()
                }
            }
        }
    }
}

#Preview {
    RootView()
}
