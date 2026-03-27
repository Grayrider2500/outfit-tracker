# Dressed — iOS (SwiftUI)

Native SwiftUI sibling of the Android app (`dressed-android/`). Same product idea: wardrobe pieces, search, outfits, local-first data.

## Requirements

- **Xcode 15+** (SwiftData targets **iOS 17+**)
- Apple Developer account when you are ready for devices / App Store

## Create the Xcode project (one-time)

1. Open **Xcode** → **File → New → Project…**
2. **iOS** → **App** → Next  
3. Set:
   - **Product Name:** `Dressed` (or `Dressed iOS`)
   - **Team:** your team  
   - **Organization Identifier:** e.g. `com.yourname` (you get `com.yourname.Dressed`)  
   - **Interface:** **SwiftUI**  
   - **Storage:** **SwiftData** ✓  
   - **Language:** Swift  
4. Save the project **inside this folder**  
   - Choose **`outfit-tracker/dressed-ios/`** and create the project there (Xcode will add `Dressed.xcodeproj` next to the `Dressed/` source folder that Xcode generates).
5. **Add the repo’s shared sources to the app target**  
   - If Xcode created only the default template: drag the **`Sources/DressedKit`** folder from Finder into the Xcode project navigator, check **Copy items if needed** *off* (keep files in place), and ensure the **Dressed** app target is checked.  
   - **Or** merge manually: copy the contents of `Sources/DressedKit/` into your generated app group and resolve any duplicate `*App.swift` (keep one `@main`).
6. In **Deployment Info**, set **Minimum Deployments** to **iOS 17.0** (or newer).
7. **Replace** the template `@main` `App` struct with the one in **`Sources/DressedKit/DressedApp.swift`** (or point `WindowGroup` at `RootView()` and use our `modelContainer` setup there).

**Minimal merge:** Keep Xcode’s generated app entry file but change it to:

```swift
import SwiftUI
import SwiftData

@main
struct DressedApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(for: [WardrobeItem.self, Outfit.self])
    }
}
```

Delete the default `ContentView.swift` if everything is driven from `RootView` / `LandingView`.

8. **Build** (⌘B). Fix target membership if any file is missing from the **Dressed** target (File inspector → Target Membership).

### Photo library (PhotosPicker)

Add a usage description so the system can show the access prompt:

- In the app target → **Info** → add **Privacy — Photo Library Usage Description** (`NSPhotoLibraryUsageDescription`), e.g. *“Dressed needs photo access to attach pictures of your clothing.”*

(If you use limited library / browsing APIs only, Apple may still require this string on some OS versions.)

## Layout of this folder

| Path | Purpose |
|------|---------|
| `Sources/DressedKit/` | Shared SwiftUI + SwiftData code (add to app target) |
| `README.md` | This file |

**Key sources:** `WardrobeListView` (2-column grid, category chips, total count, add sheet), `AddItemSheet` (PhotosPicker, category/size/color/season, HSV sliders), `WardrobeCatalog` / `WardrobeColorMath` (parity with Android enums and color naming), `PhotoStorage` (saves JPEG data under Documents).

After Xcode creates the project, you may have **`Dressed/`** (blue folder) from the template alongside **`Sources/`** — that is normal; consolidate into one structure over time.

## Parity with Android

- Models mirror `WardrobeItemEntity` / `OutfitEntity` (see `memory.md` in repo root).
- `seasons` / `itemIds` are stored as comma-separated strings to stay close to Room converters and future JSON backup alignment.
- **Wardrobe:** list + add-item sheet are implemented; item detail and Search / Outfits screens remain placeholders.

## Next steps

- Implement Wardrobe list + Add item (photos: `PhotosPicker`, colors, categories — match Android).
- Search and outfit collage flow per `backlog.md` / Android.
- Optional: align backup JSON with `WardrobeBackupCodec` for cross-platform restore.
