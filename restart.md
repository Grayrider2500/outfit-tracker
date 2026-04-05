# Dressed — Restart

**Assistants:** see **`CLAUDE.md`** at the repo root for where everything lives; use this file as the latest session checkpoint.

## Where We Stopped (checkpoint — verify `git status`)

**Repo:** `main` branch; last check was **clean** and **even with `origin/main`**.

### Android
- **`google-services.json` is not in git** (security). Listed in `.gitignore`.
- **Outfit detail (read-only):** `OutfitDetailScreen.kt`, route `outfits_detail/{id}`.
- **`versionCode`:** 3 in `app/build.gradle.kts`.

### iOS — Fully Functional
All core screens implemented and building successfully:
- **Wardrobe:** list grid + add + edit + detail (mark worn, delete with cascade)
- **Outfits:** list + create + edit + detail (mark worn, delete)
- **Search:** text + category + season filters, 3 sort modes
- **Picker:** rule-based suggestions + AI reasoning (BYOK — user connects own Anthropic key)
- **Backup/Restore:** v3 zip format, cross-platform with Android, merge/replace modes
- **AI Settings:** Keychain-secured API key storage, user-facing settings sheet on picker screen

### Recent iOS notes
- **`PickerView.swift` — results layout:** suggestions are a **vertical stack** (`resultsList`) inside the main `ScrollView`, not a page **`TabView`**. Users see every generated look without swiping. Per-suggestion **Save as outfit**, **Wear today**, and **detail sheet** (tap collage) are unchanged. **`WardrobePickerEngine`** logic is separate and was not changed for this.

### Earlier session batch (2026-04-02) — already landed
1. Deleted dead `PlaceholderScreens.swift`
2. Added Mark as Worn + Delete + Edit to `OutfitDetailView`
3. Added Edit mode to `AddItemSheet` (reused via `editingItem` param)
4. Created `EditOutfitSheet` for editing outfit name + pieces
5. Added Edit button to `WardrobeItemDetailView`
6. Created `APIKeyStore.swift` — Keychain wrapper for Anthropic API key
7. Created `AISettingsSheet.swift` — BYOK settings UI
8. Updated `PickerAnthropicReasoner` — removed `#if DEBUG` gate, reads from Keychain first
9. Updated `PickerView` — AI status banner + settings sheet entry point

### Security follow-up (optional)
- The old `google-services.json` may still exist in **git history**. If the repo is public, consider **rotating / restricting** the Firebase/Android API key in Google Cloud Console.

### Session batch (2026-04-05) — just landed
1. `OutfitDetailScreen` — added Mark as Worn button, Delete Outfit (with confirmation dialog), Edit pencil icon in TopAppBar
2. `OutfitsViewModel` — added `updateOutfit(updated, onUpdated)`
3. Created `EditOutfitScreen.kt` — pre-populated name + item picker, calls `updateOutfit` on save
4. `OutfitsScreen` (OutfitsNav) — added `outfits_edit/{id}` route, wired `onEdit` from detail → edit screen

### Session batch (2026-04-05 continued) — pending push
5. `OutfitsScreen` — Sort / Filter Outfits shipped (Android)
6. **Occasion hashtag tags** — shipped on **both Android and iOS**:
   - Tags: `#date night`, `#concert`, `#brunch`, `#work`, `#gym`, `#staying in`
   - **Android:** `WardrobeOccasions` model, DB migration 4→5, `AddItemScreen` chip picker, `WardrobeSearchScreen` OCCASION filter, backup codec updated
   - **iOS:** `WardrobeItem.occasionsJoined`, `WardrobeCatalog.occasions`, `AddItemSheet` chip picker, `WardrobeSearchView` OCCASION filter, `BackupRestore` DTO updated
   - Keys identical on both platforms → cross-platform backup files remain compatible

## Workflow Note
Code writing is being split: **architecture / debugging / cross-platform sync → here; single-platform UI implementation → Cursor/Codex**. Keep specs in `backlog.md` tight enough that Cursor can execute without ambiguity.

## Completed This Session
- Occasion hashtag tags — both platforms (Android DB v5, iOS SwiftData auto-migrated)
- Borrowable Library Phase 1 — iOS complete (Cursor); Android spec ready in backlog

## Next Up — Cursor Tasks (see backlog.md for full specs)
1. **Occasion tag editing on existing items** (both platforms) — Item Detail chip picker + save
2. **Borrowable Library — Android** — lendable toggle + DB migration 5→6, export, import, Libraries screen (iOS already done)

## Borrowable Library — Design Spec
- **Concept:** "Chris has these items available to borrow" — file-based, no backend
- **Sharer:** marks items `lendable = true` via toggle on Item Detail → exports `.dressed-library` zip
- **File format:** same zip structure as backup; manifest adds `"type": "library"` + `"sharerName": "Chris"`; only `lendable` items included
- **Borrower:** opens file → imports into a separate `BorrowedLibrary` store (NOT wardrobe) → shown on a dedicated Libraries screen
- **Read-only:** borrowed items cannot be edited, worn-counted, or added to outfits
- **Multiple libraries:** borrower can hold libraries from multiple people simultaneously
- **Refresh:** re-importing a library from the same sharer replaces the old one (match by sharerName or a library ID in the manifest)
- **Android DB:** `lendable INTEGER NOT NULL DEFAULT 0` on `wardrobe_items` (migration 5→6); new `borrowed_libraries` + `borrowed_items` tables
- **iOS SwiftData:** `lendable: Bool = false` on `WardrobeItem` (auto-migrates); new `BorrowedLibrary` + `BorrowedItem` models

## Next Session — Quick Start

1. Read **`memory.md`** and **`backlog.md`**, then **`CLAUDE.md`** for paths.
2. Run **`git status`**; push pending commits from Android Studio if not done.
3. Pick up next Cursor task from `backlog.md`.
4. **Tests:** no unit/UI tests yet.
