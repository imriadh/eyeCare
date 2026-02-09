package com.eyecare.app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.*
import com.eyecare.app.ui.theme.EyeCareTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import android.provider.AlarmClock

/**
 * MainActivity - Enhanced Eye Care App
 * 
 * Features:
 * - Blue Light Filter with toggle
 * - 20-20-20 Rule Reminders with countdown timer
 * - Sleep Cycle Calculator with alarm feature
 * - Customizable reminder interval (15-60 minutes)
 * - Pause/Snooze functionality (30 min, 1 hour, 2 hours)
 * - Sound notification toggle
 * - Break instructions
 * - Settings screen
 * - Bottom navigation for clean UI
 * - Permission handling
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    
    private var permissionCheckTrigger by mutableStateOf(0)

    private var permissionCheckTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EyeCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        permissionCheckTrigger = permissionCheckTrigger
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Trigger permission check when returning from settings
        permissionCheckTrigger++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onRequestNotificationPermission: () -> Unit,
    permissionCheckTrigger: Int
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (selectedTab) {
                            0 -> "👁️ Eye Care"
                            1 -> "😴 Sleep Cycles"
                            else -> "⚙️ Settings"
                        },
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Bedtime, "Sleep") },
                    label = { Text("Sleep") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> EyeCareHomeScreen(
                paddingValues = paddingValues,
                onRequestNotificationPermission = onRequestNotificationPermission,
                permissionCheckTrigger = permissionCheckTrigger
            )
            1 -> SleepCycleScreen(paddingValues = paddingValues)
            2 -> SettingsScreen(paddingValues = paddingValues)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EyeCareHomeScreen(
    paddingValues: PaddingValues,
    onRequestNotificationPermission: () -> Unit,
    permissionCheckTrigger: Int
) {
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
    
    // Permission states - check again when permissionCheckTrigger changes
    val hasOverlayPermission = remember(permissionCheckTrigger) {
        Settings.canDrawOverlays(context)
    }
    
    val hasNotificationPermission = remember(permissionCheckTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

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
fun SleepCycleScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf(Calendar.getInstance()) }
    var isCustomTime by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Calculate sleep cycles (90 minutes each)
    val sleepCycleDuration = 90
    val currentTime = remember { Calendar.getInstance() }
    
    val sleepCycles = remember(selectedTime, isCustomTime) {
        val baseTime = if (isCustomTime) selectedTime else currentTime
        listOf(
            Pair(3, calculateWakeUpTime(baseTime.clone() as Calendar, sleepCycleDuration * 3)),
            Pair(4, calculateWakeUpTime(baseTime.clone() as Calendar, sleepCycleDuration * 4)),
            Pair(5, calculateWakeUpTime(baseTime.clone() as Calendar, sleepCycleDuration * 5)),
            Pair(6, calculateWakeUpTime(baseTime.clone() as Calendar, sleepCycleDuration * 6)),
            Pair(7, calculateWakeUpTime(baseTime.clone() as Calendar, sleepCycleDuration * 7))
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "😴 Sleep Cycle Calculator",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "One complete sleep cycle is 90 minutes. Waking up at the end of a cycle helps you feel more refreshed!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Time Selection Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⏰ When do you plan to go to bed?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCustomTime,
                        onClick = { isCustomTime = false },
                        label = { Text("Now") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCustomTime,
                        onClick = { 
                            isCustomTime = true
                            showTimePicker = true
                        },
                        label = { Text("Custom Time") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (isCustomTime) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selected: ${timeFormat.format(selectedTime.time)}")
                    }
                }
            }
        }
        
        // Sleep Cycles Results
        Text(
            text = "🌙 Recommended Wake Up Times",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        sleepCycles.forEach { (cycles, wakeUpTime) ->
            SleepCycleCard(
                cycles = cycles,
                wakeUpTime = wakeUpTime,
                onSetAlarm = {
                    setAlarm(context, wakeUpTime, cycles)
                }
            )
        }
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 Sleep Cycle Facts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Adults need 4-6 complete sleep cycles (6-9 hours)\n" +
                            "• Waking up mid-cycle makes you feel groggy\n" +
                            "• Add 10-15 minutes to fall asleep\n" +
                            "• Consistent sleep schedule is key!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                showTimePicker = false
            }
        )
    }
}

@Composable
fun SleepCycleCard(cycles: Int, wakeUpTime: Calendar, onSetAlarm: () -> Unit) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val totalHours = (cycles * 90) / 60.0
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeFormat.format(wakeUpTime.time),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$cycles cycles (${String.format("%.1f", totalHours)} hours)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            FilledTonalButton(onClick = onSetAlarm) {
                Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Set Alarm")
            }
        }
    }
}

@Composable
fun SettingsScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👁️",
                    fontSize = 64.sp
                )
                Text(
                    text = "Eye Care App",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 2.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Protect your eyes and sleep better!",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Info About 20-20-20 Rule
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ℹ️ About the 20-20-20 Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Every 20 minutes, take a 20-second break to look at something 20 feet away. This simple rule helps reduce eye strain, prevent headaches, and maintain healthy vision.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Sleep Cycle Info
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "😴 About Sleep Cycles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A complete sleep cycle lasts about 90 minutes. During this time, you go through different stages of sleep including REM (dream sleep). Waking up at the end of a cycle helps you feel more refreshed than waking up in the middle of one.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Blue Light Filter Info
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔶 About Blue Light Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Blue light from screens can cause eye strain and disrupt sleep patterns. The orange overlay filter reduces blue light exposure, especially useful in the evening hours.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Features List
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨ App Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                listOf(
                    "👁️ Blue light filter overlay",
                    "⏰ 20-20-20 reminder notifications",
                    "⏱️ Customizable reminder intervals",
                    "⏸️ Pause/snooze functionality",
                    "🔊 Sound notification controls",
                    "😴 Sleep cycle calculator",
                    "⏰ Smart alarm suggestions",
                    "🎨 Material 3 design"
                ).forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
        
        // Quick Actions
        Text(
            text = "⚡ Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage app permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val currentTime = Calendar.getInstance()
    var selectedHour by remember { mutableStateOf(currentTime.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(currentTime.get(Calendar.MINUTE)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Bedtime") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Choose your bedtime")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                            Icon(Icons.Default.KeyboardArrowUp, "Increase hour")
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedHour = if (selectedHour == 0) 23 else selectedHour - 1 }) {
                            Icon(Icons.Default.KeyboardArrowDown, "Decrease hour")
                        }
                    }
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    // Minute selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { selectedMinute = (selectedMinute + 15) % 60 }) {
                            Icon(Icons.Default.KeyboardArrowUp, "Increase minute")
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedMinute = if (selectedMinute < 15) 45 else selectedMinute - 15 }) {
                            Icon(Icons.Default.KeyboardArrowDown, "Decrease minute")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun calculateWakeUpTime(baseTime: Calendar, minutesToAdd: Int): Calendar {
    baseTime.add(Calendar.MINUTE, minutesToAdd + 15) // Add 15 minutes to fall asleep
    return baseTime
}

private fun setAlarm(context: android.content.Context, wakeUpTime: Calendar, cycles: Int) {
    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
    
    // Create intent for alarm
    val intent = Intent(AlarmClock.ACTION_SET_ALARM)
    intent.putExtra(AlarmClock.EXTRA_HOUR, wakeUpTime.get(Calendar.HOUR_OF_DAY))
    intent.putExtra(AlarmClock.EXTRA_MINUTES, wakeUpTime.get(Calendar.MINUTE))
    intent.putExtra(AlarmClock.EXTRA_MESSAGE, "$cycles Sleep Cycles")
    intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
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
