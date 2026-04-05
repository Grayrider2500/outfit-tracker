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
5. `OutfitsScreen` — Sort / Filter Outfits shipped:
   - **Sort:** Newest (createdAtEpochMs desc) | Most Worn (wornCount desc) | A–Z (name asc)
   - **Season filter:** All | Spring | Summer | Autumn | Winter — inferred from constituent wardrobe piece seasons
   - **Size filter:** Any | Solo (1) | 2–3 pcs | 4+ pcs
   - Three horizontal `LazyRow` chip bars above the grid; state persists via `rememberSaveable`
   - `EmptyOutfitsState` now shows contextual message ("No outfits match / Try adjusting filters") when filters are active

## Next Session — Quick Start

1. Read **`memory.md`** and **`backlog.md`**, then **`CLAUDE.md`** for paths.
2. Run **`git status`**; push the pending commit if not done from Android Studio.
3. **Next backlog item:** Wear-count stats card on Landing Screen.
4. **Tests:** no unit/UI tests yet.
