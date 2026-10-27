# Changelog — AURÈS TV

All notable changes to the AURÈS TV project are documented here.

## [2.0.0] — 2026-10-25

### Added
- **System Boot Animation Replacement**: Custom 100-frame 1280x720 animation featuring Djebel Chélia sunrise bloom, metallic golden typography, and Algerian crescent emblem installed to `/system/media/bootanimation.zip`.
- **Authentic Adhan Playback Engine (`ADHANALL.mp3`)**:
  - Full-length studio Adhan recording bundled in `app/src/main/assets/adhan/ADHANALL.mp3` (3m 07s, 320 kbps).
  - Media stream ducking and pause dispatch (`KEYCODE_MEDIA_PAUSE`).
  - Temporary mute of `STREAM_MUSIC` during Adhan with automatic volume restoration.
- **Offline Studio Voice Announcements**:
  - Prepackaged studio voice clips for all 5 daily prayers (`fajr.mp3`, `dhuhr.mp3`, `asr.mp3`, `maghrib.mp3`, `isha.mp3`).
  - Graceful fallback when Android TV box firmware lacks offline Arabic TTS voice.
- **15 Algerian Wilayas Astronomical Database**:
  - Exact coordinates for 15 cities, defaulting to Kaïs / Khenchela (`35.4925° N, 6.9344° E`).
- **Full Automated Test Suite**:
  - 35 unit test assertions covering astronomical calculations, Wilayas boundaries, date conversions, and multilingual localization.
- **Zero-Dependency Reproducible Build System**:
  - `build.ps1` and `scripts/build.sh` compiling APK cleanly in ~7 seconds without requiring heavy Gradle downloads.

### Changed
- Complete removal of mock streaming/movie carousels in favor of authentic Algerian identity ("REMOLVE MOVIES").
- Upgraded Admin PIN security to salted SHA-256 with brute-force rate-limiting.
- Factory stock launcher (`com.ansub.tvhome`) fallback setting and one-click exit.

---

## [1.0.0] — 2026-10-23
- Initial proof-of-concept and prototype launcher for Tanix TX1 Android TV hardware.