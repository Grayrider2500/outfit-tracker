import SwiftData
import SwiftUI

/// Entry point: add this file to your Xcode app target, or paste this `App` body into Xcode's generated `*App.swift`.
@main
struct DressedApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(for: [WardrobeItem.self, Outfit.self])
    }
}
