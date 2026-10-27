<#
.SYNOPSIS
    Flashes AURÈS TV boot animation to physical Tanix TX1 Android TV Box.
.PARAMETER DeviceIp
    IP address and port of the Android TV device (Default: 192.168.1.10:5555)
.PARAMETER Reboot
    Switch to reboot device immediately after installation.
#>
param (
    [string]$DeviceIp = "192.168.1.10:5555",
    [switch]$Reboot
)
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }
$zipPath = Join-Path $rootDir "assets\boot-animation\aures_bootanimation.zip"

if (!(Test-Path $zipPath)) {
    throw "Boot animation file not found at $zipPath. Run scripts\package_bootanimation.ps1 first."
}

Write-Host "Connecting to Tanix TX1 at $DeviceIp..." -ForegroundColor Cyan
& adb connect $DeviceIp
Start-Sleep -Seconds 1

Write-Host "Remounting /system via root OverlayFS..." -ForegroundColor Cyan
& adb -s $DeviceIp remount

Write-Host "Pushing aures_bootanimation.zip to /system/media/bootanimation.zip..." -ForegroundColor Cyan
& adb -s $DeviceIp push $zipPath /system/media/bootanimation.zip

Write-Host "Setting standard Android permissions (0644 root:root)..." -ForegroundColor Cyan
& adb -s $DeviceIp shell "chmod 644 /system/media/bootanimation.zip; chown root:root /system/media/bootanimation.zip"

$stat = & adb -s $DeviceIp shell "ls -l /system/media/bootanimation.zip"
Write-Host "Verified on device: $stat" -ForegroundColor Green

if ($Reboot) {
    Write-Host "Rebooting device to display AURÈS TV boot animation..." -ForegroundColor Yellow
    & adb -s $DeviceIp reboot
} else {
    Write-Host "Installation complete! Run 'adb -s $DeviceIp reboot' to see the animation." -ForegroundColor Green
}