package com.focuscrew.app.ui.apps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.model.InstalledAppItem
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.domain.usecase.GetInstalledAppsUseCase
import com.focuscrew.app.service.AntiPornDefenderManager
import com.focuscrew.app.service.BlockProfileManager
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.service.FocusCrewVpnService
import com.focuscrew.app.ui.timer.TimerManager

enum class AppFilterTab {
    ALL,
    BLOCKED_ONLY,
    COMMON
}

@Composable
fun AppPickerScreen(
    onDone: () -> Unit,
    onNavigateToFreedomVault: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blockedPackages by BlockerManager.blockedPackages.collectAsState()
    val isDopamineDetoxEnabled by AntiPornDefenderManager.isDefenderEnabled.collectAsState()

    val timerSnapshot by TimerManager.snapshot.collectAsState()
    val isTimerActive = timerSnapshot.status is TimerStatus.Running ||
            timerSnapshot.status is TimerStatus.Paused ||
            timerSnapshot.status is TimerStatus.InBreak

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
        }
    }

    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showUnblockGuiltDialog by remember { mutableStateOf(false) }
    var attemptedUnblockAppName by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(AppFilterTab.ALL) }

    val commonPackages = remember {
        setOf(
            "com.google.android.youtube",
            "com.android.chrome",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.twitter.android",
            "com.facebook.katana",
            "com.reddit.frontpage",
            "com.snapchat.android",
            "com.netflix.mediaclient"
        )
    }

    // Load installed launchable apps asynchronously on entry
    LaunchedEffect(Unit) {
        val useCase = GetInstalledAppsUseCase(context)
        installedApps = useCase.execute()
        isLoading = false
    }

    val filteredApps = remember(installedApps, searchQuery, blockedPackages, selectedFilterTab) {
        val query = searchQuery.trim().lowercase()
        installedApps.filter { app ->
            val matchesQuery = query.isEmpty() ||
                app.appName.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)

            val matchesTab = when (selectedFilterTab) {
                AppFilterTab.ALL -> true
                AppFilterTab.BLOCKED_ONLY -> blockedPackages.contains(app.packageName)
                AppFilterTab.COMMON -> commonPackages.contains(app.packageName)
            }

            matchesQuery && matchesTab
        }
    }

    if (showUnblockGuiltDialog) {
        AlertDialog(
            onDismissRequest = { showUnblockGuiltDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚨", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHEATING DETECTED!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DURING THE TIMER YOU CANNOT UNBLOCK APPS!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "STAY FOCUSED — YOU ARE CHEATING YOURSELF! 🚨",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "You set this timer to conquer your goals and build iron self-discipline.\n\n" +
                                "Trying to unblock $attemptedUnblockAppName right now is weakness taking over. " +
                                "Are you really going to surrender your ambition and self-respect for a cheap dopamine rush?\n\n" +
                                "Every time you look for a loophole, you train yourself to quit. " +
                                "Your future self is watching you right now.\n\n" +
                                "Face the discomfort, stop cheating, and GET BACK TO WORK! 💪",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showUnblockGuiltDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I'll Stop Cheating & Stay Focused 💪", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. DOPAMINE DETOX BLOCKER (Clean, Minimalist Bar)
            val toggleDopamineDetox: (Boolean) -> Unit = { newState ->
                AntiPornDefenderManager.setDefenderEnabled(newState)
                AntiPornDefenderManager.setSafeSearchEnforced(newState)
                FocusCrewVpnService.setVpnEnabled(newState)
                if (newState) {
                    val prepare = android.net.VpnService.prepare(context)
                    if (prepare == null) {
                        val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
                        context.startService(vpnIntent)
                    } else {
                        vpnPermissionLauncher.launch(prepare)
                    }
                } else {
                    val vpnIntent = Intent(context, FocusCrewVpnService::class.java).apply {
                        action = "STOP_VPN"
                    }
                    context.startService(vpnIntent)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDopamineDetoxEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isDopamineDetoxEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable {
                        toggleDopamineDetox(!isDopamineDetoxEnabled)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isDopamineDetoxEnabled) "🛡️" else "⚪",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Dopamine Detox Blocker",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Switch(
                        checked = isDopamineDetoxEnabled,
                        onCheckedChange = { newState ->
                            toggleDopamineDetox(newState)
                        },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. FULL-WIDTH CLEAN SEARCH BAR (Zero clutter, zero duplicate counters)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search apps...",
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. CLEAN FILTER TABS (Simple, easy to read, no clutter)
        // 3. CLEAN 2-TAB FILTER SELECTOR (Simple, balanced, zero clutter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAllSelected = selectedFilterTab == AppFilterTab.ALL
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = if (!isAllSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedFilterTab = AppFilterTab.ALL }
            ) {
                Text(
                    text = "All Apps (${installedApps.size})",
                    fontSize = 12.5.sp,
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            val isBlockedSelected = selectedFilterTab == AppFilterTab.BLOCKED_ONLY
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isBlockedSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (isBlockedSelected) MaterialTheme.colorScheme.error
                    else if (blockedPackages.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedFilterTab = AppFilterTab.BLOCKED_ONLY }
            ) {
                Text(
                    text = "Blocked (${blockedPackages.size})",
                    fontSize = 12.5.sp,
                    fontWeight = if (isBlockedSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isBlockedSelected) MaterialTheme.colorScheme.onError else if (blockedPackages.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. FULL-SCREEN LAZYCOLUMN (Apps immediately start with compact spacing)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Scanning installed apps on your phone...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
            ) {
                if (isTimerActive) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔒", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Focus Timer Active — Unblocking Locked!",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Apps cannot be unblocked until session completes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedFilterTab == AppFilterTab.BLOCKED_ONLY)
                                    "No apps are currently blocked.\nSwitch to 'All' or 'Distractions' to select apps to block!"
                                else
                                    "No apps found matching \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        val isBlocked = blockedPackages.contains(app.packageName)
                        AppPickerRow(
                            app = app,
                            isBlocked = isBlocked,
                            onToggle = {
                                if (isTimerActive && isBlocked) {
                                    attemptedUnblockAppName = app.appName
                                    showUnblockGuiltDialog = true
                                } else {
                                    BlockProfileManager.togglePackageInActiveProfile(app.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppPickerRow(
    app: InstalledAppItem,
    isBlocked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isBlocked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            1.dp,
            if (isBlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AppIcon(
                    drawable = app.icon,
                    appName = app.appName,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isBlocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "BLOCKED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isBlocked) "Blocked during focus" else app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isBlocked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
