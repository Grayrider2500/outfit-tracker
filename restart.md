# Dressed — Restart

**Assistants:** see **`CLAUDE.md`** at the repo root for where everything lives; use this file as the latest session checkpoint.

## Where We Stopped (2026-04-01)

**Repo:** `main` is pushed to GitHub and matches local (no pending diff vs `origin/main` for the last known state).

### Android
- **`google-services.json` is not in git** (security). It is listed in **`.gitignore`** at `dressed-android/app/google-services.json`. Keep your real file there locally (or from CI secrets) so the Google Services Gradle plugin can run; fresh clones must add it from Firebase Console.
- **Outfit detail (read-only):** `OutfitDetailScreen.kt`, route `outfits_detail/{id}` in `OutfitsScreen.kt`, list cards navigate via `onOutfitClick`.
- **Photos:** New picks are resized in `ImageStorage.copyFromUri` (long edge ≤ 1600px, JPEG ~87%). UI uses `coilPhotoFileOrNull` in wardrobe + outfits so Coil only loads existing readable files.
- **`versionCode`:** 3 in `app/build.gradle.kts`.

### iOS
- Picked photos use **`PhotoStorage.saveOptimizedPickedPhotoJPEG`** (same resize/compress idea as Android). `CURRENT_PROJECT_VERSION` bumped in the Xcode project.

### Security follow-up (optional)
- The old `google-services.json` may still exist in **git history**. If the repo is public, consider **rotating / restricting** the Firebase/Android API key in Google Cloud Console.

## Next Session — Quick Start

1. Read **`memory.md`** and **`backlog.md`**.
2. Android Studio: open `dressed-android/`, confirm `app/google-services.json` exists locally if you use Firebase features in this build.
3. Verify: `./gradlew :app:compileDebugKotlin`
4. **Backlog highlights:** outfit detail still needs Mark as Worn, delete, and edit-outfit flows (see `backlog.md`).

## Recent Session Log (condensed)

- Stopped tracking Firebase config; ignore + local-only `google-services.json`.
- Android outfit read-only detail + navigation; image pipeline fixes (`ImageStorage`, `coilPhotoFileOrNull`).
- iOS photo optimization on add-item path.
