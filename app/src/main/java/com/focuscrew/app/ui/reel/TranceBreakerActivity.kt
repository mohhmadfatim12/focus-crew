package com.focuscrew.app.ui.reel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.R
import com.focuscrew.app.data.model.BrainHealthState
import com.focuscrew.app.data.model.ReelStats
import com.focuscrew.app.service.ReelCounterManager
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.CashmereCream
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.FocusCrewTheme
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SagePillBg
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.TerracottaClay
import com.focuscrew.app.ui.theme.TerracottaContainer
import com.focuscrew.app.ui.theme.TerracottaTint
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder

/**
 * TranceBreakerActivity: The psychological trance-breaker & reality check modal.
 * Daylight Editorial Edition (100% Light Theme • Soft Text).
 *
 * Interrupts the hypnotic dopamine motor loop with the Amnesia Test,
 * real-time neurobiological dopamine sensitivity metrics, and competitor gap friction.
 */
class TranceBreakerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Safely navigate to Android Home on back press
        onBackPressedDispatcher.addCallback(this) {
            goToHomeScreen()
        }

        setContent {
            FocusCrewTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CashmereCream
                ) { innerPadding ->
                    TranceBreakerScreen(
                        onKillReels = {
                            Toast.makeText(
                                this,
                                "⚡ Focus Reclaimed! Dopamine recovery started (+12%/hr)",
                                Toast.LENGTH_LONG
                            ).show()
                            goToHomeScreen()
                        },
                        onGraceGranted = {
                            Toast.makeText(
                                this,
                                "⏳ Conscious grace granted: 5 more reels allowed",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_TRIGGERED_BY_MILESTONE = "extra_triggered_by_milestone"

        fun launch(context: Context, triggeredByMilestone: Boolean = false) {
            val intent = Intent(context, TranceBreakerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TRIGGERED_BY_MILESTONE, triggeredByMilestone)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun TranceBreakerScreen(
    onKillReels: () -> Unit,
    onGraceGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by ReelCounterManager.stats.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Pulsing heartbeat animation for brain mascot
    val infiniteTransition = rememberInfiniteTransition(label = "brain_pulse")
    val brainScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_scale"
    )

    val dopamineLevel = stats.calculateDopamineSensitivity()
    val dopamineStatus = stats.dopamineStatus
    val brainStage = stats.brainState

    var amnesiaChoice by remember { mutableStateOf<Boolean?>(null) }
    var showFullNeuroAudit by remember { mutableStateOf(false) }

    if (showFullNeuroAudit) {
        NeuroAuditDialog(
            onDismiss = { showFullNeuroAudit = false },
            onComplete = { targetLimit ->
                ReelCounterManager.setDailyLimit(targetLimit)
                showFullNeuroAudit = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Warning Pill Badge
        item {
            Box(
                modifier = Modifier
                    .background(
                        TerracottaContainer,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, TerracottaClay.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⚠️ TRANCE-BREAKER // REALITY CHECK",
                    color = TerracottaClay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 2. Mascot & Devolution Status
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .scale(brainScale)
                        .background(WarmLinen, CircleShape)
                        .border(1.5.dp, WhisperBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = getBrainDrawable(brainStage)),
                        contentDescription = "Brain State Mascot",
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Wake Up Your Brain",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "State: ${brainStage.title} • ${stats.todayTotal} Reels (${ReelStats.formatDuration(stats.todayTotalTimeMs)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoalOlive,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Card 1: The Interactive Amnesia Trap (Cognitive Dissonance)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DaylightPaper
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "THE AMNESIA TEST",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AntiqueHoneyBrass
                        )
                    }

                    Text(
                        text = "Without looking back: What was the exact topic or punchline of the reel you watched 20 seconds ago?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 20.sp
                        ),
                        color = SoftEspressoInk
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                amnesiaChoice = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (amnesiaChoice == false) TerracottaClay else TerracottaContainer,
                                contentColor = if (amnesiaChoice == false) Color.White else TerracottaClay
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🤯 Total Blank", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                amnesiaChoice = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (amnesiaChoice == true) BritishRacingSage else SagePillBg,
                                contentColor = if (amnesiaChoice == true) Color.White else BritishRacingSage
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🧠 I Remember", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(
                        visible = amnesiaChoice != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (amnesiaChoice == false) TerracottaTint else SagePillBg,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (amnesiaChoice == false) TerracottaClay.copy(alpha = 0.2f) else BritishRacingSage.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (amnesiaChoice == false)
                                    "💥 PREFRONTAL BLACKOUT: You just proved your prefrontal cortex is offline. Your thumb is swiping on pure hypnotic reflex while algorithms harvest your attention."
                                else
                                    "⚖️ COGNITIVE AUDIT: Say what you learned out loud right now. If it didn't increase your wealth, fitness, or intellect, you traded your finite life for algorithmic filler.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                color = if (amnesiaChoice == false) TerracottaClay else BritishRacingSage,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. Card 2: Dopamine Sensitivity & Competitor Reality
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DaylightPaper
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DOPAMINE SENSITIVITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AntiqueHoneyBrass
                        )
                        Text(
                            text = "$dopamineLevel% (${dopamineStatus.title})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dopamineLevel >= 70) BritishRacingSage else if (dopamineLevel >= 40) AntiqueHoneyBrass else TerracottaClay
                        )
                    }

                    val progress = (dopamineLevel / 100f).coerceIn(0.1f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (dopamineLevel >= 70) BritishRacingSage else if (dopamineLevel >= 40) AntiqueHoneyBrass else TerracottaClay,
                        trackColor = WarmLinen
                    )

                    val annualReels = stats.todayTotal * 365
                    val annualHours = ((stats.todayTotalTimeMs * 365) / (3600 * 1000L)).coerceAtLeast(1L)

                    Text(
                        text = "⚔️ Competitor Reality: Today you watched ${stats.todayTotal} reels (${ReelStats.formatDuration(stats.todayTotalTimeMs)}). At this pace: ~$annualReels reels ($annualHours hrs/yr) surrendered to feeds. While you scrolled, your competitors studied and built.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = MutedCharcoalOlive
                    )

                    Text(
                        text = "🌱 Off-Reels Healing: Receptors regenerate at +12% per hour off feeds.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BritishRacingSage,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Button to Open Deep Neuro-Audit
        item {
            OutlinedButton(
                onClick = { showFullNeuroAudit = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AntiqueHoneyBrass)
            ) {
                Text("🔬 Take Full Neuro-Audit & Brainrot Test", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 6. Action 1: Kill App & Reclaim Focus (Hero Action)
        item {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onKillReels()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AntiqueHoneyBrass,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "⚡ KILL REELS & RECLAIM FOCUS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        // 7. Action 2: Forced Conscious Choice (Psychological Friction)
        item {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onGraceGranted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MutedCharcoalOlive
                )
            ) {
                Text(
                    text = "❌ I Actually Remember (5 More Reels)",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun getBrainDrawable(stage: BrainHealthState): Int {
    return when (stage) {
        BrainHealthState.FRESH -> R.drawable.ic_brain_state_0
        BrainHealthState.CONCERNED -> R.drawable.ic_brain_state_1
        BrainHealthState.WEARY -> R.drawable.ic_brain_state_2
        BrainHealthState.EXHAUSTED -> R.drawable.ic_brain_state_3
        BrainHealthState.BRAINROT -> R.drawable.ic_brain_state_4
    }
}
