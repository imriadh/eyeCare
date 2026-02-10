package com.eyecare.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

/**
 * Timer Notification Service
 * Shows persistent notification with countdown timer and action buttons
 */
class TimerNotificationService : Service() {

    private var serviceJob: Job? = null
    private var serviceScope: CoroutineScope? = null

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "eye_care_timer_channel"
        private const val CHANNEL_NAME = "Eye Care Timer"
        
        const val ACTION_PAUSE_RESUME = "com.eyecare.app.ACTION_PAUSE_RESUME"
        const val ACTION_RESET = "com.eyecare.app.ACTION_RESET"
        const val ACTION_STOP = "com.eyecare.app.ACTION_STOP"
        const val ACTION_START_NEXT = "com.eyecare.app.ACTION_START_NEXT"
        
        fun startService(context: Context) {
            val intent = Intent(context, TimerNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, TimerNotificationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always start foreground immediately
        val notification = createTimerNotification(applicationContext)
        startForeground(NOTIFICATION_ID, notification)
        
        when (intent?.action) {
            ACTION_PAUSE_RESUME -> handlePauseResume()
            ACTION_RESET -> handleReset()
            ACTION_START_NEXT -> handleStartNext()
            ACTION_STOP -> {
                // Fully disable reminders when user closes notification
                val context = applicationContext
                
                // 1. Cancel all WorkManager periodic jobs
                androidx.work.WorkManager.getInstance(context)
                    .cancelAllWorkByTag("eye_care_reminder_work")
                
                // 2. Clear timer state
                PreferencesHelper.setLastNotificationTime(context, 0)
                PreferencesHelper.setPauseUntil(context, 0)
                PreferencesHelper.setPausedRemainingTime(context, 0)
                
                // 3. Disable reminders in preferences
                PreferencesHelper.setRemindersEnabled(context, false)
                
                // 4. Stop this service
                stopSelf()
            }
            else -> startTimerUpdates()
        }
        
        return START_STICKY
    }

    private fun startTimerUpdates() {
        serviceJob?.cancel()
        serviceJob = serviceScope?.launch {
            while (isActive) {
                updateNotification()
                delay(1000) // Update every second
            }
        }
    }

    private fun handlePauseResume() {
        val context = applicationContext
        val isPaused = PreferencesHelper.isPaused(context)
        
        if (isPaused) {
            // Resume with preserved remaining time
            val savedRemainingTime = PreferencesHelper.getPausedRemainingTime(context)
            if (savedRemainingTime > 0) {
                // Calculate new lastNotificationTime to preserve remaining time
                val intervalMillis = PreferencesHelper.getReminderInterval(context) * 60 * 1000L
                val newLastNotificationTime = System.currentTimeMillis() - (intervalMillis - savedRemainingTime)
                PreferencesHelper.setLastNotificationTime(context, newLastNotificationTime)
            } else {
                // Fallback: just reset to current time
                PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
            }
            PreferencesHelper.setPauseUntil(context, 0)
            PreferencesHelper.setPausedRemainingTime(context, 0)
            
            // Sync timer state (resumed)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                SyncManager.getInstance(context).uploadTimerState(
                    isPaused = false,
                    remainingTimeMillis = PreferencesHelper.getTimeRemainingMillis(context),
                    lastNotificationTime = PreferencesHelper.getLastNotificationTime(context)
                )
            }
        } else {
            // Pause - save current remaining time
            val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
            PreferencesHelper.setPausedRemainingTime(context, timeRemaining)
            val pauseUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // 24 hours
            PreferencesHelper.setPauseUntil(context, pauseUntil)
            
            // Sync timer state (paused)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                SyncManager.getInstance(context).uploadTimerState(
                    isPaused = true,
                    remainingTimeMillis = timeRemaining,
                    lastNotificationTime = PreferencesHelper.getLastNotificationTime(context)
                )
            }
        }
        
        updateNotification()
    }

    private fun handleReset() {
        val context = applicationContext
        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
        PreferencesHelper.setPauseUntil(context, 0)
        PreferencesHelper.setTimerCompleted(context, false)
        updateNotification()
    }
    
    private fun handleStartNext() {
        val context = applicationContext
        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
        PreferencesHelper.setPauseUntil(context, 0)
        PreferencesHelper.setPausedRemainingTime(context, 0)
        PreferencesHelper.setTimerCompleted(context, false)
        startTimerUpdates()
        updateNotification()
    }

    private fun updateNotification() {
        val context = applicationContext
        val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
        
        // Check quiet hours - if in quiet hours and smart breaks enabled, auto-pause
        if (PreferencesHelper.isSmartBreaksEnabled(context) && 
            PreferencesHelper.isInQuietHours(context) && 
            !PreferencesHelper.isPaused(context)) {
            // Auto-pause during quiet hours
            PreferencesHelper.setPausedRemainingTime(context, timeRemaining)
            val pauseUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            PreferencesHelper.setPauseUntil(context, pauseUntil)
        }
        
        // Track break completion when timer reaches 0
        if (timeRemaining <= 1000 && !PreferencesHelper.isPaused(context) && !PreferencesHelper.isTimerCompleted(context)) {
            // Check if we haven't already recorded this break
            val lastBreakTime = context.getSharedPreferences("eye_care_prefs", Context.MODE_PRIVATE)
                .getLong("last_break_recorded_time", 0L)
            val currentTime = System.currentTimeMillis()
            
            // Only record if it's been more than 30 seconds since last break
            // This prevents duplicate recordings
            if (currentTime - lastBreakTime > 30000) {
                PreferencesHelper.recordBreakCompleted(context)
                
                // Sync statistics to Firebase
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    SyncManager.getInstance(context).uploadStatistics()
                }
                
                // Check for newly unlocked achievements
                checkAndNotifyAchievements(context)
                
                // Sync achievements to Firebase
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    SyncManager.getInstance(context).uploadAchievements()
                }
                
                context.getSharedPreferences("eye_care_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_break_recorded_time", currentTime)
                    .apply()
                
                // Mark timer as completed and stop countdown
                PreferencesHelper.setTimerCompleted(context, true)
                serviceJob?.cancel() // Stop the auto-update loop
            }
        }
        
        val notification = createTimerNotification(context)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Update all widgets with current timer state
        EyeCareWidgetProvider.updateAllWidgets(context)
    }

    private fun createTimerNotification(context: Context): Notification {
        val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
        val isPaused = PreferencesHelper.isPaused(context)
        val isCompleted = PreferencesHelper.isTimerCompleted(context)
        
        val minutes = (timeRemaining / 1000 / 60).toInt().coerceAtLeast(0)
        val seconds = ((timeRemaining / 1000) % 60).toInt().coerceAtLeast(0)
        
        val timerText = if (isCompleted) {
            "🎉 Break Complete!"
        } else if (isPaused) {
            "⏸️ Paused"
        } else {
            String.format("⏰ %02d:%02d", minutes, seconds)
        }
        
        val contentText = if (isCompleted) {
            "Great job! Tap 'Start Next Break' to begin another timer"
        } else if (isPaused) {
            "Reminders are paused • Tap Resume to continue"
        } else {
            "Next break in ${String.format("%02d:%02d", minutes, seconds)} • Rest your eyes!"
        }

        // Main activity intent
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume action
        val pauseResumeIntent = Intent(context, TimerNotificationService::class.java).apply {
            action = ACTION_PAUSE_RESUME
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            context,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reset action
        val resetIntent = Intent(context, TimerNotificationService::class.java).apply {
            action = ACTION_RESET
        }
        val resetPendingIntent = PendingIntent.getService(
            context,
            2,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Start Next Break action
        val startNextIntent = Intent(context, TimerNotificationService::class.java).apply {
            action = ACTION_START_NEXT
        }
        val startNextPendingIntent = PendingIntent.getService(
            context,
            5,
            startNextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Close action
        val closeIntent = Intent(context, TimerNotificationService::class.java).apply {
            action = ACTION_STOP
        }
        val closePendingIntent = PendingIntent.getService(
            context,
            3,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Delete intent (when user swipes notification away)
        val deleteIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            action = NotificationDismissReceiver.ACTION_DISMISS
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeText = if (isPaused) "▶️ Resume" else "⏸️ Pause"
        val isNonDismissible = PreferencesHelper.isNonDismissible(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(timerText)
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(if (isCompleted) false else isNonDismissible)
            .setOnlyAlertOnce(true)
            .setPriority(if (isCompleted) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(if (isNonDismissible && !isCompleted) null else deletePendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        
        if (isCompleted) {
            // Show different actions when timer is completed
            builder.addAction(
                android.R.drawable.ic_media_play,
                "▶️ Start Next Break",
                startNextPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_delete,
                "✕ Close",
                closePendingIntent
            )
        } else {
            // Show normal actions during countdown
            builder.addAction(
                android.R.drawable.ic_media_pause,
                pauseResumeText,
                pauseResumePendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_revert,
                "🔄 Reset",
                resetPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_delete,
                "✕ Close",
                closePendingIntent
            )
        }
        
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows eye care countdown timer"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        serviceScope?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
