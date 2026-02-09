# 👁️ Eye Care App

A modern Android app built with Kotlin and Jetpack Compose to help you protect your eyes with the 20-20-20 rule.

## 🌟 Version 4.0 - What's New!

### ✨ Persistent Notification with Live Timer
- **Real-time countdown** displayed in notification bar
- **Always visible** - See your next break timer without opening the app
- **Updates every second** for accurate timing
- **Works when app is closed** or in background

### 🎮 Interactive Notification Controls
- **Pause/Resume Button** - Toggle reminders directly from notification
- **Reset Button** - Restart countdown when you take a manual break
- **Quick Actions** - No need to open the app!

### 🎨 Completely Redesigned UI
- Modern card design with dynamic elevation
- Gradient surfaces and icon badges
- Smooth transitions and better spacing
- Polished typography and color theming

### 👁️ Brand New Logo
- Custom eye icon with professional design
- Beautiful purple gradient background
- Adaptive icon for all Android launchers

## 📱 Features

### 1. 20-20-20 Rule Reminders ⏰
- **Persistent notification** shows countdown timer throughout your phone
- **Customizable intervals** from 15 to 60 minutes
- **Live countdown** updates every second in notification
- **Pause/Resume/Reset controls** in notification
- Reminds you to take breaks following the 20-20-20 rule:
  - Every 20 minutes
  - Look at something 20 feet away
  - For 20 seconds
- Uses WorkManager for reliable background scheduling
- **Sound Notifications** - Optional sound alerts (toggle on/off)

### 2. Sleep Cycle Calculator 😴
- Calculate optimal wake-up times based on 90-minute sleep cycles
- 5 different wake-up time options
- Custom bedtime selection
- Direct alarm integration - opens clock app to set alarm
- Helps you wake up refreshed

### 3. Settings & Information ⚙️
- Educational content about eye health
- 20-20-20 rule explained
- Why taking breaks matters
- Proper break instructions
- App information and version details

### 4. Customization Options 🎛️
- **Adjustable Interval** - Set reminder from 15 to 60 minutes via slider
- **Sound Control** - Enable/disable notification sounds
- **Pause Options** - Temporarily pause reminders (30 min, 1 hour, 2 hours)
- **Bottom Navigation** - Clean 3-tab interface (Home, Sleep, Settings)
- Settings persist across app restarts

## 🛠️ Technical Stack

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose with Material 3
- **Background Work**: WorkManager for periodic notifications
- **Foreground Service**: Persistent notification with live timer
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **APK Size**: 8.8 MB

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/eyecare/app/
│   │   ├── MainActivity.kt                    # Main UI with Compose (3-tab navigation)
│   │   ├── TimerNotificationService.kt        # Persistent notification service
│   │   ├── EyeCareWorker.kt                   # WorkManager for scheduled reminders
│   │   ├── PreferencesHelper.kt               # Settings management
│   │   └── ui/theme/                          # Theme files (Color, Type, Theme)
│   ├── res/
│   │   ├── drawable/
│   │   │   ├── ic_launcher_foreground.xml     # Custom eye icon
│   │   │   └── ic_launcher_background.xml     # Gradient background
│   │   ├── mipmap-anydpi-v26/                 # Adaptive icons
│   │   ├── values/
│   │   │   ├── strings.xml                    # String resources
│   │   │   ├── colors.xml                     # Color palette
│   │   │   └── themes.xml                     # App themes
│   │   └── xml/
│   │       ├── backup_rules.xml               # Backup configuration
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml                    # App configuration & permissions
├── build.gradle.kts                           # App-level Gradle config
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

The app requires the following permissions:

### 1. **POST_NOTIFICATIONS** (Android 13+)
- **Why**: To send eye care reminder notifications
- **When**: You'll be prompted when you first enable reminders
- **Required**: Yes - without it, you won't receive break reminders

### 2. **FOREGROUND_SERVICE**
- **Why**: To show persistent notification with live countdown timer
- **When**: Granted automatically by the system
- **Required**: Yes - for notification to stay visible when app is closed

### 3. **SCHEDULE_EXACT_ALARM**
- **Why**: To send reminders at precise intervals
- **When**: Granted automatically by the system
- **Required**: Yes - for accurate reminder timing

## 📖 How to Use the App

### Enable 20-20-20 Reminders

1. Open the Eye Care app
2. Look for the **"⏰ 20-20-20 Rule"** card on the Home tab
3. If on Android 13+, grant notification permission when prompted
4. Toggle the switch to **ON**
5. **Persistent notification appears** with live countdown timer
6. **Timer updates every second** showing time until next break
7. You'll receive reminder notifications at your set interval
8. **Use notification buttons** to control reminders:
   - **⏸️ Pause** - Temporarily pause reminders
   - **▶️ Resume** - Continue reminders
   - **🔄 Reset** - Restart countdown timer

### Customize Your Experience

**Adjust Reminder Interval:**
1. Enable reminders first
2. Use the slider to set interval (15-60 minutes)
3. The interval badge shows current setting
4. Changes apply immediately

**Enable/Disable Sound:**
1. Enable reminders first
2. Toggle the "🔊 Sound Alerts" switch
3. Notifications will play sound when enabled

**Pause Reminders Temporarily:**
1. Enable reminders first
2. Tap "Pause Reminders" button in app, OR
3. Tap "⏸️ Pause" button in the notification
4. Choose duration: 30 minutes, 1 hour, or 2 hours
5. Reminders automatically resume after the pause period

### Use Sleep Cycle Calculator

1. Switch to the **"Sleep"** tab in bottom navigation
2. Choose "Sleep Now" or "Custom Time"
3. If custom, select your bedtime
4. View 5 optimal wake-up time options (based on 90-min cycles)
5. Tap "Set Alarm" to open your clock app
6. Confirm the alarm time

### View Settings & Information

1. Switch to the **"Settings"** tab
2. Read about why eye breaks matter
3. Learn the 20-20-20 rule
4. View app information

## 🐛 Troubleshooting

### Notification Not Showing

**Problem**: Persistent notification doesn't appear
- **Solution 1**: Make sure you granted notification permission
- **Solution 2**: Check that reminders are enabled in the app
- **Solution 3**: Go to Android Settings → Apps → Eye Care → Notifications → Enable all categories

### Timer Not Updating

**Problem**: Countdown timer is frozen
- **Solution**: Disable and re-enable reminders to restart the service

### App Stops Working

**Problem**: Reminders stop after phone restarts
- **Solution**: Open the app once after restart to reschedule reminders

## 🎨 Screenshots

(Screenshots will be added here)

## 💡 Why Eye Care Matters

### The Problem
- **Digital Eye Strain**: Prolonged screen time causes fatigue, dryness, and discomfort
- **Computer Vision Syndrome**: Affects 50-90% of computer users
- **Sleep Disruption**: Blue light before bed can affect sleep quality

### The Solution - 20-20-20 Rule
- **Every 20 minutes**: Take a break
- **Look 20 feet away**: Focus on distant objects
- **For 20 seconds**: Give your eyes time to relax

### Benefits
- ✅ Reduces eye strain and fatigue
- ✅ Prevents dry eyes
- ✅ Improves focus and productivity
- ✅ Better long-term eye health

## 🔮 Version History

### v4.0 (Current) - February 9, 2025
- ✨ Persistent notification with live countdown timer
- 🎮 Notification action buttons (Pause/Resume/Reset)
- 🎨 Completely redesigned UI
- 👁️ New custom eye care logo
- 🗑️ Removed blue light filter (phones have built-in)

### v3.0
- Bottom navigation with 3 tabs
- Sleep cycle calculator
- Settings screen
- Fixed blue light filter permission

### v2.0
- Blue light filter with overlay
- Enhanced notifications

### v1.0
- Initial release
- Basic 20-20-20 reminders

## 🙏 Credits

Built with ❤️ using:
- **Kotlin** - Modern programming language
- **Jetpack Compose** - Declarative UI framework
- **Material 3** - Latest Material Design

## 📄 License

This project is open source and available under the MIT License.

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest new features
- Submit pull requests

---

**Made with care for your eyes** 👁️✨

*Last updated: February 9, 2025 - Version 4.0*
