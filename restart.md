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

## Next Session — Quick Start

1. Read **`memory.md`** and **`backlog.md`**, then **`CLAUDE.md`** for paths.
2. Run **`git status`**; pull if you work on another machine.
3. **iOS sanity:** open Picker → Surprise me → confirm **multiple** suggestion cards stack vertically and scroll.
4. **Android backlog:** outfit detail actions (mark worn / delete / edit) still per **`backlog.md`**.
5. **Tests:** no unit/UI tests yet.
