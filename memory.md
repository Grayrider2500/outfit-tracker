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

## Database
- `DressedDatabase` — current version **3**
- MIGRATION_1_2: added `sizeLabel` column to `wardrobe_items`
- MIGRATION_2_3: created `outfits` table
- Entities: `WardrobeItemEntity` (tableName = "wardrobe_items"), `OutfitEntity` (tableName = "outfits")

## Data Models
**WardrobeItemEntity**: id, name, category, sizeLabel, colorHex, colorName, seasons (List<String>), photoPath (String?), wornCount, addedAtEpochMs

**OutfitEntity**: id, name, itemIds (List<String>), wornCount, createdAtEpochMs

## Navigation Routes (DressedApp.kt)
- `landing` → LandingScreen
- `wardrobe` → WardrobeNav (nested: wardrobe_list, wardrobe_add, wardrobe_detail/{id})
- `search` → WardrobeSearchNav
- `outfits` → OutfitsNav (nested: `outfits_list`, `outfits_create`, `outfits_detail/{id}`)

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
- **Outfit detail:** read-only screen shipped (`OutfitDetailScreen`); **Mark as worn**, **delete outfit**, and **edit outfit** still on backlog
- The HTML mockup (`dressed-mockup.html`) serves as the web/design reference

## WardrobeCategories / Seasons
- Categories: tops, bottoms, dresses, shoes, outerwear, accessories (+ ALL filter)
- Seasons: spring, summer, fall, winter
- `WardrobeCategories.emoji(category)` and `.label(category)` for display

## iOS (SwiftUI sources under `dressed-ios/Dressed iOS/.../Sources/DressedKit/`)
SwiftUI + SwiftData scaffold inside the **Dressed iOS** Xcode project (see `restart.md` for setup history).

- **Wardrobe grid photos**: `WardrobeItemCard` in `WardrobeListView.swift` needs a **floating-point** aspect ratio (e.g. `.aspectRatio(3.0 / 4.0, contentMode: .fit)`). Literal `3 / 4` is integer division → **0** and hides the image strip.
- **Where photos are set**: `AddItemSheet` (PhotosPicker / camera) → **`PhotoStorage.saveOptimizedPickedPhotoJPEG`** (or `saveJPEGData` for non-picker paths) → `WardrobeItem.photoPath`. The list screen only displays thumbnails, not a full pick-a-photo hero.
