import SwiftData
import SwiftUI

/// List of imported borrowed libraries (matches Android `LibrariesListScreen`).
struct LibrariesListView: View {
    var onNavigateHome: () -> Void
    var onSelectLibrary: (String) -> Void

    @Query(sort: \BorrowedLibrary.importedAtEpochMs, order: .reverse)
    private var libraries: [BorrowedLibrary]
    @Environment(\.modelContext) private var modelContext

    private let navPurple = Color(red: 0.42, green: 0.29, blue: 0.68)

    var body: some View {
        Group {
            if libraries.isEmpty {
                VStack(spacing: 12) {
                    Text("No shared libraries yet")
                        .font(.title3.weight(.semibold))
                    Text("When someone sends you a .dressed-library file, open it with Dressed or use Import on the home screen.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(libraries, id: \.id) { lib in
                        Button {
                            onSelectLibrary(lib.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(lib.sharerName)'s library")
                                    .font(.headline)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                Text("\(lib.items.count) item" + (lib.items.count == 1 ? "" : "s"))
                                    .font(.caption)
                                    .foregroundStyle(navPurple)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    .onDelete(perform: deleteLibraries)
                }
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("← Home") { onNavigateHome() }
                    .foregroundStyle(.white)
            }
            ToolbarItem(placement: .principal) {
                Text("Borrowed libraries")
                    .font(.headline)
                    .foregroundStyle(.white)
            }
        }
        .toolbarBackground(navPurple, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private func deleteLibraries(at offsets: IndexSet) {
        for index in offsets {
            modelContext.delete(libraries[index])
        }
        try? modelContext.save()
    }
}
