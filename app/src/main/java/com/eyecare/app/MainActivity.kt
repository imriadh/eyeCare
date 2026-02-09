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
import androidx.work.*
import com.eyecare.app.ui.theme.EyeCareTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import android.provider.AlarmClock

/**
 * MainActivity - Enhanced Eye Care App
 * 
 * Features:
 * - 20-20-20 Rule Reminders with persistent countdown timer
 * - Real-time notification with timer and control buttons
 * - Pause/Resume/Reset actions from notification
 * - Sleep Cycle Calculator with alarm feature
 * - Customizable reminder interval (15-60 minutes)
 * - Pause/Snooze functionality (30 min, 1 hour, 2 hours)
 * - Sound notification toggle
 * - Break instructions
 * - Settings screen with educational content
 * - Bottom navigation for clean UI
 * - Permission handling
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    
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
                    icon = { Icon(Icons.Default.Star, "Sleep") },
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
                        // Start persistent notification service
                        TimerNotificationService.startService(context)
                    } else {
                        cancelEyeCareReminders(context)
                        PreferencesHelper.setLastNotificationTime(context, 0)
                        // Stop persistent notification service
                        TimerNotificationService.stopService(context)
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
                onReset = {
                    PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                    PreferencesHelper.setPauseUntil(context, 0)
                    isPaused = false
                    timeRemainingMillis = PreferencesHelper.getTimeRemainingMillis(context)
                },
                onRequestPermission = onRequestNotificationPermission,
                isPaused = isPaused
            )
            
            // Break Instructions Card
            BreakInstructionsCard()
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
                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👁️",
                    fontSize = 72.sp
                )
                Text(
                    text = "Eye Care App",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 4.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Text(
                    text = "Protect your eyes with the 20-20-20 rule",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Text(
            text = "📚 About",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Info About 20-20-20 Rule
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⏰", fontSize = 24.sp)
                    Text(
                        text = "The 20-20-20 Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Every 20 minutes, take a 20-second break to look at something 20 feet away. This simple rule helps reduce eye strain, prevent headaches, and maintain healthy vision during screen time.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
        
        // Why It Matters
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "💡", fontSize = 24.sp)
                    Text(
                        text = "Why It Matters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Digital eye strain affects up to 90% of screen users. Regular breaks help prevent dry eyes, blurred vision, and headaches while improving focus and productivity.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
        
        // Sleep Cycle Info
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "😴", fontSize = 24.sp)
                    Text(
                        text = "Sleep Cycles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "A complete sleep cycle lasts about 90 minutes. Waking up at the end of a cycle helps you feel more refreshed than waking in the middle of one. Use our Sleep tab to find your optimal wake-up times!",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
        
        Text(
            text = "⚙️ Quick Actions",
            style = MaterialTheme.typography.titleLarge,
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
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "App Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage notifications & settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
        
        // Copyright Section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Developed by",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Riad Hossain",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedCard(
                    modifier = Modifier
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://riad.iam.bd"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "riad.iam.bd",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = "© 2026 All Rights Reserved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Made with ❤️ for eye health",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
    try {
        // Create intent for alarm
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, wakeUpTime.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, wakeUpTime.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "$cycles Sleep Cycles - Eye Care")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: try to open clock app
            val clockIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.deskclock")
            }
            context.startActivity(clockIntent)
        }
    } catch (e: Exception) {
        // Silent fail - alarm app not available
        e.printStackTrace()
    }
}

@Composable
fun CountdownTimerCard(timeRemainingMillis: Long) {
    val minutes = (timeRemainingMillis / 1000 / 60).toInt()
    val seconds = ((timeRemainingMillis / 1000) % 60).toInt()
    val totalSeconds = (timeRemainingMillis / 1000).toFloat()
    val intervalSeconds = PreferencesHelper.DEFAULT_REMINDER_INTERVAL * 60f
    val progress = 1f - (totalSeconds / intervalSeconds)
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🍅 Pomodoro Timer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Circular progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 48.sp
                    )
                    Text(
                        text = "until break",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Text(
                text = "Focus on your work, we'll remind you!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
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
fun RemindersCard(
    enabled: Boolean,
    hasPermission: Boolean,
    interval: Int,
    soundEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onIntervalChange: (Float) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onRequestPermission: () -> Unit,
    isPaused: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⏰",
                            fontSize = 28.sp
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "20-20-20 Rule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (enabled) "Protecting your eyes ✓" else "Start eye care reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = hasPermission
                )
            }
            
            if (!hasPermission) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Notification Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "Allow notifications to receive eye care reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        FilledTonalButton(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Permission")
                        }
                    }
                }
            }
            
            if (enabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                // Interval Slider with better styling
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reminder Interval",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "$interval min",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        
                        Slider(
                            value = interval.toFloat(),
                            onValueChange = onIntervalChange,
                            valueRange = 15f..60f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "15 min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "60 min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Sound Toggle with better styling
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🔊", fontSize = 20.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "Sound Alerts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (soundEnabled) "Enabled" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = onSoundToggle
                        )
                    }
                }
                
                // Pause and Reset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        enabled = !isPaused,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (!isPaused) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Paused" else "Pause",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔄 Reset",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
