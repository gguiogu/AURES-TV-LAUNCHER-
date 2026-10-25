<#
.SYNOPSIS
    Installs and launches AURÈS TV APK on Tanix TX1 Android TV Box.
.PARAMETER DeviceIp
    IP address and port of the Android TV device (Default: 192.168.1.10:5555)
.PARAMETER SetAsHome
    Switch to set AURÈS TV as the default Android TV Home launcher.
#>
param (
    [string]$DeviceIp = "192.168.1.10:5555",
    [switch]$SetAsHome
)
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }
$apkPath = Join-Path $rootDir "dist\aures-tv-2.0.0-release.apk"

if (!(Test-Path $apkPath)) {
    Write-Host "APK not found. Building first with scripts\build.ps1..." -ForegroundColor Yellow
    & powershell -ExecutionPolicy Bypass -File (Join-Path $rootDir "scripts\build.ps1")
}

Write-Host "Connecting to Tanix TX1 ($DeviceIp)..." -ForegroundColor Cyan
& adb connect $DeviceIp
Start-Sleep -Seconds 1

Write-Host "Installing AURÈS TV APK..." -ForegroundColor Cyan
& adb -s $DeviceIp install -r -d $apkPath

Write-Host "Launching AURÈS TV 1962 Launcher..." -ForegroundColor Cyan
& adb -s $DeviceIp shell am start -W -n dz.vision.launcher1962/.MainActivity -f 0x10000000

if ($SetAsHome) {
    Write-Host "Setting AURÈS TV as default Home Activity..." -ForegroundColor Cyan
    & adb -s $DeviceIp shell cmd package set-home-activity dz.vision.launcher1962/.MainActivity
    Write-Host "Default Home set to dz.vision.launcher1962" -ForegroundColor Green
}

Write-Host "Deployment complete!" -ForegroundColor Green