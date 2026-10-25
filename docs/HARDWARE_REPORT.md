================================================================================
           TANIX TX1 — COMPREHENSIVE HARDWARE DIAGNOSTIC REPORT
================================================================================
Generated Date: 2026-10-25 12:05
Connection: Wireless ADB (192.168.1.10:5555)
Toolchain: Android Platform-Tools (ADB 34.0.0)

--------------------------------------------------------------------------------
1. TRUE SILICON & PLATFORM IDENTITY (SUNXI / ALLWINNER)
--------------------------------------------------------------------------------
SoC Platform:               sun50iw9 (Allwinner H313 / H616 Family)
SoC Platform Code:          cupid (ro.board.platform)
Hardware ID:                sun50iw9p1 (ro.hardware / ro.boot.hardware)
Board / SDK Name:           exdroid (Allwinner BSP SDK)
Sunxi Secure Mode:          normal
Sunxi Chip ID:              32c05c002c00480801072076387d1ed1
Sunxi Serial Number:        387d1ed10107207600002c0000000000
Sunxi Chip Type:            00005c00
Sunxi Batch No:             0x18230000
CPU Architecture:           ARMv8 Cortex-A53 quad-core (Part 0xd03, rev 4)
Userland ABI:               armeabi-v7a (32-bit userland on 64-bit kernel)
GPU:                        ARM Mali-G31 MP2 (Driver: mali_kbase 528 KB)
Kernel Version:             Linux localhost 4.9.170 #2 SMP PREEMPT Thu May 9 17:45:52 CST 2024 armv8l

CRITICAL OBSERVATION:
Vendor properties `ro.product.manufacturer=Amlogic` and `ro.product.device=p281` 
are SPOOFED / MASKED by the stock firmware factory builder (eng.lizhon) to 
improve compatibility with certain DRM/streaming services. The real physical 
hardware is 100% ALLWINNER H313 (sun50iw9p1), NOT AMLOGIC.
Flashing an Amlogic firmware image will instantly and permanently BRICK this device.

--------------------------------------------------------------------------------
2. WIRELESS / NETWORK HARDWARE
--------------------------------------------------------------------------------
Wi-Fi Kernel Module:        atbm613x_wifi_sdio (Size: 770,048 bytes)
Physical Wi-Fi Controller:  AltoBeam ATBM6031 / ATBM613x (SDIO interface)
Standard:                   802.11 b/g/n (2.4 GHz)
Bluetooth:                  Not detected / Module not loaded

--------------------------------------------------------------------------------
3. RAM & STORAGE CONFIGURATION
--------------------------------------------------------------------------------
Total Physical RAM:         2,024,120 kB (~2 GB RAM)
Available RAM:              1,113,864 kB (~1.1 GB available)
ZRAM Configuration:         1,518,088 kB (~1.5 GB zram swap)
Total Internal Storage:     15,267,840 blocks (~16 GB eMMC)
User Data Partition (/data):11 GB total, 2.8 GB used, 8.4 GB available (mmcblk0p17 / UDISK)

--------------------------------------------------------------------------------
4. PARTITION SCHEME (ANDROID 10 DYNAMIC PARTITIONS)
--------------------------------------------------------------------------------
mmcblk0p1  : bootloader (32 MB)
mmcblk0p2  : env (16 MB)
mmcblk0p3  : boot (32 MB - Kernel + Ramdisk)
mmcblk0p4  : super (2048 MB - Dynamic Partitions dm-0: system, dm-1: vendor, dm-2: product)
mmcblk0p5  : misc (16 MB)
mmcblk0p6  : recovery (32 MB)
mmcblk0p7  : cache (1024 MB)
mmcblk0p8  : vbmeta (16 MB)
mmcblk0p9  : vbmeta_system (16 MB)
mmcblk0p10 : vbmeta_vendor (16 MB)
mmcblk0p11 : metadata (16 MB)
mmcblk0p12 : private (16 MB)
mmcblk0p13 : frp (512 KB)
mmcblk0p14 : empty (15.8 MB)
mmcblk0p15 : media_data (16 MB)
mmcblk0p16 : Reserve0 (16 MB)
mmcblk0p17 : UDISK (/data - 11.8 GB)

--------------------------------------------------------------------------------
5. CURRENT FIRMWARE FINGERPRINT
--------------------------------------------------------------------------------
Model:                      TX1
Android OS Version:         10 (Quince Tart)
API Level / SDK:            29
Build ID:                   QP1A.191105.004
Display ID:                 eros_p2-userdebug 10 QP1A.191105.004 eng.lizhon.20240513.100219 test-keys
Incremental Build:          eng.lizhon.20240513.100219 (Factory build date: May 13, 2024)
Security Patch:             November 2019 baseline with May 2024 vendor patches
================================================================================
