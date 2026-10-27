package com.focuscrew.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.data.repository.FocusRepository
import com.focuscrew.app.service.BlockProfileManager
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.service.ReelCounterManager
import com.focuscrew.app.service.ReelNotificationManager
import com.focuscrew.app.ui.apps.AppPickerScreen
import com.focuscrew.app.ui.history.HistoryScreen
import com.focuscrew.app.ui.onboarding.OnboardingPermissionDialog
import com.focuscrew.app.ui.onboarding.OnboardingPreferences
import com.focuscrew.app.ui.reel.NeuroAuditScreen
import com.focuscrew.app.ui.reel.ReelCounterScreen
import com.focuscrew.app.ui.settings.SettingsScreen
import com.focuscrew.app.ui.theme.FocusCrewTheme
import com.focuscrew.app.ui.timer.TimerManager
import com.focuscrew.app.ui.timer.TimerScreen
import kotlinx.coroutines.launch

import com.focuscrew.app.service.FreedomVaultManager
import com.focuscrew.app.service.AntiPornDefenderManager
import com.focuscrew.app.service.FocusCrewVpnService
import com.focuscrew.app.service.TrialSubscriptionManager
import com.focuscrew.app.service.AppSecurityManager
import com.focuscrew.app.ui.freedom.FreedomVaultScreen
import com.focuscrew.app.ui.paywall.PaywallScreen
import com.focuscrew.app.ui.security.AppLockScreen
import com.focuscrew.app.data.repository.TaskAlarmRepository
import com.focuscrew.app.ui.taskalarm.TaskAlarmScreen

enum class AppScreen {
    TIMER,
    TASK_ALARM,
    REEL_COUNTER,
    APP_PICKER,
    HISTORY,
    SETTINGS,
    FREEDOM_VAULT
}

class MainActivity : ComponentActivity() {

    private val navScreenState = mutableStateOf(AppScreen.TIMER)
    private val navEventToken = mutableStateOf(0)
    private val forceNeuroAudit = mutableStateOf(false)
    private val forceUrgeBreaker = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("FocusCrew", "POST_NOTIFICATIONS granted=$isGranted")
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()

        handleIntentNavigation(intent)

        setContent {
            FocusCrewTheme {
                MainContent(
                    initialScreen = navScreenState.value,
                    navToken = navEventToken.value,
                    forceNeuroAudit = forceNeuroAudit.value,
                    forceUrgeBreaker = forceUrgeBreaker.value,
                    onScreenChange = { navScreenState.value = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentNavigation(intent)
    }

    private fun handleIntentNavigation(intent: Intent?) {
        val targetScreen = intent?.getStringExtra("navigate_to")
        if (targetScreen != null) {
            try {
                navScreenState.value = AppScreen.valueOf(targetScreen)
                navEventToken.value++
            } catch (_: Exception) {}
        }
        if (intent?.getBooleanExtra("exit_strict_mode", false) == true) {
            TimerManager.setStrictLockdown(false)
            TimerManager.stop(isEmergency = true)
            BlockerManager.setBlockingActive(false)
            navScreenState.value = AppScreen.TIMER
            navEventToken.value++
        }
        if (intent?.action == "com.focuscrew.app.ACTION_LAUNCH_URGE_BREAKER" ||
            intent?.getBooleanExtra("open_urge_breaker", false) == true) {
            navScreenState.value = AppScreen.FREEDOM_VAULT
            navEventToken.value++
            forceUrgeBreaker.value = true
        }
        if (intent?.getBooleanExtra("open_neuro_audit", false) == true) {
            forceNeuroAudit.value = true
        }
        val startDebugTimer = intent?.getBooleanExtra("start_preset_timer", false) ?: false
        if (startDebugTimer) {
            val planned = intent?.getIntExtra("planned_minutes", 20) ?: 20
            val elapsed = intent?.getIntExtra("elapsed_minutes", 4) ?: 4
            android.util.Log.d("FocusCrew", "handleIntentNavigation: preset timer planned=$planned, elapsed=$elapsed")
            TimerManager.stop()
            TimerManager.startSessionWithElapsed(
                plannedMinutes = planned,
                elapsedMinutes = elapsed
            )
            navScreenState.value = AppScreen.TIMER
            navEventToken.value++
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    initialScreen: AppScreen,
    navToken: Int = 0,
    forceNeuroAudit: Boolean = false,
    forceUrgeBreaker: Boolean = false,
    onScreenChange: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val database = remember { FocusDatabase.getInstance(context.applicationContext) }
    val repository = remember { FocusRepository(database) }
    val taskAlarmRepository = remember { TaskAlarmRepository(database.taskAlarmDao(), context.applicationContext) }
    val isServiceConnected by BlockerManager.isServiceConnected.collectAsState()
    val reelStats by ReelCounterManager.stats.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by rememberSaveable { mutableStateOf(initialScreen) }

    LaunchedEffect(initialScreen, navToken) {
        currentScreen = initialScreen
    }

    var showNeuroAuditOnboarding by remember {
        mutableStateOf(forceNeuroAudit)
    }

    LaunchedEffect(forceNeuroAudit) {
        if (forceNeuroAudit) {
            showNeuroAuditOnboarding = true
        }
    }

    val isAppLocked by AppSecurityManager.isLocked.collectAsState()
    val subState by TrialSubscriptionManager.subscriptionState.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }

    var showOnboarding by remember {
        mutableStateOf(!isServiceConnected && !OnboardingPreferences.hasCompleted(context))
    }

    LaunchedEffect(Unit) {
        BlockProfileManager.initialize(context)
        ReelCounterManager.initialize(context)
        FreedomVaultManager.initialize(context)
        AntiPornDefenderManager.initialize(context)
        AntiPornDefenderManager.setDefenderEnabled(true)
        AntiPornDefenderManager.setSafeSearchEnforced(true)
        FocusCrewVpnService.initialize(context)
        TrialSubscriptionManager.initialize(context)
        AppSecurityManager.initialize(context)
        ReelNotificationManager.createNotificationChannel(context)
        if (reelStats.isNotificationEnabled) {
            ReelNotificationManager.updateNotification(context, reelStats)
        }
    }

    LaunchedEffect(repository) {
        TimerManager.initialize(context)
        TimerManager.initRepository(repository)
    }

    // Dismiss onboarding once service is connected
    LaunchedEffect(isServiceConnected) {
        if (isServiceConnected) {
            OnboardingPreferences.markCompleted(context)
            showOnboarding = false
        }
    }

    // Security PIN Lock Gate
    if (isAppLocked) {
        AppLockScreen(onUnlocked = { AppSecurityManager.unlockApp() })
        return
    }

    if (showNeuroAuditOnboarding) {
        NeuroAuditScreen(
            onDismiss = {
                OnboardingPreferences.markNeuroAuditCompleted(context)
                showNeuroAuditOnboarding = false
            },
            onComplete = { targetDailyLimit ->
                ReelCounterManager.setDailyLimit(targetDailyLimit)
                OnboardingPreferences.markNeuroAuditCompleted(context)
                showNeuroAuditOnboarding = false
            }
        )
        return
    } else if (showOnboarding) {
        OnboardingPermissionDialog(
            onDismiss = { showOnboarding = false }
        )
    }

    BackHandler(enabled = currentScreen != AppScreen.TIMER || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = AppScreen.TIMER
            onScreenChange(AppScreen.TIMER)
        }
    }

    val screenTitle = when (currentScreen) {
        AppScreen.TIMER -> "FocusCrew"
        AppScreen.TASK_ALARM -> "Task Alarm"
        AppScreen.REEL_COUNTER -> "Reel Counter"
        AppScreen.APP_PICKER -> "App Blocker"
        AppScreen.HISTORY -> "Stats & Streaks"
        AppScreen.SETTINGS -> "Settings"
        AppScreen.FREEDOM_VAULT -> "Freedom Vault"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 2.dp,
                modifier = Modifier.width(300.dp)
            ) {
                // Side Menu Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛡️", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FocusCrew",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calm focus & anti-doomscroll",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Menu Item: Focus Timer
                NavigationDrawerItem(
                    label = { Text("Focus Timer", fontWeight = FontWeight.SemiBold) },
                    icon = { Text("⏱️", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.TIMER,
                    onClick = {
                        currentScreen = AppScreen.TIMER
                        onScreenChange(AppScreen.TIMER)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Menu Item: Task Alarm (User requested: TaskAlarm feature in FocusCrew)
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Task Alarm", fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "⏰ Alarms",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    icon = { Text("⏰", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.TASK_ALARM,
                    onClick = {
                        currentScreen = AppScreen.TASK_ALARM
                        onScreenChange(AppScreen.TASK_ALARM)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Menu Item: Reel Counter (Requested by user)
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reel Counter", fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${reelStats.todayTotal}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    icon = { Text("📱", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.REEL_COUNTER,
                    onClick = {
                        currentScreen = AppScreen.REEL_COUNTER
                        onScreenChange(AppScreen.REEL_COUNTER)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Menu Item: App Blocker
                NavigationDrawerItem(
                    label = { Text("App Blocker", fontWeight = FontWeight.SemiBold) },
                    icon = { Text("🛡️", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.APP_PICKER,
                    onClick = {
                        currentScreen = AppScreen.APP_PICKER
                        onScreenChange(AppScreen.APP_PICKER)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Menu Item: History & Streaks
                NavigationDrawerItem(
                    label = { Text("Stats & Streaks", fontWeight = FontWeight.SemiBold) },
                    icon = { Text("📊", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.HISTORY,
                    onClick = {
                        currentScreen = AppScreen.HISTORY
                        onScreenChange(AppScreen.HISTORY)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                // Menu Item: App Settings
                NavigationDrawerItem(
                    label = { Text("App Settings", fontWeight = FontWeight.SemiBold) },
                    icon = { Text("⚙️", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = {
                        currentScreen = AppScreen.SETTINGS
                        onScreenChange(AppScreen.SETTINGS)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Menu Item: Freedom Vault (Private & Isolated Recovery Section)
                NavigationDrawerItem(
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Freedom Vault", fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔒 Private",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    icon = { Text("🛡️", fontSize = 18.sp) },
                    selected = currentScreen == AppScreen.FREEDOM_VAULT,
                    onClick = {
                        currentScreen = AppScreen.FREEDOM_VAULT
                        onScreenChange(AppScreen.FREEDOM_VAULT)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )


                Spacer(modifier = Modifier.weight(1f))

                // Drawer Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "FocusCrew v1.0 • 100% Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("☰", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {

                        // Corner Settings Gear Button (User requested: "THERE WILL BE A SETTING BUTTON ALSO IN MY APP IN THE CORNER APP SETTING")
                        IconButton(onClick = {
                            currentScreen = AppScreen.SETTINGS
                            onScreenChange(AppScreen.SETTINGS)
                        }) {
                            Text("⚙️", fontSize = 20.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                val navItemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TIMER,
                        onClick = {
                            currentScreen = AppScreen.TIMER
                            onScreenChange(AppScreen.TIMER)
                        },
                        icon = { Text("⏱️", fontSize = 18.sp) },
                        label = { Text("Timer") },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.TASK_ALARM,
                        onClick = {
                            currentScreen = AppScreen.TASK_ALARM
                            onScreenChange(AppScreen.TASK_ALARM)
                        },
                        icon = { Text("⏰", fontSize = 18.sp) },
                        label = { Text("Tasks") },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.REEL_COUNTER,
                        onClick = {
                            currentScreen = AppScreen.REEL_COUNTER
                            onScreenChange(AppScreen.REEL_COUNTER)
                        },
                        icon = { Text("📱", fontSize = 18.sp) },
                        label = { Text("Reels") },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.APP_PICKER,
                        onClick = {
                            currentScreen = AppScreen.APP_PICKER
                            onScreenChange(AppScreen.APP_PICKER)
                        },
                        icon = { Text("🛡️", fontSize = 18.sp) },
                        label = { Text("Blocker") },
                        colors = navItemColors
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HISTORY,
                        onClick = {
                            currentScreen = AppScreen.HISTORY
                            onScreenChange(AppScreen.HISTORY)
                        },
                        icon = { Text("📊", fontSize = 18.sp) },
                        label = { Text("History") },
                        colors = navItemColors
                    )
                }
            }
        ) { innerPadding ->
            when (currentScreen) {
                AppScreen.TIMER -> {
                    TimerScreen(
                        onOpenAppPicker = {
                            currentScreen = AppScreen.APP_PICKER
                            onScreenChange(AppScreen.APP_PICKER)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                AppScreen.TASK_ALARM -> {
                    TaskAlarmScreen(
                        repository = taskAlarmRepository,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }


                AppScreen.REEL_COUNTER -> {
                    ReelCounterScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                AppScreen.APP_PICKER -> {
                    AppPickerScreen(
                        onDone = {
                            currentScreen = AppScreen.TIMER
                            onScreenChange(AppScreen.TIMER)
                        },
                        onNavigateToFreedomVault = {
                            currentScreen = AppScreen.FREEDOM_VAULT
                            onScreenChange(AppScreen.FREEDOM_VAULT)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        repository = repository,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        onNavigateToReelCounter = {
                            currentScreen = AppScreen.REEL_COUNTER
                            onScreenChange(AppScreen.REEL_COUNTER)
                        },
                        repository = repository,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                AppScreen.FREEDOM_VAULT -> {
                    FreedomVaultScreen(
                        initialOpenUrgeBreaker = forceUrgeBreaker,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
