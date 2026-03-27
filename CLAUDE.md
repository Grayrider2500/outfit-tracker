# CLAUDE.md

Guidance for **Claude Code** and other AI assistants working in this repository.

## Start here — repo map

| Path | What it is |
|------|------------|
| **`dressed-android/`** | **Primary product:** Android app (Jetpack Compose, Room, Material 3). Entry: `app/src/main/java/com/dressed/app/`. |
| **`index.html`** | **Web prototype:** single-file wardrobe + outfits UI, `localStorage`, no build. |
| **`dressed-mockup.html`** | **Design reference** aligned with the Android app (layout / flows for review). |
| **`memory.md`** | Long-lived **Android** facts: architecture, DB version, migrations, decisions. |
| **`restart.md`** | **Session checkpoint:** where work stopped, last session summary, immediate next steps. |
| **`backlog.md`** | Prioritized **todo** list for the Android app. |

**At the beginning of a session, read:** `restart.md` → `memory.md` → `backlog.md` (then open code as needed).

This file lives at the **repository root** so tools that load `CLAUDE.md` automatically will find it.

---

## Android app (`dressed-android/`)

- **Package:** `com.dressed.app`
- **MainActivity** wires `DressedApp(viewModel, outfitsViewModel)`.
- **Navigation:** `ui/DressedApp.kt` — root routes: `landing`, `wardrobe`, `search`, `outfits`.
- **Nested navigation:** `WardrobeNav`, `WardrobeSearchNav`, `OutfitsNav` in `ui/wardrobe/` and `ui/outfits/`.
- **DB:** `data/local/DressedDatabase.kt` (see `memory.md` for version and migrations).
- **Build:** open `dressed-android` in Android Studio, or `./gradlew :app:compileDebugKotlin`.

---

## Web prototype (`index.html`)

- No build step; open in a browser or use GitHub Pages root URL.
- Vanilla HTML/CSS/JS; data in `localStorage`.
- `font-size: 16px` on form inputs (avoids iOS zoom).

---

## Design mockup (`dressed-mockup.html`)

- Static HTML/CSS/JS; optional live previews use network images (see file footer).
- **Client / stakeholder URL (when Pages enabled):**  
  `https://grayrider2500.github.io/outfit-tracker/dressed-mockup.html`

---

## GitHub

- **Repo:** https://github.com/Grayrider2500/outfit-tracker  
- **Pages:** https://grayrider2500.github.io/outfit-tracker/  
- Commit and push after meaningful changes; use clear commit messages.

---

## After a work session

Update as appropriate:

1. **`memory.md`** — durable Android/architecture facts and decisions.  
2. **`restart.md`** — where you stopped and what to do next.  
3. **`backlog.md`** — remaining work, priority order.
