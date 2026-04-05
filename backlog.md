# Dressed — Backlog (priority order)

**Assistants:** repo map and session flow → **`CLAUDE.md`** (root).

## Done (reference)

- Android library export — share sheet (`FileProvider` + `ACTION_SEND`).
- Occasion tag editing on existing items (Android + iOS).
- Borrowable Library (both platforms).
- Wear-count / **stats card** on landing (both platforms).
- **iOS Outfits sort/filter parity** — `OutfitsListView` matches Android chip rows (`displayedOutfits`, `filterBar`, `FilterChip`).
- **Item detail: “Worn in outfits”** (both platforms).
- Backup/restore including outfits (both platforms).
- **Android photo persistence fix** (build 5) — gallery reads bytes once on main thread (`copyFromBytes`); camera uses `copyFromFile`; fixes Samsung one-shot stream pipe.
- **Android edit item** (build 5) — Edit icon on `ItemDetailScreen`; `AddItemScreen(editingItemId)`; `saveEdit` in ViewModel; edit routes in `WardrobeNav` + `WardrobeSearchNav`.
- **versionCode 5 / versionName 1.1.0** — signed APK distributed to testers.

## Medium priority

1. **Outfit seasons tag** — inherit seasons from constituent pieces or allow manual override.

## Lower priority

2. **Search across outfits** — extend search or add dedicated outfits search.

3. **Outfit photo** — user-taken or uploaded full-outfit photo in addition to collage.

4. **Dark mode polish** — verify outfit screens in dark theme.

5. **Accessibility** — content descriptions on collage cells; semantic roles on picker rows.

6. **Onboarding tooltips** — first-launch hints for Wardrobe / Outfits FABs.
