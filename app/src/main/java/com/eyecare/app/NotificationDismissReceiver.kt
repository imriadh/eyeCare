package com.eyecare.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Handles notification dismissal events
 */
class NotificationDismissReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_DISMISS = "com.eyecare.app.ACTION_DISMISS"
        const val ACTION_UNDO_DISMISS = "com.eyecare.app.ACTION_UNDO_DISMISS"
        const val EXTRA_SHOW_UNDO = "show_undo"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISS -> handleDismiss(context)
            ACTION_UNDO_DISMISS -> handleUndoDismiss(context)
        }
    }
    
    private fun handleDismiss(context: Context) {
        val currentStreak = PreferencesHelper.getCurrentStreak(context)
        
        // Check if we should ask for confirmation
        if (currentStreak > 3) {
            // Show toast informing about the confirmation requirement
            Toast.makeText(
                context,
                "⚠️ You have a ${currentStreak}-day streak! Open app to confirm closing.",
                Toast.LENGTH_LONG
            ).show()
            
            // Don't actually close - user needs to confirm in app
            return
        }
        
        // Record dismiss time for undo functionality
        PreferencesHelper.setLastDismissedTime(context, System.currentTimeMillis())
        
        // Show undo toast
        showUndoToast(context)
        
        // Actually dismiss after a delay (allowing undo)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val dismissTime = PreferencesHelper.getLastDismissedTime(context)
            // Only dismiss if it hasn't been undone
            if (System.currentTimeMillis() - dismissTime < 3500) {
                actuallyDismiss(context)
            }
        }, 3000)
    }
    
    private fun handleUndoDismiss(context: Context) {
        // Clear the dismiss time to prevent actual dismissal
        PreferencesHelper.setLastDismissedTime(context, 0L)
        
        Toast.makeText(
            context,
            "✓ Notification restored",
            Toast.LENGTH_SHORT
        ).show()
        
        // Restart service to restore notification
        TimerNotificationService.startService(context)
    }
    
    private fun actuallyDismiss(context: Context) {
        // Stop the service and clear preferences
        PreferencesHelper.setRemindersEnabled(context, false)
        PreferencesHelper.setLastNotificationTime(context, 0)
        PreferencesHelper.setPauseUntil(context, 0)
        PreferencesHelper.setPausedRemainingTime(context, 0)
        TimerNotificationService.stopService(context)
        
        // Update widgets
        EyeCareWidgetProvider.updateAllWidgets(context)
    }
    
    private fun showUndoToast(context: Context) {
        Toast.makeText(
            context,
            "Notification dismissed • Tap UNDO within 3 seconds",
            Toast.LENGTH_LONG
        ).show()
    }
}
