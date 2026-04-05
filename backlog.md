# Dressed — Backlog (priority order)

**Assistants:** repo map and session flow → **`CLAUDE.md`** (root).

## Next Up (Cursor tasks)

1. **Android library export — switch to share sheet** — Currently uses `CreateDocument` save picker. Replace with: write zip to `context.cacheDir`, expose via `FileProvider`, launch `ACTION_SEND` intent with `setType("application/zip")` and `putExtra(EXTRA_STREAM, uri)`. Matches iOS `UIActivityViewController` share sheet behaviour. `FileProvider` authority is already declared for camera (`${packageName}.fileprovider`).

2. ~~Occasion tag editing on existing items~~ — ✅ Complete (Android + iOS, 2026-04-05)

3. ~~Borrowable Library~~ — ✅ Both platforms complete (Cursor, 2026-04-05)

## Medium Priority

1. **Wear-count on Landing Screen** — quick stats card showing total wears or recently worn items

2. **Outfit seasons tag** — inherit seasons from constituent pieces or allow manual override

6. **Backup / Restore for Outfits** — extend `WardrobeBackupCodec` to also export/import outfit data

7. **Item detail: "Worn in outfits" list** — show which outfits an item belongs to on the item detail screen

## Lower Priority

8. **Search across outfits** — extend search or add dedicated outfits search

9. **Outfit photo** — allow user to take/upload a full outfit photo instead of / in addition to the collage

10. **Dark mode polish** — verify all new outfit screens render correctly in dark theme

11. **Accessibility** — content descriptions on collage cells; semantic roles on picker row

12. **Onboarding tooltips** — first-launch hint cards pointing to + FAB on Wardrobe and Outfits screens
