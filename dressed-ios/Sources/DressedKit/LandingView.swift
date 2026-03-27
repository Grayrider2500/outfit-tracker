import SwiftUI

/// Hub screen aligned with Android `LandingScreen` / HTML mockup landing tiles.
struct LandingView: View {
    var onMyWardrobe: () -> Void
    var onSearch: () -> Void
    var onOutfits: () -> Void

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
            VStack(alignment: .leading, spacing: 20) {
                Text("Dressed")
                    .font(.largeTitle.weight(.semibold))
                    .foregroundStyle(.white)
                Text("Your closet, organized.")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.85))

                VStack(spacing: 14) {
                    hubButton(title: "My Wardrobe", icon: "tshirt.fill", subtitle: "Browse and add pieces", action: onMyWardrobe)
                    hubButton(title: "Search", icon: "magnifyingglass", subtitle: "Filter by name, category, season", action: onSearch)
                    hubButton(title: "Outfits", icon: "rectangle.stack.fill", subtitle: "Build and save your looks", action: onOutfits)
                }
                .padding(.top, 24)

                Spacer(minLength: 0)
            }
            .padding(24)
        }
    }

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

#Preview {
    LandingView(onMyWardrobe: {}, onSearch: {}, onOutfits: {})
}
