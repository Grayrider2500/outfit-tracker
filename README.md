# Outfit Tracker — *Dressed*

Wardrobe and outfit tracking in two forms: an **Android app** (Compose) and a **single-file web** prototype, plus an HTML **mockup** for design review.

## Repository layout

| Path | Description |
|------|-------------|
| **`dressed-android/`** | Android app (Kotlin, Compose, Room). Primary development target. |
| **`dressed-ios/`** | iOS app (SwiftUI, SwiftData) — scaffold + models; open in Xcode per `dressed-ios/README.md`. |
| **`index.html`** | Web app: vanilla JS, `localStorage`, no build. |
| **`dressed-mockup.html`** | Interactive layout reference (Android-aligned). |
| **`CLAUDE.md`** | Map for AI assistants; points to `memory.md`, `restart.md`, `backlog.md`. |
| **`memory.md` / `restart.md` / `backlog.md`** | Session notes and prioritized tasks (Android-focused). |

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
