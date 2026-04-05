# Dressed — Session Memory

**Assistants:** read **`CLAUDE.md`** (repo root) first for the full map, then this file for Android technical memory.

## Project
Android wardrobe + outfit tracking app built with Jetpack Compose / Material3 / Room.
Package: `com.dressed.app`
Base path: `dressed-android/app/src/main/java/com/dressed/app`
Repo: https://github.com/Grayrider2500/outfit-tracker
Live URL: https://grayrider2500.github.io/outfit-tracker/

## Architecture
- MVVM: Room DAO → Repository → AndroidViewModel → Composable screens
- Single `WardrobeViewModel` for all wardrobe operations
- `OutfitsViewModel` for all outfit operations
- NavHost in `DressedApp.kt` with nested NavHosts inside `WardrobeNav` and `OutfitsNav`
- Coil (`AsyncImage`) for local file images; photos stored as absolute paths via `ImageStorage` (new URIs copied as downscaled JPEGs, long edge ≤ 1600px, quality ~87%)
- **`coilPhotoFileOrNull`** in `ui/wardrobe/WardrobeComponents.kt` — use for `AsyncImage` when loading `photoPath` so missing/unreadable files fall back to emoji
- `Converters.kt` handles `List<String>` → comma-joined string for Room (used for both seasons and itemIds)

## Build Config
- `minSdk = 23` (Android 6.0) — lowered from 26 to support Samsung SM-S727VL test device
- `targetSdk = 36`, `compileSdk = 36`
- **Firebase:** `com.google.gms.google-services` is applied; **`dressed-android/app/google-services.json` is gitignored** — add locally from Firebase Console (or CI) so builds succeed; never commit real keys to a public repo.
- **Debug-only seed data:** `com.dressed.app.data.dev.TestDataSeeder` — implementation in **`app/src/debug/java/...`**, **`app/src/release/java/...`** stub throws if called (UI + ViewModel still `BuildConfig.DEBUG`-gated).

## Database
- `DressedDatabase` — current version **6**
- MIGRATION_1_2: added `sizeLabel` to `wardrobe_items`
- MIGRATION_2_3: created `outfits` table
- MIGRATION_3_4: added `lastWornAtEpochMs` to `wardrobe_items`
- MIGRATION_4_5: added `occasions TEXT NOT NULL DEFAULT ''` to `wardrobe_items`
- MIGRATION_5_6: added `lendable INTEGER NOT NULL DEFAULT 0` to `wardrobe_items`; created `borrowed_libraries` and `borrowed_items` tables
- Entities: `WardrobeItemEntity`, `OutfitEntity`, `BorrowedLibraryEntity`, `BorrowedItemEntity`

## Data Models
**WardrobeItemEntity**: id, name, category, sizeLabel, colorHex, colorName, seasons (List<String>), occasions (List<String>), lendable (Boolean = false), photoPath (String?), wornCount, lastWornAtEpochMs, addedAtEpochMs

**OutfitEntity**: id, name, itemIds (List<String>), wornCount, createdAtEpochMs

**BorrowedLibraryEntity**: id, sharerName, importedAtEpochMs

**BorrowedItemEntity**: mirrors WardrobeItemEntity fields + libraryId (FK → BorrowedLibraryEntity)

## Navigation Routes (DressedApp.kt)
- `landing` → LandingScreen
- `wardrobe` → WardrobeNav (nested: wardrobe_list, wardrobe_add, wardrobe_detail/{id})
- `search` → WardrobeSearchNav
- `outfits` → OutfitsNav (nested: `outfits_list`, `outfits_create`, `outfits_detail/{id}`, `outfits_edit/{id}`)
- `picker` → PickerScreen
- `libraries` → LibrariesScreen (list of imported BorrowedLibrary cards)
- `library_detail/{id}` → BorrowedLibraryDetailScreen (read-only item grid)

## Borrowable Library — File Format
- Extension: `.dressed-library` (ZIP under the hood, same structure as backup)
- Manifest (`metadata.json`) adds: `"type": "library"`, `"sharerName": "Chris"`
- Only `lendable = true` items included; photos bundled same as backup
- Import replaces existing library from same sharerName (matched by sharerName or manifest library ID)
- Explainer dialog: shown on first visit to Libraries or first Export attempt; dismissed with optional "Don't show again" checkbox; resettable from overflow menu ("About Sharing Libraries")
- SharedPreferences key (Android): `library_explainer_seen`; `@AppStorage` key (iOS): `library_explainer_seen`
- Sharer name stored in SharedPreferences (Android) / UserDefaults key `library_sharer_name` (iOS)
- **Android export:** `WardrobeViewModel.exportBorrowableLibraryForShare(sharerName, onDone)` writes to `cacheDir`, returns a `FileProvider` URI (authority `${packageName}.fileprovider`), launches `ACTION_SEND` share sheet. `res/xml/file_paths.xml` has `<cache-path name="cache_root" path="." />` for this. No `CreateDocument` picker.

## App Icon
- Generated PNG icons at all mipmap densities (mdpi→xxxhdpi) using Python/cairosvg
- Design: purple gradient (#4A3370→#8B62D4) + frosted rounded container + white dress silhouette with waist line
- `ic_launcher_foreground.xml` updated with vector version of the same dress design
- `ic_launcher_background` color: `#5A3A96`
- Both square (`ic_launcher.png`) and round (`ic_launcher_round.png`) variants generated

## Add Item Screen
- Removed emulator-only "Browse files (Mac → drag onto emulator)" button and warning text
- Removed `openImageDocument` launcher (was emulator-only workaround)
- Photo preview tap now opens Gallery picker (PickVisualMedia) — same as Gallery button
- Camera error message no longer mentions emulator
- Three photo options remain: tap preview / Gallery button → photo picker; Take Photo → camera

## Key Decisions
- Outfit card style: **Collage** (2×2 grid of item photos, square aspect ratio)
- Empty collage cells show muted surfaceVariant background
- Single-item outfits show the photo full-size (no 2×2 split)
- **Outfit detail:** full actions shipped — Mark as Worn button, Delete (with confirmation dialog), Edit pencil icon in TopAppBar → `EditOutfitScreen`
- **`updateOutfit(updated, onUpdated)`** added to `OutfitsViewModel` — uses `OnConflictStrategy.REPLACE` to update in place
- The HTML mockup (`dressed-mockup.html`) serves as the web/design reference

## WardrobeCategories / Seasons
- Categories: tops, bottoms, dresses, shoes, outerwear, accessories (+ ALL filter)
- Seasons: spring, summer, fall, winter
- `WardrobeCategories.emoji(category)` and `.label(category)` for display

## iOS (SwiftUI sources under `dressed-ios/Dressed iOS/.../Sources/DressedKit/`)
SwiftUI + SwiftData inside the **Dressed iOS** Xcode project (see `restart.md` for setup history).

- **Outfits list (`OutfitsListView.swift`):** `@Query(sort: \Outfit.createdAtEpochMs, order: .reverse)` unchanged; **`displayedOutfits`** applies in-memory **sort** (newest / most worn / name via `localizedLowercase`) and **filters**: season = any constituent piece’s **`WardrobeItem.seasonsList`** contains key (`fall` chip labeled Autumn); piece-count bands solo (1), 2–3, 4+. Horizontal **`filterBar`** with **`FilterChip`**; **`noResultsState`** when filters empty the list.
- **Dev/test seed:** **`DevTestDataSeeder.swift`** wrapped in **`#if DEBUG`** only (no seed strings in Release binaries). Landing seed action is `#if DEBUG`.
- **Picker results UI:** `PickerView.swift` lists suggestions in a **vertical `ForEach`** within the screen `ScrollView` (`resultsList`). A paged **`TabView`** was removed so all 1–3 engine results are visible without horizontal swipe.
- **Wardrobe grid photos**: `WardrobeItemCard` in `WardrobeListView.swift` needs a **floating-point** aspect ratio (e.g. `.aspectRatio(3.0 / 4.0, contentMode: .fit)`). Literal `3 / 4` is integer division → **0** and hides the image strip.
- **Where photos are set**: `AddItemSheet` (PhotosPicker / camera) → **`PhotoStorage.saveOptimizedPickedPhotoJPEG`** (or `saveJPEGData` for non-picker paths) → `WardrobeItem.photoPath`. The list screen only displays thumbnails, not a full pick-a-photo hero.
- **Borrowable Library (iOS):** `Dressed_iOSApp.swift` modelContainer includes `BorrowedLibrary` + `BorrowedItem`. `RootView` handles `.onOpenURL` for `.dressed-library` files. `WardrobeListView` has ⋯ menu for export + about. `LandingView` has Libraries hub button. `LibraryExplainerSheet` is reused across both entry points. `DressedLibraryShare.sharerNameDefaultsKey = "library_sharer_name"`.
- **UTI / document type:** may need entry in Xcode target Info tab for system to route `.dressed-library` files to the app from Files/Mail.
