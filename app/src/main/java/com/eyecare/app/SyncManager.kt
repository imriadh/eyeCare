package com.eyecare.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages Firebase synchronization for settings, statistics, achievements, and timer state
 * across multiple devices.
 */
class SyncManager private constructor(private val context: Context) {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val deviceId = getOrCreateDeviceId()
    
    private var settingsListener: ListenerRegistration? = null
    private var statsListener: ListenerRegistration? = null
    private var achievementsListener: ListenerRegistration? = null
    private var timerStateListener: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "SyncManager"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SETTINGS = "settings"
        private const val COLLECTION_STATISTICS = "statistics"
        private const val COLLECTION_ACHIEVEMENTS = "achievements"
        private const val COLLECTION_TIMER_STATE = "timer_state"
        private const val PREF_DEVICE_ID = "device_id"
        private const val PREF_SYNC_ENABLED = "sync_enabled"
        
        @Volatile
        private var INSTANCE: SyncManager? = null
        
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Authentication
    
    fun isSignedIn(): Boolean = auth.currentUser != null
    
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    
    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            
            // Start sync after successful sign-in
            startRealtimeSync()
            uploadAllData()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun signOut() {
        stopRealtimeSync()
        auth.signOut()
    }
    
    // Sync Control
    
    fun isSyncEnabled(): Boolean {
        val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SYNC_ENABLED, false) && isSignedIn()
    }
    
    fun setSyncEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_SYNC_ENABLED, enabled).apply()
        
        if (enabled && isSignedIn()) {
            startRealtimeSync()
        } else {
            stopRealtimeSync()
        }
    }
    
    // Upload Data to Firebase
    
    suspend fun uploadSettings(): Result<Unit> {
        if (!isSyncEnabled()) return Result.success(Unit)
        
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val settings = SyncSettings(
                intervalMinutes = PreferencesHelper.getReminderInterval(context),
                breakDurationSeconds = PreferencesHelper.getBreakDuration(context),
                notificationSoundEnabled = PreferencesHelper.isSoundEnabled(context),
                vibrationEnabled = PreferencesHelper.isVibrationEnabled(context),
                nonDismissibleNotifications = PreferencesHelper.isNonDismissible(context),
                smartBreaksEnabled = PreferencesHelper.isSmartBreaksEnabled(context),
                workHoursStart = PreferencesHelper.getWorkHoursStart(context),
                workHoursEnd = PreferencesHelper.getWorkHoursEnd(context),
                quietHoursStart = PreferencesHelper.getQuietHoursStart(context),
                quietHoursEnd = PreferencesHelper.getQuietHoursEnd(context),
                presetName = PreferencesHelper.getSelectedPreset(context),
                waterReminderEnabled = PreferencesHelper.isWaterReminderEnabled(context),
                postureReminderEnabled = PreferencesHelper.isPostureReminderEnabled(context),
                stretchReminderEnabled = PreferencesHelper.isStretchReminderEnabled(context),
                brightnessCheckEnabled = PreferencesHelper.isBrightnessCheckEnabled(context),
                combinedNotifications = PreferencesHelper.isCombinedNotificationsEnabled(context),
                lastUpdated = System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("current")
                .set(settings)
                .await()
            
            Log.d(TAG, "Settings uploaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload settings", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadStatistics(): Result<Unit> {
        if (!isSyncEnabled()) return Result.success(Unit)
        
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val stats = SyncStatistics(
                totalBreaks = PreferencesHelper.getTotalBreaks(context),
                currentStreak = PreferencesHelper.getCurrentStreak(context),
                longestStreak = PreferencesHelper.getLongestStreak(context),
                lastBreakDate = PreferencesHelper.getLastBreakDate(context),
                dailyHistory = PreferencesHelper.getDailyHistory(context),
                totalBlinks = PreferencesHelper.getTotalBlinks(context),
                totalFocusShifts = PreferencesHelper.getTotalFocusShifts(context),
                totalRotations = PreferencesHelper.getTotalRotations(context),
                totalPalming = PreferencesHelper.getTotalPalming(context),
                totalFollowDot = PreferencesHelper.getTotalFollowDot(context),
                lastUpdated = System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_STATISTICS)
                .document("current")
                .set(stats)
                .await()
            
            Log.d(TAG, "Statistics uploaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload statistics", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadAchievements(): Result<Unit> {
        if (!isSyncEnabled()) return Result.success(Unit)
        
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val achievements = SyncAchievements(
                firstBreakUnlocked = PreferencesHelper.isAchievementUnlocked(context, "first_break"),
                weekWarriorUnlocked = PreferencesHelper.isAchievementUnlocked(context, "week_warrior"),
                centuryClubUnlocked = PreferencesHelper.isAchievementUnlocked(context, "century_club"),
                dedicationUnlocked = PreferencesHelper.isAchievementUnlocked(context, "dedication"),
                nightOwlUnlocked = PreferencesHelper.isAchievementUnlocked(context, "night_owl"),
                exerciseMasterUnlocked = PreferencesHelper.isAchievementUnlocked(context, "exercise_master"),
                blinkChampionUnlocked = PreferencesHelper.isAchievementUnlocked(context, "blink_champion"),
                focusGuruUnlocked = PreferencesHelper.isAchievementUnlocked(context, "focus_guru"),
                rotationExpertUnlocked = PreferencesHelper.isAchievementUnlocked(context, "rotation_expert"),
                zenMasterUnlocked = PreferencesHelper.isAchievementUnlocked(context, "zen_master"),
                lastUpdated = System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_ACHIEVEMENTS)
                .document("current")
                .set(achievements)
                .await()
            
            Log.d(TAG, "Achievements uploaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload achievements", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadTimerState(isPaused: Boolean, remainingTimeMillis: Long, lastNotificationTime: Long): Result<Unit> {
        if (!isSyncEnabled()) return Result.success(Unit)
        
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val timerState = SyncTimerState(
                isPaused = isPaused,
                remainingTimeMillis = remainingTimeMillis,
                lastNotificationTime = lastNotificationTime,
                deviceId = deviceId,
                lastUpdated = System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TIMER_STATE)
                .document("current")
                .set(timerState)
                .await()
            
            Log.d(TAG, "Timer state uploaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload timer state", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadAllData(): Result<Unit> {
        return try {
            uploadSettings()
            uploadStatistics()
            uploadAchievements()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Download Data from Firebase
    
    suspend fun downloadAndApplySettings(): Result<Unit> {
        if (!isSyncEnabled()) return Result.success(Unit)
        
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        return try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_SETTINGS)
                .document("current")
                .get()
                .await()
            
            val settings = doc.toObject(SyncSettings::class.java) ?: return Result.success(Unit)
            
            // Apply settings to local preferences
            PreferencesHelper.setReminderInterval(context, settings.intervalMinutes)
            PreferencesHelper.setBreakDuration(context, settings.breakDurationSeconds)
            PreferencesHelper.setSoundEnabled(context, settings.notificationSoundEnabled)
            PreferencesHelper.setVibrationEnabled(context, settings.vibrationEnabled)
            PreferencesHelper.setNonDismissible(context, settings.nonDismissibleNotifications)
            PreferencesHelper.setSmartBreaksEnabled(context, settings.smartBreaksEnabled)
            PreferencesHelper.setWorkHoursStart(context, settings.workHoursStart)
            PreferencesHelper.setWorkHoursEnd(context, settings.workHoursEnd)
            PreferencesHelper.setQuietHoursStart(context, settings.quietHoursStart)
            PreferencesHelper.setQuietHoursEnd(context, settings.quietHoursEnd)
            PreferencesHelper.setSelectedPreset(context, settings.presetName)
            PreferencesHelper.setWaterReminderEnabled(context, settings.waterReminderEnabled)
            PreferencesHelper.setPostureReminderEnabled(context, settings.postureReminderEnabled)
            PreferencesHelper.setStretchReminderEnabled(context, settings.stretchReminderEnabled)
            PreferencesHelper.setBrightnessCheckEnabled(context, settings.brightnessCheckEnabled)
            PreferencesHelper.setCombinedNotificationsEnabled(context, settings.combinedNotifications)
            
            Log.d(TAG, "Settings downloaded and applied successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download settings", e)
            Result.failure(e)
        }
    }
    
    // Real-time Sync Listeners
    
    fun startRealtimeSync() {
        if (!isSyncEnabled()) return
        
        val userId = auth.currentUser?.uid ?: return
        
        // Listen to settings changes
        settingsListener = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_SETTINGS)
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Settings listener error", error)
                    return@addSnapshotListener
                }
                
                snapshot?.toObject(SyncSettings::class.java)?.let { settings ->
                    applySettingsLocally(settings)
                }
            }
        
        // Listen to timer state changes from other devices
        timerStateListener = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_TIMER_STATE)
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Timer state listener error", error)
                    return@addSnapshotListener
                }
                
                snapshot?.toObject(SyncTimerState::class.java)?.let { state ->
                    // Only apply if this update came from a different device
                    if (state.deviceId != deviceId) {
                        applyTimerStateLocally(state)
                    }
                }
            }
        
        Log.d(TAG, "Real-time sync started")
    }
    
    fun stopRealtimeSync() {
        settingsListener?.remove()
        statsListener?.remove()
        achievementsListener?.remove()
        timerStateListener?.remove()
        
        settingsListener = null
        statsListener = null
        achievementsListener = null
        timerStateListener = null
        
        Log.d(TAG, "Real-time sync stopped")
    }
    
    // Helper Methods
    
    private fun applySettingsLocally(settings: SyncSettings) {
        PreferencesHelper.setReminderInterval(context, settings.intervalMinutes)
        PreferencesHelper.setBreakDuration(context, settings.breakDurationSeconds)
        PreferencesHelper.setSoundEnabled(context, settings.notificationSoundEnabled)
        PreferencesHelper.setVibrationEnabled(context, settings.vibrationEnabled)
        PreferencesHelper.setNonDismissible(context, settings.nonDismissibleNotifications)
        PreferencesHelper.setSmartBreaksEnabled(context, settings.smartBreaksEnabled)
        PreferencesHelper.setWorkHoursStart(context, settings.workHoursStart)
        PreferencesHelper.setWorkHoursEnd(context, settings.workHoursEnd)
        PreferencesHelper.setQuietHoursStart(context, settings.quietHoursStart)
        PreferencesHelper.setQuietHoursEnd(context, settings.quietHoursEnd)
        PreferencesHelper.setSelectedPreset(context, settings.presetName)
        PreferencesHelper.setWaterReminderEnabled(context, settings.waterReminderEnabled)
        PreferencesHelper.setPostureReminderEnabled(context, settings.postureReminderEnabled)
        PreferencesHelper.setStretchReminderEnabled(context, settings.stretchReminderEnabled)
        PreferencesHelper.setBrightnessCheckEnabled(context, settings.brightnessCheckEnabled)
        PreferencesHelper.setCombinedNotificationsEnabled(context, settings.combinedNotifications)
        
        // Restart timers with new settings
        if (TimerNotificationService.isServiceRunning) {
            TimerNotificationService.startService(context)
        }
        
        Log.d(TAG, "Settings applied locally from sync")
    }
    
    private fun applyTimerStateLocally(state: SyncTimerState) {
        if (state.isPaused) {
            PreferencesHelper.setPausedRemainingTime(context, state.remainingTimeMillis)
            PreferencesHelper.setLastNotificationTime(context, state.lastNotificationTime)
        } else {
            PreferencesHelper.setPausedRemainingTime(context, 0)
            PreferencesHelper.setLastNotificationTime(context, state.lastNotificationTime)
        }
        
        // Update timer service
        if (TimerNotificationService.isServiceRunning) {
            TimerNotificationService.startService(context)
        }
        
        Log.d(TAG, "Timer state applied locally from sync")
    }
    
    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        var id = prefs.getString(PREF_DEVICE_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, id).apply()
        }
        return id
    }
}
