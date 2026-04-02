# Outfit Tracker — *Dressed*

Wardrobe and outfit tracking in two forms: an **Android app** (Compose) and a **single-file web** prototype, plus an HTML **mockup** for design review.

### What's New in v1.1

### ✨ Major New Features

**Automatic Wardrobe Picker**  
Get intelligent outfit suggestions based on your existing wardrobe. Choose an occasion (Casual, Work, Date Night, Formal, Gym, etc.), add weather or mood tags, or tap “Surprise Me” for fresh combinations.  
Suggestions include smart scoring for color harmony, variety, and wear history.

**AI-Powered Reasoning** (Debug builds only)  
Each suggested outfit comes with a friendly natural-language explanation powered by Claude (e.g., “This navy blazer complements the gray pants perfectly and hasn’t been worn in 12 days — great for today’s mild weather”).  
In release builds, the picker falls back to strong rule-based suggestions for security reasons.

**Full Edit Support**  
- Edit individual clothing items (name, tags, photo, etc.)  
- Edit entire outfits (name and piece selection)  
- Delete outfits with confirmation dialog

### 🔧 Key Improvements

- **Safer Restore** — Replace-all now only deletes old photos after the new restore fully succeeds.
- **Better Data Integrity** — Deleting an item cleanly removes it from all outfits (no stale references).
- **Performance** — Smoother iOS scrolling thanks to smart image caching.
- **Photo Handling** — Automatic resizing + correct orientation on Android.
- **Backup v3** — Efficient .zip format with separate photo files (much smaller and faster for large wardrobes).
- **Security** — Firebase configuration is now properly gitignored.

### 📋 Other Changes
- Version bumped to **1.1.0**
- Cross-platform consistency improvements
- Debug-only test data seeder (100 items + sample outfits) for easier development and testing

---

**How to use the Picker:**  
Tap “Suggest Outfits” from the main screen. In debug builds, you can enable AI reasoning by entering your own Anthropic API key in Settings.

## Repository layout

| Path | Description |
|------|-------------|
| **`dressed-android/`** | Android app (Kotlin, Compose, Room). Primary development target. |
| **`dressed-ios/`** | iOS app (SwiftUI, SwiftData) — scaffold + models; open in Xcode per `dressed-ios/README.md`. |
| **`index.html`** | Web app: vanilla JS, `localStorage`, no build. |
| **`dressed-mockup.html`** | Interactive layout reference (Android-aligned). |
| **`CLAUDE.md`** | Map for AI assistants; points to `memory.md`, `restart.md`, `backlog.md`. |
| **`memory.md` / `restart.md` / `backlog.md`** | Session notes and prioritized tasks (Android-focused). |

## Features

### Automatic Wardrobe Picker

Dressed can intelligently suggest complete outfits based on your wardrobe, tags, wear history, and chosen occasion.

**Features:**
- Occasion-based suggestions (Casual, Work, Date Night, Formal, Gym, etc.)
- Weather-aware filtering
- Smart scoring for color harmony, variety, and wear history
- "Surprise Me" mode for fresh combinations
- One-tap "Save as outfit" or "Wear today"

**AI Reasoning (Debug builds only):**  
In development builds, each suggested outfit includes a friendly natural-language explanation powered by Claude.  
In release builds, the AI feature is disabled for security reasons (to avoid shipping API keys in the app binary). The picker remains fully functional with strong rule-based suggestions.

To enable AI reasoning during development:
- Android: Add `anthropicApiKey=sk-ant-...` to `local.properties` (already gitignored)
- iOS: Add `ANTHROPIC_API_KEY` to your scheme environment variables

## Live site (GitHub Pages)

- **Web app (root):** https://grayrider2500.github.io/outfit-tracker/  
- **Android mockup:** https://grayrider2500.github.io/outfit-tracker/dressed-mockup.html  

## Web app — run locally

Open `index.html` in a browser. No install or server required.

## Android app — run locally

Open the **`dressed-android`** folder in Android Studio and run the `app` configuration, or from that directory:

```bash
./gradlew :app:installDebug
```

### Firebase configuration (Android)

The real **`google-services.json` file must never be committed**. It contains your Firebase project identifiers and API key material; anyone with the repo must add their own file locally.

**Setup:**

1. In [Firebase Console](https://console.firebase.google.com/), open your project and add an **Android** app with package name **`com.crossmountproducts.dressed`** (must match `applicationId` in `dressed-android/app/build.gradle.kts`).
2. Download **`google-services.json`** from the Console and save it as **`dressed-android/app/google-services.json`**, **or** copy the repo root file **`google-services-sample.json`** to that path, remove the entire **`__comment`** object from the copy, and replace all **`YOUR_*`** / placeholder values with values from your Firebase project.
3. For iOS Firebase (if used), add **`GoogleService-Info.plist`** to your Xcode target locally; that filename is gitignored and must not be committed.

## iOS app — run locally

Create the Xcode project inside **`dressed-ios/`** (SwiftUI + SwiftData, iOS 17+) and add **`Sources/DressedKit/**`** to the app target. See **`dressed-ios/README.md`**.

## Tech summary

- **Android:** Jetpack Compose, Material 3, Room, Coil, Navigation-Compose.  
- **Web:** HTML/CSS/JS, Google Fonts (Cormorant Garamond + DM Sans), mobile-first (~430px).  

## AI / pair-programming

For **Claude Code** or similar: start with **`CLAUDE.md`** at the repo root, then **`restart.md`** and **`memory.md`**.
