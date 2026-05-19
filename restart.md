# Dressed — Restart

**Assistants:** see **`CLAUDE.md`** at the repo root for where everything lives; use this file as the latest session checkpoint.

## Where We Stopped (2026-03-30)

iOS **Dressed** builds and runs (simulator + device-ready). Latest **`main`** on GitHub includes search, wardrobe → detail, App Icon fix, and navigation fixes. Android unchanged in recent iOS-focused work.

## What Exists Now (iOS — `dressed-ios/`)

### Navigation (`RootView`, single `NavigationStack`)
- **Landing** → **My Wardrobe** | **Search & Filter** | **Outfits** (placeholder)
- **`MainRoute`**: `wardrobe`, `search`, `outfits`, **`itemDetail(String)`** (shared by wardrobe + search; avoid nested `NavigationStack` inside destinations — broke Search on simulator for some runtimes)

### Wardrobe
- **`WardrobeListView`** — grid, category chips, `+` → **`AddItemSheet`**, **tap card** → item detail
- **`WardrobeItemDetailView`** — layout near Android `ItemDetailScreen`: hero, size, color swatch, seasons, times worn + **outfit count** (outfits referencing item id), **Mark as Worn Today**, **Remove from Wardrobe** (confirms, deletes SwiftData row + **`PhotoStorage.deleteFileIfExists`**)

### Search
- **`WardrobeSearchView`** — name field, category / season chips, sort (recent / most worn / A→Z), list rows → same **`itemDetail`** route
- **`WardrobeCatalog.searchSeasonFilters`** — fall shown as Autumn
- **`WardrobeItem.wardrobeSubtitleLine`**, **`sortedForDisplay(_:)`** on `[WardrobeItem]`

### Other
- **Landing** backup/restore JSON (unchanged pattern)
- **App icon** — `AppIcon.appiconset/Contents.json` is **iOS-only** single 1024×1024 entry (removed invalid mac slots that reused one PNG and caused asset validation errors)
- **Placeholders removed** from `PlaceholderScreens.swift` (only **Outfits** placeholder remains)

### Git
- Pushes included: search + detail + AppIcon ; then wardrobe grid + detail actions (`1b8424f` area — verify with `git log` on `main`)

## Next Steps (when resuming)

1. **Device QA** — user testing add items, search, wardrobe, backup/restore, camera/photos on iPhone
2. **Outfits** — replace placeholder: list, create outfit, collage cards (mirror Android `OutfitsNav`)
3. **Item edit** — Android may not have full edit on item; optional: edit piece from detail or separate flow
4. **Android backlog** — outfit detail / edit outfit per `backlog.md` if still open

## Quick paths

| Area | Primary sources |
|------|-----------------|
| iOS app entry | `dressed-ios/Dressed iOS/Dressed iOS/Dressed_iOSApp.swift` (SwiftData container) |
| Kit sources | `dressed-ios/Dressed iOS/Dressed iOS/Sources/DressedKit/` |
| Xcode project | `dressed-ios/Dressed iOS/Dressed iOS.xcodeproj` |
| Android parity | `dressed-android/app/.../wardrobe/` |
