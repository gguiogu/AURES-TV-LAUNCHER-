<#
.SYNOPSIS
    Restores factory Tanix boot animation to /system/media/bootanimation.zip.
.PARAMETER DeviceIp
    IP address and port of the Android TV device (Default: 192.168.1.10:5555)
.PARAMETER Reboot
    Switch to reboot device immediately after restoration.
#>
param (
    [string]$DeviceIp = "192.168.1.10:5555",
    [switch]$Reboot
)
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }
$origZip = Join-Path $rootDir "assets\boot-animation\original_bootanimation.zip"

if (!(Test-Path $origZip)) {
    throw "Factory backup not found at $origZip."
}

Write-Host "Connecting to Tanix TX1 at $DeviceIp..." -ForegroundColor Cyan
& adb connect $DeviceIp
Start-Sleep -Seconds 1

Write-Host "Remounting /system..." -ForegroundColor Cyan
& adb -s $DeviceIp remount

Write-Host "Restoring factory original_bootanimation.zip..." -ForegroundColor Cyan
& adb -s $DeviceIp push $origZip /system/media/bootanimation.zip
& adb -s $DeviceIp shell "chmod 644 /system/media/bootanimation.zip; chown root:root /system/media/bootanimation.zip"

$stat = & adb -s $DeviceIp shell "ls -l /system/media/bootanimation.zip"
Write-Host "Restored original boot animation on device: $stat" -ForegroundColor Green

if ($Reboot) {
    Write-Host "Rebooting device..." -ForegroundColor Yellow
    & adb -s $DeviceIp reboot
}