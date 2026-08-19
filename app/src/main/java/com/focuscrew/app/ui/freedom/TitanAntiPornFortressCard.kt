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

/**
 * TitanAntiPornFortressCard — High-Extreme Master Anti-Porn Blocker (Pure Self-Contained VPN).
 *
 * 100% VPN & LIVE DEFENSE (Zero Manual DNS Configuration):
 *  1. Self-Contained Split-Tunnel Loopback VPN (1000+ porn domains blackholed to 0.0.0.0)
 *  2. SafeSearch VIP Server-Lock (Google 216.239.38.120, Bing, DuckDuckGo)
 *     -> Forces SafeSearch permanently on at server level; explicit photos & sex videos cannot load.
 *  3. SafeSearch Anti-Tamper Guard:
 *     -> Prevents user from accessing or turning off SafeSearch in Google / browsers.
 *  4. Live Search & Browser Interceptor (Chrome, Brave, Opera, Edge, Samsung Internet, Firefox)
 *     -> 4-layer engine catches 'sex', 'sex videos', 'hot sex', 'porn', 'xxx', and adult keywords.
 *  5. Zero-Leak Incognito Blocker (instantly terminates private browser sessions).
 *  6. 100% Full Speed Normal Browsing (normal sites, banking, apps, YouTube bypass untouched).
 */
@Composable
fun TitanAntiPornFortressCard() {
    val context = LocalContext.current
    val isVpnRunning by FocusCrewVpnService.isVpnRunning.collectAsState()
    val isDefenderActive by AntiPornDefenderManager.isDefenderEnabled.collectAsState()
    val isAccessibilityConnected by BlockerManager.isServiceConnected.collectAsState()
    val domainsBlocked by FocusCrewVpnService.domainsBlockedCount.collectAsState()
    val blockedSearches by AntiPornDefenderManager.blockedSearchesCount.collectAsState()

    // Master state: Active if VPN or Defender is running
    val isFortressActive = isVpnRunning || isDefenderActive

    val ulyssesState by com.focuscrew.app.service.UlyssesLockManager.state.collectAsState()
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var showUlyssesCoolDownDialog by remember { mutableStateOf(false) }
    var showPurgeConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.focuscrew.app.service.UlyssesLockManager.initialize(context)
    }

    // Launcher for Android OS VPN Permission Dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            armAllDefenses(context)
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
            Toast.makeText(context, "🛡️ TITAN VPN ARMED: 100% Extreme Protection Active!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "VPN activation cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    val trigger1ClickMasterActivation: () -> Unit = {
        // Arm Defender, SafeSearch VIP, and Incognito Blocker
        armAllDefenses(context)

        // Prepare and start Self-Contained High-Extreme VPN
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
            Toast.makeText(context, "🛡️ TITAN VPN ARMED: 100% Extreme Protection Active!", Toast.LENGTH_LONG).show()
        }
    }

    val executeDeactivation: () -> Unit = {
        disarmAllDefenses(context)
        val vpnIntent = Intent(context, FocusCrewVpnService::class.java).apply {
            action = "STOP_VPN"
        }
        context.startService(vpnIntent)
        Toast.makeText(context, "Titan Fortress deactivated.", Toast.LENGTH_SHORT).show()
    }

    // Auto-arm VPN if defenses are active and permission is already granted
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(
            if (isFortressActive) 1.5.dp else 1.dp,
            if (isFortressActive) BritishRacingSage.copy(alpha = 0.5f) else WhisperBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isFortressActive) BritishRacingSage.copy(alpha = 0.15f)
                                else TerracottaClay.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFortressActive) "🛡️" else "⚡",
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "DOPAMINE DETOX SHIELD",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SoftEspressoInk,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isFortressActive) "Dopamine Protection • 100% Active" else "1-Click Focus & Detox Shield",
                            fontSize = 12.sp,
                            color = if (isFortressActive) BritishRacingSage else MutedCharcoalOlive,
                            fontWeight = if (isFortressActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Switch(
                    checked = isFortressActive,
                    onCheckedChange = { shouldEnable ->
                        if (shouldEnable) {
                            trigger1ClickMasterActivation()
                        } else {
                            if (ulyssesState.isEnabled) {
                                if (com.focuscrew.app.service.UlyssesLockManager.isEligibleToDeactivate()) {
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

            Spacer(modifier = Modifier.height(16.dp))

            if (!isFortressActive) {
                // ── OFF STATE: 1-CLICK ACTIVATION HERO ──
                Button(
                    onClick = trigger1ClickMasterActivation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text(
                        text = "⚡ ACTIVATE DOPAMINE DETOX (1-CLICK)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "High-focus dopamine protection shield. Permanently locks Google & Bing SafeSearch VIP so high-dopamine distractions cannot load, blackholes 1000+ distraction websites, and filters unhealthy search triggers. Zero manual setup required.",
                    fontSize = 11.sp,
                    color = MutedCharcoalOlive,
                    lineHeight = 16.sp
                )
            } else {
                // ── ACTIVE STATE: 10X TELEMETRY STATUS ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BritishRacingSage.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .border(1.dp, BritishRacingSage.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(BritishRacingSage, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DETOX ENFORCING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BritishRacingSage
                                )
                            }
                            Text(
                                text = "${domainsBlocked + blockedSearches} Distractions Filtered",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BritishRacingSage
                            )
                        }

                        if (!isVpnRunning) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { trigger1ClickMasterActivation() },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = TerracottaClay.copy(alpha = 0.12f)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, TerracottaClay)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚠️", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "VPN Network Shield Not Running",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TerracottaClay
                                            )
                                            Text(
                                                text = "Tap to arm VPN for SafeSearch VIP & 1000+ domain blackhole",
                                                fontSize = 11.sp,
                                                color = SoftEspressoInk
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = trigger1ClickMasterActivation,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("ARM VPN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High-Focus Telemetry List
                        if (isVpnRunning) {
                            FortressLayerRow("✓ Titan VPN Network Shield", "100% Active • SafeSearch VIP & 1000+ Domains Blocked", BritishRacingSage)
                        } else {
                            FortressLayerRow("⚠️ Titan VPN Network Shield", "DISARMED (Tap 'ARM VPN' button above)", TerracottaClay)
                        }
                        if (isAccessibilityConnected && isDefenderActive) {
                            FortressLayerRow("✓ Accessibility Screen Interceptor", "100% Active • Live Search & Keyword Interception", BritishRacingSage)
                        } else if (!isAccessibilityConnected) {
                            FortressLayerRow(
                                "⚠️ Accessibility Screen Interceptor",
                                "SERVICE OFF • Tap to enable FocusCrew in Accessibility Settings",
                                TerracottaClay,
                                onClick = {
                                    try {
                                        context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (_: Exception) {}
                                }
                            )
                        } else {
                            FortressLayerRow("⚠️ Accessibility Screen Interceptor", "DEFENDER PAUSED • Toggle switch above to arm", TerracottaClay)
                        }
                        FortressLayerRow("✓ SafeSearch Anti-Tamper", "SafeSearch Settings Protected (Cannot Turn Off)", BritishRacingSage)
                        FortressLayerRow("✓ Distraction Mode Blocker", "Incognito / Secret Tabs Terminated Instantly", SoftEspressoInk)
                        FortressLayerRow("✓ Normal Searches & Apps", "100% Full Speed • Zero Lag", BritishRacingSage)
                    }
                }

                // 24-Hour Ulysses Lock Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmLinen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏳", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "24-Hour Ulysses Lock",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftEspressoInk
                                    )
                                    Text(
                                        text = if (ulyssesState.isEnabled) "Deactivation requires 24h delay" else "Instant toggle permitted",
                                        fontSize = 11.sp,
                                        color = MutedCharcoalOlive
                                    )
                                }
                            }

                            Switch(
                                checked = ulyssesState.isEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        com.focuscrew.app.service.UlyssesLockManager.enable()
                                        Toast.makeText(context, "🔒 Ulysses Lock Armed: 24h delay enforced before shield can be disabled.", Toast.LENGTH_LONG).show()
                                    } else {
                                        if (com.focuscrew.app.service.UlyssesLockManager.isEligibleToDeactivate()) {
                                            com.focuscrew.app.service.UlyssesLockManager.completeDeactivation()
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
                            Spacer(modifier = Modifier.height(10.dp))
                            val remainingMs = com.focuscrew.app.service.UlyssesLockManager.getRemainingCoolDownMs()
                            val hours = remainingMs / (3600 * 1000)
                            val mins = (remainingMs % (3600 * 1000)) / (60 * 1000)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TerracottaClay.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                    .border(1.dp, TerracottaClay.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⏳ Unlock In Progress: ${hours}h ${mins}m left",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaClay
                                    )
                                    Text(
                                        text = "Protection remains 100% active. Sleep on it.",
                                        fontSize = 11.sp,
                                        color = SoftEspressoInk
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            com.focuscrew.app.service.UlyssesLockManager.cancelDeactivation()
                                            Toast.makeText(context, "Deactivation cancelled! Shield unbroken. 🛡️", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cancel Request & Stay Strong", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BritishRacingSage)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Purge Browser History Action
                OutlinedButton(
                    onClick = { showPurgeConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
                ) {
                    Text(
                        text = "🧹 Purge Browser Search History",
                        fontSize = 12.sp,
                        color = SoftEspressoInk,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Turn Off Button (With Friction Dialog)
                OutlinedButton(
                    onClick = {
                        if (ulyssesState.isEnabled) {
                            if (com.focuscrew.app.service.UlyssesLockManager.isEligibleToDeactivate()) {
                                showDeactivateDialog = true
                            } else {
                                showUlyssesCoolDownDialog = true
                            }
                        } else {
                            showDeactivateDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerracottaClay.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Turn Off Dopamine Detox",
                        fontSize = 12.sp,
                        color = TerracottaClay,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Layer Badges (Balanced 4-Column Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FortressBadge("🌐 Websites", isFortressActive, modifier = Modifier.weight(1f))
                FortressBadge("🖼️ Photos", isFortressActive, modifier = Modifier.weight(1f))
                FortressBadge("🎬 Videos", isFortressActive, modifier = Modifier.weight(1f))
                FortressBadge("🔒 SafeSearch", isFortressActive, modifier = Modifier.weight(1f))
            }
        }
    }

    // Deactivation Reflection Challenge Dialog (Anti-Impulse Protection)
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
        val remainingMs = com.focuscrew.app.service.UlyssesLockManager.getRemainingCoolDownMs()
        val isPending = ulyssesState.isPendingDeactivation
        val hours = remainingMs / (3600 * 1000)
        val mins = (remainingMs % (3600 * 1000)) / (60 * 1000)

        AlertDialog(
            onDismissRequest = { showUlyssesCoolDownDialog = false },
            title = {
                Text(
                    text = "⏳ 24-Hour Ulysses Lock Enforced",
                    fontWeight = FontWeight.Bold,
                    color = TerracottaClay
                )
            },
            text = {
                Column {
                    if (isPending) {
                        Text(
                            text = "Deactivation request is currently running.\n\nTime remaining: ${hours}h ${mins}m.\n\nProtection remains 100% active to prevent impulsive midnight relapse. Sleep on it and check back tomorrow.",
                            fontSize = 13.sp,
                            color = SoftEspressoInk,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            text = "You activated the 24-Hour Ulysses Lock to protect your future self.\n\nThe shield cannot be turned off instantly. Would you like to start the 24-hour waiting countdown? You must wait 24 hours (until tomorrow) before you can deactivate the fortress.",
                            fontSize = 13.sp,
                            color = SoftEspressoInk,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (isPending) {
                    Button(
                        onClick = {
                            com.focuscrew.app.service.UlyssesLockManager.cancelDeactivation()
                            showUlyssesCoolDownDialog = false
                            Toast.makeText(context, "Deactivation cancelled! Shield unbroken. 🛡️", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                    ) {
                        Text("Cancel Request & Stay Strong", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            com.focuscrew.app.service.UlyssesLockManager.requestDeactivation()
                            showUlyssesCoolDownDialog = false
                            Toast.makeText(context, "24-Hour Countdown Started. Protection remains active.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
                    ) {
                        Text("Start 24-Hour Countdown", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUlyssesCoolDownDialog = false }) {
                    Text("Keep Fortress Armed", color = BritishRacingSage, fontWeight = FontWeight.Bold)
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
                    text = "🧹 Purge Browser Search History",
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
            },
            text = {
                Text(
                    text = "This will open your browser's Clear Browsing Data screen so you can permanently wipe all cached search history, previews, and cookies in 1 tap.",
                    fontSize = 13.sp,
                    color = MutedCharcoalOlive,
                    lineHeight = 18.sp
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
                    Text("Open Clear History", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun FortressLayerRow(
    label: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Text(value, fontSize = 10.sp, color = MutedCharcoalOlive, lineHeight = 13.sp)
    }
}

@Composable
private fun FortressBadge(label: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                if (isActive) BritishRacingSage.copy(alpha = 0.12f) else WarmLinen,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) BritishRacingSage else MutedCharcoalOlive,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * 10-Second Anti-Impulse Challenge Dialog before allowing Fortress deactivation.
 * Ensures the user cannot break the blocker in a moment of weakness.
 */
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
                text = "⚠️ Deactivate Dopamine Detox?",
                fontWeight = FontWeight.Bold,
                color = TerracottaClay
            )
        },
        text = {
            Column {
                Text(
                    text = "Turning off Dopamine Detox will remove Safe Search enforcement and allow high-dopamine distraction triggers back onto your device.",
                    fontSize = 13.sp,
                    color = SoftEspressoInk,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (!isCountdownComplete) {
                        "Pause and breathe. Cool-down timer: ${countdownSeconds}s..."
                    } else {
                        "Cool-down finished. Are you absolutely sure you want to turn it off?"
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
                    text = if (isCountdownComplete) "Turn Off Now" else "Wait ${countdownSeconds}s",
                    color = if (isCountdownComplete) Color.White else MutedCharcoalOlive,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Shield On", color = BritishRacingSage, fontWeight = FontWeight.Bold)
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
    AntiPornDefenderManager.setSafeSearchEnforced(false)
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
