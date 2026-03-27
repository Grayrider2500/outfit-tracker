import SwiftData
import SwiftUI

@main
struct Dressed_iOSApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(for: [WardrobeItem.self, Outfit.self])
    }
}
