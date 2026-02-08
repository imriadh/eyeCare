# 🏗️ Eye Care App Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        USER INTERFACE                        │
│                      (MainActivity.kt)                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Jetpack Compose UI Layer                  │ │
│  │                                                         │ │
│  │  ┌──────────────────┐    ┌──────────────────┐        │ │
│  │  │ Blue Light Card  │    │ Reminders Card   │        │ │
│  │  │  - Title         │    │  - Title         │        │ │
│  │  │  - Description   │    │  - Description   │        │ │
│  │  │  - Permissions   │    │  - Permissions   │        │ │
│  │  │  - Toggle Switch │    │  - Toggle Switch │        │ │
│  │  └────────┬─────────┘    └────────┬─────────┘        │ │
│  └───────────┼──────────────────────┼────────────────────┘ │
└──────────────┼──────────────────────┼──────────────────────┘
               │                      │
               │                      │
      ┌────────▼────────┐    ┌────────▼─────────┐
      │  Permission     │    │   Permission     │
      │  Check/Request  │    │  Check/Request   │
      └────────┬────────┘    └────────┬─────────┘
               │                      │
    ┌──────────▼──────────┐  ┌────────▼──────────────┐
    │  SYSTEM_ALERT       │  │  POST_NOTIFICATIONS   │
    │  _WINDOW            │  │  (Android 13+)        │
    └──────────┬──────────┘  └────────┬──────────────┘
               │                      │
      ┌────────▼───────────┐   ┌──────▼───────────────┐
      │  BlueLightService  │   │   scheduleWork()     │
      │  - onCreate()      │   │   WorkManager API    │
      │  - onStartCommand()│   └──────┬───────────────┘
      │  - showOverlay()   │          │
      │  - removeOverlay() │          │
      │  - onDestroy()     │   ┌──────▼───────────────┐
      └────────┬───────────┘   │   EyeCareWorker      │
               │                │   - doWork()         │
      ┌────────▼───────────┐   │   - sendNotification│
      │  WindowManager API │   └──────┬───────────────┘
      │  - addView()       │          │
      │  - LayoutParams    │   ┌──────▼───────────────┐
      │  - Overlay View    │   │  NotificationManager │
      └────────┬───────────┘   │  - createChannel()   │
               │                │  - notify()          │
      ┌────────▼───────────┐   └──────┬───────────────┘
      │ Foreground Service │          │
      │ Notification       │   ┌──────▼───────────────┐
      └────────────────────┘   │   Notification       │
                               │   (every 20 min)     │
                               └──────────────────────┘
```

---

## Component Interaction Flow

### Flow 1: Enabling Blue Light Filter

```
User taps toggle ON
      ↓
Check if SYSTEM_ALERT_WINDOW permission granted
      ↓
   ┌──NO──→ Show "Grant Permission" button
   │              ↓
   │         User taps button
   │              ↓
   │         Open Settings page
   │              ↓
   │         User enables permission
   │              ↓
   └───YES──→ Start BlueLightService
                  ↓
           Service starts as foreground
                  ↓
           Create notification (persistent)
                  ↓
           Create overlay View with orange tint
                  ↓
           Configure WindowManager.LayoutParams
                  ↓
           WindowManager.addView(overlayView)
                  ↓
           Orange overlay appears on screen ✓
```

### Flow 2: Enabling 20-20-20 Reminders

```
User taps toggle ON
      ↓
Check if POST_NOTIFICATIONS permission granted (Android 13+)
      ↓
   ┌──NO──→ Request permission via launcher
   │              ↓
   │         User grants/denies
   │              ↓
   └───YES──→ Schedule periodic work
                  ↓
           Create PeriodicWorkRequest
           - Interval: 20 minutes
           - Tag: "eye_care_reminder_work"
                  ↓
           WorkManager.enqueueUniquePeriodicWork()
                  ↓
           WorkManager schedules EyeCareWorker
                  ↓
           [Wait 20 minutes]
                  ↓
           WorkManager executes EyeCareWorker
                  ↓
           EyeCareWorker.doWork() runs
                  ↓
           Create notification channel (if needed)
                  ↓
           Build notification with NotificationCompat
                  ↓
           NotificationManager.notify()
                  ↓
           Notification appears on screen ✓
                  ↓
           [Repeats every 20 minutes]
```

---

## File Dependency Graph

```
MainActivity.kt
    ├─→ Uses: BlueLightService.kt
    ├─→ Uses: EyeCareWorker.kt
    ├─→ Uses: ui/theme/Theme.kt
    │        ├─→ Uses: Color.kt
    │        └─→ Uses: Type.kt
    ├─→ Reads: AndroidManifest.xml (permissions)
    └─→ Reads: strings.xml (optional)

BlueLightService.kt
    ├─→ Defined in: AndroidManifest.xml
    ├─→ Uses: WindowManager (Android SDK)
    └─→ Uses: NotificationManager (Android SDK)

EyeCareWorker.kt
    ├─→ Scheduled by: MainActivity.kt
    ├─→ Uses: WorkManager (Jetpack)
    └─→ Uses: NotificationManager (Android SDK)

build.gradle.kts (app)
    ├─→ Imports: Kotlin stdlib
    ├─→ Imports: Jetpack Compose
    ├─→ Imports: WorkManager
    └─→ Imports: AndroidX libraries
```

---

## State Management Flow

```
┌─────────────────────────────────────┐
│       Compose State Layer           │
├─────────────────────────────────────┤
│ blueLightFilterEnabled: Boolean     │
│ remindersEnabled: Boolean           │
│ hasOverlayPermission: Boolean       │
│ hasNotificationPermission: Boolean  │
└────────┬──────────────────────┬─────┘
         │                      │
    ┌────▼──────┐         ┌─────▼────┐
    │  Switch   │         │  Switch  │
    │  onChange │         │ onChange │
    └────┬──────┘         └─────┬────┘
         │                      │
    ┌────▼──────────┐   ┌───────▼──────────┐
    │ startService()│   │ scheduleWork()   │
    │ stopService() │   │ cancelWork()     │
    └───────────────┘   └──────────────────┘
```

---

## Android Components Used

### Activities
- **MainActivity** - Single activity, hosts entire UI

### Services
- **BlueLightService** - Foreground service
  - Type: FOREGROUND_SERVICE_SPECIAL_USE
  - Lifecycle: START_STICKY
  - Shows: Persistent notification

### Workers
- **EyeCareWorker** - WorkManager Worker
  - Type: PeriodicWorkRequest
  - Interval: 20 minutes
  - Constraints: None

### Broadcast Receivers
- None (WorkManager handles scheduling)

### Content Providers
- WorkManager's initialization provider (auto-included)

---

## Permission Flow Chart

```
                    App Starts
                        ↓
        ┌───────────────┴───────────────┐
        ↓                               ↓
Check Overlay Permission      Check Notification Permission
        ↓                               ↓
    Granted?                        Granted?
    ↙     ↘                         ↙     ↘
  YES     NO                      YES     NO
   ↓       ↓                       ↓       ↓
Enable   Show                  Enable   Request
Switch   Warning                Switch   Runtime
         + Button                        Permission
              ↓                              ↓
         User Grants                    User Response
              ↓                         ↙           ↘
         Return to App              ALLOW        DENY
              ↓                       ↓             ↓
         Auto-recheck            Enable        Keep
              ↓                   Switch      Disabled
         Enable Switch
```

---

## Notification Channels

### Channel 1: Blue Light Filter
- **ID**: `blue_light_filter_channel`
- **Importance**: LOW
- **Purpose**: Foreground service notification
- **Dismissible**: No (ongoing)
- **Shows**: "Blue Light Filter Active"

### Channel 2: Eye Care Reminders
- **ID**: `eye_care_reminder_channel`
- **Importance**: HIGH
- **Purpose**: 20-20-20 reminders
- **Dismissible**: Yes
- **Shows**: "Take a 20-second break..."

---

## Threading Model

### Main Thread (UI Thread)
- Compose UI rendering
- User interactions
- State updates
- Service start/stop

### Background Threads
- WorkManager worker execution (EyeCareWorker.doWork())
- Notification posting

### No Threading Issues
- No coroutines needed (simple operations)
- No AsyncTask or Threads created
- WorkManager handles threading automatically

---

## Data Storage

### None Required!
- No databases
- No SharedPreferences
- No files
- State is ephemeral (resets on app restart)

**Possible Enhancement:**
- Save preferences (filter color, reminder interval)
- Track statistics (hours protected, reminders shown)
- Store user settings

---

## Memory Management

### Views
- `overlayView` - Created when service starts
- Removed when service stops
- Single view, minimal memory impact

### Services
- BlueLightService runs while filter is active
- Auto-stopped when toggle is disabled
- Releases all resources in `onDestroy()`

### Workers
- EyeCareWorker - Short-lived (< 1 second)
- Executes, sends notification, terminates
- No memory retained between executions

---

## Build Process Flow

```
Gradle Sync
    ↓
Download Dependencies
    ├─→ Kotlin stdlib
    ├─→ AndroidX core
    ├─→ Compose libraries
    └─→ WorkManager
    ↓
Compile Kotlin → JVM Bytecode
    ↓
Process Resources
    ├─→ AndroidManifest.xml
    ├─→ strings.xml
    └─→ themes.xml
    ↓
Generate R.java (resource IDs)
    ↓
DEX Compilation (Bytecode → DEX)
    ↓
Package APK
    ├─→ Add DEX files
    ├─→ Add resources
    ├─→ Add manifest
    └─→ Sign (debug keystore)
    ↓
Install via ADB
    ↓
Launch MainActivity
    ↓
App Running ✓
```

---

## Runtime Lifecycle

### App Launch Sequence

```
1. Android System launches app
         ↓
2. Application class initializes
         ↓
3. WorkManager initializes (via provider)
         ↓
4. MainActivity.onCreate() called
         ↓
5. setContent() creates Compose UI
         ↓
6. EyeCareScreen() composable executes
         ↓
7. UI renders on screen
         ↓
8. Permission checks execute
         ↓
9. UI updates based on permission state
         ↓
10. Ready for user interaction ✓
```

### Service Lifecycle (Blue Light Filter)

```
User enables toggle
    ↓
startForegroundService(Intent)
    ↓
BlueLightService.onCreate()
    ↓
BlueLightService.onStartCommand()
    ├─→ Create notification
    ├─→ startForeground(notification)
    └─→ showOverlay()
        ├─→ Create View
        ├─→ Set background color
        └─→ WindowManager.addView()
    ↓
Service running (overlay visible)
    ↓
User disables toggle
    ↓
stopService(Intent)
    ↓
BlueLightService.onDestroy()
    └─→ removeOverlay()
        └─→ WindowManager.removeView()
    ↓
Service stopped ✓
```

---

## Summary

This architecture demonstrates:

✅ Clean separation of concerns
✅ Proper Android lifecycle management
✅ Efficient resource usage
✅ Modern Android development patterns
✅ User-friendly permission handling
✅ Robust background task scheduling

**Design Philosophy:**
- Simple but complete
- Production-ready patterns
- Easy to understand
- Easy to extend
- Beginner-friendly

---

**For more details, see:**
- README.md - Complete documentation
- SETUP_GUIDE.md - Step-by-step guide
- PROJECT_SUMMARY.md - Feature checklist
- QUICK_REFERENCE.md - Quick lookup
