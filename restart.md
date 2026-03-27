# Dressed — Restart

**Assistants:** see **`CLAUDE.md`** at the repo root for where everything lives; use this file as the latest session checkpoint.

## Where We Stopped (2026-03-27)

iOS app scaffolded, building, and committed. Android app unchanged from previous session.

## What Was Done This Session (iOS)

### Project setup
- Created Xcode project, merged Cursor's DressedKit files, resolved duplicate `@main` and template conflicts
- Cleaned up stale `Sources/DressedKit/` folder (duplicates), updated `.gitignore` to track Xcode project files
- Removed nested `.git` from Xcode project directory

### Wardrobe list + Add item
- `WardrobeListView` — 2-column grid, category filter chips, item count, empty state, item cards with photo/emoji
- `AddItemSheet` — PhotosPicker + Camera, system ColorPicker (replaced manual HSV sliders), category/size chips, season toggles, validation
- `WardrobeCatalog` / `WardrobeColorMath` — category keys, emoji, size suggestions, color name presets, hex extraction from SwiftUI Color
- `PhotoStorage` — JPEG save to Documents, read helper for backup export
- Camera: `UIImagePickerController` wrapper, hidden in Simulator (no hardware), privacy keys added (NSCameraUsageDescription, NSPhotoLibraryUsageDescription)

### Landing page
- Brand mark (frosted rounded square + 👗 emoji), centered title, uppercase tagline — matches `dressed-mockup.html`
- ⋯ menu button (top-right) with Backup/Restore options

### Backup & Restore
- `BackupRestore.swift` — Codable DTOs (`DressedBackupFile` v2 with items + outfits), export (photos as Base64), import (handles Android v1 and iOS v2), merge (skip duplicates by ID) and replace (delete all + insert)
- `LandingView` — `ShareLink` for backup export, `UIDocumentPickerViewController` for import, merge/replace confirmation dialogs, toast feedback
- Cross-platform compatible with Android backup format

### App icon
- Custom icon from user's `Dressed_icon.png`, resized to 1024x1024, all slots in Contents.json

### Navigation
- `RootView` routes wardrobe to `WardrobeListView` (with Home back button), search and outfits still use placeholders

## Next Steps

- **Test on a real iOS device** — camera, photo picker, backup/restore file flow
- Implement Search screen (filter by name, category, season — mirror Android)
- Implement Outfits screen (list + create outfit + collage cards — mirror Android)
- Item detail screen (tap card → full detail view)
- Android: Outfit detail screen, edit outfit (from previous backlog)
