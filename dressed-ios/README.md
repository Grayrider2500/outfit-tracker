# Dressed — iOS (SwiftUI)

Native **SwiftUI + SwiftData** sibling of the Android app (`dressed-android/`). Same product: **local-first** wardrobe, outfits, search, wear tracking, **ZIP backup/restore**, and **borrowable `.dressed-library`** import/export—not a bare scaffold; feature set is tracked against Android in **`memory.md`** and **`backlog.md`**.

## Requirements

- **Xcode 15+** (SwiftData targets **iOS 17+**)
- Apple Developer account for devices / TestFlight / App Store

## Create the Xcode project (one-time)

1. Open **Xcode** → **File → New → Project…**
2. **iOS** → **App** → Next  
3. Set:
   - **Product Name:** `Dressed` (or `Dressed iOS`)
   - **Team:** your team  
   - **Organization Identifier:** e.g. `com.yourname`  
   - **Interface:** **SwiftUI**  
   - **Storage:** **SwiftData** ✓  
   - **Language:** Swift  
4. Save the project **inside** `outfit-tracker/dressed-ios/` (alongside this README).
5. **Add the repo’s shared sources to the app target**  
   - Drag **`Sources/DressedKit`** (or the concrete path used in this repo, e.g. **`Dressed iOS/Dressed iOS/Sources/DressedKit`**) into the navigator; **Copy items if needed** *off*; enable the app **target membership**.  
   - Resolve duplicate `*App.swift` / `@main` so a single entry uses **`RootView()`** and the same **`modelContainer`** models as in `DressedApp.swift` / `RootView.swift`.
6. **Deployment Info:** **iOS 17.0** (or newer).
7. **Build** (⌘B). Fix **target membership** for any stray files.

**Minimal app entry** (if you merge by hand):

```swift
import SwiftUI
import SwiftData

@main
struct DressedApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(for: [WardrobeItem.self, Outfit.self, BorrowedLibrary.self, BorrowedItem.self])
    }
}
```

Use the project’s actual `modelContainer` line if additional models are registered.

### Photo library (PhotosPicker)

In the app target → **Info**, add **Privacy — Photo Library Usage Description** (`NSPhotoLibraryUsageDescription`), e.g. *“Dressed needs photo access to attach pictures of your clothing.”*

### Open `.dressed-library` files

Document types / exported UTI for the library extension may need to be set in **Info** so **Files** / **Mail** can open shares into the app (see Xcode **Document Types** / **Imported Type Identifiers** as you solidify the extension).

## Layout of this folder

| Path | Purpose |
|------|---------|
| `Sources/DressedKit/` (under the Xcode project group) | SwiftUI + SwiftData: landing, wardrobe, search, outfits, picker, backup, libraries |
| `STORE_LISTING.md` | Draft **App Store** copy (subtitle, keywords, description) |
| `README.md` | This file |

**Notable modules (high level):** `RootView` / `LandingView` (stats, backup & library entry), `WardrobeListView` + `AddItemSheet`, `WardrobeItemDetailView` (wear, lend toggle, **worn-in-outfits** list), search and outfit flows, `DressedBackup` / import-export, `DressedLibraryShare`, borrowed library screens, `PickerView` (suggestions + debug-only API explanation path), `WardrobeCatalog` / color helpers aligned with Android.

After Xcode creates the project, a template **`Dressed/`** group may sit next to **`Sources/`**; consolidate over time.

## Parity with Android (current)

- Models mirror **Room** entities: wardrobe items, outfits, seasons/`itemIds` as stored strings compatible with backup codecs.  
- **Borrowed libraries:** SwiftData models + import path; sharer name / explainer prefs mirror Android.  
- **Seed / test data:** **`DevTestDataSeeder`** is wrapped in **`#if DEBUG`** only—**not** compiled into **Release** / App Store–style builds. The landing menu seed action is also `#if DEBUG`.  
- Remaining gaps vs Android: see **`backlog.md`** at repo root.

## Store copy

Draft marketing text for the App Store lives in **`STORE_LISTING.md`** in this folder.
