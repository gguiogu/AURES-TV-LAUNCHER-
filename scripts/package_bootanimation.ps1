<#
.SYNOPSIS
    Packages the AURÈS TV Boot Animation with 0% compression (STORED).
.DESCRIPTION
    Android's SurfaceFlinger bootanimation service requires bootanimation.zip
    to be packaged with 0% compression (uncompressed / STORED).
    This script verifies desc.txt and packages part0 and part1 using `jar -c0Mf`.
#>
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }
$animDir = Join-Path $rootDir "assets\boot-animation"
$descFile = Join-Path $animDir "desc.txt"
$zipFile = Join-Path $animDir "aures_bootanimation.zip"

if (!(Test-Path $descFile)) {
    $desc = "1280 720 25`np 1 0 part0`np 0 0 part1`n"
    [System.IO.File]::WriteAllText($descFile, $desc, [System.Text.Encoding]::ASCII)
}

Write-Host "Packaging $zipFile with 0% compression (STORED)..." -ForegroundColor Cyan

Push-Location $animDir
try {
    if (Test-Path $zipFile) { Remove-Item -Force $zipFile }
    & jar -c0Mf aures_bootanimation.zip desc.txt part0 part1
    if ($LASTEXITCODE -ne 0) { throw "jar -c0Mf packaging failed" }
} finally {
    Pop-Location
}

$item = Get-Item $zipFile
$sizeMb = [math]::Round($item.Length / 1MB, 2)
Write-Host "Success! Created: $($item.FullName) ($sizeMb MB)" -ForegroundColor Green