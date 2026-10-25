# Installation & Recovery Guide — AURÈS TV / 1962

## 1. Direct ADB Installation (Tanix TX1)
Ensure the TV box is powered on and connected to the same local network:
```powershell
# 1. Connect over Wi-Fi/Ethernet ADB
adb connect 192.168.1.10:5555

# 2. Install APK with downgrade and replace flags
adb -s 192.168.1.10:5555 install -r -d out\launcher1962.apk

# 3. Launch AURÈS TV 1962 directly
adb -s 192.168.1.10:5555 shell am start -W -n dz.vision.launcher1962/.MainActivity -f 0x10000000
```

## 2. Set AURÈS TV 1962 as Default Home Launcher
To make 1962 launch automatically on boot and whenever the remote HOME key is pressed:
```powershell
adb -s 192.168.1.10:5555 shell cmd package set-home-activity dz.vision.launcher1962/.MainActivity
```
*Alternatively from the TV UI:*
1. Click the top-right Settings icon (`⚙`).
2. Enter the Admin PIN (`2001`).
3. Select **"تعيين 1962 كواجهة رئيسية افتراضية / Set 1962 as Default Home"**.
4. Choose **1962** and select **"Always"**.

## 3. USB Sideloading (Without ADB)
1. Copy `dist\aures-tv-2.0.0-release.apk` to a FAT32/NTFS USB flash drive.
2. Insert the flash drive into the Tanix TX1 box.
3. Open the pre-installed File Manager or AppInstaller on the TV.
4. Click `aures-tv-2.0.0-release.apk` and confirm installation.
5. Press the **HOME** button on the remote control and choose **1962**.

## 4. Safe Recovery to Stock Factory Launcher
The Tanix TX1 stock launcher (`com.ansub.tvhome`) remains completely untouched in `/system`.

### Option A: From the 1962 UI (Instant)
1. Open Settings (`⚙`) and enter PIN `2001`.
2. Select **"العودة إلى الواجهة الأصلية / Back to Original Launcher (Exit 1962)"**.
3. The launcher safely exits and returns to the stock Tanix interface.

### Option B: Via ADB Command
```powershell
# Restore stock launcher as default
adb -s 192.168.1.10:5555 shell cmd package set-home-activity com.ansub.tvhome/.MainActivity

# Start stock launcher immediately
adb -s 192.168.1.10:5555 shell am start -n com.ansub.tvhome/.MainActivity
```

### Option C: Uninstall 1962 (Clean Removal)
```powershell
adb -s 192.168.1.10:5555 uninstall dz.vision.launcher1962
```
Android automatically reverts to the stock launcher when the custom launcher is uninstalled.
