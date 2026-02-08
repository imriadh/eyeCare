# 👁️ Eye Care App

A complete Android app built with Kotlin and Jetpack Compose to help you protect your eyes while using your device.

## 📱 Features

### 1. Blue Light Filter 🔶
- Creates a semi-transparent orange overlay over your entire screen
- Filters harmful blue light to reduce eye strain
- Runs as a foreground service to stay active
- Can be toggled on/off easily

### 2. 20-20-20 Rule Reminders ⏰
- Sends notifications every 20 minutes
- Reminds you to take a break following the 20-20-20 rule:
  - Every 20 minutes
  - Look at something 20 feet away
  - For 20 seconds
- Uses WorkManager for reliable background scheduling

## 🛠️ Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Background Work**: WorkManager for periodic notifications
- **Service**: Foreground Service for blue light filter overlay
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/eyecare/app/
│   │   ├── MainActivity.kt              # Main UI with Compose
│   │   ├── BlueLightService.kt          # Foreground service for overlay
│   │   ├── EyeCareWorker.kt             # WorkManager worker for notifications
│   │   └── ui/theme/                    # Theme files (Color, Type, Theme)
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml              # String resources
│   │   │   └── themes.xml               # App themes
│   │   └── xml/
│   │       ├── backup_rules.xml         # Backup configuration
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml              # App configuration & permissions
├── build.gradle.kts                     # App-level Gradle config
└── proguard-rules.pro
```

## 🚀 How to Build and Run

### Prerequisites

1. **Install Android Studio** (Latest stable version - Hedgehog or later)
   - Download from: https://developer.android.com/studio

2. **Install JDK 17**
   - Android Studio usually includes this
   - Or download from: https://adoptium.net/

### Step-by-Step Instructions

#### 1. Open the Project

1. Launch Android Studio
2. Click **"Open"** (or File → Open)
3. Navigate to the `eyeCare` folder and click **"OK"**
4. Wait for Gradle sync to complete (may take a few minutes on first run)

#### 2. Set Up an Emulator

1. In Android Studio, click the **Device Manager** icon (phone icon in the toolbar)
2. Click **"Create Device"**
3. Select a phone (e.g., **Pixel 6**)
4. Click **"Next"**
5. Download a system image:
   - **Recommended**: API 34 (Android 14) - "UpsideDownCake"
   - Click **"Download"** next to it
6. Click **"Next"** then **"Finish"**

#### 3. Build and Run

1. Make sure your emulator is selected in the device dropdown (top toolbar)
2. Click the green **"Run"** button (▶️) or press **Shift+F10**
3. Wait for the app to build and install
4. The app will automatically launch on the emulator

### Running on a Physical Device

1. Enable **Developer Options** on your Android phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
2. Enable **USB Debugging**:
   - Go to Settings → System → Developer Options
   - Enable "USB Debugging"
3. Connect your phone via USB
4. Accept the USB debugging prompt on your phone
5. Select your device from the device dropdown in Android Studio
6. Click the **"Run"** button

## 🔐 Permissions

The app requires two important permissions:

### 1. Display Over Other Apps (SYSTEM_ALERT_WINDOW)
- **Required for**: Blue Light Filter overlay
- **What happens**: When you toggle the Blue Light Filter, the app will show a button to grant this permission
- **Action**: Tap the button → You'll be taken to Settings → Enable "Display over other apps"

### 2. Notifications (POST_NOTIFICATIONS)
- **Required for**: 20-20-20 Rule Reminders
- **What happens**: When you toggle Reminders on Android 13+, the app will request this permission
- **Action**: Tap "Allow" on the permission dialog

## 📖 How to Use the App

### Enable Blue Light Filter

1. Open the Eye Care app
2. Look for the **"🔶 Blue Light Filter"** card
3. If prompted, grant the **"Display over other apps"** permission
4. Toggle the switch to **ON**
5. You'll see an orange tint over your entire screen
6. A persistent notification shows "Blue Light Filter Active"
7. Toggle **OFF** to remove the filter

### Enable 20-20-20 Reminders

1. Open the Eye Care app
2. Look for the **"⏰ 20-20-20 Rule Reminders"** card
3. If on Android 13+, grant notification permission when prompted
4. Toggle the switch to **ON**
5. You'll receive a notification every 20 minutes
6. The notification reminds you to look away for 20 seconds
7. Toggle **OFF** to stop reminders

## 🐛 Troubleshooting

### Build Errors

**Problem**: "Gradle sync failed"
- **Solution**: 
  - Go to File → Invalidate Caches → Check "Clear file system cache and Local History" → Invalidate and Restart
  - Or delete the `.gradle` folder and sync again

**Problem**: "SDK location not found"
- **Solution**:
  - Go to File → Project Structure → SDK Location
  - Set Android SDK location (usually: `/Users/YourName/Library/Android/sdk` on Mac or `C:\Users\YourName\AppData\Local\Android\Sdk` on Windows)

### Runtime Issues

**Problem**: Blue Light Filter doesn't show
- **Solution**: Make sure you granted "Display over other apps" permission in Settings

**Problem**: Notifications don't appear
- **Solution**: 
  - Check notification permission is granted
  - Go to Settings → Apps → Eye Care → Notifications → Ensure "All Eye Care notifications" is ON

**Problem**: Reminders stop after device reboot
- **Solution**: This is expected behavior. Open the app and toggle reminders ON again after restart

## 📝 Code Overview

### MainActivity.kt
- Main UI built with Jetpack Compose
- Handles permission requests
- Controls service and worker lifecycle
- Contains two toggle switches for features

### BlueLightService.kt
- Foreground Service that runs while the filter is active
- Creates a full-screen semi-transparent overlay using `WindowManager`
- Shows a persistent notification
- Automatically stops when toggled off

### EyeCareWorker.kt
- WorkManager Worker for periodic notifications
- Executes every 20 minutes
- Sends notification with 20-20-20 rule reminder
- Continues running even if app is closed

## 🎯 Key Learning Points for Beginners

1. **Jetpack Compose**: Modern declarative UI framework
2. **Material 3**: Latest Material Design components
3. **WorkManager**: Reliable background task scheduling
4. **Foreground Services**: Long-running background operations with user awareness
5. **Runtime Permissions**: Handling dangerous permissions properly
6. **WindowManager**: Drawing overlays on top of other apps

## 🔄 Version Information

- **Kotlin**: 1.9.22
- **Gradle**: 8.2
- **Android Gradle Plugin**: 8.2.2
- **Compose BOM**: 2024.02.00
- **WorkManager**: 2.9.0
- **Compile SDK**: 34
- **Min SDK**: 26

## 📄 License

This project is for educational purposes.

## 🤝 Contributing

This is a learning project. Feel free to fork and experiment!

## 📧 Support

If you encounter any issues:
1. Check the Troubleshooting section above
2. Make sure all Gradle dependencies downloaded successfully
3. Verify your Android Studio is up to date

---

**Built with ❤️ for eye health and Android learning**