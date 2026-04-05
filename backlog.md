# Dressed — Backlog (priority order)

**Assistants:** repo map and session flow → **`CLAUDE.md`** (root).

## Next Up (Cursor tasks)

~~Android library export — switch to share sheet~~ — ✅ Complete (2026-04-05). `cacheDir` + `FileProvider` + `ACTION_SEND` share sheet, matching iOS.

~~Occasion tag editing on existing items~~ — ✅ Complete (Android + iOS, 2026-04-05)

~~Borrowable Library~~ — ✅ Both platforms complete (Cursor, 2026-04-05)

## Medium Priority

~~1. **Wear-count on Landing Screen**~~ — ✅ Cursor spec written (2026-04-05). Stats card (pieces · total wears · outfits) on both Android + iOS landing screens.

2. **Outfit seasons tag** — inherit seasons from constituent pieces or allow manual override

~~6. **Backup / Restore for Outfits**~~ — ✅ Already complete. Android `WardrobeBackupCodec.writeZipArchive` + both restore modes include outfits; iOS `BackupRestore.swift` has `OutfitDTO` + merge/replace paths.

~~7. **Item detail: "Worn in outfits" list**~~ — ✅ Cursor spec written (2026-04-05). Shows outfit names on item detail screen on both platforms via `observeOutfitsForItem` (Android) and filtered `@Query` (iOS).

## Lower Priority

8. **Search across outfits** — extend search or add dedicated outfits search

9. **Outfit photo** — allow user to take/upload a full outfit photo instead of / in addition to the collage

10. **Dark mode polish** — verify all new outfit screens render correctly in dark theme

11. **Accessibility** — content descriptions on collage cells; semantic roles on picker row

12. **Onboarding tooltips** — first-launch hint cards pointing to + FAB on Wardrobe and Outfits screens
