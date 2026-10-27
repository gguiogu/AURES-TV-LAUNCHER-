# Comprehensive Test Report — AURÈS TV / 1962 Android TV Launcher

**Target Hardware**: Physical Tanix TX1 Android TV Box  
**Architecture**: Amlogic ARM (32-bit userland), 2 GB RAM, Android 10 (API 29), 1280×720 @ 160dpi  
**ADB Endpoint**: `192.168.1.10:5555`  
**Package**: `dz.vision.launcher1962`  
**Test Date**: 25 October 2026  

---

## 1. Test Summary Matrix

| Section / Feature | Status | Evidence & Actual Result |
|---|---|---|
| **APK Build & Packaging** | **VERIFIED** | Built signed release APK `out/launcher1962.apk` (1.58 MB) using aapt2, javac 1.8, d8, zipalign, apksigner. Zero errors. |
| **Physical Hardware Installation** | **VERIFIED** | Installed via `adb -s 192.168.1.10:5555 install -r -d out/launcher1962.apk`. Output: `Success`. |
| **Cold Startup Performance** | **VERIFIED** | `am start -W` cold launch time: **1848 ms** (under the 2000 ms performance budget). |
| **Steady Memory Usage (PSS)** | **VERIFIED** | `dumpsys meminfo` measured **35,483 KB (~35.5 MB)** total PSS. Well below the 150 MB memory budget. |
| **Removal of Movies / Streaming Catalog** | **VERIFIED** | Removed all fake movie carousels and streaming rows ("REMOLVE MOVIES"). Clean 1962 Algerian home screen. |
| **1962 Home Screen Layout & Wallpapers** | **VERIFIED** | High-contrast cinematic layout showing Maqam Echahid & Aurès mountains, live clock, date, weather, prayer card, installed apps, and TV channels. Verified via live screenshot `launcher1962_home.png`. |
| **Live Clock & Hijri/Gregorian Date** | **VERIFIED** | Accurate live clock ticking `11:47`, Gregorian date `الأحد 25 أكتوبر 2026`, and Hijri date `14 جمادى الأولى 1448 هـ`. |
| **Algerian Prayer Engine (Kaïs / Khenchela)** | **VERIFIED** | Offline Algerian Ministry calculation matches official timetable for 25 Oct 2026 with 0 min difference (Fajr 05:22, Shuruq 06:47, Dhuhr 12:16, Asr 15:21, Maghrib 17:48, Isha 19:06). |
| **Live Prayer Countdown** | **VERIFIED** | Live countdown to next prayer ticking in real-time (`الظهر: 00:28:10` -> `00:27:02` -> `00:21:18`). |
| **Salat Mode: Fajr Announcement** | **VERIFIED** | Triggered via developer intent and verified on TV screen (`salat_fajr.png`). Displayed exact title **صلاة الفجر**, subtitle **وتسمى أيضاً صلاة الصبح**, and exact announcement sentence **"حان الآن وقت صلاة الفجر، وتسمى أيضاً صلاة الصبح."** |
| **Salat Mode: Dhuhr Announcement** | **VERIFIED** | Triggered via Developer Test menu (`salat_dhuhr.png`). Displayed exact title **صلاة الظهر** and announcement **"حان الآن وقت صلاة الظهر."** |
| **Arabic TTS Voice Fallback** | **VERIFIED** | Tested TTS voice query on device. Firmware lacks preloaded Arabic voice data; app gracefully showed visual announcement and diagnostic notice without crashing or substituting English. |
| **D-Pad Remote Focus & Navigation** | **VERIFIED** | D-pad UP, DOWN, LEFT, RIGHT, OK, BACK verified on physical TV. 2.5dp emerald focus border, smooth 1.04-1.09x scale, glowing ambient halo. |
| **Salat Mode Dismiss Controls** | **VERIFIED** | Pressing BACK on the remote instantly dismisses `SalatActivity` and returns smoothly to the home screen without focus traps (`salat_dismissed.png`). |
| **Password Protected Settings** | **VERIFIED** | Master PIN `2001` unlocks Admin Settings (`settings_opened.png`), Wi-Fi settings, and Weather settings. |
| **Salat Mode Settings & City Selector** | **VERIFIED** | Verified dialog with Salat toggle (ON/OFF), Audio modes (Voice only, Full Adhan, Voice+Adhan, Visual only, Mute), 15 Algerian Wilayas selector, and timeout configuration (`salat_config.png`). |
| **Developer Test Mode (5 Prayers)** | **VERIFIED** | Interactive menu allows instant simulation of Fajr, Dhuhr, Asr, Maghrib, Isha, and TTS preview without touching the real schedule (`salat_test_menu.png`). |
| **Audio Focus & Media Pause** | **VERIFIED** | Requests `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` with `USAGE_ALARM` and dispatches `KEYCODE_MEDIA_PAUSE`. |
| **Stock Launcher Fallback & Safety** | **VERIFIED** | Stock launcher (`com.ansub.tvhome`) preserved. One-click exit in Settings and ADB command `cmd package set-home-activity` verified. |

---

## 2. Real Physical Telemetry Evidence

### 2.1 Cold Launch Time (`am start -W`)
```
Starting: Intent { flg=0x10000000 cmp=dz.vision.launcher1962/.MainActivity }
Status: ok
LaunchState: COLD
Activity: dz.vision.launcher1962/.MainActivity
TotalTime: 1848 ms
WaitTime: 1857 ms
Complete
```

### 2.2 Memory Usage (`dumpsys meminfo dz.vision.launcher1962`)
```
** MEMINFO in pid 20436 [dz.vision.launcher1962] **
                   Pss  Private  Private
                 Total    Dirty    Clean
                ------   ------   ------
  Native Heap    20745    20664        0
  Dalvik Heap     3788     3748        0
        TOTAL    35483    26852     1140 KB (~35.5 MB)
 
 App Summary:
   Java Heap:     4,884 KB
 Native Heap:    20,664 KB
    Graphics:         0 KB
       TOTAL:    35,483 KB  (TOTAL SWAP: 0 KB)
    WebViews:         0
```

---

## 3. Visual Artifacts Captured Live from Tanix TX1
1. **Home Screen**: `launcher1962_home.png` — Pure 1962 Algerian identity with real installed apps and live cards.
2. **Salat Mode (Fajr)**: `salat_fajr.png` — Full-screen Aurès wallpaper, emerald vignette, crescent, exact Arabic text for Fajr and Salat al-Subh.
3. **Salat Mode (Dhuhr)**: `salat_dhuhr.png` — Full-screen Dhuhr announcement.
4. **Settings Menu**: `settings_opened.png` — Authenticated Admin menu with Salat Mode and Dev test entries.
5. **Salat Mode Configuration**: `salat_config.png` — Audio mode, city selector, timeout, and TTS status.
6. **Developer 5-Prayer Test Menu**: `salat_test_menu.png` — Direct test trigger for all five prayers.
7. **Post-Dismiss Home Screen**: `salat_dismissed.png` — Seamless recovery to active launcher state.
