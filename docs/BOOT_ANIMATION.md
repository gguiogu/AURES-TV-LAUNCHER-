# AURÈS TV — System Boot Animation Specification & Rollback Guide

## 1. System Boot Animation Architecture
The boot animation in this repository is the **authentic, system-level Android boot animation** executed by Android's native C++ `bootanimation` binary (`/system/bin/bootanimation`) and rendered via `SurfaceFlinger`.

It is **NOT** a launcher activity splash screen or post-boot video. It displays during early kernel/Android boot stages (between 15.7s and 34.2s of hardware boot) prior to System Server, Zygote, and Launcher initialization.

| Hardware / OS Parameter | Value on Target Device | Verification Status |
| :--- | :--- | :--- |
| **Target Device** | Tanix TX1 Android TV Box | **VERIFIED** |
| **SoC / Architecture** | Allwinner H313 / Amlogic ARM (32-bit userland) | **VERIFIED** |
| **Android Version** | Android 10 (API Level 29) | **VERIFIED** |
| **Build Fingerprint** | `eng.lizhon.20240513.100219:userdebug/test-keys` | **VERIFIED** |
| **Native Root Access** | Native root active (`adbd` runs as `uid=0`, `gid=0` by default) | **VERIFIED** |
| **Display Resolution** | 1280 × 720 (Native framebuffer @ 160 dpi) | **VERIFIED** |
| **Filesystem Architecture** | Android 10 System-as-Root with ext4 + OverlayFS (`/mnt/scratch`) | **VERIFIED** |
| **Boot Animation Path** | `/system/media/bootanimation.zip` | **VERIFIED** |
| **File Permissions** | `-rw-r--r-- 1 root root` (`0644`) | **VERIFIED** |
| **Original Factory Backup** | `assets/boot-animation/original_bootanimation.zip` (2,279,715 bytes) | **VERIFIED** |
| **AURÈS TV Boot Animation** | `assets/boot-animation/aures_bootanimation.zip` (7,292,253 bytes) | **VERIFIED** |

---

## 2. Visual Animation Sequence & Framerate

- **Resolution**: 1280 × 720 pixels (1:1 pixel match to native TV framebuffer).
- **Framerate**: 25 frames per second (fps).
- **Compression**: **0% Compression (STORED)** using `jar -c0Mf` (mandatory for Android SurfaceFlinger).

### Part 0: Cinematic Dawn Sequence (`part0/`, 75 frames = 3.0 seconds)
1. **Frames 000–020**: Deep midnight sky (`#040710`) with atmospheric gradient.
2. **Frames 020–045**: The silhouette of the Aurès mountain ranges (Djebel Chélia, 2,328m) rises with warm amber dawn bloom radiating from behind the central peak.
3. **Frames 045–055**: The golden Algerian crescent & star emblem and metallic "AURÈS TV" wordmark emerge into focus.
4. **Frames 055–074**: A specular golden sweep glides across the metallic typography from left to right, accompanied by the national motto: *"الجزائر دائماً وأبداً  •  TANIX TX1"*.

### Part 1: Seamless Ambient Loop (`part1/`, 25 frames = 1.0 second loop)
- Subtle ambient golden breathing glow on the mountain crests and crescent until Android finishes loading all services and hands off to the AURÈS TV launcher.

---

## 3. Configuration File (`desc.txt`)

Stored at `assets/boot-animation/desc.txt`:
```
1280 720 25
p 1 0 part0
p 0 0 part1
```

- Line 1: `Width=1280`, `Height=720`, `FPS=25`.
- Line 2: `p` (part), `1` (play 1 time), `0` (pause frames), `part0` (folder name).
- Line 3: `p` (part), `0` (loop indefinitely until boot completes), `0` (pause frames), `part1` (folder name).

---

## 4. Live Hardware Boot Telemetry
Captured directly from Tanix TX1 system `logcat` during physical device boot:

```logcat
10-25 14:05:48.226  2159  2159 D BootAnimation: BootAnimationPreloadTiming start time: 15736ms
10-25 14:05:48.456  2159  2199 D BootAnimation: BootAnimationShownTiming start time: 15967ms
10-25 14:06:06.723  2159  2159 D BootAnimation: BootAnimationStopTiming start time: 34233ms
```
- **Total Display Duration on TV**: **18.267 seconds** (played intro sequence + looped smoothly).
- **Handoff to Launcher**: Smooth transition into `dz.vision.launcher1962/.MainActivity` with zero black frame flicker.

---

## 5. Scripts & Automation

### 5.1 Re-Render All 100 Frames from Source Code
To regenerate all 100 PNG frames from the pure C# / GDI+ rendering engine:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate_bootanimation.ps1
```

### 5.2 Package Uncompressed ZIP
To repackage `desc.txt`, `part0/`, and `part1/` into `aures_bootanimation.zip`:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package_bootanimation.ps1
```

### 5.3 Install to Tanix TX1 via ADB
To flash the animation to `/system/media/bootanimation.zip` over Wi-Fi/Ethernet:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install_bootanimation.ps1 -DeviceIp "192.168.1.10:5555" -Reboot
```

### 5.4 Restore Factory Tanix Boot Animation
If you ever wish to rollback to the original stock factory boot animation:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\restore_bootanimation.ps1 -DeviceIp "192.168.1.10:5555" -Reboot
```

---

## 6. Manual ADB Commands for Rollback & Recovery

```powershell
# 1. Connect to TV box
adb connect 192.168.1.10:5555

# 2. Remount root filesystem (OverlayFS)
adb -s 192.168.1.10:5555 remount

# 3. Push original factory backup
adb -s 192.168.1.10:5555 push assets\boot-animation\original_bootanimation.zip /system/media/bootanimation.zip

# 4. Set standard permissions
adb -s 192.168.1.10:5555 shell "chmod 644 /system/media/bootanimation.zip; chown root:root /system/media/bootanimation.zip"

# 5. Reboot to confirm factory animation restored
adb -s 192.168.1.10:5555 reboot
```