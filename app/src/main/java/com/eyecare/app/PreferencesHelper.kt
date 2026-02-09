package com.eyecare.app

import android.content.Context
import android.content.SharedPreferences

/**
 * PreferencesHelper - Manages app settings and preferences
 * Stores user preferences like reminder interval, last notification time, etc.
 */
object PreferencesHelper {
    
    private const val PREFS_NAME = "eye_care_prefs"
    
    // Keys
    private const val KEY_REMINDER_INTERVAL = "reminder_interval"
    private const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time"
    private const val KEY_PAUSE_UNTIL = "pause_until"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_PAUSED_REMAINING_TIME = "paused_remaining_time"
    
    // Default values
    const val DEFAULT_REMINDER_INTERVAL = 20 // minutes
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Reminder Interval (in minutes)
    fun getReminderInterval(context: Context): Int {
        return getPrefs(context).getInt(KEY_REMINDER_INTERVAL, DEFAULT_REMINDER_INTERVAL)
    }
    
    fun setReminderInterval(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_REMINDER_INTERVAL, minutes).apply()
    }
    
    // Last Notification Time
    fun getLastNotificationTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_NOTIFICATION_TIME, 0L)
    }
    
    fun setLastNotificationTime(context: Context, timeMillis: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_NOTIFICATION_TIME, timeMillis).apply()
    }
    
    // Pause Until (timestamp)
    fun getPauseUntil(context: Context): Long {
        return getPrefs(context).getLong(KEY_PAUSE_UNTIL, 0L)
    }
    
    fun setPauseUntil(context: Context, timeMillis: Long) {
        getPrefs(context).edit().putLong(KEY_PAUSE_UNTIL, timeMillis).apply()
    }
    
    fun isPaused(context: Context): Boolean {
        val pauseUntil = getPauseUntil(context)
        return pauseUntil > System.currentTimeMillis()
    }
    
    // Paused Remaining Time
    fun getPausedRemainingTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_PAUSED_REMAINING_TIME, 0L)
    }
    
    fun setPausedRemainingTime(context: Context, timeMillis: Long) {
        getPrefs(context).edit().putLong(KEY_PAUSED_REMAINING_TIME, timeMillis).apply()
    }
    
    // Sound Enabled
    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    // Reminders Enabled
    fun areRemindersEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMINDERS_ENABLED, true)
    }
    
    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }
    
    // Calculate time remaining until next break
    fun getTimeRemainingMillis(context: Context): Long {
        val lastNotificationTime = getLastNotificationTime(context)
        if (lastNotificationTime == 0L) {
            // First time - return full interval
            return getReminderInterval(context) * 60 * 1000L
        }
        
        val intervalMillis = getReminderInterval(context) * 60 * 1000L
        val nextNotificationTime = lastNotificationTime + intervalMillis
        val remaining = nextNotificationTime - System.currentTimeMillis()
        
        return if (remaining > 0) remaining else 0L
    }
}
