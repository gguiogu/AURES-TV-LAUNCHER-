#!/bin/bash
# ==========================================================
#   AURÈS TV (1962) — Automated Reproducible Build Engine (Bash)
# ==========================================================
set -e

echo "=== AURÈS TV: Building Release APK ==="

# 1. Locate Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/lib/android-sdk" ]; then
        ANDROID_HOME="/usr/lib/android-sdk"
    else
        echo "Error: ANDROID_HOME is not set." >&2
        exit 1
    fi
fi

BT=$(ls -d "$ANDROID_HOME/build-tools/"* | sort -V | tail -n 1)
PLAT=$(ls -d "$ANDROID_HOME/platforms/android-"* | sort -V | tail -n 1)
JAR="$PLAT/android.jar"

AAPT2="$BT/aapt2"
D8="$BT/d8"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$ROOT_DIR/out"
DIST_DIR="$ROOT_DIR/dist"

rm -rf "$OUT_DIR/classes" "$OUT_DIR/dex"
mkdir -p "$OUT_DIR/classes" "$OUT_DIR/dex" "$DIST_DIR"

echo "[1/6] Compiling resources with aapt2..."
"$AAPT2" link -o "$OUT_DIR/base.apk" -I "$JAR" \
    --manifest "$ROOT_DIR/app/src/main/AndroidManifest.xml" \
    --min-sdk-version 21 --target-sdk-version 29

echo "[2/6] Compiling Java sources with javac..."
find "$ROOT_DIR/app/src/main/java" -name "*.java" > "$OUT_DIR/sources.txt"
javac -nowarn -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR" -d "$OUT_DIR/classes" @"$OUT_DIR/sources.txt"

echo "[3/6] Converting bytecode to DEX with d8..."
find "$OUT_DIR/classes" -name "*.class" > "$OUT_DIR/classes.txt"
"$D8" --min-api 21 --release --lib "$JAR" --output "$OUT_DIR/dex" $(cat "$OUT_DIR/classes.txt")

echo "[4/6] Packaging APK..."
cp "$OUT_DIR/base.apk" "$OUT_DIR/unsigned.apk"
cd "$OUT_DIR/dex" && jar uf "$OUT_DIR/unsigned.apk" classes.dex
cd "$ROOT_DIR/app/src/main" && jar uf "$OUT_DIR/unsigned.apk" assets

echo "[5/6] Aligning APK..."
"$ZIPALIGN" -f 4 "$OUT_DIR/unsigned.apk" "$OUT_DIR/aligned.apk"

echo "[6/6] Signing APK..."
KS="$ROOT_DIR/scripts/release.keystore"
PASS="algeria1962"
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" \
        -alias aures1962 -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=AuresTV, OU=DZ, O=Algeria, C=DZ"
fi

"$APKSIGNER" sign --ks "$KS" --ks-pass "pass:$PASS" --out "$DIST_DIR/aures-tv-2.0.0-release.apk" "$OUT_DIR/aligned.apk"

echo "=== BUILD SUCCESSFUL ==="
echo "APK: $DIST_DIR/aures-tv-2.0.0-release.apk"