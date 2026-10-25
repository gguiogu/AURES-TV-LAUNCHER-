@rem ====================================================================
@rem AURÈS TV — Gradle / Standalone Build Dispatcher for Windows
@rem ====================================================================
@if "%DEBUG%" == "" @echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.

@rem Check if standard gradle wrapper jar exists
if exist "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" (
    java -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
    goto end
)

@rem If gradle is installed on system PATH
where gradle >nul 2>nul
if %ERRORLEVEL% equ 0 (
    gradle %*
    goto end
)

@rem Otherwise fallback to standalone fast build script
echo [AURÈS TV] Running zero-dependency standalone build engine...
powershell -ExecutionPolicy Bypass -File "%DIRNAME%build.ps1"

:end
@endlocal