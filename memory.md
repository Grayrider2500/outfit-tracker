# Dressed — Session Memory

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
- Coil (`AsyncImage`) for local file images; photos stored as absolute paths via `ImageStorage`
- `Converters.kt` handles `List<String>` → comma-joined string for Room (used for both seasons and itemIds)

## Build Config
- `minSdk = 23` (Android 6.0) — lowered from 26 to support Samsung SM-S727VL test device
- `targetSdk = 35`, `compileSdk = 35`

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
- `outfits` → OutfitsNav (nested: outfits_list, outfits_create)

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
- Outfit detail screen deferred to future iteration
- Mark-as-worn on outfit detail screen deferred to future iteration
- The HTML mockup (`dressed-mockup.html`) serves as the web/design reference

## WardrobeCategories / Seasons
- Categories: tops, bottoms, dresses, shoes, outerwear, accessories (+ ALL filter)
- Seasons: spring, summer, fall, winter
- `WardrobeCategories.emoji(category)` and `.label(category)` for display
