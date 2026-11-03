package com.focuscrew.app.ui.freedom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.service.AntiPornDefenderManager
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.service.FocusCrewVpnService
import com.focuscrew.app.service.FreedomVaultManager
import com.focuscrew.app.service.UlyssesLockManager
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.CashmereCream
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.TerracottaClay
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder
import kotlinx.coroutines.delay

@Composable
fun TitanAntiPornFortressCard() {
    val context = LocalContext.current
    val isVpnRunning by FocusCrewVpnService.isVpnRunning.collectAsState()
    val isDefenderActive by AntiPornDefenderManager.isDefenderEnabled.collectAsState()
    val isAccessibilityConnected by BlockerManager.isServiceConnected.collectAsState()
    val domainsBlocked by FocusCrewVpnService.domainsBlockedCount.collectAsState()
    val blockedSearches by AntiPornDefenderManager.blockedSearchesCount.collectAsState()

    val isFortressActive = isVpnRunning || isDefenderActive
    val ulyssesState by UlyssesLockManager.state.collectAsState()

    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showUlyssesCoolDownDialog by remember { mutableStateOf(false) }
    var showPurgeConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UlyssesLockManager.initialize(context)
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            armAllDefenses(context)
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
            Toast.makeText(context, "🛡️ Shield Armed: Protection Active!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "VPN activation cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    val trigger1ClickMasterActivation: () -> Unit = {
        armAllDefenses(context)
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
            Toast.makeText(context, "🛡️ Shield Armed: Protection Active!", Toast.LENGTH_SHORT).show()
        }
    }

    val executeDeactivation: () -> Unit = {
        disarmAllDefenses(context)
        val vpnIntent = Intent(context, FocusCrewVpnService::class.java).apply {
            action = "STOP_VPN"
        }
        context.startService(vpnIntent)
        Toast.makeText(context, "Dopamine Shield deactivated.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(isFortressActive, isVpnRunning) {
        if (isFortressActive && !isVpnRunning) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent == null) {
                val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
                context.startService(vpnIntent)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFortressActive) BritishRacingSage.copy(alpha = 0.4f) else WhisperBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Master Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isFortressActive) BritishRacingSage.copy(alpha = 0.12f)
                                else TerracottaClay.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFortressActive) "🛡️" else "⚡",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dopamine Shield",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SoftEspressoInk
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isFortressActive) BritishRacingSage else MutedCharcoalOlive,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFortressActive) "Active & Enforcing" else "Disarmed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFortressActive) BritishRacingSage else MutedCharcoalOlive
                            )
                        }
                    }
                }

                Switch(
                    checked = isFortressActive,
                    onCheckedChange = { shouldEnable ->
                        if (shouldEnable) {
                            trigger1ClickMasterActivation()
                        } else {
                            if (ulyssesState.isEnabled) {
                                if (UlyssesLockManager.isEligibleToDeactivate()) {
                                    showDeactivateDialog = true
                                } else {
                                    showUlyssesCoolDownDialog = true
                                }
                            } else {
                                showDeactivateDialog = true
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isFortressActive) {
                // OFF STATE: 1-Click Compact Button
                Button(
                    onClick = trigger1ClickMasterActivation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ Arm Dopamine Shield (1-Click)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Locks SafeSearch, blocks 1000+ adult domains & filters incognito mode.",
                    fontSize = 11.sp,
                    color = MutedCharcoalOlive,
                    lineHeight = 15.sp
                )
            } else {
                // ACTIVE STATE: Sleek Compact Telemetry

                // Alerts if background services need attention
                if (!isVpnRunning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerracottaClay.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, TerracottaClay.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VPN Shield Disarmed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaClay
                                )
                            }
                            Button(
                                onClick = trigger1ClickMasterActivation,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Arm", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!isAccessibilityConnected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AntiqueHoneyBrass.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, AntiqueHoneyBrass.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ℹ️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Accessibility Interceptor Off",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AntiqueHoneyBrass
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        com.focuscrew.app.util.RestrictedSettingsHelper.openAccessibilitySettings(context)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AntiqueHoneyBrass),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Enable", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    OutlinedButton(
                                        onClick = {
                                            com.focuscrew.app.util.RestrictedSettingsHelper.openAppInfoForRestrictedSettings(context)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Unlock", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Compact 3-Pill Status Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactStatusPill(
                        icon = "🔒",
                        label = "SafeSearch",
                        status = "Locked",
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatusPill(
                        icon = "🛡️",
                        label = "Domains",
                        status = "${domainsBlocked + blockedSearches} blocked",
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatusPill(
                        icon = "🚫",
                        label = "Incognito",
                        status = "Filtered",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact 24h Ulysses Lock Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmLinen, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "24-Hour Ulysses Lock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftEspressoInk
                            )
                            Text(
                                text = if (ulyssesState.isEnabled) "24h delay enforced" else "Instant toggle permitted",
                                fontSize = 10.sp,
                                color = MutedCharcoalOlive
                            )
                        }
                    }

                    Switch(
                        checked = ulyssesState.isEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                UlyssesLockManager.enable()
                                Toast.makeText(context, "🔒 Ulysses Lock Armed: 24h delay enforced.", Toast.LENGTH_SHORT).show()
                            } else {
                                if (UlyssesLockManager.isEligibleToDeactivate()) {
                                    UlyssesLockManager.completeDeactivation()
                                    Toast.makeText(context, "Ulysses Lock disabled.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showUlyssesCoolDownDialog = true
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AntiqueHoneyBrass
                        )
                    )
                }

                if (ulyssesState.isPendingDeactivation) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val remainingMs = UlyssesLockManager.getRemainingCoolDownMs()
                    val hours = remainingMs / (3600 * 1000)
                    val mins = (remainingMs % (3600 * 1000)) / (60 * 1000)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerracottaClay.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏳ Unlock in: ${hours}h ${mins}m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaClay
                        )
                        TextButton(
                            onClick = {
                                UlyssesLockManager.cancelDeactivation()
                                Toast.makeText(context, "Cancelled! Shield stays active.", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Keep Armed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BritishRacingSage)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Quick Action Buttons (Compact Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPurgeConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
                    ) {
                        Text(
                            text = "🧹 Clear History",
                            fontSize = 11.sp,
                            color = SoftEspressoInk,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (ulyssesState.isEnabled) {
                                if (UlyssesLockManager.isEligibleToDeactivate()) {
                                    showDeactivateDialog = true
                                } else {
                                    showUlyssesCoolDownDialog = true
                                }
                            } else {
                                showDeactivateDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerracottaClay.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Turn Off",
                            fontSize = 11.sp,
                            color = TerracottaClay,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Deactivation Reflection Challenge Dialog
    if (showDeactivateDialog) {
        DeactivateFortressDialog(
            onConfirmDeactivate = {
                showDeactivateDialog = false
                executeDeactivation()
            },
            onDismiss = { showDeactivateDialog = false }
        )
    }

    // 24-Hour Ulysses Cool-Down Dialog
    if (showUlyssesCoolDownDialog) {
        val remainingMs = UlyssesLockManager.getRemainingCoolDownMs()
        val isPending = ulyssesState.isPendingDeactivation
        val hours = remainingMs / (3600 * 1000)
        val mins = (remainingMs % (3600 * 1000)) / (60 * 1000)

        AlertDialog(
            onDismissRequest = { showUlyssesCoolDownDialog = false },
            title = {
                Text(
                    text = "⏳ 24-Hour Ulysses Lock",
                    fontWeight = FontWeight.Bold,
                    color = TerracottaClay
                )
            },
            text = {
                Text(
                    text = if (isPending) {
                        "Deactivation in progress. Time left: ${hours}h ${mins}m.\nShield stays active until tomorrow to prevent impulsive relapse."
                    } else {
                        "Ulysses Lock is active. A 24-hour waiting period is required before turning off the shield."
                    },
                    fontSize = 13.sp,
                    color = SoftEspressoInk
                )
            },
            confirmButton = {
                if (isPending) {
                    Button(
                        onClick = {
                            UlyssesLockManager.cancelDeactivation()
                            showUlyssesCoolDownDialog = false
                            Toast.makeText(context, "Cancelled! Shield unbroken. 🛡️", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                    ) {
                        Text("Stay Strong", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            UlyssesLockManager.requestDeactivation()
                            showUlyssesCoolDownDialog = false
                            Toast.makeText(context, "24-Hour Countdown Started.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
                    ) {
                        Text("Start 24h Delay", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUlyssesCoolDownDialog = false }) {
                    Text("Cancel", color = MutedCharcoalOlive)
                }
            },
            containerColor = DaylightPaper
        )
    }

    // 1-Click History Purge Dialog
    if (showPurgeConfirmation) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmation = false },
            title = {
                Text(
                    text = "🧹 Clear Browser History",
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
            },
            text = {
                Text(
                    text = "This opens your browser's Clear Browsing Data screen to wipe cached searches and history.",
                    fontSize = 12.sp,
                    color = MutedCharcoalOlive
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeConfirmation = false
                        openBrowserHistorySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text("Open Browser Settings", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmation = false }) {
                    Text("Cancel", color = MutedCharcoalOlive)
                }
            },
            containerColor = DaylightPaper
        )
    }
}

@Composable
private fun CompactStatusPill(
    icon: String,
    label: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(WarmLinen, RoundedCornerShape(8.dp))
            .border(1.dp, WhisperBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk,
                    maxLines = 1
                )
            }
            Text(
                text = status,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = BritishRacingSage,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DeactivateFortressDialog(
    onConfirmDeactivate: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdownSeconds by remember { mutableIntStateOf(10) }
    var isCountdownComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        }
        isCountdownComplete = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚠️ Turn Off Shield?",
                fontWeight = FontWeight.Bold,
                color = TerracottaClay
            )
        },
        text = {
            Column {
                Text(
                    text = "Disabling will remove SafeSearch and adult content filters.",
                    fontSize = 12.sp,
                    color = SoftEspressoInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!isCountdownComplete) {
                        "Pause and breathe: ${countdownSeconds}s..."
                    } else {
                        "Are you sure you want to proceed?"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isCountdownComplete) TerracottaClay else MutedCharcoalOlive
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDeactivate,
                enabled = isCountdownComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerracottaClay,
                    disabledContainerColor = WarmLinen
                )
            ) {
                Text(
                    text = if (isCountdownComplete) "Turn Off" else "Wait ${countdownSeconds}s",
                    color = if (isCountdownComplete) Color.White else MutedCharcoalOlive,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep On", color = BritishRacingSage, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DaylightPaper
    )
}

private fun armAllDefenses(context: Context) {
    FocusCrewVpnService.setVpnEnabled(true)
    AntiPornDefenderManager.setDefenderEnabled(true)
    AntiPornDefenderManager.setSafeSearchEnforced(true)
    FreedomVaultManager.setBlockIncognito(true)
}

private fun disarmAllDefenses(context: Context) {
    FocusCrewVpnService.setVpnEnabled(false)
    AntiPornDefenderManager.setDefenderEnabled(false)
    AntiPornDefenderManager.setSafeSearchEnforced(true) // SafeSearch is permanently locked
    FreedomVaultManager.setBlockIncognito(false)
}

private fun openBrowserHistorySettings(context: Context) {
    val browserPackages = listOf(
        "com.android.chrome",
        "com.brave.browser",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "org.mozilla.firefox"
    )

    var intentFired = false
    val pm = context.packageManager
    for (pkg in browserPackages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            try {
                val clearIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("chrome://history")
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(clearIntent)
                intentFired = true
                break
            } catch (_: Exception) {}
        }
    }

    if (!intentFired) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://myactivity.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            Toast.makeText(context, "Open your browser -> History -> Clear Browsing Data", Toast.LENGTH_LONG).show()
        }
    }
}
