package com.eyecare.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    
    // Statistics Keys
    private const val KEY_TOTAL_BREAKS = "total_breaks"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_BEST_STREAK = "best_streak"
    private const val KEY_LAST_BREAK_DATE = "last_break_date"
    private const val KEY_BREAKS_TODAY = "breaks_today"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_DAILY_HISTORY = "daily_history"
    
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
    
    // ============ Statistics Methods ============
    
    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    // Check if we need to reset daily counter
    private fun checkAndResetDaily(context: Context) {
        val today = getTodayString()
        val lastReset = getPrefs(context).getString(KEY_LAST_RESET_DATE, "")
        
        if (today != lastReset) {
            // New day - reset daily counter
            getPrefs(context).edit()
                .putInt(KEY_BREAKS_TODAY, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
        }
    }
    
    // Total Breaks
    fun getTotalBreaks(context: Context): Int {
        return getPrefs(context).getInt(KEY_TOTAL_BREAKS, 0)
    }
    
    // Current Streak
    fun getCurrentStreak(context: Context): Int {
        return getPrefs(context).getInt(KEY_CURRENT_STREAK, 0)
    }
    
    // Best Streak
    fun getBestStreak(context: Context): Int {
        return getPrefs(context).getInt(KEY_BEST_STREAK, 0)
    }
    
    // Breaks Today
    fun getBreaksToday(context: Context): Int {
        checkAndResetDaily(context)
        return getPrefs(context).getInt(KEY_BREAKS_TODAY, 0)
    }
    
    // Record a completed break
    fun recordBreakCompleted(context: Context) {
        checkAndResetDaily(context)
        
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        
        // Increment counters
        val totalBreaks = prefs.getInt(KEY_TOTAL_BREAKS, 0) + 1
        val breaksToday = prefs.getInt(KEY_BREAKS_TODAY, 0) + 1
        
        editor.putInt(KEY_TOTAL_BREAKS, totalBreaks)
        editor.putInt(KEY_BREAKS_TODAY, breaksToday)
        
        // Update streak
        val today = getTodayString()
        val lastBreakDate = prefs.getString(KEY_LAST_BREAK_DATE, "")
        
        if (lastBreakDate != today) {
            // First break of the day
            val yesterday = getYesterday()
            val currentStreak = if (lastBreakDate == yesterday) {
                // Continuing streak
                prefs.getInt(KEY_CURRENT_STREAK, 0) + 1
            } else {
                // Streak broken, start new
                1
            }
            
            editor.putInt(KEY_CURRENT_STREAK, currentStreak)
            editor.putString(KEY_LAST_BREAK_DATE, today)
            
            // Update best streak
            val bestStreak = prefs.getInt(KEY_BEST_STREAK, 0)
            if (currentStreak > bestStreak) {
                editor.putInt(KEY_BEST_STREAK, currentStreak)
            }
            
            // Save daily history
            saveDailyHistory(context, today, breaksToday)
        }
        
        editor.apply()
    }
    
    private fun getYesterday(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
    
    // Daily History (JSON format: {"2026-02-09": 8, "2026-02-08": 12})
    fun getDailyHistory(context: Context): Map<String, Int> {
        val json = getPrefs(context).getString(KEY_DAILY_HISTORY, "{}")
        val result = mutableMapOf<String, Int>()
        
        try {
            val jsonObject = JSONObject(json ?: "{}")
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = jsonObject.getInt(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return result
    }
    
    private fun saveDailyHistory(context: Context, date: String, breaks: Int) {
        val history = getDailyHistory(context).toMutableMap()
        history[date] = breaks
        
        // Keep only last 90 days
        if (history.size > 90) {
            val sortedDates = history.keys.sorted()
            val toRemove = sortedDates.take(history.size - 90)
            toRemove.forEach { history.remove(it) }
        }
        
        val jsonObject = JSONObject(history as Map<*, *>)
        getPrefs(context).edit()
            .putString(KEY_DAILY_HISTORY, jsonObject.toString())
            .apply()
    }
    
    // Get average breaks per day (last 7 days)
    fun getAverageBreaksPerDay(context: Context): Float {
        val history = getDailyHistory(context)
        if (history.isEmpty()) return 0f
        
        val last7Days = history.entries
            .sortedByDescending { it.key }
            .take(7)
        
        if (last7Days.isEmpty()) return 0f
        
        val total = last7Days.sumOf { it.value }
        return total.toFloat() / last7Days.size
    }
}
