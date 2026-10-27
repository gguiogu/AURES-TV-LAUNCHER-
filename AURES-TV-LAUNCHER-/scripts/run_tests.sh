#!/bin/bash
set -e
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then ANDROID_HOME="$HOME/Android/Sdk"; fi
fi
PLAT=$(ls -d "$ANDROID_HOME/platforms/android-"* | sort -V | tail -n 1)
JAR="$PLAT/android.jar"

OUT="$ROOT_DIR/out/test-classes"
rm -rf "$OUT" && mkdir -p "$OUT"

find "$ROOT_DIR/app/src/main/java" -name "*.java" > "$OUT/src.txt"
find "$ROOT_DIR/tests" -name "*.java" >> "$OUT/src.txt"

javac -nowarn -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR" -d "$OUT" @"$OUT/src.txt"
java -cp "$OUT:$JAR" tests.TestRunner