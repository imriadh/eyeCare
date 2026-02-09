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
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.eyecare.app.ui.theme.EyeCareTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * MainActivity - Enhanced Eye Care App
 * 
 * Features:
 * - Blue Light Filter with toggle
 * - 20-20-20 Rule Reminders with countdown timer
 * - Customizable reminder interval (15-60 minutes)
 * - Pause/Snooze functionality (30 min, 1 hour, 2 hours)
 * - Sound notification toggle
 * - Break instructions
 * - Permission handling
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EyeCareScreen(onRequestNotificationPermission: () -> Unit) {
    val context = LocalContext.current
    
    // State variables
    var blueLightFilterEnabled by remember { 
        mutableStateOf(PreferencesHelper.isBlueLightFilterEnabled(context)) 
    }
    var remindersEnabled by remember { 
        mutableStateOf(PreferencesHelper.areRemindersEnabled(context)) 
    }
    var reminderInterval by remember { 
        mutableStateOf(PreferencesHelper.getReminderInterval(context).toFloat()) 
    }
    var soundEnabled by remember { 
        mutableStateOf(PreferencesHelper.isSoundEnabled(context)) 
    }
    var timeRemainingMillis by remember { mutableStateOf(0L) }
    var isPaused by remember { mutableStateOf(PreferencesHelper.isPaused(context)) }
    var showPauseDialog by remember { mutableStateOf(false) }
    
    // Update countdown timer
    LaunchedEffect(remindersEnabled) {
        while (remindersEnabled) {
            timeRemainingMillis = PreferencesHelper.getTimeRemainingMillis(context)
            isPaused = PreferencesHelper.isPaused(context)
            delay(1000) // Update every second
        }
    }
    
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
                true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "👁️ Eye Care",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Countdown Timer Card
            if (remindersEnabled && !isPaused) {
                CountdownTimerCard(timeRemainingMillis)
            }
            
            // Paused Status Card
            if (isPaused) {
                PausedStatusCard()
            }
            
            // Blue Light Filter Card
            BlueLightFilterCard(
                enabled = blueLightFilterEnabled,
                hasPermission = hasOverlayPermission,
                onToggle = { isChecked ->
                    if (isChecked && !hasOverlayPermission) {
                        return@BlueLightFilterCard
                    }
                    blueLightFilterEnabled = isChecked
                    PreferencesHelper.setBlueLightFilterEnabled(context, isChecked)
                    
                    if (isChecked) {
                        val intent = Intent(context, BlueLightService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    } else {
                        context.stopService(Intent(context, BlueLightService::class.java))
                    }
                },
                onRequestPermission = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
            
            // Reminders Card
            RemindersCard(
                enabled = remindersEnabled,
                hasPermission = hasNotificationPermission,
                interval = reminderInterval.roundToInt(),
                soundEnabled = soundEnabled,
                onToggle = { isChecked ->
                    if (isChecked && !hasNotificationPermission) {
                        return@RemindersCard
                    }
                    remindersEnabled = isChecked
                    PreferencesHelper.setRemindersEnabled(context, isChecked)
                    
                    if (isChecked) {
                        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                        scheduleEyeCareReminders(context, reminderInterval.roundToInt())
                    } else {
                        cancelEyeCareReminders(context)
                        PreferencesHelper.setLastNotificationTime(context, 0)
                    }
                },
                onIntervalChange = { newInterval ->
                    reminderInterval = newInterval
                    PreferencesHelper.setReminderInterval(context, newInterval.roundToInt())
                    if (remindersEnabled) {
                        scheduleEyeCareReminders(context, newInterval.roundToInt())
                        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                    }
                },
                onSoundToggle = { isChecked ->
                    soundEnabled = isChecked
                    PreferencesHelper.setSoundEnabled(context, isChecked)
                },
                onPause = {
                    showPauseDialog = true
                },
                onRequestPermission = onRequestNotificationPermission,
                isPaused = isPaused
            )
            
            // Break Instructions Card
            BreakInstructionsCard()
            
            // Info Card
            InfoCard()
        }
    }
    
    // Pause Dialog
    if (showPauseDialog) {
        PauseDialog(
            onDismiss = { showPauseDialog = false },
            onPause = { minutes ->
                val pauseUntil = System.currentTimeMillis() + (minutes * 60 * 1000)
                PreferencesHelper.setPauseUntil(context, pauseUntil)
                isPaused = true
                showPauseDialog = false
            }
        )
    }
}

@Composable
fun CountdownTimerCard(timeRemainingMillis: Long) {
    val minutes = (timeRemainingMillis / 1000 / 60).toInt()
    val seconds = ((timeRemainingMillis / 1000) % 60).toInt()
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⏰ Next Break In",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 56.sp
            )
            Text(
                text = "minutes : seconds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun PausedStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⏸️",
                style = MaterialTheme.typography.headlineMedium
            )
            Column {
                Text(
                    text = "Reminders Paused",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "You won't receive notifications during this time",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun BlueLightFilterCard(
    enabled: Boolean,
    hasPermission: Boolean,
    onToggle: (Boolean) -> Unit,
    onRequestPermission: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔶 Blue Light Filter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Reduces harmful blue light with an orange overlay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!hasPermission) {
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
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "App needs permission to display over other apps",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onRequestPermission,
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
                    text = if (enabled) "✓ Active" else "Inactive",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = hasPermission
                )
            }
        }
    }
}

@Composable
fun RemindersCard(
    enabled: Boolean,
    hasPermission: Boolean,
    interval: Int,
    soundEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onIntervalChange: (Float) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onPause: () -> Unit,
    onRequestPermission: () -> Unit,
    isPaused: Boolean
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⏰ 20-20-20 Rule Reminders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Get reminded to take breaks and protect your eyes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "App needs notification permission",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }
            
            // Main toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (enabled) "✓ Active" else "Inactive",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = hasPermission
                )
            }
            
            if (enabled) {
                HorizontalDivider()
                
                // Interval Slider
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Reminder Interval: $interval minutes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = interval.toFloat(),
                        onValueChange = onIntervalChange,
                        valueRange = 15f..60f,
                        steps = 8, // 15, 20, 25, 30, 35, 40, 45, 50, 55, 60
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("15 min", style = MaterialTheme.typography.labelSmall)
                        Text("60 min", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                HorizontalDivider()
                
                // Sound Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔊 Sound Notification",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Play sound with notifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundToggle
                    )
                }
                
                HorizontalDivider()
                
                // Pause Button
                Button(
                    onClick = onPause,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPaused
                ) {
                    Text(if (isPaused) "Currently Paused" else "⏸️ Pause Reminders")
                }
            }
        }
    }
}

@Composable
fun BreakInstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🌿 How to Take a Proper Break",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "1️⃣ Look at something 20 feet (6 meters) away",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "2️⃣ Keep looking for at least 20 seconds",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "3️⃣ Blink frequently to refresh your eyes",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "4️⃣ Stretch your neck and shoulders",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "5️⃣ Stand up and move around if possible",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InfoCard() {
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
                text = "ℹ️ About the 20-20-20 Rule",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Every 20 minutes, take a 20-second break to look at something 20 feet away. This simple rule helps reduce eye strain, prevent headaches, and maintain healthy vision.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "💡 Regular breaks also help prevent neck pain, back problems, and repetitive strain injuries (RSI).",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun PauseDialog(
    onDismiss: () -> Unit,
    onPause: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⏸️ Pause Reminders") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How long would you like to pause notifications?")
                
                Button(
                    onClick = { onPause(30) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("30 Minutes")
                }
                Button(
                    onClick = { onPause(60) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1 Hour")
                }
                Button(
                    onClick = { onPause(120) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2 Hours")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Schedules the periodic eye care reminders using WorkManager
 */
private fun scheduleEyeCareReminders(context: android.content.Context, intervalMinutes: Int) {
    // Cancel existing work first
    WorkManager.getInstance(context).cancelAllWorkByTag(EyeCareWorker.WORK_TAG)
    
    val workRequest = PeriodicWorkRequestBuilder<EyeCareWorker>(
        intervalMinutes.toLong(), TimeUnit.MINUTES
    )
        .setInitialDelay(intervalMinutes.toLong(), TimeUnit.MINUTES)
        .addTag(EyeCareWorker.WORK_TAG)
        .build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        EyeCareWorker.WORK_TAG,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}

/**
 * Cancels the periodic eye care reminders
 */
private fun cancelEyeCareReminders(context: android.content.Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(EyeCareWorker.WORK_TAG)
}
