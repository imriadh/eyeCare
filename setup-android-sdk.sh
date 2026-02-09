#!/bin/bash

# Android SDK Setup Script for GitHub Codespace
# This script installs everything needed to build Android APKs

set -e

echo "🚀 Setting up Android development environment..."
echo ""

# Install OpenJDK 17
echo "📦 Installing OpenJDK 17..."
sudo apt-get update -qq
sudo apt-get install -y openjdk-17-jdk wget unzip

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> ~/.bashrc

# Create Android SDK directory
echo "📁 Creating Android SDK directory..."
mkdir -p $HOME/android-sdk/cmdline-tools

# Download Android Command Line Tools
echo "⬇️  Downloading Android Command Line Tools..."
cd $HOME/android-sdk/cmdline-tools
wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip -q commandlinetools-linux-9477386_latest.zip
mv cmdline-tools latest
rm commandlinetools-linux-9477386_latest.zip

# Set environment variables
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Add to bashrc for persistence
echo "export ANDROID_HOME=$HOME/android-sdk" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/platform-tools" >> ~/.bashrc

# Accept licenses
echo "📜 Accepting Android SDK licenses..."
yes | sdkmanager --licenses > /dev/null 2>&1

# Install required SDK components
echo "⚙️  Installing SDK components..."
sdkmanager --install "platforms;android-34" "build-tools;34.0.0" "platform-tools" > /dev/null 2>&1

echo ""
echo "✅ Android SDK setup complete!"
echo ""
echo "📋 Installed components:"
echo "   - OpenJDK 17"
echo "   - Android SDK"
echo "   - Platform: Android 14 (API 34)"
echo "   - Build Tools: 34.0.0"
echo ""
echo "🎯 Next steps:"
echo "   1. Reload your terminal: source ~/.bashrc"
echo "   2. Build your app: ./build-apk.sh"
echo ""
echo "💡 Verify installation:"
echo "   java -version"
echo "   echo \$ANDROID_HOME"
echo ""
