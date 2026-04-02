# Dressed — Restart

**Assistants:** see **`CLAUDE.md`** at the repo root for where everything lives; use this file as the latest session checkpoint.

## Where We Stopped (2026-04-02)

**Repo:** `main` branch, local changes not yet committed/pushed.

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

### Changes This Session (2026-04-02)
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

## Next Session — Quick Start

1. Read **`memory.md`** and **`backlog.md`**.
2. Commit and push the iOS changes from this session.
3. Test on a real device: edit item, edit outfit, delete outfit, BYOK AI key flow.
4. **Remaining gaps:** no unit/UI tests yet; Android outfit detail still needs mark worn/delete/edit.
