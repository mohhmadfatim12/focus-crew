package com.focuscrew.app.ui.reel

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.R
import com.focuscrew.app.data.model.BrainHealthState
import com.focuscrew.app.data.model.ReelStats
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.service.ReelCounterManager
import com.focuscrew.app.service.ReelNotificationManager
import com.focuscrew.app.service.ReelOverlayManager
import com.focuscrew.app.ui.theme.*

@Composable
fun ReelCounterScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by ReelCounterManager.stats.collectAsState()
    val isServiceConnected by BlockerManager.isServiceConnected.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Screen Header
        Column {
            Text(
                text = "Reel Counter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Autonomous swipe tracker for Instagram & YouTube",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Accessibility Warning Card if service is inactive
        if (!isServiceConnected) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tracking Inactive",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Enable FocusCrew Accessibility Service to count reels automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Enable", fontSize = 12.sp)
                    }
                }
            }
        }

        // Brain Health & Rot Status Card
        BrainHealthStatusCard(stats = stats)

        // NeuroShield Dopamine Sensitivity & Recovery Engine Card
        NeuroShieldDopamineCard(stats = stats)

        // Hero Today vs Yesterday Card
        HeroTodayCard(stats = stats)

        // 7-Day Weekly Reel Schedule (Mon - Sun with Sunday 12:01 AM Auto-Reset)
        WeeklyReelScheduleCard()

        // Platform Breakdown Card (Instagram vs YouTube)
        PlatformBreakdownCard(stats = stats)

        // HUD & Notification Preferences Card
        DisplayPreferencesCard(
            stats = stats,
            onOverlayToggle = { enabled ->
                ReelCounterManager.setOverlayEnabled(enabled)
                if (!enabled) {
                    ReelOverlayManager.hide()
                }
            },
            onNotificationToggle = { enabled ->
                ReelCounterManager.setNotificationEnabled(enabled)
                if (enabled) {
                    ReelNotificationManager.updateNotification(context, stats.copy(isNotificationEnabled = true))
                } else {
                    ReelNotificationManager.cancelNotification(context)
                }
            }
        )

        // Reset Counter Button
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("🔄 Reset Today's Count", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Today's Reels?") },
            text = { Text("This will reset your reel counter for today back to 0. Yesterday's count will remain untouched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ReelCounterManager.resetCounts()
                        ReelOverlayManager.update(ReelCounterManager.stats.value)
                        ReelNotificationManager.updateNotification(context, ReelCounterManager.stats.value)
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BrainHealthStatusCard(stats: ReelStats) {
    val stage = stats.brainState
    val badgeBg = when (stage) {
        BrainHealthState.FRESH -> SagePillBg
        BrainHealthState.CONCERNED -> AntiqueHoneyBrassContainer
        BrainHealthState.WEARY -> Color(0xFFFBF1E6)
        BrainHealthState.EXHAUSTED, BrainHealthState.BRAINROT -> TerracottaContainer
    }
    val badgeTextColor = when (stage) {
        BrainHealthState.FRESH -> BritishRacingSage
        BrainHealthState.CONCERNED -> OnAntiqueHoneyBrassContainer
        BrainHealthState.WEARY -> Color(0xFF8A5320)
        BrainHealthState.EXHAUSTED, BrainHealthState.BRAINROT -> TerracottaClay
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = DaylightPaper
        ),
        border = BorderStroke(1.dp, WhisperBorder),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BRAIN HEALTH & ROT STATUS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueHoneyBrass,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stage.title.uppercase(),
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = getBrainStateDrawable(stage)),
                    contentDescription = "Brain Mascot",
                    modifier = Modifier.size(68.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stage.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SoftEspressoInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stage.quote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoalOlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brainrot Progress Bar
            val limit = if (stats.dailyLimit > 0) stats.dailyLimit else 50
            val ratio = (stats.todayTotal.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
            val progressColor = when (stage) {
                BrainHealthState.FRESH -> BritishRacingSage
                BrainHealthState.CONCERNED -> AntiqueHoneyBrass
                BrainHealthState.WEARY -> Color(0xFFD97706)
                BrainHealthState.EXHAUSTED, BrainHealthState.BRAINROT -> TerracottaClay
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daily Limit: ${stats.todayTotal}/$limit Reels",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedCharcoalOlive
                    )
                    Text(
                        text = "${(stats.todayTotal * 100 / limit)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = WarmLinenHigh
                )
            }
        }
    }
}

private fun getBrainStateDrawable(stage: BrainHealthState): Int {
    return when (stage) {
        BrainHealthState.FRESH -> R.drawable.ic_brain_state_0
        BrainHealthState.CONCERNED -> R.drawable.ic_brain_state_1
        BrainHealthState.WEARY -> R.drawable.ic_brain_state_2
        BrainHealthState.EXHAUSTED -> R.drawable.ic_brain_state_3
        BrainHealthState.BRAINROT -> R.drawable.ic_brain_state_4
    }
}

@Composable
private fun HeroTodayCard(stats: ReelStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DaylightPaper
        ),
        border = BorderStroke(1.dp, WhisperBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "SCROLLED TODAY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AntiqueHoneyBrass
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large Clean Hero Count & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "${stats.todayTotal}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = " reels",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedCharcoalOlive,
                        modifier = Modifier.padding(bottom = 10.dp, start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Total Screen Time Badge
                Box(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .background(
                            AntiqueHoneyBrassContainer,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⏱️ ${ReelStats.formatDuration(stats.todayTotalTimeMs)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnAntiqueHoneyBrassContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Yesterday Comparison Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        WarmLinen,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yesterday",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedCharcoalOlive
                        )
                        Text(
                            text = "${stats.yesterdayTotal} reels (${ReelStats.formatDuration(stats.yesterdayTotalTimeMs)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delta badge
                    val diff = stats.todayTotal - stats.yesterdayTotal
                    val diffColor = if (diff > 0) TerracottaClay else BritishRacingSage
                    val diffBg = if (diff > 0) TerracottaContainer else SagePillBg
                    val diffIcon = if (diff > 0) "▲ +" else if (diff < 0) "▼ " else "= "

                    Box(
                        modifier = Modifier
                            .background(diffBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (diff == 0) "Same as yesterday" else "$diffIcon$diff vs yesterday",
                            color = diffColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformBreakdownCard(stats: ReelStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DaylightPaper
        ),
        border = BorderStroke(1.dp, WhisperBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Platform Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Instagram Row
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
                            .size(40.dp)
                            .background(Color(0xFFE1306C).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📸", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Instagram Reels",
                            fontWeight = FontWeight.SemiBold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "⏱️ Time: ${ReelStats.formatDuration(stats.todayInstagramTimeMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFC13584)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.todayInstagram}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "reels",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedCharcoalOlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YouTube Row
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
                            .size(40.dp)
                            .background(Color(0xFFFF0000).copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "YouTube Shorts",
                            fontWeight = FontWeight.SemiBold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "⏱️ Time: ${ReelStats.formatDuration(stats.todayYoutubeTimeMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TerracottaClay
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stats.todayYoutube}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "shorts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedCharcoalOlive
                    )
                }
            }

            // Ratio Bar
            val total = stats.todayTotal
            val igRatio = if (total > 0) stats.todayInstagram.toFloat() / total else 0.5f

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { igRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFC13584),
                trackColor = TerracottaContainer
            )
        }
    }
}

@Composable
private fun DisplayPreferencesCard(
    stats: ReelStats,
    onOverlayToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DaylightPaper
        ),
        border = BorderStroke(1.dp, WhisperBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Widget & Notification Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Upper Top Screen Pill Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating Capsule Widget",
                        fontWeight = FontWeight.SemiBold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "Minimal capsule under punch-hole camera during Reels/Shorts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoalOlive
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = stats.isOverlayEnabled,
                    onCheckedChange = onOverlayToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DaylightPaper,
                        checkedTrackColor = AntiqueHoneyBrass
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lock Screen Notification Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lock Screen & Status Bar Card",
                        fontWeight = FontWeight.SemiBold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "Persistent live stats visible on Lock Screen and Notification shade",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoalOlive
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = stats.isNotificationEnabled,
                    onCheckedChange = onNotificationToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DaylightPaper,
                        checkedTrackColor = AntiqueHoneyBrass
                    )
                )
            }
        }
    }
}

@Composable
private fun NeuroShieldDopamineCard(
    stats: ReelStats
) {
    val dopamineLevel = stats.calculateDopamineSensitivity()
    val dopamineStatus = stats.dopamineStatus
    val levelColor = when {
        dopamineLevel >= 70 -> BritishRacingSage
        dopamineLevel >= 40 -> AntiqueHoneyBrass
        else -> TerracottaClay
    }
    val badgeBg = when {
        dopamineLevel >= 70 -> SagePillBg
        dopamineLevel >= 40 -> AntiqueHoneyBrassContainer
        else -> TerracottaContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = DaylightPaper
        ),
        border = BorderStroke(1.dp, WhisperBorder),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEUROSHIELD DOPAMINE RECOVERY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueHoneyBrass,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$dopamineLevel% BASELINE",
                        color = levelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Dopamine Sensitivity Level Bar
            val ratio = (dopamineLevel / 100f).coerceIn(0.1f, 1.0f)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dopamineStatus.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "${dopamineLevel}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = levelColor
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = levelColor,
                    trackColor = WarmLinenHigh
                )
            }

            // Neurobiology Truth Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerracottaTint, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, WhisperBorder), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🔬 Biological State",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = dopamineStatus.neurobiologyNotice,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = MutedCharcoalOlive
                    )
                }
            }

            // Active Recovery Engine indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌱", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recovery: +12% / hour off short feeds",
                        style = MaterialTheme.typography.labelSmall,
                        color = BritishRacingSage,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

        }
    }
}


