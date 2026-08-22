package com.focuscrew.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.repository.FocusRepository
import com.focuscrew.app.service.BlockProfileManager
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.service.ReelCounterManager
import com.focuscrew.app.ui.theme.*
import com.focuscrew.app.util.ApkShareHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateToReelCounter: () -> Unit,
    repository: FocusRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isServiceConnected by BlockerManager.isServiceConnected.collectAsState()
    val activeProfile by BlockProfileManager.activeProfile.collectAsState()
    val reelStats by ReelCounterManager.stats.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetReelsDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        Column {
            Text(
                text = "Settings & Privacy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Permissions transparency, telemetry, and on-device privacy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 🌟 1-Tap Share App Installer Card (WhatsApp, Telegram, etc.)
        Card(
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = BorderStroke(1.5.dp, AntiqueHoneyBrass),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(SagePillBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎁", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Share App with Friends",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftEspressoInk
                            )
                            Text(
                                text = "Send APK directly via WhatsApp (Free & Offline)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedCharcoalOlive
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        ApkShareHelper.shareApk(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BritishRacingSage,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "📤 Share APK Installer via WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Reel Counter Quick Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = BorderStroke(1.dp, WhisperBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AntiqueHoneyBrassContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📱", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reel Counter",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftEspressoInk
                            )
                            Text(
                                text = "Counting metric: ${reelStats.countingMetric.badge}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedCharcoalOlive
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onNavigateToReelCounter,
                        border = BorderStroke(1.dp, AntiqueHoneyBrass.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AntiqueHoneyBrass),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Configure")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = OutlineVariantColor)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Today's Count", style = MaterialTheme.typography.labelSmall, color = MutedCharcoalOlive)
                        Text("${reelStats.todayTotal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SoftEspressoInk)
                    }
                    Column {
                        Text("Metric Mode", style = MaterialTheme.typography.labelSmall, color = MutedCharcoalOlive)
                        Text(reelStats.countingMetric.name.replace("_", " "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AntiqueHoneyBrass)
                    }
                    Column {
                        Text("Watch Threshold", style = MaterialTheme.typography.labelSmall, color = MutedCharcoalOlive)
                        Text("${reelStats.watchTimeThresholdSec}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BritishRacingSage)
                    }
                }
            }
        }

        // ==========================================
        // FEATURE C: TRANSPARENT PERMISSIONS & SIGNALS CARD
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = BorderStroke(1.dp, WhisperBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Transparent Permissions & Signals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "Why each permission is needed and what signals are processed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedCharcoalOlive
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Accessibility Service Detailed Disclosure
                PermissionDeepDisclosureItem(
                    title = "Accessibility Service",
                    isGranted = isServiceConnected,
                    whyNeeded = "Detects when blocked apps are opened and counts short-form reels/shorts in Instagram and YouTube.",
                    signalsProcessed = listOf(
                        "Foreground app package name (packageName) to match against your blocked apps list.",
                        "View scroll events (TYPE_VIEW_SCROLLED) and swipe displacement deltas (deltaY, deltaX)."
                    ),
                    privacyBoundary = "STRICT: Zero text scraping. FocusCrew NEVER reads, inspects, or logs messages, post text, comments, usernames, or images.",
                    leavesDevice = "0 bytes (Stays 100% on-device).",
                    onGrantClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = OutlineVariantColor)
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Overlay Permission Detailed Disclosure
                val canDrawOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true

                PermissionDeepDisclosureItem(
                    title = "Display Over Other Apps (Overlay)",
                    isGranted = canDrawOverlay,
                    whyNeeded = "Renders the floating upper-top pill HUD displaying live reel count when scrolling reels.",
                    signalsProcessed = listOf(
                        "Screen window overlay coordinate parameters only."
                    ),
                    privacyBoundary = "STRICT: Does not capture, record, or screenshot any underlying application screen contents.",
                    leavesDevice = "0 bytes (Stays 100% on-device).",
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = OutlineVariantColor)
                Spacer(modifier = Modifier.height(14.dp))

                // 3. Notification & Lock-Screen Disclosure
                PermissionDeepDisclosureItem(
                    title = "Notifications & Lock Screen",
                    isGranted = true,
                    whyNeeded = "Displays the persistent focus session timer countdown and daily reel stats directly on your lock screen.",
                    signalsProcessed = listOf(
                        "Internal FocusCrew timer snapshot and today's reel totals."
                    ),
                    privacyBoundary = "STRICT: Never accesses or reads notifications from any other installed applications.",
                    leavesDevice = "0 bytes (Stays 100% on-device).",
                    onGrantClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        // ==========================================
        // FEATURE C: PRIVACY, DATA MINIMIZATION & DELETION CENTER
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = BorderStroke(1.dp, WhisperBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔒", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Privacy & Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "100% on-device storage with 1-click data deletion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedCharcoalOlive
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Data Minimization Oath
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SagePillBg,
                    border = BorderStroke(1.dp, BritishRacingSage.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✓ Zero-Data Footprint Guarantee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BritishRacingSage
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• No User Accounts: No email, password, login, or identifier is ever requested.\n" +
                                    "• No Internet Permissions: The app cannot transmit data to any server.\n" +
                                    "• 100% Local Storage: All data stays in your phone's private Room SQLite database.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = SoftEspressoInk,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Data Management & Deletion",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
                Text(
                    text = "Easily erase your local tracking logs or reset the app completely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedCharcoalOlive
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action 1: Clear History & Distractions
                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AntiqueHoneyBrass.copy(alpha = 0.5f))
                ) {
                    Text("🗑️ Clear Session & Distraction History", color = SoftEspressoInk, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Reset Reel Stats
                OutlinedButton(
                    onClick = { showResetReelsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AntiqueHoneyBrass.copy(alpha = 0.5f))
                ) {
                    Text("🔄 Reset All Reel Counter Statistics", color = SoftEspressoInk, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 3: Complete Account & Data Wipe (Factory Reset)
                Button(
                    onClick = { showFactoryResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
                ) {
                    Text("⚠️ Account & Complete Factory Data Wipe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Active Profile Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = BorderStroke(1.dp, WhisperBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "App Blocker Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Preset: ${activeProfile.name}",
                    fontWeight = FontWeight.SemiBold,
                    color = AntiqueHoneyBrass
                )
                Text(
                    text = "${activeProfile.packages.size} apps configured for calm interception.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedCharcoalOlive
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialog 1: Clear History Confirmation
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All Session History?") },
            text = { Text("This will permanently delete all logged focus sessions and distraction journal thoughts from this device's local database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository?.clearAllHistory()
                            Toast.makeText(context, "Session & distraction history deleted", Toast.LENGTH_SHORT).show()
                            showClearHistoryDialog = false
                        }
                    }
                ) {
                    Text("Delete History", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 2: Reset Reel Statistics Confirmation
    if (showResetReelsDialog) {
        AlertDialog(
            onDismissRequest = { showResetReelsDialog = false },
            title = { Text("Reset Reel Statistics?") },
            text = { Text("This will reset all today and yesterday reel counters and dwell time tracking back to zero.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ReelCounterManager.resetAllStats()
                        Toast.makeText(context, "Reel counter reset to 0", Toast.LENGTH_SHORT).show()
                        showResetReelsDialog = false
                    }
                ) {
                    Text("Reset Stats", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetReelsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 3: Complete Factory Data Wipe Confirmation
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            title = { Text("Complete Factory Data Wipe?") },
            text = { Text("This is a complete on-device data purge. It will erase all focus sessions, distraction thoughts, blocked app lists, and reel statistics. FocusCrew will return to its initial pristine state.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository?.factoryResetAllData()
                            ReelCounterManager.resetAllStats()
                            BlockProfileManager.resetToDefaults()
                            Toast.makeText(context, "All on-device data erased successfully", Toast.LENGTH_LONG).show()
                            showFactoryResetDialog = false
                        }
                    }
                ) {
                    Text("Factory Wipe", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PermissionDeepDisclosureItem(
    title: String,
    isGranted: Boolean,
    whyNeeded: String,
    signalsProcessed: List<String>,
    privacyBoundary: String,
    leavesDevice: String,
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SoftEspressoInk
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isGranted) SagePillBg else TerracottaContainer,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isGranted) "✓ Granted" else "⚠️ Missing",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) BritishRacingSage else TerracottaClay
                    )
                }
            }

            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) WarmLinen else AntiqueHoneyBrass,
                    contentColor = if (isGranted) SoftEspressoInk else DaylightPaper
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isGranted) "Settings" else "Enable", fontSize = 11.sp)
            }
        }

        // Why Needed
        Text(
            text = "• Why Necessary: $whyNeeded",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MutedCharcoalOlive,
            lineHeight = 15.sp
        )

        // Signals Processed
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "• Signals Processed:",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoftEspressoInk
            )
            signalsProcessed.forEach { signal ->
                Text(
                    text = "   - $signal",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.5.sp,
                    color = MutedCharcoalOlive
                )
            }
        }

        // Privacy Boundary
        Text(
            text = "• Privacy Boundary: $privacyBoundary",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.5.sp,
            color = BritishRacingSage
        )

        // Leaves Device
        Text(
            text = "• Leaves Device: $leavesDevice",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (leavesDevice.contains("0 bytes")) BritishRacingSage else TerracottaClay
        )
    }
}
