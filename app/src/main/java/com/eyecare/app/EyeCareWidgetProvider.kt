package com.eyecare.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.concurrent.TimeUnit

/**
 * Base widget provider for Eye Care widgets
 */
abstract class EyeCareWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_TOGGLE_PAUSE = "com.eyecare.app.ACTION_TOGGLE_PAUSE"
        const val ACTION_REFRESH = "com.eyecare.app.ACTION_REFRESH"
        
        /**
         * Update all widget instances
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, SmallWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
            
            val intent2 = Intent(context, MediumWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent2)
            
            val intent3 = Intent(context, LargeWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent3)
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_TOGGLE_PAUSE -> {
                handleTogglePause(context)
                updateAllWidgetsForProvider(context)
            }
            ACTION_REFRESH -> {
                updateAllWidgetsForProvider(context)
            }
        }
    }
    
    private fun handleTogglePause(context: Context) {
        if (PreferencesHelper.isPaused(context)) {
            // Resume timer
            val pausedTime = PreferencesHelper.getPausedRemainingTime(context)
            if (pausedTime > 0) {
                val newEndTime = System.currentTimeMillis() + pausedTime
                PreferencesHelper.setEndTimeMillis(context, newEndTime)
            }
            PreferencesHelper.setPauseUntil(context, 0L)
            PreferencesHelper.setPausedRemainingTime(context, 0L)
            
            // Start service
            val serviceIntent = Intent(context, TimerNotificationService::class.java)
            context.startForegroundService(serviceIntent)
        } else {
            // Pause timer
            val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
            PreferencesHelper.setPausedRemainingTime(context, timeRemaining)
            val pauseUntil = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
            PreferencesHelper.setPauseUntil(context, pauseUntil)
            
            // Stop service
            val serviceIntent = Intent(context, TimerNotificationService::class.java)
            context.stopService(serviceIntent)
        }
    }
    
    private fun updateAllWidgetsForProvider(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, this::class.java)
        )
        widgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }
    
    abstract fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    )
    
    protected fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    protected fun getOpenAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    protected fun getTogglePauseIntent(context: Context): PendingIntent {
        val intent = Intent(context, this::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Small widget (2x2) - Timer only
 */
class SmallWidgetProvider : EyeCareWidgetProvider() {
    override fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_small)
        
        // Update timer
        val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
        views.setTextViewText(R.id.widget_timer, formatTime(timeRemaining))
        
        // Update pause/resume button
        val isPaused = PreferencesHelper.isPaused(context)
        views.setImageViewResource(
            R.id.widget_toggle_button,
            if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause
        )
        
        // Set click listeners
        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_toggle_button, getTogglePauseIntent(context))
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

/**
 * Medium widget (4x2) - Timer + next break time
 */
class MediumWidgetProvider : EyeCareWidgetProvider() {
    override fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_medium)
        
        // Update timer
        val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
        views.setTextViewText(R.id.widget_timer, formatTime(timeRemaining))
        
        // Calculate next break time
        val nextBreakTime = System.currentTimeMillis() + timeRemaining
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = nextBreakTime
        val hours = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(java.util.Calendar.MINUTE)
        val period = if (hours < 12) "AM" else "PM"
        val displayHours = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
        val nextBreakText = String.format("Next break: %d:%02d %s", displayHours, minutes, period)
        views.setTextViewText(R.id.widget_next_break, nextBreakText)
        
        // Update pause/resume button
        val isPaused = PreferencesHelper.isPaused(context)
        views.setImageViewResource(
            R.id.widget_toggle_button,
            if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause
        )
        
        // Set click listeners
        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_toggle_button, getTogglePauseIntent(context))
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
}

/**
 * Large widget (4x4) - Timer + stats + quick controls
 */
class LargeWidgetProvider : EyeCareWidgetProvider() {
    override fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_large)
        
        // Update timer
        val timeRemaining = PreferencesHelper.getTimeRemainingMillis(context)
        views.setTextViewText(R.id.widget_timer, formatTime(timeRemaining))
        
        // Calculate next break time
        val nextBreakTime = System.currentTimeMillis() + timeRemaining
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = nextBreakTime
        val hours = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(java.util.Calendar.MINUTE)
        val period = if (hours < 12) "AM" else "PM"
        val displayHours = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
        val nextBreakText = String.format("Next at %d:%02d %s", displayHours, minutes, period)
        views.setTextViewText(R.id.widget_next_break, nextBreakText)
        
        // Update stats
        val breaksToday = PreferencesHelper.getBreaksToday(context)
        val currentStreak = PreferencesHelper.getCurrentStreak(context)
        views.setTextViewText(R.id.widget_breaks_today, "Today: $breaksToday breaks")
        views.setTextViewText(R.id.widget_streak, "Streak: $currentStreak days")
        
        // Update pause/resume button
        val isPaused = PreferencesHelper.isPaused(context)
        views.setImageViewResource(
            R.id.widget_toggle_button,
            if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause
        )
        views.setTextViewText(
            R.id.widget_toggle_text,
            if (isPaused) "Resume" else "Pause"
        )
        
        // Set click listeners
        views.setOnClickPendingIntent(R.id.widget_container, getOpenAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_toggle_button, getTogglePauseIntent(context))
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
