package com.focuscrew.app.ui.freedom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.service.AntiPornDefenderManager
import com.focuscrew.app.service.FreedomVaultManager
import com.focuscrew.app.ui.security.SecuritySettingsCard
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
    val blockedSearches by AntiPornDefenderManager.blockedSearchesCount.collectAsState()
    val isCurfewActive = remember(state) {
        FreedomVaultManager.isCurrentTimeInDangerZone()
    }

    var showUrgeBreaker by remember(initialOpenUrgeBreaker) { mutableStateOf(initialOpenUrgeBreaker) }
    var showEditWhyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CashmereCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Freedom Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftEspressoInk
                )
                Text(
                    text = "Dopamine Detox & Privacy Hub",
                    fontSize = 11.sp,
                    color = MutedCharcoalOlive
                )
            }

            // Clean pill badge with single-line no-wrap protection
            Box(
                modifier = Modifier
                    .background(BritishRacingSage.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔒 Private",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BritishRacingSage,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Component 1: DOPAMINE DETOX SHIELD (Compact Master Blocker)
        TitanAntiPornFortressCard()

        Spacer(modifier = Modifier.height(10.dp))

        // Component 2: Hero Streak & Emergency SOS (Combined Compact Hero)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Streak metrics
                    Column {
                        Text(
                            text = "DETOX STREAK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AntiqueHoneyBrass,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.cleanDaysStreak}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SoftEspressoInk
                            )
                            Text(
                                text = if (state.cleanDaysStreak == 1) " day" else " days",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedCharcoalOlive,
                                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 Best: ${state.bestStreakDays}d",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MutedCharcoalOlive
                            )
                            Text("•", fontSize = 10.sp, color = WhisperBorder)
                            Text(
                                text = "⚡ ${state.urgesOvercomeCount} Urges Defeated",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BritishRacingSage
                            )
                        }
                    }

                    // Right Side: Compact Emergency SOS Button
                    Button(
                        onClick = { showUrgeBreaker = true },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AntiqueHoneyBrass),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Break Urge",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact Milestone 1-Line Indicator
                val (milestoneTitle, _) = state.getRecoveryMilestone()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmLinen, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧠", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Phase: $milestoneTitle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Component 3: Life Reclaimed (Compact Glanceable 3-Stat Bar)
        LifeReclaimedCard(
            cleanDays = state.cleanDaysStreak,
            searchesIntercepted = blockedSearches,
            urgesConquered = state.urgesOvercomeCount
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Component 4: Neuroplasticity Timeline (Compact & Collapsible)
        NeuroplasticityMapCard(cleanDays = state.cleanDaysStreak)

        Spacer(modifier = Modifier.height(10.dp))

        // Component 5: "My Personal Anchor" (Compact Quote Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEditWhyDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚓", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "My Personal Anchor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedCharcoalOlive
                        )
                        Text(
                            text = "\"${state.whyIStartedNote}\"",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = SoftEspressoInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit ✏️",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AntiqueHoneyBrass
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Component 6: Night Danger Zone Scheduler
        DangerZoneSettingsCard(
            state = state,
            isCurfewActiveNow = isCurfewActive
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Component 7: Security & Anti-Tamper Passcode Protection
        SecuritySettingsCard()

        Spacer(modifier = Modifier.height(10.dp))

        // Reset Streak Button (Compact)
        TextButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Had a slip? Debrief and reset streak",
                fontSize = 11.sp,
                color = MutedCharcoalOlive
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
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
                        text = "Write your core reason for staying focused. This will appear during urge emergencies.",
                        fontSize = 12.sp,
                        color = MutedCharcoalOlive
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Text("Save", color = Color.White)
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
                Text(
                    text = "A slip is a moment, not your identity. Your previous clean days are never erased—they rewired your brain.\n\nTake 3 deep breaths, forgive yourself, learn what triggered the slip, and let's begin anew.",
                    fontSize = 12.sp,
                    color = MutedCharcoalOlive,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FreedomVaultManager.resetStreak()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text("Reset & Rebuild", color = Color.White)
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
