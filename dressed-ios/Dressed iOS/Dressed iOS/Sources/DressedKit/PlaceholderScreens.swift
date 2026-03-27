import SwiftUI

struct WardrobePlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "tshirt.fill")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("Wardrobe")
                .font(.title2.weight(.semibold))
            Text("NEXT: Implement grid, categories, add-piece flow — mirror Android WardrobeNav.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle("Wardrobe")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
    }
}

struct SearchPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("Search")
                .font(.title2.weight(.semibold))
            Text("NEXT: Name / filters / sort — mirror WardrobeSearchScreen.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle("Search")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
    }
}

struct OutfitsPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "rectangle.stack.fill")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("Outfits")
                .font(.title2.weight(.semibold))
            Text("NEXT: List + create outfit + collage cards — mirror OutfitsNav.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle("Outfits")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
    }
}
