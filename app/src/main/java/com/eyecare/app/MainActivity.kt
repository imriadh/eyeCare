package com.eyecare.app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowForward

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

/**
 * Achievement Data Class
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val requiredProgress: Int,
    val getCurrentProgress: (Context) -> Int
)

/**
 * Break Rule Preset Data Class
 */
data class BreakRulePreset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val intervalMinutes: Int,
    val durationSeconds: Int
)

/**
 * Get all break rule presets
 */
fun getBreakRulePresets(): List<BreakRulePreset> {
    return listOf(
        BreakRulePreset(
            id = "20-20-20",
            name = "20-20-20 Rule",
            description = "Classic eye care • Every 20 min, 20 sec break",
            icon = "👁️",
            intervalMinutes = 20,
            durationSeconds = 20
        ),
        BreakRulePreset(
            id = "30-20-20",
            name = "30-20-20",
            description = "For developers • Longer focus time",
            icon = "💻",
            intervalMinutes = 30,
            durationSeconds = 20
        ),
        BreakRulePreset(
            id = "15-15-15",
            name = "15-15-15",  
            description = "Intense work • Frequent short breaks",
            icon = "⚡",
            intervalMinutes = 15,
            durationSeconds = 15
        ),
        BreakRulePreset(
            id = "50-10",
            name = "50-10 Pomodoro",
            description = "Pomodoro style • Long focus, short eye rest",
            icon = "🍅",
            intervalMinutes = 50,
            durationSeconds = 10
        ),
        BreakRulePreset(
            id = "custom",
            name = "Custom",
            description = "Define your own interval and duration",
            icon = "⚙️",
            intervalMinutes = 20,
            durationSeconds = 20
        )
    )
}

/**
 * Get all achievements
 */
fun getAllAchievements(): List<Achievement> {
    return listOf(
        Achievement(
            id = "first_break",
            title = "First Break",
            description = "Take your first break",
            icon = "🌟",
            requiredProgress = 1,
            getCurrentProgress = { context: Context -> PreferencesHelper.getTotalBreaks(context) }
        ),
        Achievement(
            id = "week_warrior",
            title = "Week Warrior",
            description = "Maintain a 7 day streak",
            icon = "🔥",
            requiredProgress = 7,
            getCurrentProgress = { context: Context -> PreferencesHelper.getCurrentStreak(context) }
        ),
        Achievement(
            id = "century_club",
            title = "Century Club",
            description = "Complete 100 total breaks",
            icon = "💯",
            requiredProgress = 100,
            getCurrentProgress = { context: Context -> PreferencesHelper.getTotalBreaks(context) }
        ),
        Achievement(
            id = "night_owl",
            title = "Night Owl",
            description = "Use sleep calculator 10 times",
            icon = "🌙",
            requiredProgress = 10,
            getCurrentProgress = { context: Context -> PreferencesHelper.getSleepCalcUsed(context) }
        ),
        Achievement(
            id = "data_lover",
            title = "Data Lover",
            description = "Check stats 5 times",
            icon = "📊",
            requiredProgress = 5,
            getCurrentProgress = { context: Context -> PreferencesHelper.getStatsViewed(context) }
        ),
        Achievement(
            id = "perfect_day",
            title = "Perfect Day",
            description = "Take 12+ breaks in one day",
            icon = "⚡",
            requiredProgress = 12,
            getCurrentProgress = { context: Context -> PreferencesHelper.getBreaksToday(context) }
        ),
        Achievement(
            id = "month_master",
            title = "Month Master",
            description = "Maintain a 30 day streak",
            icon = "🏅",
            requiredProgress = 30,
            getCurrentProgress = { context: Context -> PreferencesHelper.getCurrentStreak(context) }
        ),
        Achievement(
            id = "dedicated",
            title = "Dedicated",
            description = "Complete 50 total breaks",
            icon = "💪",
            requiredProgress = 50,
            getCurrentProgress = { context: Context -> PreferencesHelper.getTotalBreaks(context) }
        ),
        Achievement(
            id = "exercise_starter",
            title = "Exercise Starter",
            description = "Complete 10 eye exercises",
            icon = "👁️",
            requiredProgress = 10,
            getCurrentProgress = { context: Context -> PreferencesHelper.getTotalExercises(context) }
        ),
        Achievement(
            id = "exercise_master",
            title = "Exercise Master",
            description = "Complete 50 eye exercises",
            icon = "🎖️",
            requiredProgress = 50,
            getCurrentProgress = { context: Context -> PreferencesHelper.getTotalExercises(context) }
        )
    )
}

/**
 * Exercise Data Classes
 */
enum class ExerciseType {
    BLINK, FOCUS_SHIFT, EYE_ROTATION, PALMING, FOLLOW_DOT
}

data class Exercise(
    val id: String,
    val type: ExerciseType,
    val title: String,
    val description: String,
    val icon: String,
    val durationSeconds: Int,
    val instructions: List<String>
)

/**
 * Get all available exercises
 */
fun getAllExercises(): List<Exercise> {
    return listOf(
        Exercise(
            id = "blink",
            type = ExerciseType.BLINK,
            title = "Blink Exercise",
            description = "Refresh and lubricate your eyes",
            icon = "👁️",
            durationSeconds = 20,
            instructions = listOf(
                "Blink rapidly for 10 times",
                "Close your eyes gently",
                "Relax for 10 seconds",
                "Repeat 2 more times"
            )
        ),
        Exercise(
            id = "focus_shift",
            type = ExerciseType.FOCUS_SHIFT,
            title = "Focus Shift",
            description = "Train your focus muscles",
            icon = "🎯",
            durationSeconds = 30,
            instructions = listOf(
                "Hold finger 10 inches from face",
                "Focus on finger for 5 seconds",
                "Look at distant object (20+ feet)",
                "Focus on distance for 5 seconds",
                "Repeat 3 times"
            )
        ),
        Exercise(
            id = "eye_rotation",
            type = ExerciseType.EYE_ROTATION,
            title = "Eye Rotation",
            description = "Relax eye muscles",
            icon = "🔄",
            durationSeconds = 25,
            instructions = listOf(
                "Look up slowly, then right",
                "Then down, then left",
                "Complete a full circle",
                "Repeat 3 times clockwise",
                "Then 3 times counter-clockwise"
            )
        ),
        Exercise(
            id = "palming",
            type = ExerciseType.PALMING,
            title = "Palming",
            description = "Rest and relax your eyes",
            icon = "🙌",
            durationSeconds = 30,
            instructions = listOf(
                "Rub hands together to warm them",
                "Cup hands over closed eyes",
                "Don't press on eyeballs",
                "Relax in darkness for 30 seconds",
                "Breathe deeply and calmly"
            )
        ),
        Exercise(
            id = "follow_dot",
            type = ExerciseType.FOLLOW_DOT,
            title = "Follow the Dot",
            description = "Track moving objects smoothly",
            icon = "⚫",
            durationSeconds = 20,
            instructions = listOf(
                "Watch the dot move on screen",
                "Follow smoothly with eyes only",
                "Keep head still",
                "Track for 20 seconds"
            )
        )
    )
}

/**
 * Check for newly unlocked achievements and show notification
 */
fun checkAndNotifyAchievements(context: Context) {
    val achievements = getAllAchievements()
    val unlockedAchievements = PreferencesHelper.getUnlockedAchievements(context)
    
    achievements.forEach { achievement ->
        val currentProgress = achievement.getCurrentProgress(context)
        val isAlreadyUnlocked = unlockedAchievements.contains(achievement.id)
        
        // If achievement is newly unlocked
        if (!isAlreadyUnlocked && currentProgress >= achievement.requiredProgress) {
            PreferencesHelper.unlockAchievement(context, achievement.id)
            
            // Show toast notification
            android.widget.Toast.makeText(
                context,
                "${achievement.icon} Achievement Unlocked: ${achievement.title}!",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}

/**
 * Onboarding Screens
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var selectedPreset by remember { mutableStateOf("20-20-20") }
    var workHoursStart by remember { mutableStateOf(8) }
    var workHoursEnd by remember { mutableStateOf(18) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomeScreen()
                1 -> CustomizeScreen(
                    selectedPreset = selectedPreset,
                    onPresetSelected = { selectedPreset = it },
                    workHoursStart = workHoursStart,
                    workHoursEnd = workHoursEnd,
                    onWorkHoursStartChanged = { workHoursStart = it },
                    onWorkHoursEndChanged = { workHoursEnd = it }
                )
                2 -> PermissionsScreen()
            }
        }
        
        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        // Save preferences
                        val preset = getBreakRulePresets().find { it.id == selectedPreset }
                        if (preset != null) {
                            PreferencesHelper.setReminderInterval(context, preset.intervalMinutes)
                            PreferencesHelper.setBreakDuration(context, preset.durationSeconds)
                            PreferencesHelper.setSelectedPreset(context, selectedPreset)
                        }
                        PreferencesHelper.setWorkHoursStart(context, workHoursStart)
                        PreferencesHelper.setWorkHoursEnd(context, workHoursEnd)
                        PreferencesHelper.setOnboardingCompleted(context, true)
                        onComplete()
                    }
                }
            ) {
                Text(if (pagerState.currentPage < 2) "Next" else "Get Started")
            }
        }
        
        // Page indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (index == pagerState.currentPage) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "👁️",
            fontSize = 80.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to Eye Care!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Take care of your eyes with the 20-20-20 rule",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 20-20-20 Rule Explanation
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RuleCard("⏰", "Every 20 Minutes", "Take a break from your screen")
                HorizontalDivider()
                RuleCard("👀", "Look 20 Feet Away", "Focus on something distant")
                HorizontalDivider()
                RuleCard("⏱️", "For 20 Seconds", "Give your eyes a rest")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Why Eye Care Matters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FactCard("🔬 Reduces digital eye strain")
            FactCard("💪 Prevents eye fatigue")
            FactCard("😌 Improves focus and productivity")
            FactCard("🌟 Protects long-term eye health")
        }
    }
}

@Composable
fun CustomizeScreen(
    selectedPreset: String,
    onPresetSelected: (String) -> Unit,
    workHoursStart: Int,
    workHoursEnd: Int,
    onWorkHoursStartChanged: (Int) -> Unit,
    onWorkHoursEndChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚙️",
            fontSize = 60.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Customize Your Experience",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Choose Preset
        Text(
            text = "Choose Your Break Interval",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val presets = getBreakRulePresets()
        presets.forEach { preset ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPresetSelected(preset.id) },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (selectedPreset == preset.id) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = preset.icon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedPreset == preset.id) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Work Hours
        Text(
            text = "Set Your Work Hours",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Reminders will be active during these hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Start:", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${workHoursStart}:00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = workHoursStart.toFloat(),
                    onValueChange = { onWorkHoursStartChanged(it.toInt()) },
                    valueRange = 0f..23f,
                    steps = 22
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("End:", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${workHoursEnd}:00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = workHoursEnd.toFloat(),
                    onValueChange = { onWorkHoursEndChanged(it.toInt()) },
                    valueRange = 0f..23f,
                    steps = 22
                )
            }
        }
    }
}

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔔",
            fontSize = 60.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Grant Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To give you the best experience, we need a few permissions",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Notification Permission
        PermissionCard(
            icon = "🔔",
            title = "Notification Permission",
            description = "Required to remind you about eye breaks",
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exact Alarm Permission
        PermissionCard(
            icon = "⏰",
            title = "Exact Alarm Permission",
            description = "For accurate break timing and sleep calculator",
            isRequired = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "👁️", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text ="You're all set to start taking care of your eyes!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RuleCard(icon: String, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = icon, fontSize = 32.sp)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FactCard(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.small
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PermissionCard(icon: String, title: String, description: String, isRequired: Boolean) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = icon, fontSize = 40.sp)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRequired) {
                        Text(
                            text = "REQUIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    
    private var permissionCheckTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize reminders on first launch if enabled by default
        initializeReminders()
        
        setContent {
            EyeCareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showOnboarding by remember { 
                        mutableStateOf(!PreferencesHelper.isOnboardingCompleted(this@MainActivity)) 
                    }
                    
                    if (showOnboarding) {
                        OnboardingScreen(
                            onComplete = {
                                showOnboarding = false
                                // Request permissions after onboarding
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    if (!alarmManager.canScheduleExactAlarms()) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        startActivity(intent)
                                    }
                                }
                                // Initialize reminders after onboarding
                                initializeReminders()
                            }
                        )
                    } else {
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
    }
    
    private fun initializeReminders() {
        // Check if this is first launch and reminders are enabled by default
        val prefs = getSharedPreferences("eye_care_prefs", android.content.Context.MODE_PRIVATE)
        val isFirstLaunch = !prefs.contains("reminders_enabled")
        
        if (isFirstLaunch && PreferencesHelper.areRemindersEnabled(this)) {
            // Set initial notification time so timer starts from full interval
            if (PreferencesHelper.getLastNotificationTime(this) == 0L) {
                PreferencesHelper.setLastNotificationTime(this, System.currentTimeMillis())
            }
            
            // Start the timer notification service
            TimerNotificationService.startService(this)
            
            // Schedule periodic reminders
            val interval = PreferencesHelper.getReminderInterval(this)
            scheduleEyeCareReminders(this, interval)
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
                            2 -> "📊 Statistics"
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
                    icon = { Icon(Icons.Default.AccountBox, "Stats") },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
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
            2 -> StatsScreen(paddingValues = paddingValues)
            3 -> SettingsScreen(paddingValues = paddingValues)
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
    var showExerciseSelection by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExerciseCompletion by remember { mutableStateOf(false) }
    
    // Update countdown timer
    LaunchedEffect(remindersEnabled, isPaused) {
        while (true) {
            if (remindersEnabled && !isPaused) {
                timeRemainingMillis = PreferencesHelper.getTimeRemainingMillis(context)
            } else if (remindersEnabled && isPaused) {
                // When paused, show the saved remaining time
                val savedTime = PreferencesHelper.getPausedRemainingTime(context)
                if (savedTime > 0) {
                    timeRemainingMillis = savedTime
                }
            } else if (!remindersEnabled) {
                // Show default 20 minutes when not running
                timeRemainingMillis = PreferencesHelper.DEFAULT_REMINDER_INTERVAL * 60 * 1000L
            }
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
            
            // Countdown Timer Card - Always visible
            CountdownTimerCard(
                timeRemainingMillis = timeRemainingMillis,
                enabled = remindersEnabled,
                isPaused = isPaused,
                onTogglePause = {
                    if (remindersEnabled) {
                        if (isPaused) {
                            // Resume with preserved remaining time
                            val savedRemainingTime = PreferencesHelper.getPausedRemainingTime(context)
                            if (savedRemainingTime > 0) {
                                val intervalMillis = PreferencesHelper.getReminderInterval(context) * 60 * 1000L
                                val newLastNotificationTime = System.currentTimeMillis() - (intervalMillis - savedRemainingTime)
                                PreferencesHelper.setLastNotificationTime(context, newLastNotificationTime)
                            } else {
                                PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                            }
                            PreferencesHelper.setPauseUntil(context, 0)
                            PreferencesHelper.setPausedRemainingTime(context, 0)
                            isPaused = false
                            // Restart notification service
                            TimerNotificationService.stopService(context)
                            TimerNotificationService.startService(context)
                            // Update widgets
                            EyeCareWidgetProvider.updateAllWidgets(context)
                        } else {
                            // Pause - save current remaining time
                            PreferencesHelper.setPausedRemainingTime(context, timeRemainingMillis)
                            val pauseUntil = System.currentTimeMillis() + (1 * 60 * 60 * 1000L)
                            PreferencesHelper.setPauseUntil(context, pauseUntil)
                            isPaused = true
                            // Update widgets
                            EyeCareWidgetProvider.updateAllWidgets(context)
                        }
                    }
                },
                onStop = {
                    if (remindersEnabled) {
                        // Stop completely - reset timer to full interval
                        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                        PreferencesHelper.setPauseUntil(context, 0)
                        PreferencesHelper.setPausedRemainingTime(context, 0)
                        isPaused = false
                        timeRemainingMillis = PreferencesHelper.getTimeRemainingMillis(context)
                        // Restart notification service
                        TimerNotificationService.stopService(context)
                        TimerNotificationService.startService(context)
                        // Update widgets
                        EyeCareWidgetProvider.updateAllWidgets(context)
                    }
                },
                onClose = {
                    // Fully disable reminders
                    remindersEnabled = false
                    PreferencesHelper.setRemindersEnabled(context, false)
                    cancelEyeCareReminders(context)
                    PreferencesHelper.setLastNotificationTime(context, 0)
                    PreferencesHelper.setPauseUntil(context, 0)
                    PreferencesHelper.setPausedRemainingTime(context, 0)
                    isPaused = false
                    TimerNotificationService.stopService(context)
                    // Update widgets
                    EyeCareWidgetProvider.updateAllWidgets(context)
                }
            )
            
            // Paused Status Card
            if (isPaused) {
                PausedStatusCard(
                    onResume = {
                        // Resume with preserved remaining time
                        val savedRemainingTime = PreferencesHelper.getPausedRemainingTime(context)
                        if (savedRemainingTime > 0) {
                            // Calculate new lastNotificationTime to preserve remaining time
                            val intervalMillis = PreferencesHelper.getReminderInterval(context) * 60 * 1000L
                            val newLastNotificationTime = System.currentTimeMillis() - (intervalMillis - savedRemainingTime)
                            PreferencesHelper.setLastNotificationTime(context, newLastNotificationTime)
                        } else {
                            // Fallback: just reset to current time
                            PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                        }
                        PreferencesHelper.setPauseUntil(context, 0)
                        PreferencesHelper.setPausedRemainingTime(context, 0)
                        isPaused = false
                        // Restart notification service
                        TimerNotificationService.stopService(context)
                        TimerNotificationService.startService(context)
                        // Update widgets
                        EyeCareWidgetProvider.updateAllWidgets(context)
                    }
                )
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
                    // Update widgets
                    EyeCareWidgetProvider.updateAllWidgets(context)
                },
                onIntervalChange = { newInterval ->
                    reminderInterval = newInterval
                    PreferencesHelper.setReminderInterval(context, newInterval.roundToInt())
                    if (remindersEnabled) {
                        scheduleEyeCareReminders(context, newInterval.roundToInt())
                        PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                        // Update widgets to show new timer
                        EyeCareWidgetProvider.updateAllWidgets(context)
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
            
            // Eye Exercises Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "👁️ Eye Exercises",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "${PreferencesHelper.getExercisesToday(context)} exercises today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    Text(
                        text = "Strengthen and relax your eyes with guided exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Button(
                        onClick = { showExerciseSelection = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Exercise")
                    }
                }
            }
            
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
    
    // Exercise Selection Dialog
    if (showExerciseSelection) {
        ExerciseSelectionDialog(
            onExerciseSelected = { exercise ->
                selectedExercise = exercise
                showExerciseSelection = false
            },
            onDismiss = { showExerciseSelection = false }
        )
    }
    
    // Exercise Execution
    selectedExercise?.let { exercise ->
        ExerciseExecutionScreen(
            exercise = exercise,
            onComplete = {
                selectedExercise = null
                showExerciseCompletion = true
            },
            onSkip = {
                selectedExercise = null
            }
        )
    }
    
    // Exercise Completion Dialog
    if (showExerciseCompletion) {
        selectedExercise?.let { exercise ->
            ExerciseCompletionDialog(
                exercise = exercise,
                onDismiss = {
                    showExerciseCompletion = false
                    selectedExercise = null
                }
            )
        }
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
fun StatsScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    
    // Track stats view for achievements
    LaunchedEffect(Unit) {
        PreferencesHelper.incrementStatsViewed(context)
    }
    
    // Get statistics
    val breaksToday = remember { PreferencesHelper.getBreaksToday(context) }
    val totalBreaks = remember { PreferencesHelper.getTotalBreaks(context) }
    val currentStreak = remember { PreferencesHelper.getCurrentStreak(context) }
    val bestStreak = remember { PreferencesHelper.getBestStreak(context) }
    val averageBreaks = remember { PreferencesHelper.getAverageBreaksPerDay(context) }
    val dailyHistory = remember { PreferencesHelper.getDailyHistory(context) }
    
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
                    text = "📊 Your Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Track your eye care journey!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Today's Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📅 Today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Breaks Taken:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$breaksToday",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Streak Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔥 Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$currentStreak days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Best Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$bestStreak days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                if (currentStreak > 0) {
                    Text(
                        text = "🎉 Keep it up! You're building a healthy habit!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        
        // Overall Stats Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📈 Overall Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatsRow(label = "Total Breaks", value = "$totalBreaks", icon = "✅")
                StatsRow(
                    label = "Average (7 days)", 
                    value = String.format("%.1f", averageBreaks),
                    icon = "📊"
                )
                StatsRow(label = "Current Streak", value = "$currentStreak days", icon = "🔥")
            }
        }
        
        // Exercise Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "👁️ Exercise Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                val totalExercises = PreferencesHelper.getTotalExercises(context)
                val exercisesToday = PreferencesHelper.getExercisesToday(context)
                val exerciseHistory = PreferencesHelper.getExerciseHistory(context)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Exercises",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$totalExercises",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "$exercisesToday",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                if (exerciseHistory.isNotEmpty()) {
                    Divider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                    Text(
                        text = "Most Practiced:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    exerciseHistory.entries.sortedByDescending { it.value }.take(3).forEach { (type, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = type.capitalize(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "$count times",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Weekly History Card
        if (dailyHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📆 Last 7 Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val sortedHistory = dailyHistory.entries
                        .sortedByDescending { it.key }
                        .take(7)
                    
                    sortedHistory.forEach { (date, breaks) ->
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                        val displayDate = dateFormat.format(parsedDate ?: Date())
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(minOf(breaks, 10)) {
                                    Text(text = "✓", color = MaterialTheme.colorScheme.primary)
                                }
                                if (breaks > 10) {
                                    Text(
                                        text = " +${breaks - 10}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Achievements Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏆 Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                val achievements = remember { getAllAchievements() }
                val unlockedAchievements = remember { PreferencesHelper.getUnlockedAchievements(context) }
                
                achievements.forEach { achievement ->
                    val currentProgress = achievement.getCurrentProgress(context)
                    val isUnlocked = unlockedAchievements.contains(achievement.id) || 
                                    currentProgress >= achievement.requiredProgress
                    
                    // Auto-unlock if criteria met
                    if (!isUnlocked && currentProgress >= achievement.requiredProgress) {
                        PreferencesHelper.unlockAchievement(context, achievement.id)
                    }
                    
                    AchievementCard(
                        achievement = achievement,
                        currentProgress = currentProgress,
                        isUnlocked = isUnlocked
                    )
                }
            }
        }
        
        // Motivational Message
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        currentStreak >= 30 -> "🏆 Amazing! 30+ day streak!"
                        currentStreak >= 7 -> "⭐ Great job! Keep it going!"
                        currentStreak >= 3 -> "💪 You're building a habit!"
                        totalBreaks >= 100 -> "🎉 Century club member!"
                        totalBreaks >= 50 -> "👏 Halfway to 100 breaks!"
                        else -> "🌱 Every break counts!"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your eyes thank you for every break! 👁️💚",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun StatsRow(label: String, value: String, icon: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement,
    currentProgress: Int,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                Text(
                    text = if (isUnlocked) achievement.icon else "🔒",
                    style = MaterialTheme.typography.headlineMedium
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                    
                    if (!isUnlocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (currentProgress.toFloat() / achievement.requiredProgress).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$currentProgress / ${achievement.requiredProgress}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            if (isUnlocked) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SmartBreaksSettings() {
    val context = LocalContext.current
    var smartBreaksEnabled by remember { mutableStateOf(PreferencesHelper.isSmartBreaksEnabled(context)) }
    var showWorkHoursPicker by remember { mutableStateOf(false) }
    var showQuietHoursPicker by remember { mutableStateOf(false) }
    var isSelectingStart by remember { mutableStateOf(true) }
    
    val workStart = remember { mutableStateOf(PreferencesHelper.getWorkHoursStart(context)) }
    val workEnd = remember { mutableStateOf(PreferencesHelper.getWorkHoursEnd(context)) }
    val quietStart = remember { mutableStateOf(PreferencesHelper.getQuietHoursStart(context)) }
    val quietEnd = remember { mutableStateOf(PreferencesHelper.getQuietHoursEnd(context)) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Smart Breaks Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (smartBreaksEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Break Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (smartBreaksEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "Automatically pause during quiet hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (smartBreaksEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                    )
                }
                Switch(
                    checked = smartBreaksEnabled,
                    onCheckedChange = {
                        smartBreaksEnabled = it
                        PreferencesHelper.setSmartBreaksEnabled(context, it)
                    }
                )
            }
        }
        
        // Work Hours Card
        if (smartBreaksEnabled) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWorkHoursPicker = true }
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
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "💼", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Work Hours",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatHour(workStart.value)} - ${formatHour(workEnd.value)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
            
            // Quiet Hours Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showQuietHoursPicker = true }
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
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🌙", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Quiet Hours",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatHour(quietStart.value)} - ${formatHour(quietEnd.value)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No reminders during this time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
    
    // Work Hours Picker Dialog
    if (showWorkHoursPicker) {
        HoursRangePickerDialog(
            title = "Set Work Hours",
            startHour = workStart.value,
            endHour = workEnd.value,
            onDismiss = { showWorkHoursPicker = false },
            onConfirm = { start, end ->
                workStart.value = start
                workEnd.value = end
                PreferencesHelper.setWorkHoursStart(context, start)
                PreferencesHelper.setWorkHoursEnd(context, end)
                showWorkHoursPicker = false
            }
        )
    }
    
    // Quiet Hours Picker Dialog
    if (showQuietHoursPicker) {
        HoursRangePickerDialog(
            title = "Set Quiet Hours",
            startHour = quietStart.value,
            endHour = quietEnd.value,
            onDismiss = { showQuietHoursPicker = false },
            onConfirm = { start, end ->
                quietStart.value = start
                quietEnd.value = end
                PreferencesHelper.setQuietHoursStart(context, start)
                PreferencesHelper.setQuietHoursEnd(context, end)
                showQuietHoursPicker = false
            }
        )
    }
}

@Composable
fun HoursRangePickerDialog(
    title: String,
    startHour: Int,
    endHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedStart by remember { mutableStateOf(startHour) }
    var selectedEnd by remember { mutableStateOf(endHour) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Start Hour
                Column {
                    Text(
                        text = "From:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HourSelector(
                        selectedHour = selectedStart,
                        onHourSelected = { selectedStart = it }
                    )
                }
                
                // End Hour
                Column {
                    Text(
                        text = "To:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HourSelector(
                        selectedHour = selectedEnd,
                        onHourSelected = { selectedEnd = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStart, selectedEnd) }) {
                Text("Set Hours")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HourSelector(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (0..23).forEach { hour ->
            FilterChip(
                selected = hour == selectedHour,
                onClick = { onHourSelected(hour) },
                label = {
                    Text(
                        text = formatHour(hour),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when (hour) {
        0 -> 12
        in 1..12 -> hour
        else -> hour - 12
    }
    return "$displayHour:00 $period"
}

@Composable
fun NotificationManagementSettings() {
    val context = LocalContext.current
    var nonDismissible by remember { mutableStateOf(PreferencesHelper.isNonDismissible(context)) }
    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Non-Dismissible Notification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (nonDismissible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "🔒", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Non-Dismissible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Prevent accidental notification swipe",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = nonDismissible,
                    onCheckedChange = {
                        nonDismissible = it
                        PreferencesHelper.setNonDismissible(context, it)
                        // Restart service to apply changes
                        if (PreferencesHelper.areRemindersEnabled(context)) {
                            TimerNotificationService.stopService(context)
                            TimerNotificationService.startService(context)
                        }
                    }
                )
            }
        }
        
        // Sound Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Play notification sound
                        try {
                            val notification = android.app.Notification.Builder(context, "eye_care_timer")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("Sound Preview")
                                .build()
                            
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            notificationManager.notify(9999, notification)
                            
                            // Cancel the preview notification after a moment
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                notificationManager.cancel(9999)
                            }, 1000)
                            
                            Toast.makeText(context, "🔔 Sound preview played", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sound preview not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "🔊", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Test Notification Sound",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Preview your notification sound",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Streak Protection Info Card
        val currentStreak = PreferencesHelper.getCurrentStreak(context)
        if (currentStreak > 3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Streak Protection Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Your $currentStreak-day streak is protected! Closing notification requires confirmation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
        
        // Close Confirmation Dialog
        if (showCloseConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCloseConfirmDialog = false },
                title = { Text("⚠️ Close Reminders?") },
                text = {
                    Text("You have a ${currentStreak}-day streak! Are you sure you want to close the notification and stop reminders?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Actually close
                            PreferencesHelper.setRemindersEnabled(context, false)
                            TimerNotificationService.stopService(context)
                            showCloseConfirmDialog = false
                            Toast.makeText(context, "Reminders stopped", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Yes, Close")
                    }
                },
                dismissButton = {
                    Button(onClick = { showCloseConfirmDialog = false }) {
                        Text("Keep Streak!")
                    }
                }
            )
        }
    }
}

@Composable
fun BreakRulesSettings() {
    val context = LocalContext.current
    val presets = remember { getBreakRulePresets() }
    var selectedPresetId by remember { mutableStateOf(PreferencesHelper.getSelectedPreset(context)) }
    var customInterval by remember { mutableStateOf(PreferencesHelper.getReminderInterval(context).toFloat()) }
    var customDuration by remember { mutableStateOf(PreferencesHelper.getBreakDuration(context)) }
    var showCustomDialog by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preset Cards
        presets.forEach { preset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (preset.id == "custom") {
                            showCustomDialog = true
                        } else {
                            selectedPresetId = preset.id
                            PreferencesHelper.setSelectedPreset(context, preset.id)
                            PreferencesHelper.setReminderInterval(context, preset.intervalMinutes)
                            PreferencesHelper.setBreakDuration(context, preset.durationSeconds)
                            
                            // Restart timer with new settings
                            if (PreferencesHelper.areRemindersEnabled(context)) {
                                PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                                TimerNotificationService.stopService(context)
                                TimerNotificationService.startService(context)
                                EyeCareWidgetProvider.updateAllWidgets(context)
                            }
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedPresetId == preset.id) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (selectedPresetId == preset.id) 6.dp else 2.dp
                )
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
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = preset.icon, fontSize = 28.sp)
                        Column {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (preset.id != "custom") {
                                Text(
                                    text = "${preset.intervalMinutes} min • ${preset.durationSeconds} sec break",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (selectedPresetId == "custom") {
                                Text(
                                    text = "${customInterval.toInt()} min • ${customDuration} sec break",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (selectedPresetId == preset.id) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Custom Settings Dialog
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("⚙️ Custom Break Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Interval Slider
                    Column {
                        Text(
                            text = "Break Interval: ${customInterval.toInt()} minutes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = customInterval,
                            onValueChange = { customInterval = it },
                            valueRange = 5f..60f,
                            steps = 54
                        )
                        Text(
                            text = "How often to take breaks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // Duration Selector
                    Column {
                        Text(
                            text = "Break Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 15, 20, 30, 60).forEach { duration ->
                                FilterChip(
                                    selected = customDuration == duration,
                                    onClick = { customDuration = duration },
                                    label = {
                                        Text(
                                            text = if (duration < 60) "${duration}s" else "1m",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            text = "How long each break should last",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedPresetId = "custom"
                        PreferencesHelper.setSelectedPreset(context, "custom")
                        PreferencesHelper.setReminderInterval(context, customInterval.toInt())
                        PreferencesHelper.setBreakDuration(context, customDuration)
                        
                        // Restart timer with new settings
                        if (PreferencesHelper.areRemindersEnabled(context)) {
                            PreferencesHelper.setLastNotificationTime(context, System.currentTimeMillis())
                            TimerNotificationService.stopService(context)
                            TimerNotificationService.startService(context)
                            EyeCareWidgetProvider.updateAllWidgets(context)
                        }
                        
                        showCustomDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HealthRemindersSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncManager = remember { SyncManager.getInstance(context) }
    var waterEnabled by remember { mutableStateOf(PreferencesHelper.isWaterReminderEnabled(context)) }
    var postureEnabled by remember { mutableStateOf(PreferencesHelper.isPostureReminderEnabled(context)) }
    var stretchEnabled by remember { mutableStateOf(PreferencesHelper.isStretchReminderEnabled(context)) }
    var brightnessEnabled by remember { mutableStateOf(PreferencesHelper.isBrightnessCheckEnabled(context)) }
    var combinedEnabled by remember { mutableStateOf(PreferencesHelper.isCombinedNotificationsEnabled(context)) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Water Reminder Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "💧 Water Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every 2 hours • Stay hydrated",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = waterEnabled,
                        onCheckedChange = { enabled ->
                            waterEnabled = enabled
                            PreferencesHelper.setWaterReminderEnabled(context, enabled)
                            HealthReminderWorker.scheduleHealthReminders(context)
                            scope.launch {
                                syncManager.uploadSettings()
                            }
                        }
                    )
                }
            }
        }
        
        // Posture Check Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🪑 Posture Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every hour • Sit up straight",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = postureEnabled,
                        onCheckedChange = { enabled ->
                            postureEnabled = enabled
                            PreferencesHelper.setPostureReminderEnabled(context, enabled)
                            HealthReminderWorker.scheduleHealthReminders(context)
                            scope.launch {
                                syncManager.uploadSettings()
                            }
                        }
                    )
                }
            }
        }
        
        // Stand & Stretch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🚶 Stand & Stretch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every 90 minutes • Move around",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = stretchEnabled,
                        onCheckedChange = { enabled ->
                            stretchEnabled = enabled
                            PreferencesHelper.setStretchReminderEnabled(context, enabled)
                            HealthReminderWorker.scheduleHealthReminders(context)
                            scope.launch {
                                syncManager.uploadSettings()
                            }
                        }
                    )
                }
            }
        }
        
        // Screen Brightness Check Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "☀️ Screen Brightness Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every 30 minutes • Adjust screen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = brightnessEnabled,
                        onCheckedChange = { enabled ->
                            brightnessEnabled = enabled
                            PreferencesHelper.setBrightnessCheckEnabled(context, enabled)
                            HealthReminderWorker.scheduleHealthReminders(context)
                            scope.launch {
                                syncManager.uploadSettings()
                            }
                        }
                    )
                }
            }
        }
        
        // Combined Notifications Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "📱 Combined Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Group all health reminders together",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Switch(
                        checked = combinedEnabled,
                        onCheckedChange = { enabled ->
                            combinedEnabled = enabled
                            PreferencesHelper.setCombinedNotificationsEnabled(context, enabled)
                            HealthReminderWorker.scheduleHealthReminders(context)
                            scope.launch {
                                syncManager.uploadSettings()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MultiDeviceSyncSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncManager = remember { SyncManager.getInstance(context) }
    
    var isSignedIn by remember { mutableStateOf(syncManager.isSignedIn()) }
    var userEmail by remember { mutableStateOf(syncManager.getCurrentUserEmail()) }
    var syncEnabled by remember { mutableStateOf(syncManager.isSyncEnabled()) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sync Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSignedIn && syncEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSignedIn) "✓ Signed In" else "☁️ Cloud Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSignedIn) {
                                userEmail ?: "Unknown"
                            } else {
                                "Sign in to sync across devices"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isSignedIn) {
                        IconButton(
                            onClick = { showSignOutDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out"
                            )
                        }
                    }
                }
                
                if (!isSignedIn) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Google Sign-In requires google-services.json configuration. Please add your Firebase project configuration.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                }
            }
        }
        
        // Sync Features Card (only show when signed in)
        if (isSignedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enable Sync Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Automatically sync data across devices",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { enabled ->
                                syncEnabled = enabled
                                syncManager.setSyncEnabled(enabled)
                                if (enabled) {
                                    Toast.makeText(context, "Sync enabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    
                    if (syncEnabled) {
                        HorizontalDivider()
                        
                        // What's Synced
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "What's synced:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            SyncFeatureItem("⚙️", "All Settings")
                            SyncFeatureItem("📊", "Statistics & Streaks")
                            SyncFeatureItem("🏆", "Achievements")
                            SyncFeatureItem("⏸️", "Timer State (pause/resume)")
                        }
                        
                        HorizontalDivider()
                        
                        // Manual Sync Button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    syncManager.uploadAllData()
                                    delay(1000)
                                    isSyncing = false
                                    Toast.makeText(context, "Sync complete", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null
                )
            },
            title = { Text("Sign Out?") },
            text = { Text("Your data will remain on this device, but will stop syncing across devices.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            syncManager.signOut()
                            isSignedIn = false
                            userEmail = null
                            syncEnabled = false
                            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                        }
                        showSignOutDialog = false
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SyncFeatureItem(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 16.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        
        // Info About Current Break Rule
        val selectedPreset = PreferencesHelper.getSelectedPreset(context)
        val currentInterval = PreferencesHelper.getReminderInterval(context)
        val currentDuration = PreferencesHelper.getBreakDuration(context)
        val presetInfo = getBreakRulePresets().find { it.id == selectedPreset }
        
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = presetInfo?.icon ?: "⏰", fontSize = 24.sp)
                    Text(
                        text = presetInfo?.name ?: "Your Break Rule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Every $currentInterval minutes, take a $currentDuration-second break to look at something 20 feet away. This simple rule helps reduce eye strain, prevent headaches, and maintain healthy vision during screen time.",
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
        
        // Smart Breaks Settings
        Text(
            text = "🧠 Smart Breaks",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        SmartBreaksSettings()
        
        Text(
            text = "⏱️ Break Rules",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        BreakRulesSettings()
        
        Text(
            text = "🏥 Health & Wellness",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        HealthRemindersSettings()
        
        Text(
            text = "☁️ Multi-Device Sync",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        MultiDeviceSyncSettings()
        
        Text(
            text = "🔔 Notification Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        NotificationManagementSettings()
        
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
    val currentHour24 = currentTime.get(Calendar.HOUR_OF_DAY)
    val initialHour12 = if (currentHour24 == 0) 12 else if (currentHour24 > 12) currentHour24 - 12 else currentHour24
    val initialAmPm = if (currentHour24 < 12) "AM" else "PM"
    
    var selectedHour by remember { mutableStateOf(initialHour12) }
    var selectedMinute by remember { mutableStateOf(currentTime.get(Calendar.MINUTE)) }
    var selectedAmPm by remember { mutableStateOf(initialAmPm) }
    
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
                    // Hour selector (1-12)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { 
                            selectedHour = if (selectedHour == 12) 1 else selectedHour + 1
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, "Increase hour")
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { 
                            selectedHour = if (selectedHour == 1) 12 else selectedHour - 1
                        }) {
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
                    // AM/PM selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { 
                            selectedAmPm = if (selectedAmPm == "AM") "PM" else "AM"
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, "Toggle AM/PM")
                        }
                        Text(
                            text = selectedAmPm,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { 
                            selectedAmPm = if (selectedAmPm == "AM") "PM" else "AM"
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, "Toggle AM/PM")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                // Convert 12-hour to 24-hour format
                val hour24 = when {
                    selectedAmPm == "AM" && selectedHour == 12 -> 0
                    selectedAmPm == "PM" && selectedHour != 12 -> selectedHour + 12
                    else -> selectedHour
                }
                onTimeSelected(hour24, selectedMinute)
            }) {
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

// ========== EXERCISE UI SCREENS ==========

@Composable
fun ExerciseSelectionDialog(
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "👁️ Choose an Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rest your eyes with a guided exercise:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                getAllExercises().forEach { exercise ->
                    ExerciseSelectionCard(
                        exercise = exercise,
                        onClick = { onExerciseSelected(exercise) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip for Now")
            }
        }
    )
}

@Composable
fun ExerciseSelectionCard(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exercise.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "${exercise.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Start",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ExerciseExecutionScreen(
    exercise: Exercise,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var timeRemaining by remember { mutableStateOf(exercise.durationSeconds) }
    var isPaused by remember { mutableStateOf(false) }
    
    // Timer countdown
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
            }
            if (timeRemaining == 0) {
                onComplete()
            }
        }
    }
    
    // Animation for follow the dot
    var dotOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(0f, 0f)) }
    
    if (exercise.type == ExerciseType.FOLLOW_DOT) {
        LaunchedEffect(timeRemaining) {
            while (timeRemaining > 0 && !isPaused) {
                val random = java.util.Random()
                dotOffset = androidx.compose.ui.geometry.Offset(
                    random.nextFloat() * 200f - 100f,
                    random.nextFloat() * 200f - 100f
                )
                delay(2000) // Move every 2 seconds
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = exercise.icon,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "⏱️ $timeRemaining seconds",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Instructions/Animation Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (exercise.type) {
                    ExerciseType.FOLLOW_DOT -> {
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(dotOffset.x.dp, dotOffset.y.dp)
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Follow the moving dot with your eyes",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            exercise.instructions.forEachIndexed { index, instruction ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = if (index <= currentStep) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                color = if (index <= currentStep) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = instruction,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Controls
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (exercise.type != ExerciseType.FOLLOW_DOT && currentStep < exercise.instructions.size - 1) {
                Button(
                    onClick = { currentStep++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Next Step")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }
                Button(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isPaused) "Resume" else "Pause")
                }
            }
        }
    }
}

@Composable
fun ExerciseCompletionDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        PreferencesHelper.recordExerciseCompleted(context, exercise.id)
        checkAndNotifyAchievements(context)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(text = "🎉", style = MaterialTheme.typography.displayLarge)
        },
        title = {
            Text(
                text = "Exercise Complete!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Great job! You completed the ${exercise.title}.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your eyes feel refreshed! 👁️💚",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
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
        // Track sleep calculator usage for achievements
        PreferencesHelper.incrementSleepCalcUsed(context)
        
        // Primary method: Use AlarmClock intent to open alarm app with pre-filled data
        // This is the most reliable method that works on all Android versions
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, wakeUpTime.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, wakeUpTime.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_MESSAGE, "$cycles Sleep Cycles - Eye Care")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Show the alarm UI so user can confirm
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        // Check if there's an app that can handle this intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            
            // Show confirmation
            android.widget.Toast.makeText(
                context,
                "⏰ Setting alarm for ${String.format("%02d:%02d", wakeUpTime.get(Calendar.HOUR_OF_DAY), wakeUpTime.get(Calendar.MINUTE))} ($cycles cycles)",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } else {
            // Fallback: Try to open default clock app
            val clockIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Try common clock app package names
            val clockPackages = listOf(
                "com.google.android.deskclock",
                "com.android.deskclock",
                "com.samsung.android.app.clockpackage",
                "com.sec.android.app.clockpackage"
            )
            
            var opened = false
            for (packageName in clockPackages) {
                try {
                    clockIntent.setPackage(packageName)
                    if (clockIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(clockIntent)
                        opened = true
                        android.widget.Toast.makeText(
                            context,
                            "⏰ Opening clock app - Please set alarm for ${String.format("%02d:%02d", wakeUpTime.get(Calendar.HOUR_OF_DAY), wakeUpTime.get(Calendar.MINUTE))}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        break
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            if (!opened) {
                android.widget.Toast.makeText(
                    context,
                    "❌ Unable to open clock app. Please set alarm manually for ${String.format("%02d:%02d", wakeUpTime.get(Calendar.HOUR_OF_DAY), wakeUpTime.get(Calendar.MINUTE))}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "❌ Error setting alarm: ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CountdownTimerCard(
    timeRemainingMillis: Long, 
    enabled: Boolean,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    val minutes = (timeRemainingMillis / 1000 / 60).toInt()
    val seconds = ((timeRemainingMillis / 1000) % 60).toInt()
    val totalSeconds = (timeRemainingMillis / 1000).toFloat()
    val context = LocalContext.current
    val intervalSeconds = PreferencesHelper.getReminderInterval(context) * 60f
    val progress = if (enabled) 1f - (totalSeconds / intervalSeconds) else 0f
    
    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (!enabled) "Ready to Focus"
                                   else if (isPaused) "Paused" 
                                   else "Focus Time",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!enabled) "Start your eye care routine"
                                   else if (isPaused) "Timer paused" 
                                   else "Next break in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (enabled) {
                        FilledIconButton(
                            onClick = onClose,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // Timer Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .padding(16.dp)
                ) {
                    // Background circle
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 16.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    // Animated progress circle
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 16.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                        color = if (enabled) {
                            if (isPaused) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        strokeCap = StrokeCap.Round
                    )
                    // Timer text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 56.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (enabled && !isPaused) "minutes" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    if (enabled) {
                        // Reset button
                        FilledTonalIconButton(
                            onClick = onStop,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // Play/Pause button (larger, primary)
                        FilledIconButton(
                            onClick = onTogglePause,
                            modifier = Modifier.size(72.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isPaused) 
                                    MaterialTheme.colorScheme.tertiary 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Settings,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "👁️",
                            fontSize = 48.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PausedStatusCard(onResume: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResume() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏸️",
                    fontSize = 32.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reminders Paused",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap anywhere to resume",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Resume",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Control Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (enabled) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (enabled) 4.dp else 0.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (enabled)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👁️",
                                fontSize = 24.sp
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Eye Care Timer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (enabled) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (enabled) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Modern Toggle Button
                    FilledTonalButton(
                        onClick = { onToggle(!enabled) },
                        enabled = hasPermission,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (enabled) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (enabled)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = if (enabled) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (enabled) "ON" else "OFF",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            
                if (!hasPermission) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permission Needed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable notifications to get reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onRequestPermission,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Grant",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            
                if (enabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    // Interval Settings
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Break Interval",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = "$interval min",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
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
                
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    // Sound Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSoundToggle(!soundEnabled) }
                            .padding(vertical = 8.dp),
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
                
                // Pause Button (removed Reset - now in timer card)
                FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }
}

@Composable
fun BreakInstructionsCard() {
    val context = LocalContext.current
    val breakDuration = PreferencesHelper.getBreakDuration(context)
    val durationText = if (breakDuration >= 60) {
        "${breakDuration / 60} minute${if (breakDuration / 60 > 1) "s" else ""}"
    } else {
        "$breakDuration seconds"
    }
    
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
                text = "2️⃣ Keep looking for at least $durationText",
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
