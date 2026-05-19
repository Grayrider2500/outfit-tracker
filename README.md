# Outfit Tracker — *Dressed*

**Dressed** is a **local-first** wardrobe and outfit organizer: pieces, search, saved outfits, wear history, backups, and **borrowable library** shares—on **Android** (primary), **iOS** (SwiftUI + SwiftData), plus a **single-file web** prototype and an HTML **mockup** for design review.

## What the apps do today

| Area | Highlights |
|------|------------|
| **Wardrobe** | Add and edit pieces (photo, category, size, color, seasons, occasions on Android). Grid browse, item detail, mark pieces worn. |
| **Outfits** | Create outfits from your pieces, collage-style cards, edit, delete, wear counts. **Item detail** shows **which outfits use that piece** (name list). |
| **Search** | Filter wardrobe by category, seasons, and more (Android-aligned behavior on both platforms where implemented). |
| **Suggest outfits** | **Picker** scores combinations by occasion, weather/mood tags, color harmony, variety, and wear history; optional **AI-style explanations in debug builds** (rule-based suggestions always in release). |
| **Backup & restore** | Export/import **ZIP** backups with photos; merge or replace modes on restore (Android). |
| **Shared libraries** | Export a **`.dressed-library`** file of **lendable** pieces for friends; import libraries, browse **Borrowed** hubs, explainer on first use. |
| **Home** | Landing summary with **stats** (e.g. piece count, total wears, outfit count) where implemented. |
| **Distribution** | **Debug-only** “seed professional closet” test data on both platforms; **release builds do not ship** that dataset (Android: `debug` source set; iOS: `#if DEBUG`). |

## Repository layout

| Path | Description |
|------|-------------|
| **`dressed-android/`** | Android app (Kotlin, Jetpack Compose, Material 3, Room). Primary product. |
| **`dressed-ios/`** | iOS app (SwiftUI, SwiftData, iOS 17+). See **`dressed-ios/README.md`** for Xcode setup. |
| **`dressed-android/STORE_LISTING.md`** | Draft **Google Play** short / full description (copy-paste). |
| **`dressed-ios/STORE_LISTING.md`** | Draft **App Store** subtitle, keywords, description (copy-paste). |
| **`index.html`** | Web app: vanilla JS, `localStorage`, no build. |
| **`dressed-mockup.html`** | Interactive layout reference (Android-aligned). |
| **`CLAUDE.md`** | Map for AI assistants; points to `memory.md`, `restart.md`, `backlog.md`. |
| **`memory.md` / `restart.md` / `backlog.md`** | Architecture notes and prioritized tasks. |

## Suggest outfits (Picker)

- Occasions (e.g. casual, work, formal), optional weather/mood, **Surprise me**, scoring for harmony and rotation.
- **Save as outfit** / wear flows where the app supports them.
- **AI explanations (debug only):** natural-language blurbs via Claude when configured; **release builds** use rule-based suggestions only (see below).

**Enable AI text in development**

- **Android:** `anthropicApiKey=sk-ant-...` in `local.properties` (gitignored).
- **iOS:** `ANTHROPIC_API_KEY` in the run scheme environment.

## Live site (GitHub Pages)

- **Web app:** https://grayrider2500.github.io/outfit-tracker/  
- **Mockup:** https://grayrider2500.github.io/outfit-tracker/dressed-mockup.html  

## Web app — run locally

Open `index.html` in a browser. No install or server required.

## Android app — run locally

Open **`dressed-android`** in Android Studio and run **`app`**, or:

```bash
cd dressed-android && ./gradlew :app:installDebug
```

### Firebase configuration (Android)

Do **not** commit real **`google-services.json`**. Add it locally as **`dressed-android/app/google-services.json`**.

1. In [Firebase Console](https://console.firebase.google.com/), add an Android app whose package matches **`applicationId`** in `dressed-android/app/build.gradle.kts` (currently **`com.crossmountproducts.dressed`**).
2. Download **`google-services.json`** into **`dressed-android/app/`**, or adapt **`google-services-sample.json`** and replace placeholders.

For iOS Firebase (if you use it), add **`GoogleService-Info.plist`** locally; do not commit secrets.

## iOS app — run locally

Create or open the Xcode project under **`dressed-ios/`**, wire in **`Sources/DressedKit/`**, minimum **iOS 17**. Step-by-step: **`dressed-ios/README.md`**.

## Tech summary

- **Android:** Jetpack Compose, Material 3, Room, Coil, Navigation-Compose.  
- **iOS:** SwiftUI, SwiftData.  
- **Web:** HTML/CSS/JS, Google Fonts (Cormorant Garamond + DM Sans), mobile-first (~430px).  

## AI / pair-programming

For **Claude Code** or similar: start with **`CLAUDE.md`**, then **`restart.md`** and **`memory.md`**.
