package com.focuscrew.app.ui.freedom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun FreedomVaultScreen(
    initialOpenUrgeBreaker: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by FreedomVaultManager.state.collectAsState()
    val blockedSearches by com.focuscrew.app.service.AntiPornDefenderManager.blockedSearchesCount.collectAsState()
    val isCurfewActive = remember(state) {
        FreedomVaultManager.isCurrentTimeInDangerZone()
    }

    var showUrgeBreaker by remember(initialOpenUrgeBreaker) { mutableStateOf(initialOpenUrgeBreaker) }
    var showEditWhyDialog by remember { mutableStateOf(false) }
    var showAntiImpulseDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CashmereCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Banner / Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Freedom Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftEspressoInk
                )
                Text(
                    text = "100% Private Dopamine Detox & Focus Hub",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedCharcoalOlive
                )
            }

            Box(
                modifier = Modifier
                    .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔒 PRIVATE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BritishRacingSage
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Component 1: DOPAMINE DETOX SHIELD (Unified Master Blocker)
        TitanAntiPornFortressCard()

        Spacer(modifier = Modifier.height(10.dp))

        // Hero Streak & Milestones Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DOPAMINE DETOX STREAK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueHoneyBrass,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.cleanDaysStreak}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = if (state.cleanDaysStreak == 1) " day" else " days",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedCharcoalOlive,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Best: ${state.bestStreakDays} days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedCharcoalOlive
                    )
                    Text("•", color = WhisperBorder)
                    Text(
                        text = "⚡ ${state.urgesOvercomeCount} Urges Defeated",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BritishRacingSage
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Neuro-recovery milestone badge
                val (milestoneTitle, milestoneDesc) = state.getRecoveryMilestone()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmLinen, RoundedCornerShape(14.dp))
                        .border(1.dp, WhisperBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧠", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = milestoneTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftEspressoInk
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = milestoneDesc,
                            fontSize = 12.sp,
                            color = MutedCharcoalOlive,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Big Glowing Emergency SOS Button
        Button(
            onClick = { showUrgeBreaker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AntiqueHoneyBrass
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("⚡", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BREAK THE URGE (SOS)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Life Reclaimed Capital & Hours Saved
        LifeReclaimedCard(
            cleanDays = state.cleanDaysStreak,
            searchesIntercepted = blockedSearches,
            urgesConquered = state.urgesOvercomeCount
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Neuroplasticity Dopamine Brain Rewiring Map
        NeuroplasticityMapCard(cleanDays = state.cleanDaysStreak)

        Spacer(modifier = Modifier.height(10.dp))

        // "Why I Started" Anchor Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEditWhyDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚓", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Personal Anchor",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                    }
                    Text(
                        text = "Edit ✏️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AntiqueHoneyBrass
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${state.whyIStartedNote}\"",
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = SoftEspressoInk,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Component 2: Nighttime Danger Zone Curfew Scheduler
        DangerZoneSettingsCard(
            state = state,
            isCurfewActiveNow = isCurfewActive
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Component 3: Security & Anti-Tamper Passcode Protection
        com.focuscrew.app.ui.security.SecuritySettingsCard()

        Spacer(modifier = Modifier.height(20.dp))

        // Reset Streak Button (Compassionate)
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Had a slip? Debrief and reset streak",
                fontSize = 13.sp,
                color = MutedCharcoalOlive
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Dialogs
    if (showUrgeBreaker) {
        UrgeBreakerDialog(
            whyIStartedNote = state.whyIStartedNote,
            onDismiss = { showUrgeBreaker = false },
            onUrgeOvercome = { showUrgeBreaker = false }
        )
    }

    if (showEditWhyDialog) {
        var tempWhyText by remember { mutableStateOf(state.whyIStartedNote) }

        AlertDialog(
            onDismissRequest = { showEditWhyDialog = false },
            title = {
                Text(
                    text = "Edit Your Personal Anchor",
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
            },
            text = {
                Column {
                    Text(
                        text = "Write the raw truth of why you are quitting. This text will be shown to you during urgent cravings.",
                        fontSize = 12.sp,
                        color = MutedCharcoalOlive
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempWhyText,
                        onValueChange = { tempWhyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AntiqueHoneyBrass,
                            unfocusedBorderColor = WhisperBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FreedomVaultManager.saveWhyIStarted(tempWhyText)
                        showEditWhyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text("Save Anchor", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWhyDialog = false }) {
                    Text("Cancel", color = MutedCharcoalOlive)
                }
            },
            containerColor = DaylightPaper
        )
    }

    if (showAntiImpulseDialog) {
        var selectedEmotion by remember { mutableStateOf("Boredom") }

        AlertDialog(
            onDismissRequest = { showAntiImpulseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏳", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "15-Minute Impulse Pause",
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "To protect you from impulsive decisions, shields cannot be disabled immediately. What feeling are you trying to escape right now?",
                        fontSize = 13.sp,
                        color = MutedCharcoalOlive,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val emotions = listOf("Boredom", "Stress / Anxiety", "Loneliness", "Fatigue / Exhaustion", "Anger")
                    emotions.forEach { emotion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEmotion = emotion }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        if (selectedEmotion == emotion) AntiqueHoneyBrass else WarmLinen,
                                        CircleShape
                                    )
                                    .border(1.dp, WhisperBorder, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = emotion,
                                fontSize = 13.sp,
                                fontWeight = if (selectedEmotion == emotion) FontWeight.Bold else FontWeight.Normal,
                                color = SoftEspressoInk
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FreedomVaultManager.startAntiImpulseCooldown()
                        showAntiImpulseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
                ) {
                    Text("Start 15-Min Cooldown", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAntiImpulseDialog = false }) {
                    Text("Keep Shields On", color = BritishRacingSage, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DaylightPaper
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Compassionate Debrief",
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
            },
            text = {
                Column {
                    Text(
                        text = "A slip is a moment, not your identity. Your previous clean days are never erased—they rewired your brain.\n\nTake 3 deep breaths, forgive yourself, learn what triggered the slip, and let's begin anew.",
                        fontSize = 13.sp,
                        color = MutedCharcoalOlive,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        FreedomVaultManager.resetStreak()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text("Reset Streak & Rebuild", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = MutedCharcoalOlive)
                }
            },
            containerColor = DaylightPaper
        )
    }
}
