package com.focuscrew.app.ui.timer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextDecoration
import com.focuscrew.app.domain.model.CommitmentMode
import com.focuscrew.app.domain.model.SessionMode
import com.focuscrew.app.domain.model.SessionTask
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.domain.statemachine.SessionStateMachine
import com.focuscrew.app.service.BlockProfileManager
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun TimerScreen(
    onOpenAppPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snapshot by TimerManager.snapshot.collectAsState()
    val isServiceConnected by BlockerManager.isServiceConnected.collectAsState()
    val blockedPackages by BlockerManager.blockedPackages.collectAsState()
    val activeProfile by BlockProfileManager.activeProfile.collectAsState()
    val isWhitelistMode by BlockerManager.isWhitelistMode.collectAsState()
    val whitelistPackages by BlockerManager.whitelistPackages.collectAsState()

    var selectedMode by remember { mutableStateOf(SessionMode.POMODORO) }
    var customMinutes by remember { mutableIntStateOf(25) }
    val focusSubjects = remember { listOf("Deep Focus", "Study", "Coding", "Reading", "Writing") }
    var selectedSubject by remember { mutableStateOf("Deep Focus") }
    var selectedCommitmentMode by remember { mutableStateOf(snapshot.commitmentMode) }
    var taskInputText by remember { mutableStateOf("") }
    val isStrictMode = true // Permanently active - strict friction is never disabled
    var showFrictionDialog by remember { mutableStateOf(false) }
    var showDistractionDialog by remember { mutableStateOf(false) }
    var isStrictLockdownEnabled by remember { mutableStateOf(snapshot.isStrictLockdown) }
    var showStrictModeWarningDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(snapshot.isStrictLockdown) {
        isStrictLockdownEnabled = snapshot.isStrictLockdown
    }

    val isBreak = snapshot.isBreak
    val isRunning = snapshot.status is TimerStatus.Running
    val isPaused = snapshot.status is TimerStatus.Paused
    val isCompleted = snapshot.status is TimerStatus.Completed

    if (showStrictModeWarningDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showStrictModeWarningDialog = false },
            icon = { Text("🔒", fontSize = 32.sp) },
            title = {
                Text(
                    text = "You are entering Strict Mode!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "⚠️ You CANNOT exit or stop the timer once you have started it and selected your blocked apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "• There will be NO End Button during the session — only a Pause button.\n• If you pause the timer, the app will STILL block the apps in your blocked list.\n• Blocking only disarms when the timer reaches 00:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showStrictModeWarningDialog = false
                            onOpenAppPicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("🛡️ Choose / Select Apps to Block (${blockedPackages.size} Selected)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isStrictLockdownEnabled = true
                        TimerManager.setStrictLockdown(true)
                        showStrictModeWarningDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirm Strict Mode")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStrictModeWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFrictionDialog) {
        StrictFrictionDialog(
            isStrict = isStrictMode,
            onConfirmedStop = { isEmergency ->
                TimerManager.stop(isEmergency = isEmergency)
                showFrictionDialog = false
            },
            onDismiss = { showFrictionDialog = false }
        )
    }

    if (showDistractionDialog) {
        DistractionJournalDialog(
            onSaveThought = { thought ->
                TimerManager.addDistractionThought(thought) {
                    showDistractionDialog = false
                }
            },
            onDismiss = { showDistractionDialog = false }
        )
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Cute Animated BrainPal Mascot with breathing & manipulative psychology
        BrainPalMascot(
            status = snapshot.status,
            isStrictLockdown = snapshot.isStrictLockdown || isStrictLockdownEnabled,
            isBreak = isBreak
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Selector Chips (Enabled when not actively running)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SessionMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (!isRunning && !isPaused) {
                            selectedMode = mode
                        }
                    },
                    enabled = !isRunning && !isPaused,
                    label = {
                        Text(
                            text = when (mode) {
                                SessionMode.POMODORO -> "25m"
                                SessionMode.FOCUS_50 -> "50m"
                                SessionMode.DEEP_90 -> "90m"
                                SessionMode.CUSTOM -> "Custom"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Custom duration slider if Custom is selected and idle
        AnimatedVisibility(visible = selectedMode == SessionMode.CUSTOM && !isRunning && !isPaused) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Custom Duration: $customMinutes min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val previewBreakMins = TimerManager.calculateIntelligentBreakMinutes(customMinutes * 60000L)
                val previewDesc = TimerManager.getBreakScheduleDescription(customMinutes)
                Text(
                    text = "☕ $previewDesc",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (previewBreakMins > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = customMinutes.toFloat(),
                    onValueChange = { customMinutes = it.toInt() },
                    valueRange = 1f..180f,
                    steps = 35, // 5 min increments
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Circular Timer Progress Ring
        CircularTimerRing(
            formattedTime = snapshot.formattedTime,
            progressFraction = snapshot.progressFraction,
            status = snapshot.status,
            activeMode = selectedMode,
            customMinutes = customMinutes,
            activeSubject = if (isRunning || isPaused || isCompleted) snapshot.activeSubject else selectedSubject,
            isBreak = isBreak,
            breakLabel = snapshot.breakLabel,
            isStrictLockdown = snapshot.isStrictLockdown
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Timer Control Action Buttons
        when {
            isBreak -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🌿 Resting Eyes & Mind",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All blocked apps are temporarily unlocked. Stand up, stretch, and relax.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { TimerManager.skipBreak() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(
                                text = if (snapshot.hasPausedFocusSession) "Skip Break & Resume Focus" else "Skip Break & Return to Focus",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            isRunning -> {
                val elapsedMs = (snapshot.totalPlannedMs - snapshot.remainingMs).coerceAtLeast(0L)
                val intelligentBreakMins = TimerManager.calculateIntelligentBreakMinutes(snapshot.totalPlannedMs)
                val unlockThresholdMs = TimerManager.calculateRestUnlockThresholdMs(snapshot.totalPlannedMs)

                if (intelligentBreakMins > 0) {
                    if (elapsedMs >= unlockThresholdMs) {
                        Button(
                            onClick = { TimerManager.takeInSessionBreak(intelligentBreakMins) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(
                                text = "☕ Take $intelligentBreakMins-Minute Rest",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val unlockRemainingMs = (unlockThresholdMs - elapsedMs).coerceAtLeast(0L)
                        val unlockTimeFormatted = SessionStateMachine.formatTime(unlockRemainingMs)
                        val plannedMins = (snapshot.totalPlannedMs / 60000L).toInt()
                        val scheduleText = when {
                            plannedMins == 25 -> "☕ 5 minutes rest will come after $unlockTimeFormatted"
                            plannedMins == 50 -> "☕ 10 min rest occur in $unlockTimeFormatted"
                            plannedMins == 90 -> "☕ 15 min rest occur in $unlockTimeFormatted"
                            plannedMins in 130..150 -> "☕ 25 min rest comes after $unlockTimeFormatted"
                            plannedMins in 151..180 -> "☕ 35 min rest comes after $unlockTimeFormatted"
                            else -> "☕ $intelligentBreakMins min rest comes after $unlockTimeFormatted"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scheduleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }


                if (snapshot.isStrictLockdown) {
                    Button(
                        onClick = { TimerManager.pause() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("⏸ Pause Session (Strict Mode)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { TimerManager.pause() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pause", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = { showFrictionDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        ) {
                            Text("End Session", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDistractionDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📝 Jot Down Thought (Park Urge)", style = MaterialTheme.typography.labelMedium)
                }
            }

            isPaused -> {
                val elapsedMs = (snapshot.totalPlannedMs - snapshot.remainingMs).coerceAtLeast(0L)
                val intelligentBreakMins = TimerManager.calculateIntelligentBreakMinutes(snapshot.totalPlannedMs)
                val unlockThresholdMs = TimerManager.calculateRestUnlockThresholdMs(snapshot.totalPlannedMs)

                if (intelligentBreakMins > 0) {
                    if (elapsedMs >= unlockThresholdMs) {
                        Button(
                            onClick = { TimerManager.takeInSessionBreak(intelligentBreakMins) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text(
                                text = "☕ Take $intelligentBreakMins-Minute Rest",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val unlockRemainingMs = (unlockThresholdMs - elapsedMs).coerceAtLeast(0L)
                        val unlockTimeFormatted = SessionStateMachine.formatTime(unlockRemainingMs)
                        val plannedMins = (snapshot.totalPlannedMs / 60000L).toInt()
                        val scheduleText = when {
                            plannedMins == 25 -> "☕ 5 minutes rest will come after $unlockTimeFormatted"
                            plannedMins == 50 -> "☕ 10 min rest occur in $unlockTimeFormatted"
                            plannedMins == 90 -> "☕ 15 min rest occur in $unlockTimeFormatted"
                            plannedMins in 130..150 -> "☕ 25 min rest comes after $unlockTimeFormatted"
                            plannedMins in 151..180 -> "☕ 35 min rest comes after $unlockTimeFormatted"
                            else -> "☕ $intelligentBreakMins min rest comes after $unlockTimeFormatted"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scheduleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (snapshot.isStrictLockdown) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (snapshot.isStrictLockdown) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (snapshot.isStrictLockdown) "🔒 Strict Mode Active — Apps Remain Blocked" else "⏸️ Session Paused",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (snapshot.isStrictLockdown) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = { TimerManager.resume() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("▶ Resume Focus", style = MaterialTheme.typography.labelLarge)
                }
            }

            isCompleted -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val wasTaskUnlocked = (snapshot.status as? TimerStatus.Completed)?.wasTaskUnlocked == true
                    if (wasTaskUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BritishRacingSage.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BritishRacingSage),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🏆 All declared goals completed! Apps unlocked.",
                                    fontWeight = FontWeight.Bold,
                                    color = BritishRacingSage,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Milestone 8: Scheduled Break Quick Launchers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { TimerManager.startBreak(5, "Short Break") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("☕ 5m Short Break", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = { TimerManager.startBreak(15, "Long Break") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("🌿 15m Long Break", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = { TimerManager.reset() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Start Another Session", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            else -> {
                // Focus Goal Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    focusSubjects.forEach { subject ->
                        val isSubjectSelected = selectedSubject == subject
                        FilterChip(
                            selected = isSubjectSelected,
                            onClick = { selectedSubject = subject },
                            label = {
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Start Session Action Button
                Button(
                    onClick = {
                        val duration = if (selectedMode == SessionMode.CUSTOM) {
                            customMinutes * 60 * 1000L
                        } else {
                            selectedMode.durationMs
                        }
                        TimerManager.startSession(
                            mode = selectedMode,
                            customDurationMs = duration,
                            subject = selectedSubject,
                            commitmentMode = selectedCommitmentMode,
                            isStrictLockdown = isStrictLockdownEnabled
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStrictLockdownEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isStrictLockdownEnabled) "🔒 Lock In Focus • $selectedSubject" else "⚡ Start Focus Session • $selectedSubject",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Compact Bento Settings Card (Replaces 3 oversized boxes with 1 sleek container)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        // Row 1: Strict Lockdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔒", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Strict Lockdown",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isStrictLockdownEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isStrictLockdownEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (isStrictLockdownEnabled) "LOCKED" else "OFF",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = if (isStrictLockdownEnabled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isStrictLockdownEnabled) "0 Loopholes • Apps remain blocked on pause" else "No early exit once timer begins",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            androidx.compose.material3.Switch(
                                checked = isStrictLockdownEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showStrictModeWarningDialog = true
                                    } else {
                                        isStrictLockdownEnabled = false
                                        TimerManager.setStrictLockdown(false)
                                    }
                                },
                                modifier = Modifier.scale(0.8f),
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onError,
                                    checkedTrackColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )

                        // Row 2: Study Bubble Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🫧", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Study Bubble",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWhitelistMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isWhitelistMode) "Whitelist active: Only study tools allowed" else "Blocks selected distracting apps",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            androidx.compose.material3.Switch(
                                checked = isWhitelistMode,
                                onCheckedChange = { BlockerManager.setWhitelistMode(it) },
                                modifier = Modifier.scale(0.8f),
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                                    checkedTrackColor = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )

                        // Row 3: Commitment Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎯", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Commitment",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedCommitmentMode == CommitmentMode.STRICT_TIMER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        selectedCommitmentMode = CommitmentMode.STRICT_TIMER
                                        TimerManager.setCommitmentMode(CommitmentMode.STRICT_TIMER)
                                    }
                                ) {
                                    Text(
                                        text = "⏱️ Timer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCommitmentMode == CommitmentMode.STRICT_TIMER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedCommitmentMode == CommitmentMode.TASK_BASED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        selectedCommitmentMode = CommitmentMode.TASK_BASED
                                        TimerManager.setCommitmentMode(CommitmentMode.TASK_BASED)
                                    }
                                ) {
                                    Text(
                                        text = "✅ Tasks",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCommitmentMode == CommitmentMode.TASK_BASED) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Focus Tasks (Rendered ONCE here for all session states - zero duplicate)
        FocusTodoListCard(
            tasks = snapshot.sessionTasks,
            onAddTask = { TimerManager.addTask(it) },
            onToggleTask = { TimerManager.toggleTask(it) },
            onRemoveTask = { TimerManager.removeTask(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
fun CircularTimerRing(
    formattedTime: String,
    progressFraction: Float,
    status: TimerStatus,
    activeMode: SessionMode,
    customMinutes: Int,
    activeSubject: String = "Deep Focus",
    isBreak: Boolean = false,
    breakLabel: String = "Short Break",
    isStrictLockdown: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        label = "timerProgressAnimation"
    )

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ringPulseAnimation")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.04f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val primaryColor = if (isBreak) MaterialTheme.colorScheme.tertiary else if (isStrictLockdown) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(190.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()

            // Outer pulsing focus aura when running
            if (status is TimerStatus.Running) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.14f * pulseGlow),
                    radius = (size.minDimension / 2f) - 2.dp.toPx(),
                    style = Stroke(width = strokeWidth * 1.5f * pulseGlow)
                )
            }

            // Background track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc (clockwise from top)
            val sweepAngle = animatedProgress * 360f
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isStrictLockdown && (status is TimerStatus.Running || status is TimerStatus.Paused)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "🔒 STRICT LOCK",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = onSurfaceColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = when {
                    isBreak -> "☕ $breakLabel (Resting)"
                    status is TimerStatus.Running -> if (isStrictLockdown) "🔒 Strict Lock Active" else "🔒 $activeSubject Active"
                    status is TimerStatus.Paused -> if (isStrictLockdown) "⏸ Paused (Apps Blocked)" else "⏸ Paused • $activeSubject"
                    status is TimerStatus.Completed -> "✨ Completed • $activeSubject"
                    else -> when (activeMode) {
                        SessionMode.POMODORO -> "$activeSubject • 25m"
                        SessionMode.FOCUS_50 -> "$activeSubject • 50m"
                        SessionMode.DEEP_90 -> "$activeSubject • 90m"
                        SessionMode.CUSTOM -> "$activeSubject • $customMinutes min"
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isBreak) primaryColor else if (status is TimerStatus.Running) primaryColor else onSurfaceVariant
            )
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}


