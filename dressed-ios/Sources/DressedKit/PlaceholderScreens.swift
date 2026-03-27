import SwiftUI

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
        .navigationBarTitleDisplayMode(.inline)
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
        .navigationBarTitleDisplayMode(.inline)
    }
}
