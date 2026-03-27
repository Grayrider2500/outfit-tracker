# Dressed — Backlog (priority order)

**Assistants:** repo map and session flow → **`CLAUDE.md`** (root).

## High Priority

1. **Outfit Detail Screen** (`OutfitDetailScreen.kt`)
   - Show full collage + outfit name
   - "Mark as Worn" button (calls `outfitsViewModel.markWorn(id)`)
   - Delete outfit option (with confirmation dialog)
   - Wire click in `OutfitCollageCard` → navigate to detail

2. **Edit Outfit** — ability to rename or change which pieces are in an outfit

## Medium Priority

3. **Wear-count on Landing Screen** — quick stats card showing total wears or recently worn items

4. **Sort / Filter Outfits** — filter by season or piece count; sort by recently worn vs. created date

5. **Outfit seasons tag** — inherit seasons from constituent pieces or allow manual override

6. **Backup / Restore for Outfits** — extend `WardrobeBackupCodec` to also export/import outfit data

7. **Item detail: "Worn in outfits" list** — show which outfits an item belongs to on the item detail screen

## Lower Priority

8. **Search across outfits** — extend search or add dedicated outfits search

9. **Outfit photo** — allow user to take/upload a full outfit photo instead of / in addition to the collage

10. **Dark mode polish** — verify all new outfit screens render correctly in dark theme

11. **Accessibility** — content descriptions on collage cells; semantic roles on picker row

12. **Onboarding tooltips** — first-launch hint cards pointing to + FAB on Wardrobe and Outfits screens
