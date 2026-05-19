import SwiftUI

/// First-use explainer for borrowable libraries (matches Android dialog).
struct LibraryExplainerSheet: View {
    @Binding var isPresented: Bool
    @Binding var dontShowAgain: Bool
    var onCancel: () -> Void = {}
    let onContinue: () -> Void

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(
                        "Mark pieces as “Available to lend” in your wardrobe, then export a .dressed-library file to share. " +
                            "Recipients import it into a read-only area—their own wardrobe and outfits are untouched."
                    )
                    .font(.body)
                    Text(
                        "Re-importing a file from the same person replaces their previous shared library (matched by name on the file)."
                    )
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    Toggle("Don't show again", isOn: $dontShowAgain)
                        .padding(.top, 8)
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .navigationTitle("Borrowable library")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                        isPresented = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Continue") {
                        onContinue()
                        isPresented = false
                    }
                    .tint(navPurple)
                }
            }
        }
    }
}
