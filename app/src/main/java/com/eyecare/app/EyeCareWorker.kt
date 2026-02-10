package com.eyecare.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * EyeCareWorker - WorkManager Worker
 * This worker triggers periodic notifications reminding users
 * to follow the 20-20-20 rule (take a 20-second break every 20 minutes)
 */
class EyeCareWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "eye_care_reminder_channel"
        const val CHANNEL_NAME = "Eye Care Reminders"
        const val WORK_TAG = "eye_care_reminder_work"
    }

    override fun doWork(): Result {
        // Check if reminders are paused
        if (PreferencesHelper.isPaused(context)) {
            return Result.success() // Skip notification if paused
        }
        
        // Send the reminder notification
        sendNotification()
        
        // Update last notification time
        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
        
        // Return success
        return Result.success()
    }

    /**
     * Sends a notification reminding the user to take a break
     */
    private fun sendNotification() {
        // Create notification channel if needed
        createNotificationChannel()

        // Create an intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get break duration from preferences
        val breakDuration = PreferencesHelper.getBreakDuration(context)
        val durationText = if (breakDuration >= 60) {
            "${breakDuration / 60} minute${if (breakDuration / 60 > 1) "s" else ""}"
        } else {
            "$breakDuration seconds"
        }

        // Build the notification with sound if enabled
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Eye Care Reminder 👁️")
            .setContentText("Take a $durationText break to look away from the screen!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🌿 Break Instructions:\n" +
                            "1. Look at something 20 feet (6 meters) away\n" +
                            "2. Keep looking for $durationText\n" +
                            "3. Blink frequently to refresh your eyes\n" +
                            "4. Gently stretch your neck and shoulders")
            )
        
        // Add sound if enabled
        if (PreferencesHelper.isSoundEnabled(context)) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            notificationBuilder.setSound(soundUri)
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
        }

        // Show the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Creates the notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic reminders to take eye care breaks"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
