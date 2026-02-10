package com.eyecare.app

import com.google.firebase.firestore.PropertyName

/**
 * Data models for Firebase synchronization
 */

// User Settings Sync Model
data class SyncSettings(
    @PropertyName("interval_minutes") val intervalMinutes: Int = 20,
    @PropertyName("break_duration_seconds") val breakDurationSeconds: Int = 20,
    @PropertyName("notification_sound_enabled") val notificationSoundEnabled: Boolean = true,
    @PropertyName("vibration_enabled") val vibrationEnabled: Boolean = true,
    @PropertyName("non_dismissible_notifications") val nonDismissibleNotifications: Boolean = false,
    
    // Smart Breaks
    @PropertyName("smart_breaks_enabled") val smartBreaksEnabled: Boolean = false,
    @PropertyName("work_hours_start") val workHoursStart: Int = 8,
    @PropertyName("work_hours_end") val workHoursEnd: Int = 18,
    @PropertyName("quiet_hours_start") val quietHoursStart: Int = 22,
    @PropertyName("quiet_hours_end") val quietHoursEnd: Int = 7,
    
    // Break Rules
    @PropertyName("preset_name") val presetName: String = "Classic 20-20-20",
    
    // Health Reminders
    @PropertyName("water_reminder_enabled") val waterReminderEnabled: Boolean = false,
    @PropertyName("posture_reminder_enabled") val postureReminderEnabled: Boolean = false,
    @PropertyName("stretch_reminder_enabled") val stretchReminderEnabled: Boolean = false,
    @PropertyName("brightness_check_enabled") val brightnessCheckEnabled: Boolean = false,
    @PropertyName("combined_notifications") val combinedNotifications: Boolean = false,
    
    @PropertyName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firestore
    constructor() : this(
        intervalMinutes = 20,
        breakDurationSeconds = 20,
        notificationSoundEnabled = true,
        vibrationEnabled = true,
        nonDismissibleNotifications = false,
        smartBreaksEnabled = false,
        workHoursStart = 8,
        workHoursEnd = 18,
        quietHoursStart = 22,
        quietHoursEnd = 7,
        presetName = "Classic 20-20-20",
        waterReminderEnabled = false,
        postureReminderEnabled = false,
        stretchReminderEnabled = false,
        brightnessCheckEnabled = false,
        combinedNotifications = false,
        lastUpdated = System.currentTimeMillis()
    )
}

// Statistics Sync Model
data class SyncStatistics(
    @PropertyName("total_breaks") val totalBreaks: Int = 0,
    @PropertyName("current_streak") val currentStreak: Int = 0,
    @PropertyName("longest_streak") val longestStreak: Int = 0,
    @PropertyName("last_break_date") val lastBreakDate: String = "",
    @PropertyName("daily_history") val dailyHistory: Map<String, Int> = emptyMap(),
    @PropertyName("total_blinks") val totalBlinks: Int = 0,
    @PropertyName("total_focus_shifts") val totalFocusShifts: Int = 0,
    @PropertyName("total_rotations") val totalRotations: Int = 0,
    @PropertyName("total_palming") val totalPalming: Int = 0,
    @PropertyName("total_follow_dot") val totalFollowDot: Int = 0,
    @PropertyName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firestore
    constructor() : this(
        totalBreaks = 0,
        currentStreak = 0,
        longestStreak = 0,
        lastBreakDate = "",
        dailyHistory = emptyMap(),
        totalBlinks = 0,
        totalFocusShifts = 0,
        totalRotations = 0,
        totalPalming = 0,
        totalFollowDot = 0,
        lastUpdated = System.currentTimeMillis()
    )
}

// Achievements Sync Model
data class SyncAchievements(
    @PropertyName("first_break_unlocked") val firstBreakUnlocked: Boolean = false,
    @PropertyName("week_warrior_unlocked") val weekWarriorUnlocked: Boolean = false,
    @PropertyName("century_club_unlocked") val centuryClubUnlocked: Boolean = false,
    @PropertyName("dedication_unlocked") val dedicationUnlocked: Boolean = false,
    @PropertyName("night_owl_unlocked") val nightOwlUnlocked: Boolean = false,
    @PropertyName("exercise_master_unlocked") val exerciseMasterUnlocked: Boolean = false,
    @PropertyName("blink_champion_unlocked") val blinkChampionUnlocked: Boolean = false,
    @PropertyName("focus_guru_unlocked") val focusGuruUnlocked: Boolean = false,
    @PropertyName("rotation_expert_unlocked") val rotationExpertUnlocked: Boolean = false,
    @PropertyName("zen_master_unlocked") val zenMasterUnlocked: Boolean = false,
    @PropertyName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firestore
    constructor() : this(
        firstBreakUnlocked = false,
        weekWarriorUnlocked = false,
        centuryClubUnlocked = false,
        dedicationUnlocked = false,
        nightOwlUnlocked = false,
        exerciseMasterUnlocked = false,
        blinkChampionUnlocked = false,
        focusGuruUnlocked = false,
        rotationExpertUnlocked = false,
        zenMasterUnlocked = false,
        lastUpdated = System.currentTimeMillis()
    )
}

// Timer State Sync Model (for cross-device pause/resume)
data class SyncTimerState(
    @PropertyName("is_paused") val isPaused: Boolean = false,
    @PropertyName("remaining_time_millis") val remainingTimeMillis: Long = 0,
    @PropertyName("last_notification_time") val lastNotificationTime: Long = 0,
    @PropertyName("device_id") val deviceId: String = "",
    @PropertyName("last_updated") val lastUpdated: Long = System.currentTimeMillis()
) {
    // No-arg constructor for Firestore
    constructor() : this(
        isPaused = false,
        remainingTimeMillis = 0,
        lastNotificationTime = 0,
        deviceId = "",
        lastUpdated = System.currentTimeMillis()
    )
}
