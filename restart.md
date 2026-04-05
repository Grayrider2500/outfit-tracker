# Dressed — Restart

**Assistants:** see **`CLAUDE.md`** at the repo root for where everything lives; use this file as the latest session checkpoint.

## Where we stopped (checkpoint — run `git status` to verify)

**Repo:** `main` branch, **pushed** to **`origin/main`** (last push included iOS outfits sort/filter + rebased docs/seed commits).

### Android
- **`google-services.json` is not in git** (security). Listed in `.gitignore`.
- **`TestDataSeeder`:** full dataset only in **`app/src/debug/`**; **`app/src/release/`** has a tiny stub (`run` throws). `LandingScreen` seed menu remains `BuildConfig.DEBUG`. `WardrobeViewModel.seedDebugTestData` still guards on `BuildConfig.DEBUG`.
- **`versionCode`:** bump to **5** for the photo-fix build before distributing.

### iOS — fully functional
Same scope as before, plus:
- **Outfits list:** **`OutfitsListView.swift`** — horizontal **`filterBar`** (sort: newest / most worn / A–Z; season chips incl. Autumn→`fall` key; piece-count: solo / 2–3 / 4+). **`displayedOutfits`** filters/sorts in memory; `@Query` unchanged. **`FilterChip`** + **`noResultsState`**. Name sort uses **`localizedLowercase`**; sorts use **`sorted(by:)`** for Swift 7.
- **`DevTestDataSeeder`:** entire file **`#if DEBUG`** — not compiled in Release. **`DevTestDataSeeder.run`** uses optional `targetItemCount` to avoid main-actor default-arg issues.

### Docs / distribution copy
- Root **`README.md`** refreshed (features, repo map, Firebase `applicationId` note).
- **`dressed-android/STORE_LISTING.md`** and **`dressed-ios/STORE_LISTING.md`** — draft Play / App Store text.
- **`CLAUDE.md`** — iOS path + store listing pointers.

### iOS picker note
- **`PickerView.swift`:** suggestions are a **vertical stack** in the main `ScrollView` (no paged `TabView` for results).

## Completed recently (already on `main`)

- Landing **stats** card (both platforms).
- Item detail **”Worn in outfits”** list (both platforms).
- **iOS outfits sort/filter** parity with Android chip rows.
- **Docs & store drafts**; **seed data excluded from release builds** (Android flavor sources + iOS `#if DEBUG`).
- **Borrowable library** Android export: **`FileProvider` + `ACTION_SEND`** share sheet (not `CreateDocument`-only).
- **Android photo persistence fix + edit item** (build 5):
  - `AddItemScreen` copies photo immediately on selection/capture (`IO` dispatcher); `isCopyingPhoto` guard disables Save while copy runs.
  - Camera now uses `copyFromFile(context, file)` bypassing FileProvider URI round-trip (unreliable on Samsung).
  - `ImageStorage.copyFromFile` added; both paths log via `Log.e`.
  - `ItemDetailScreen`: Edit icon in top bar → `wardrobe_edit/{id}` route.
  - `WardrobeNav` + `WardrobeSearchNav`: edit routes added.
  - `WardrobeViewModel.saveEdit`: loads existing entity, upserts copy, deletes old photo file if photo changed.
  - **Build 5 APK needed** — bump `versionCode` to 5 before distributing.

## Next up — see `backlog.md`

**Medium priority:** Outfit seasons tag (inherit from pieces or manual).

**Lower priority:** search across outfits, outfit hero photo, dark mode polish, a11y, onboarding.

## Workflow note

Architecture / cross-platform sync → this repo; tight specs live in **`backlog.md`** for Cursor-style execution.

## Next session — quick start

1. Read **`memory.md`** and **`backlog.md`**, then **`CLAUDE.md`** for paths.
2. **`git pull`** and check **`git status`**.
3. Pick work from **`backlog.md`** or tester feedback.
4. **Tests:** still no unit/UI suite.

## Optional security follow-up

- Old **`google-services.json`** may exist in **git history** on a public repo — consider rotating/restricting Firebase/Android keys in Google Cloud Console.
