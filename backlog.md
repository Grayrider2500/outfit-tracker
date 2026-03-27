# Dressed — Backlog (priority order)

## High Priority

1. **Fix "coming soon" subtitle on Outfits button** (LandingScreen.kt)
   - Change `"Put looks together (coming soon)"` → `"Build and save your looks"`

2. **Outfit Detail Screen** (`OutfitDetailScreen.kt`)
   - Show full collage + outfit name
   - "Mark as Worn" button (calls `outfitsViewModel.markWorn(id)`)
   - Delete outfit option (with confirmation dialog)
   - Wire click in `OutfitCollageCard` → navigate to detail

3. **Edit Outfit** — ability to rename or change which pieces are in an outfit

## Medium Priority

4. **Wear-count on Landing Screen** — quick stats card showing total wears or recently worn items

5. **Sort / Filter Outfits** — filter by season or piece count; sort by recently worn vs. created date

6. **Outfit seasons tag** — inherit seasons from constituent pieces or allow manual override

7. **Backup / Restore for Outfits** — extend `WardrobeBackupCodec` to also export/import outfit data

8. **Item detail: "Worn in outfits" list** — show which outfits an item belongs to on the item detail screen

## Lower Priority

9. **Search across outfits** — extend search or add dedicated outfits search

10. **Outfit photo** — allow user to take/upload a full outfit photo instead of / in addition to the collage

11. **Dark mode polish** — verify all new outfit screens render correctly in dark theme

12. **Accessibility** — content descriptions on collage cells; semantic roles on picker row

13. **Onboarding tooltips** — first-launch hint cards pointing to + FAB on Wardrobe and Outfits screens
