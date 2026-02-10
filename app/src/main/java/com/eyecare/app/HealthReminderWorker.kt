package com.eyecare.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * HealthReminderWorker - Handles water, posture, stretch, and brightness reminders
 */
class HealthReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val CHANNEL_ID_WATER = "health_water_reminder"
        private const val CHANNEL_ID_POSTURE = "health_posture_reminder"
        private const val CHANNEL_ID_STRETCH = "health_stretch_reminder"
        private const val CHANNEL_ID_BRIGHTNESS = "health_brightness_reminder"
        private const val CHANNEL_ID_COMBINED = "health_combined_reminder"
        
        private const val NOTIFICATION_ID_WATER = 2001
        private const val NOTIFICATION_ID_POSTURE = 2002
        private const val NOTIFICATION_ID_STRETCH = 2003
        private const val NOTIFICATION_ID_BRIGHTNESS = 2004
        private const val NOTIFICATION_ID_COMBINED = 2000
        
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val TYPE_WATER = "water"
        const val TYPE_POSTURE = "posture"
        const val TYPE_STRETCH = "stretch"
        const val TYPE_BRIGHTNESS = "brightness"
        
        /**
         * Schedule all enabled health reminders
         */
        fun scheduleHealthReminders(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // Cancel all existing health reminders first
            cancelHealthReminders(context)
            
            // Schedule water reminder (every 2 hours)
            if (PreferencesHelper.isWaterReminderEnabled(context)) {
                val waterRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
                    2, TimeUnit.HOURS
                ).setInputData(
                    workDataOf(EXTRA_REMINDER_TYPE to TYPE_WATER)
                ).setInitialDelay(2, TimeUnit.HOURS)
                    .build()
                
                workManager.enqueue(waterRequest)
            }
            
            // Schedule posture reminder (every 1 hour)
            if (PreferencesHelper.isPostureReminderEnabled(context)) {
                val postureRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
                    1, TimeUnit.HOURS
                ).setInputData(
                    workDataOf(EXTRA_REMINDER_TYPE to TYPE_POSTURE)
                ).setInitialDelay(1, TimeUnit.HOURS)
                    .build()
                
                workManager.enqueue(postureRequest)
            }
            
            // Schedule stretch reminder (every 90 minutes)
            if (PreferencesHelper.isStretchReminderEnabled(context)) {
                val stretchRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
                    90, TimeUnit.MINUTES
                ).setInputData(
                    workDataOf(EXTRA_REMINDER_TYPE to TYPE_STRETCH)
                ).setInitialDelay(90, TimeUnit.MINUTES)
                    .build()
                
                workManager.enqueue(stretchRequest)
            }
            
            // Schedule brightness check (every 30 minutes during work hours)
            if (PreferencesHelper.isBrightnessCheckEnabled(context)) {
                val brightnessRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
                    30, TimeUnit.MINUTES
                ).setInputData(
                    workDataOf(EXTRA_REMINDER_TYPE to TYPE_BRIGHTNESS)
                ).setInitialDelay(30, TimeUnit.MINUTES)
                    .build()
                
                workManager.enqueue(brightnessRequest)
            }
        }
        
        /**
         * Cancel all health reminders
         */
        fun cancelHealthReminders(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("health_reminder")
        }
    }

    override fun doWork(): Result {
        createNotificationChannels()
        
        val reminderType = inputData.getString(EXTRA_REMINDER_TYPE) ?: return Result.failure()
        
        if (PreferencesHelper.isCombinedNotificationsEnabled(applicationContext)) {
            showCombinedNotification(reminderType)
        } else {
            when (reminderType) {
                TYPE_WATER -> showWaterReminder()
                TYPE_POSTURE -> showPostureReminder()
                TYPE_STRETCH -> showStretchReminder()
                TYPE_BRIGHTNESS -> showBrightnessReminder()
            }
        }
        
        return Result.success()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_WATER,
                    "Water Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds you to drink water"
                },
                NotificationChannel(
                    CHANNEL_ID_POSTURE,
                    "Posture Check",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds you to check your posture"
                },
                NotificationChannel(
                    CHANNEL_ID_STRETCH,
                    "Stretch Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds you to stand and stretch"
                },
                NotificationChannel(
                    CHANNEL_ID_BRIGHTNESS,
                    "Brightness Check",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Checks screen brightness"
                },
                NotificationChannel(
                    CHANNEL_ID_COMBINED,
                    "Health Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Combined health reminders"
                }
            )
            
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    private fun showWaterReminder() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_WATER)
            .setContentTitle("💧 Hydration Time!")
            .setContentText("Time to drink some water • Stay hydrated!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("💧 Drink 8 oz (250ml) of water\n\n" +
                            "Benefits:\n" +
                            "• Improves focus and concentration\n" +
                            "• Prevents headaches\n" +
                            "• Keeps your body hydrated")
            )
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_WATER, notification)
    }

    private fun showPostureReminder() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_POSTURE)
            .setContentTitle("🪑 Posture Check!")
            .setContentText("Check your sitting posture • Adjust if needed")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🪑 Good Posture Checklist:\n\n" +
                            "✓ Back straight against chair\n" +
                            "✓ Feet flat on floor\n" +
                            "✓ Screen at eye level\n" +
                            "✓ Shoulders relaxed\n" +
                            "✓ Arms at 90° angle")
            )
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_POSTURE, notification)
    }

    private fun showStretchReminder() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_STRETCH)
            .setContentTitle("🚶 Stand & Stretch!")
            .setContentText("Time to stand up and move • Get your blood flowing!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚶 Quick Stretch Routine:\n\n" +
                            "1. Stand up and walk around (30 sec)\n" +
                            "2. Neck rolls (both directions)\n" +
                            "3. Shoulder shrugs (10 times)\n" +
                            "4. Arm stretches (reach high)\n" +
                            "5. Twist your torso (left & right)")
            )
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_STRETCH, notification)
    }

    private fun showBrightnessReminder() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_BRIGHTNESS)
            .setContentTitle("☀️ Screen Brightness Check")
            .setContentText("Adjust screen brightness • Match your environment")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("☀️ Brightness Tips:\n\n" +
                            "• Match brightness to room lighting\n" +
                            "• Not too bright (causes strain)\n" +
                            "• Not too dim (hard to see)\n" +
                            "• Enable auto-brightness if available\n" +
                            "• Consider blue light filter at night")
            )
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BRIGHTNESS, notification)
    }

    private fun showCombinedNotification(reminderType: String) {
        val (title, message, icon) = when (reminderType) {
            TYPE_WATER -> Triple("💧 Hydration", "Time to drink water", "💧")
            TYPE_POSTURE -> Triple("🪑 Posture", "Check your posture", "🪑")
            TYPE_STRETCH -> Triple("🚶 Stretch", "Stand up and move", "🚶")
            TYPE_BRIGHTNESS -> Triple("☀️ Brightness", "Check screen brightness", "☀️")
            else -> return
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_COMBINED)
            .setContentTitle("$icon Health Reminder")
            .setContentText("$title: $message")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup("health_reminders")
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_COMBINED, notification)
    }
}
