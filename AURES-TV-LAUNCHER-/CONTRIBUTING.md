# Contributing to AURÈS TV (1962)

Thank you for your interest in contributing to AURÈS TV! We welcome bug fixes, documentation improvements, and performance optimizations.

## Core Design Principles

1. **Strict Performance & Memory Budget**:
   - Total PSS memory usage must remain under **60 MB** on 2 GB RAM Android TV boxes (target benchmark: **35.5 MB**).
   - Zero WebViews. Zero Jetpack Compose overhead for TV.
   - Clean native Android Views with hardware acceleration.

2. **Authentic Algerian Identity**:
   - Preserves Algerian cultural identity (1962 liberation, Aurès mountains, official Ministry prayer parameters).
   - Clean home screen with zero fake movie carousels or mock streaming rows.

3. **Prayer Engine Accuracy**:
   - Official Ministry calculation parameters (18.0° Fajr, 17.0° Isha, 1:1 Maliki Asr, Africa/Algiers timezone).
   - Exact Arabic announcement phrasing byte-for-byte.

## Testing Your Changes
Before submitting a pull request:
1. Run the test suite:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\run_tests.ps1
   ```
2. Verify that all 35 tests pass.
3. Rebuild the APK:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\build.ps1
   ```
4. Verify the build succeeds with 0 errors.