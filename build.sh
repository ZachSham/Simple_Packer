#!/bin/bash
set -e

echo "[*] Building packer..."

# Compile packer (SimplePacker only; StubApplication is Android)
mkdir -p build
javac -d build src/com/packer/SimplePacker.java
cd build
jar cvfe ../SimplePacker.jar com.packer.SimplePacker com/packer/SimplePacker.class
cd ..

echo "[*] Building stub..."

# Require Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "[!] ANDROID_HOME not set. Set it to your Android SDK root (e.g. from Android Studio)."
    echo "    Example: export ANDROID_HOME=~/Library/Android/sdk"
    exit 1
fi

# Prefer android-30; fallback to any platform
PLATFORM_JAR="$ANDROID_HOME/platforms/android-30/android.jar"
[ ! -f "$PLATFORM_JAR" ] && PLATFORM_JAR=$(find "$ANDROID_HOME/platforms" -name "android.jar" 2>/dev/null | head -n 1)
if [ -z "$PLATFORM_JAR" ] || [ ! -f "$PLATFORM_JAR" ]; then
    echo "[!] No android.jar found. Install a platform (e.g. Android Studio SDK Manager → Android 11.0 / API 30)."
    exit 1
fi

# Compile stub for Android
javac -source 1.8 -target 1.8 \
    -bootclasspath "$PLATFORM_JAR" \
    -d build \
    src/com/packer/StubApplication.java \
    src/com/packer/LaunchActivity.java

# Convert to DEX (d8 or dx)
D8=$(find "$ANDROID_HOME/build-tools" -name "d8" -type f 2>/dev/null | head -n 1)
DX=$(find "$ANDROID_HOME/build-tools" -name "dx" -type f 2>/dev/null | head -n 1)

cd build
if [ -n "$D8" ]; then
    "$D8" --output ../stub.zip com/packer/StubApplication.class com/packer/LaunchActivity.class
elif [ -n "$DX" ]; then
    "$DX" --dex --output=../stub.dex com/packer/StubApplication.class com/packer/LaunchActivity.class
    cd ..
    rm -rf build
    echo "[+] Done! Run: ./pack <input.apk> [output.apk]"
    exit 0
else
    echo "[!] Neither d8 nor dx found under $ANDROID_HOME/build-tools"
    exit 1
fi
cd ..

# d8 outputs a zip; extract and rename to stub.dex
unzip -o -q stub.zip 2>/dev/null
if [ -f "classes.dex" ]; then
    mv -f "classes.dex" stub.dex
else
    DEX=$(find . -maxdepth 2 -name "*.dex" -type f 2>/dev/null | head -n 1)
    if [ -n "$DEX" ]; then
        mv -f "$DEX" stub.dex
    else
        echo "[!] No .dex in stub.zip"
        exit 1
    fi
fi
rm -f stub.zip
rm -rf build com META-INF 2>/dev/null

echo "[+] Done! Run: ./pack <input.apk> [output.apk]"
