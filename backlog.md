# Dressed — Backlog (priority order)

**Assistants:** repo map and session flow → **`CLAUDE.md`** (root).

## Next Up (Cursor tasks)

1. **Occasion tag editing on existing items** — Add occasions chip picker to Item Detail screen (Android `ItemDetailScreen.kt` + iOS `WardrobeItemDetailView.swift`). Pre-populate from current `item.occasions` / `item.occasionsList`. Android needs `updateItem(entity)` on `WardrobeViewModel` calling `repository.insert(entity)`. iOS writes back to `item.occasionsJoined` and calls `try? modelContext.save()`.

2. **Borrowable Library** — ✅ Both platforms complete (Cursor, 2026-04-05)
   - Export, import, Libraries screen, delete (swipe iOS / menu Android), explainer dialog
   - Cascade deletes confirmed on both platforms

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
