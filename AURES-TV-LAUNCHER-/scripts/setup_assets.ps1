<#
.SYNOPSIS
    Verifies and configures local assets for AURÃˆS TV.
.DESCRIPTION
    Checks presence of ADHANALL.mp3 audio and offline announcement voice clips.
    If missing, searches Downloads\Music\ or guides the user on local asset placement.
#>
$rootDir = Split-Path -Parent $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }
$adhanDest = Join-Path $rootDir "app\src\main\assets\adhan\ADHANALL.mp3"
$userProfile = if ($env:USERPROFILE) { $env:USERPROFILE } else { [System.Environment]::GetFolderPath([System.Environment+SpecialFolder]::UserProfile) }; $adhanSourceDefault = Join-Path $userProfile "Downloads\Music\ADHANALL.mp3"

Write-Host "=== AURÃˆS TV Asset Verification ===" -ForegroundColor Cyan

if (Test-Path $adhanDest) {
    $item = Get-Item $adhanDest
    $mb = [math]::Round($item.Length / 1MB, 2)
    Write-Host "[OK] ADHANALL.mp3 is present: $($item.FullName) ($mb MB)" -ForegroundColor Green
} elseif (Test-Path $adhanSourceDefault) {
    Write-Host "Copying ADHANALL.mp3 from $adhanSourceDefault..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $adhanDest) | Out-Null
    Copy-Item $adhanSourceDefault $adhanDest -Force
    Write-Host "[OK] Copied ADHANALL.mp3 successfully." -ForegroundColor Green
} else {
    Write-Host "[WARNING] ADHANALL.mp3 is not yet placed in app\src\main\assets\adhan\" -ForegroundColor Yellow
    Write-Host "Please copy your Adhan recording into:" -ForegroundColor Yellow
    Write-Host "  $adhanDest" -ForegroundColor White
}

$announcements = Get-ChildItem (Join-Path $rootDir "app\src\main\assets\adhan_announcement") -Filter "*.mp3" -ErrorAction SilentlyContinue
Write-Host "[OK] Prepackaged Arabic announcement clips found: $($announcements.Count) files." -ForegroundColor Green