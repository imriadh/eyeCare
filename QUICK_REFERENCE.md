# 🚀 Quick Reference Guide - Eye Care App

## 📁 All Files at a Glance

### 📄 Documentation (Read Me First!)
- **README.md** - Complete project documentation, features, and setup
- **SETUP_GUIDE.md** - Step-by-step beginner guide with explanations
- **PROJECT_SUMMARY.md** - Complete checklist of what was built
- **QUICK_REFERENCE.md** - This file! Quick reference for everything

---

## 🏗️ Project Configuration

### Build Files
| File | Purpose |
|------|---------|
| `settings.gradle.kts` | Project settings, module inclusion |
| `build.gradle.kts` (root) | Root-level build config, plugin versions |
| `app/build.gradle.kts` | App dependencies, SDK versions, compiler settings |
| `gradle.properties` | Gradle JVM settings, AndroidX flags |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle wrapper version |

### App Configuration
| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | Permissions, components, app metadata |
| `proguard-rules.pro` | Code shrinking rules (for release builds) |

---

## 💻 Source Code Files

### Main Code (Kotlin)

| File | Lines | Purpose |
|------|-------|---------|
| **MainActivity.kt** | ~330 | Main UI, permission handling, toggle controls |
| **BlueLightService.kt** | ~160 | Foreground service, overlay creation, WindowManager |
| **EyeCareWorker.kt** | ~100 | WorkManager worker, notification scheduling |

### Theme Files (Kotlin)

| File | Lines | Purpose |
|------|-------|---------|
| **ui/theme/Color.kt** | ~15 | Color definitions for light/dark themes |
| **ui/theme/Type.kt** | ~25 | Typography styles (fonts, sizes) |
| **ui/theme/Theme.kt** | ~65 | Material 3 theme configuration |

---

## 🎨 Resource Files

### Values
| File | Purpose |
|------|---------|
| `res/values/strings.xml` | All text strings used in app |
| `res/values/themes.xml` | App theme definition |

### XML Configuration
| File | Purpose |
|------|---------|
| `res/xml/backup_rules.xml` | Android backup configuration |
| `res/xml/data_extraction_rules.xml` | Data extraction rules |

---

## 🔑 Key Code Locations

### Permission Handling

**Check overlay permission:**
```kotlin
// File: MainActivity.kt, Line ~35
Settings.canDrawOverlays(context)
```

**Request notification permission:**
```kotlin
// File: MainActivity.kt, Line ~27
notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
```

**Open overlay settings:**
```kotlin
// File: MainActivity.kt, Line ~155
Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
```

---

### Blue Light Filter

**Start service:**
```kotlin
// File: MainActivity.kt, Line ~185
context.startForegroundService(Intent(context, BlueLightService::class.java))
```

**Create overlay:**
```kotlin
// File: BlueLightService.kt, Line ~91
overlayView = View(this).apply {
    setBackgroundColor(0x99FF6600.toInt()) // Semi-transparent orange
}
```

**Overlay parameters:**
```kotlin
// File: BlueLightService.kt, Line ~96
WindowManager.LayoutParams(
    MATCH_PARENT, MATCH_PARENT,
    TYPE_APPLICATION_OVERLAY,
    FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE,
    PixelFormat.TRANSLUCENT
)
```

---

### 20-20-20 Reminders

**Schedule reminders:**
```kotlin
// File: MainActivity.kt, Line ~338
PeriodicWorkRequestBuilder<EyeCareWorker>(
    20, TimeUnit.MINUTES  // ← Change interval here
)
```

**Send notification:**
```kotlin
// File: EyeCareWorker.kt, Line ~55
NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("Eye Care Reminder 👀")
    .setContentText("Take a 20-second break to look away from the screen!")
```

---

## 🎯 Quick Customization Guide

### Change Overlay Color

**File:** `BlueLightService.kt`
**Line:** 93
```kotlin
setBackgroundColor(0x99FF6600.toInt())
                    // ↑↑ Alpha (transparency)
                    //   ↑↑↑↑↑↑ RGB color
```

**Examples:**
- `0x99FF0000` - Red
- `0x99FFA500` - Orange
- `0x996B4423` - Warm brown
- `0x99FFD700` - Gold

First two digits = transparency (00=invisible, FF=opaque, 99=40% transparent)

---

### Change Reminder Interval

**File:** `MainActivity.kt`
**Line:** 339
```kotlin
20, TimeUnit.MINUTES  // Change 20 to any number
```

**Examples:**
- `1, TimeUnit.MINUTES` - Every 1 minute (testing)
- `15, TimeUnit.MINUTES` - Every 15 minutes
- `30, TimeUnit.MINUTES` - Every 30 minutes
- `1, TimeUnit.HOURS` - Every hour

⚠️ **Note:** WorkManager minimum is 15 minutes for production. Lower values work for testing.

---

### Change Notification Text

**File:** `EyeCareWorker.kt`
**Lines:** 56-61

```kotlin
.setContentTitle("Eye Care Reminder 👀")  // Change title here
.setContentText("Take a 20-second break...")  // Change message here
```

---

### Change App Name

**File:** `res/values/strings.xml`
**Line:** 3
```xml
<string name="app_name">Eye Care</string>
```

---

### Change App Colors

**File:** `ui/theme/Color.kt`

Define new colors, then use in `Theme.kt`

---

## 🐛 Common Issues - Quick Fixes

| Problem | Solution |
|---------|----------|
| Gradle sync fails | `File → Invalidate Caches → Restart` |
| "Cannot resolve androidx" | Check `gradle.properties` has `android.useAndroidX=true` |
| Emulator slow | Enable hardware acceleration in BIOS |
| Overlay not showing | Settings → Apps → Eye Care → Display over other apps → ON |
| No notifications | Settings → Apps → Eye Care → Notifications → ON |
| Build takes forever | First build always takes 3-5 minutes |

---

## ⌨️ Android Studio Shortcuts

### Essential Shortcuts

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| Run app | `Shift + F10` | `Ctrl + R` |
| Stop app | `Ctrl + F2` | `Cmd + F2` |
| Build project | `Ctrl + F9` | `Cmd + F9` |
| Find file | `Ctrl + Shift + N` | `Cmd + Shift + O` |
| Find text | `Ctrl + Shift + F` | `Cmd + Shift + F` |
| Format code | `Ctrl + Alt + L` | `Cmd + Alt + L` |
| Auto import | `Alt + Enter` | `Option + Enter` |

---

## 📊 Project Stats

| Metric | Count |
|--------|-------|
| Total files | 21 |
| Kotlin files | 6 |
| XML files | 5 |
| Gradle files | 4 |
| Documentation | 4 |
| Lines of code | ~850 |
| Features | 2 |
| Permissions | 4 |

---

## 🎓 Learning Path

### Week 1: Understanding
- [ ] Read all documentation
- [ ] Build and run the app
- [ ] Test both features
- [ ] Read through MainActivity.kt

### Week 2: Customization
- [ ] Change overlay color
- [ ] Change reminder interval
- [ ] Modify notification text
- [ ] Change app theme colors

### Week 3: Enhancement
- [ ] Add intensity slider
- [ ] Add usage statistics
- [ ] Create settings screen
- [ ] Add app icon

### Week 4: Advanced
- [ ] Add scheduling feature
- [ ] Implement multiple presets
- [ ] Add widget
- [ ] Prepare for Play Store

---

## 🔍 Where to Find Things

### "Where is the UI defined?"
→ `MainActivity.kt` → `EyeCareScreen()` composable (Line ~54)

### "Where is the overlay created?"
→ `BlueLightService.kt` → `showOverlay()` function (Line ~86)

### "Where are notifications sent?"
→ `EyeCareWorker.kt` → `sendNotification()` function (Line ~42)

### "Where are permissions checked?"
→ `MainActivity.kt` → Lines 35-50 (`hasOverlayPermission`, `hasNotificationPermission`)

### "Where is WorkManager scheduled?"
→ `MainActivity.kt` → `scheduleEyeCareReminders()` function (Line ~337)

### "Where are colors defined?"
→ `ui/theme/Color.kt`

### "Where are strings defined?"
→ `res/values/strings.xml`

### "Where are permissions declared?"
→ `AndroidManifest.xml` → Lines 5-11

---

## 📱 Testing Checklist

### Quick Test (5 minutes)
- [ ] App builds successfully
- [ ] App launches on emulator
- [ ] Blue Light toggle works
- [ ] Reminder toggle works

### Complete Test (25 minutes)
- [ ] Grant overlay permission
- [ ] Enable blue light filter
- [ ] Verify orange overlay appears
- [ ] Verify notification shows
- [ ] Disable blue light filter
- [ ] Verify overlay disappears
- [ ] Grant notification permission
- [ ] Enable reminders
- [ ] Wait for notification (or change to 1 minute)
- [ ] Verify notification appears
- [ ] Tap notification
- [ ] Verify app opens
- [ ] Disable reminders
- [ ] Verify no more notifications

---

## 🚀 Build & Run - One Command

```bash
# From project root (/workspaces/eyeCare)
# If you have Android SDK and emulator set up:

./gradlew installDebug  # Builds and installs
adb shell am start -n com.eyecare.app/.MainActivity  # Launches app
```

**But easier:** Just click ▶️ in Android Studio!

---

## 📞 Need Help?

1. **First:** Check SETUP_GUIDE.md
2. **Second:** Check README.md troubleshooting
3. **Third:** Check PROJECT_SUMMARY.md
4. **Fourth:** Google the error message
5. **Fifth:** Check Stack Overflow

---

## ✨ Pro Tips

💡 **Use Logcat** - View real-time logs: View → Tool Windows → Logcat

💡 **Quick rebuild** - If something's broken: Build → Clean Project → Rebuild

💡 **Test faster** - Change reminder to 1 minute while testing

💡 **Save time** - Use instant run: Settings → Build → Instant Run

💡 **Learn by breaking** - Change code, see what happens, undo if needed

---

## 🎉 You're Ready!

Everything you need is here. Start with:

1. Open in Android Studio
2. Wait for Gradle sync
3. Click Run (▶️)
4. Test the app!

**Good luck! 🚀📱**
