#!/bin/bash

# Eye Care App - Build Script for GitHub Codespace
# This script builds the APK without needing Android Studio

echo "🚀 Building Eye Care App..."
echo ""

# Check if ANDROID_HOME is set
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME not set. Setting up..."
    export ANDROID_HOME=$HOME/android-sdk
fi

# Make gradlew executable
chmod +x ./gradlew

echo "📦 Building Debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build Successful!"
    echo ""
    echo "📱 Your APK is ready at:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📥 To install on your phone:"
    echo "   1. Download the APK from the file explorer"
    echo "   2. Transfer to your Android phone"
    echo "   3. Enable 'Install from Unknown Sources'"
    echo "   4. Tap the APK to install"
    echo ""
    
    # Show APK size
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        size=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
        echo "📊 APK Size: $size"
    fi
else
    echo ""
    echo "❌ Build Failed!"
    echo "Check the error messages above."
    exit 1
fi
