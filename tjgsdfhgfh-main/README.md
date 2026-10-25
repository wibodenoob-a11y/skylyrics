# SkyLyrics

A beautiful Minecraft Fabric mod that shows synchronized lyrics as a modern floating karaoke bar (like the one in the screenshot), with sky-mode floating bubbles, word-by-word highlighting, and a clean settings menu.

## Features

- **Floating Karaoke Bar** – A sleek dark capsule that shows the current line with the active word highlighted in accent color.
- **Word reveal modes**:
  - **Progressive** – words appear/highlight one after another.
  - **Whole line** – wait until the previous word finishes before showing the next full line.
- **Limit words on bar** – control how many words are visible at once (sliding window around the current word). Keeps long lines readable.
- **Split Left / Right** – put current lyrics on one side of the screen and previous on the other.
- **Sky Mode** – lyrics float in the 3D world following your camera.
- **Follow Mode** – lyrics stay in front of you smoothly.
- **Previous lines** – toggle fade-out or keep previous lines fully visible (no fade).
- **Colors & Themes** – cycle accent / past / future / background colors, plus quick presets (Purple Karaoke, Cyan Neon, Coral Warm, Gold Classic).
- **Customizable** position, size, opacity, timing offset, glow, etc.
- Works with any media player that exposes MediaSession (Spotify, YouTube Music, browser, etc. on Windows/Linux).

## Building

```bash
git clone https://github.com/yourname/skylyrics.git
cd skylyrics
./gradlew build
```

The built jar will be in `build/libs/skylyrics-0.6.0.jar`.

## Usage

- Press **J** (default) to open the settings menu.
- Make sure a song is playing in a supported media player.
- Lyrics are fetched automatically from public sources (LRCLIB etc.).

## Settings Overview

### Lyrics tab
- Show lyrics
- Lyrics mode: Follow / Sky / Screen (the floating bar)
- Split lyrics (Left / Right)
- **Limit words on bar** + **Max words per line** (3–12) – how many words appear on the karaoke bar at once
- Line position (Center / Left / Right)
- Vertical height
- **Previous lines fade out** – OFF = previous line stays strong (no fade), ON = previous lines fade out
- **Word reveal style**: Progressive / Whole line
- Lyrics size & Opacity
- Timing offset

### Display tab
- Now-playing card
- Card position
- Song title intro
- Glow on lyric bar

### Cosmetics tab
- Cycle Accent / Past / Future / Bar background colors
- Cycle Past words color
- Cycle Future words color
- Cycle Bar background
- Quick themes: Purple Karaoke, Cyan Neon, Coral Warm, Gold Classic

### Player tab
- Current track info
- Reload lyrics

## License

MIT
