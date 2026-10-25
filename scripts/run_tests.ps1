<#
.SYNOPSIS
    Compiles and executes the AURÃˆS TV test suite.
#>
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent $PSScriptRoot
if (!$rootDir) { $rootDir = Get-Location }

# Locate Android SDK for android.jar
$rawCandidates = @(
    $env:ANDROID_HOME,
    $env:ANDROID_SDK_ROOT,
    "$env:LOCALAPPDATA\Android\Sdk",
    "C:\Android\Sdk"
)
$SDK = $null
foreach ($c in $rawCandidates) {
    if ($c -and (Test-Path "$c\build-tools")) {
        $SDK = $c
        break
    }
}
if (!$SDK) { throw "Android SDK not found." }

$platDirs = Get-ChildItem -Path "$SDK\platforms" -Directory | Sort-Object Name -Descending
$JAR = "$($platDirs[0].FullName)\android.jar"
$btDirs = Get-ChildItem -Path "$SDK\build-tools" -Directory | Sort-Object Name -Descending
$coreLambda = "$($btDirs[0].FullName)\core-lambda-stubs.jar"
$bootcp = if (Test-Path $coreLambda) { "$JAR;$coreLambda" } else { $JAR }

$testOut = Join-Path $rootDir "out\test-classes"
if (Test-Path $testOut) { Remove-Item -Recurse -Force $testOut }
New-Item -ItemType Directory -Force -Path $testOut | Out-Null

$srcFiles = Get-ChildItem -Path (Join-Path $rootDir "app\src\main\java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
$testFiles = Get-ChildItem -Path (Join-Path $rootDir "tests") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

Write-Host "Compiling test runner..." -ForegroundColor Cyan
& javac -nowarn -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath $bootcp -d $testOut $srcFiles $testFiles
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed." }

Write-Host "Running AURÃˆS TV Unit Tests..." -ForegroundColor Cyan
& java -cp "$testOut;$JAR" tests.TestRunner
if ($LASTEXITCODE -ne 0) { throw "Unit tests failed." }