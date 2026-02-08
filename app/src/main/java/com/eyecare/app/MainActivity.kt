package com.eyecare.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.eyecare.app.ui.theme.EyeCareTheme
import java.util.concurrent.TimeUnit

/**
 * MainActivity - Main entry point of the Eye Care App
 * 
 * Features:
 * - Toggle for Blue Light Filter (overlay service)
 * - Toggle for 20-20-20 Rule Reminders (WorkManager notifications)
 * - Permission handling for overlay and notifications
 */
class MainActivity : ComponentActivity() {

    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled in Compose UI state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EyeCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EyeCareScreen(
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Main screen composable with all UI components
 */
@Composable
fun EyeCareScreen(
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    
    // State for toggles
    var blueLightFilterEnabled by remember { mutableStateOf(false) }
    var remindersEnabled by remember { mutableStateOf(false) }
    
    // Permission states
    val hasOverlayPermission by remember {
        derivedStateOf { Settings.canDrawOverlays(context) }
    }
    
    val hasNotificationPermission by remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission not required on older versions
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Title
        Text(
            text = "👁️ Eye Care",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Protect your eyes while using your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Blue Light Filter Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔶 Blue Light Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Reduces blue light with an orange overlay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!hasOverlayPermission) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Permission Required",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "This feature needs permission to display over other apps",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (blueLightFilterEnabled) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Switch(
                        checked = blueLightFilterEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && !hasOverlayPermission) {
                                // Don't enable if permission not granted
                                return@Switch
                            }
                            
                            blueLightFilterEnabled = isChecked
                            
                            if (isChecked) {
                                // Start the Blue Light Filter Service
                                val intent = Intent(context, BlueLightService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                // Stop the Blue Light Filter Service
                                val intent = Intent(context, BlueLightService::class.java)
                                context.stopService(intent)
                            }
                        },
                        enabled = hasOverlayPermission
                    )
                }
            }
        }
        
        // 20-20-20 Reminders Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⏰ 20-20-20 Rule Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Get reminded every 20 minutes to take a break",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Permission Required",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "This feature needs notification permission",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { onRequestNotificationPermission() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remindersEnabled) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && !hasNotificationPermission) {
                                // Don't enable if permission not granted
                                return@Switch
                            }
                            
                            remindersEnabled = isChecked
                            
                            if (isChecked) {
                                // Schedule periodic work
                                scheduleEyeCareReminders(context)
                            } else {
                                // Cancel periodic work
                                cancelEyeCareReminders(context)
                            }
                        },
                        enabled = hasNotificationPermission
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ℹ️ About 20-20-20 Rule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Every 20 minutes, look at something 20 feet away for 20 seconds to reduce eye strain.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Schedules the periodic eye care reminders using WorkManager
 */
private fun scheduleEyeCareReminders(context: android.content.Context) {
    val workRequest = PeriodicWorkRequestBuilder<EyeCareWorker>(
        20, TimeUnit.MINUTES // Repeat every 20 minutes
    )
        .setInitialDelay(20, TimeUnit.MINUTES) // First reminder after 20 minutes
        .addTag(EyeCareWorker.WORK_TAG)
        .build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        EyeCareWorker.WORK_TAG,
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}

/**
 * Cancels the periodic eye care reminders
 */
private fun cancelEyeCareReminders(context: android.content.Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(EyeCareWorker.WORK_TAG)
}
