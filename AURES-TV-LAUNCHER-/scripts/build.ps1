<#
.SYNOPSIS
    Stand-alone, zero-dependency reproducible build script for AURÈS TV Android TV Launcher.
.DESCRIPTION
    Compiles, dexes, packages, and signs the AURÈS TV APK without requiring Gradle.
    Automatically detects Android SDK and Java on any Windows machine.
#>
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   AURÈS TV (1962) — Automated Reproducible Build Engine   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Locate Android SDK
$rawCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    "$env:LOCALAPPDATA\Android\Sdk",
    "C:\Android\Sdk"
)
$sdkFound = $null
foreach ($c in $rawCandidates) {
    if ($c -and (Test-Path "$c\build-tools")) {
        $sdkFound = $c
        break
    }
}
if (!$sdkFound) {
    throw "Android SDK not found. Please set ANDROID_HOME environment variable."
}
$SDK = $sdkFound
Write-Host "[1/7] Android SDK located at: $SDK" -ForegroundColor Green

# 2. Locate Build Tools & Android Platform JAR
$btDirs = Get-ChildItem -Path "$SDK\build-tools" -Directory | Sort-Object Name -Descending
if ($btDirs.Count -eq 0) { throw "No build-tools found in $SDK\build-tools" }
$BT = $btDirs[0].FullName
Write-Host "[2/7] Using Build-Tools version: $($btDirs[0].Name)" -ForegroundColor Green

$platDirs = Get-ChildItem -Path "$SDK\platforms" -Directory | Sort-Object Name -Descending
if ($platDirs.Count -eq 0) { throw "No platforms found in $SDK\platforms" }
$JAR = "$($platDirs[0].FullName)\android.jar"
if (!(Test-Path $JAR)) { throw "android.jar not found at $JAR" }
Write-Host "[2/7] Using Android platform: $($platDirs[0].Name)" -ForegroundColor Green

$AAPT2 = "$BT\aapt2.exe"
$D8 = "$BT\d8.bat"
$ZIPALIGN = "$BT\zipalign.exe"
$APKSIGNER = "$BT\apksigner.bat"

# 3. Prepare Clean Output Directories
$rootDir = $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }
$outDir = Join-Path $rootDir "out"
$classesDir = Join-Path $outDir "classes"
$dexDir = Join-Path $outDir "dex"
$distDir = Join-Path $rootDir "dist"

Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $classesDir, $dexDir
New-Item -ItemType Directory -Force -Path $classesDir, $dexDir, $distDir | Out-Null

# 4. Compile Resources with aapt2
Write-Host "[3/7] Compiling Android manifest and resources with aapt2..." -ForegroundColor Yellow
$manifestPath = Join-Path $rootDir "app\src\main\AndroidManifest.xml"
$baseApk = Join-Path $outDir "base.apk"

& $AAPT2 link -o $baseApk -I $JAR --manifest $manifestPath --min-sdk-version 21 --target-sdk-version 29
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

# 5. Compile Java Sources
Write-Host "[4/7] Compiling Java source files with javac..." -ForegroundColor Yellow
$srcDir = Join-Path $rootDir "app\src\main\java"
$javaFiles = Get-ChildItem -Path $srcDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
$coreLambda = "$BT\core-lambda-stubs.jar"
$bootcp = if (Test-Path $coreLambda) { "$JAR;$coreLambda" } else { $JAR }

& javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath $bootcp -d $classesDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac compilation failed" }

# 6. Convert Bytecode to Dalvik Executable (DEX)
Write-Host "[5/7] Converting bytecode to DEX with d8..." -ForegroundColor Yellow
$classFiles = Get-ChildItem -Path $classesDir -Filter "*.class" -Recurse | ForEach-Object { $_.FullName }
& $D8 --min-api 21 --release --lib $JAR --output $dexDir $classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 dexing failed" }

# 7. Package APK with Assets
Write-Host "[6/7] Packaging classes.dex and assets into APK..." -ForegroundColor Yellow
$unsignedApk = Join-Path $outDir "unsigned.apk"
Copy-Item $baseApk $unsignedApk -Force

# Insert DEX
Push-Location $dexDir
& jar uf $unsignedApk classes.dex
Pop-Location

# Insert Assets
$assetsDir = Join-Path $rootDir "app\src\main\assets"
if (Test-Path $assetsDir) {
    Push-Location (Join-Path $rootDir "app\src\main")
    & jar uf $unsignedApk assets
    Pop-Location
}

# 8. Align APK
Write-Host "[7/7] Aligning and signing APK..." -ForegroundColor Yellow
$alignedApk = Join-Path $outDir "aligned.apk"
& $ZIPALIGN -f 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

# Sign APK
$keystorePath = Join-Path $rootDir "scripts\release.keystore"
$ksPass = "algeria1962"
if (!(Test-Path $keystorePath)) {
    Write-Host "Generating local signing keystore..." -ForegroundColor Cyan
    & keytool -genkeypair -keystore $keystorePath -storepass $ksPass -keypass $ksPass -alias aures1962 -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=AuresTV, OU=DZ, O=Algeria, C=DZ"
}

$finalApk = Join-Path $distDir "aures-tv-2.0.0-release.apk"
& $APKSIGNER sign --ks $keystorePath --ks-pass "pass:$ksPass" --out $finalApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

$apkItem = Get-Item $finalApk
$sizeMb = [math]::Round($apkItem.Length / 1MB, 2)
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESSFUL! AURÈS TV APK Ready for Installation" -ForegroundColor Green
Write-Host "  Location: $($apkItem.FullName)" -ForegroundColor Green
Write-Host "  Size: $sizeMb MB ($($apkItem.Length) bytes)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green