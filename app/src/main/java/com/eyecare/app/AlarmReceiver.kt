package com.eyecare.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * AlarmReceiver - Handles sleep cycle alarm notifications with morning prompts
 */
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "sleep_alarm_channel"
        private const val NOTIFICATION_ID = 3001
        const val EXTRA_ALARM_MESSAGE = "alarm_message"
        const val EXTRA_CYCLES = "cycles"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(EXTRA_ALARM_MESSAGE) ?: "Time to wake up!"
        val cycles = intent.getIntExtra(EXTRA_CYCLES, 0)
        
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel(context)
        
        // Create intent to open app with morning prompt
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_morning_prompt", true)
            putExtra("cycles", cycles)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Show alarm notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Wake Up Time!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\\n\\nTap to rate your sleep quality 👁️"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Mark that morning prompt should be shown
        PreferencesHelper.setMorningPromptShown(context)
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleep Cycle Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wake-up notifications for sleep cycle alarms"
                enableVibration(true)
                enableLights(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    null
                )
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
