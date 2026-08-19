package com.focuscrew.app.ui.freedom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focuscrew.app.domain.statemachine.UrgeDestroyerMachine
import com.focuscrew.app.domain.statemachine.UrgeDestroyerState
import com.focuscrew.app.domain.statemachine.UrgeRound
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

enum class BreathPhase(val label: String, val instruction: String) {
    INHALE("Inhale", "Fill your lungs deeply through your nose"),
    HOLD_FULL("Hold", "Keep lungs full and gently relax shoulders"),
    EXHALE("Exhale", "Release slowly through your mouth"),
    HOLD_EMPTY("Hold", "Stay completely still in the calm pause")
}

private val VICTORY_COMPLIMENTS = listOf(
    "You just conquered your primal impulse. You proved your spirit is stronger than an algorithm.",
    "Neural pathway rewired! Every urge you starve weakens the addiction and builds unshakeable masculine confidence.",
    "Tomorrow morning you will wake up with high energy, sharp eyes, deep focus, and absolute self-respect.",
    "Weakness knocked on your door and you stood like a fortress. You are in complete control of your destiny.",
    "This single victory saved you from 24 hours of brain fog, lethargy, and regret. You chose greatness."
)

@Composable
fun UrgeBreakerDialog(
    whyIStartedNote: String,
    onDismiss: () -> Unit,
    onUrgeOvercome: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val vaultState by FreedomVaultManager.state.collectAsState()

    // Ensure audio stops if dialog is dismissed
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            com.focuscrew.app.audio.BinauralAudioGenerator.stop()
        }
    }

    // Pure Kotlin Urge Destroyer State Machine
    val machine = remember { UrgeDestroyerMachine(initialLockdownSeconds = 45) }
    var currentRound by remember { mutableStateOf(machine.state.currentRound) }

    // Round 1 Breathing & Surge state
    var currentBreathPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var phaseSecondsRemaining by remember { mutableIntStateOf(4) }
    var completedCycles by remember { mutableIntStateOf(0) }
    var surgeSecondsLeft by remember { mutableIntStateOf(300) }
    val checkedGrounding = remember { mutableStateListOf<Int>() }

    // Round 2 Cognitive State
    var selectedHaltTrigger by remember { mutableStateOf<String?>("Boredom") }
    var typedOathInput by remember { mutableStateOf("") }
    val isOathMatched = remember(typedOathInput) {
        typedOathInput.trim().equals(UrgeDestroyerState.REQUIRED_OATH, ignoreCase = true)
    }

    // Round 3 Lockdown Timer
    var lockdownSecondsLeft by remember { mutableIntStateOf(45) }
    val isLockdownComplete = lockdownSecondsLeft == 0

    // Victory Compliment
    val complimentIndex = remember { (0 until VICTORY_COMPLIMENTS.size).random() }

    // Synchronize Round 1 Breathing loop
    LaunchedEffect(currentRound) {
        if (currentRound == UrgeRound.ROUND_1_BIOLOGICAL_RESET) {
            while (currentRound == UrgeRound.ROUND_1_BIOLOGICAL_RESET) {
                delay(1000L)
                if (surgeSecondsLeft > 0) surgeSecondsLeft--

                if (phaseSecondsRemaining > 1) {
                    phaseSecondsRemaining--
                } else {
                    phaseSecondsRemaining = 4
                    currentBreathPhase = when (currentBreathPhase) {
                        BreathPhase.INHALE -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            BreathPhase.HOLD_FULL
                        }
                        BreathPhase.HOLD_FULL -> BreathPhase.EXHALE
                        BreathPhase.EXHALE -> {
                            completedCycles++
                            machine.recordBreathCycleCompleted()
                            BreathPhase.HOLD_EMPTY
                        }
                        BreathPhase.HOLD_EMPTY -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            BreathPhase.INHALE
                        }
                    }
                }
            }
        }
    }

    // Synchronize Round 3 Strict Lockdown countdown
    LaunchedEffect(currentRound) {
        if (currentRound == UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN) {
            while (lockdownSecondsLeft > 0 && currentRound == UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN) {
                delay(1000L)
                val remaining = machine.tickRound3Timer()
                lockdownSecondsLeft = remaining
            }
            if (lockdownSecondsLeft == 0) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Breathing circle scale
    val targetScale = when (currentBreathPhase) {
        BreathPhase.INHALE, BreathPhase.HOLD_FULL -> 1.25f
        BreathPhase.EXHALE, BreathPhase.HOLD_EMPTY -> 0.85f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (currentBreathPhase == BreathPhase.INHALE || currentBreathPhase == BreathPhase.EXHALE) 3800 else 400,
            easing = FastOutSlowInEasing
        ),
        label = "breath_circle_scale"
    )

    // Dynamic background color based on round
    val surfaceColor by animateColorAsState(
        targetValue = when (currentRound) {
            UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN -> Color(0xFF141210) // Obsidian Dark
            UrgeRound.VICTORY_REWARD -> Color(0xFFFBF8F2) // Radiant Clean
            else -> CashmereCream
        },
        animationSpec = tween(400),
        label = "surface_bg"
    )

    Dialog(
        onDismissRequest = {
            // Cannot dismiss during Round 3 lockdown until completed
            if (currentRound != UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN || isLockdownComplete) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = (currentRound != UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN || isLockdownComplete),
            dismissOnClickOutside = (currentRound != UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN || isLockdownComplete)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = surfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Multi-Round Stage Stepper Header
                RoundProgressStepper(currentRound = currentRound)

                Spacer(modifier = Modifier.height(20.dp))

                when (currentRound) {
                    UrgeRound.ROUND_1_BIOLOGICAL_RESET -> {
                        Round1BiologicalResetView(
                            currentPhase = currentBreathPhase,
                            phaseSecondsRemaining = phaseSecondsRemaining,
                            completedCycles = completedCycles,
                            animatedScale = animatedScale,
                            surgeSecondsLeft = surgeSecondsLeft,
                            whyIStartedNote = whyIStartedNote,
                            checkedGrounding = checkedGrounding,
                            onToggleGrounding = { index ->
                                if (checkedGrounding.contains(index)) {
                                    checkedGrounding.remove(index)
                                } else {
                                    checkedGrounding.add(index)
                                }
                                machine.toggleGroundingItem(index)
                            },
                            onUrgeCrushed = {
                                machine.claimVictory(complimentIndex)
                                currentRound = machine.state.currentRound
                            },
                            onEscalateToRound2 = {
                                machine.advanceToRound2()
                                currentRound = machine.state.currentRound
                            },
                            onDismiss = onDismiss
                        )
                    }

                    UrgeRound.ROUND_2_MENTAL_MIRAGE -> {
                        Round2MentalMirageView(
                            selectedHalt = selectedHaltTrigger,
                            onSelectHalt = { trigger ->
                                selectedHaltTrigger = trigger
                                machine.selectHaltTrigger(trigger)
                            },
                            typedOath = typedOathInput,
                            onOathChange = { input ->
                                typedOathInput = input
                                machine.updateRound2Oath(input)
                            },
                            isOathMatched = isOathMatched,
                            whyIStartedNote = whyIStartedNote,
                            onUrgeCrushed = {
                                machine.claimVictory(complimentIndex)
                                currentRound = machine.state.currentRound
                            },
                            onEscalateToRound3 = {
                                machine.advanceToRound3()
                                currentRound = machine.state.currentRound
                            },
                            onDismiss = onDismiss
                        )
                    }

                    UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN -> {
                        Round3NuclearLockdownView(
                            lockdownSecondsLeft = lockdownSecondsLeft,
                            isLockdownComplete = isLockdownComplete,
                            whyIStartedNote = whyIStartedNote,
                            onUrgeObliterated = {
                                if (isLockdownComplete) {
                                    machine.claimVictory(complimentIndex)
                                    currentRound = machine.state.currentRound
                                }
                            }
                        )
                    }

                    UrgeRound.VICTORY_REWARD -> {
                        VictoryRewardView(
                            compliment = VICTORY_COMPLIMENTS[complimentIndex],
                            cleanStreak = vaultState.cleanDaysStreak,
                            urgesCount = vaultState.urgesOvercomeCount + 1,
                            onClaimAndClose = {
                                FreedomVaultManager.recordUrgeOvercome()
                                onUrgeOvercome()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual Progress Indicator for the 3 Escalation Rounds + Victory
 */
@Composable
private fun RoundProgressStepper(currentRound: UrgeRound) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepPill(
            label = "1. RESET",
            isActive = currentRound == UrgeRound.ROUND_1_BIOLOGICAL_RESET,
            isCompleted = currentRound != UrgeRound.ROUND_1_BIOLOGICAL_RESET
        )
        Text("→", fontSize = 12.sp, color = MutedCharcoalOlive)
        StepPill(
            label = "2. MIRAGE",
            isActive = currentRound == UrgeRound.ROUND_2_MENTAL_MIRAGE,
            isCompleted = currentRound == UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN || currentRound == UrgeRound.VICTORY_REWARD
        )
        Text("→", fontSize = 12.sp, color = MutedCharcoalOlive)
        StepPill(
            label = "3. TITAN",
            isActive = currentRound == UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN,
            isCompleted = currentRound == UrgeRound.VICTORY_REWARD
        )
        Text("→", fontSize = 12.sp, color = MutedCharcoalOlive)
        StepPill(
            label = "VICTORY 👑",
            isActive = currentRound == UrgeRound.VICTORY_REWARD,
            isCompleted = currentRound == UrgeRound.VICTORY_REWARD
        )
    }
}

@Composable
private fun StepPill(label: String, isActive: Boolean, isCompleted: Boolean) {
    val bgColor = when {
        isActive -> AntiqueHoneyBrass
        isCompleted -> BritishRacingSage
        else -> WarmLinen
    }
    val textColor = when {
        isActive || isCompleted -> Color.White
        else -> MutedCharcoalOlive
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * ROUND 1: Biological Reset (4-4-4-4 Box Breathing, Somatic Grounding, Surge Clock)
 */
@Composable
private fun Round1BiologicalResetView(
    currentPhase: BreathPhase,
    phaseSecondsRemaining: Int,
    completedCycles: Int,
    animatedScale: Float,
    surgeSecondsLeft: Int,
    whyIStartedNote: String,
    checkedGrounding: List<Int>,
    onToggleGrounding: (Int) -> Unit,
    onUrgeCrushed: () -> Unit,
    onEscalateToRound2: () -> Unit,
    onDismiss: () -> Unit
) {
    // Header
    Box(
        modifier = Modifier
            .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "ROUND 1: BIOLOGICAL CIRCUIT BREAKER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BritishRacingSage,
            letterSpacing = 1.1.sp
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "Ride The Dopamine Wave",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = SoftEspressoInk,
        textAlign = TextAlign.Center
    )

    Text(
        text = "Your amygdala has triggered a temporary chemical surge. It peaks and drops in 3–5 minutes if you don't feed it.",
        fontSize = 13.sp,
        color = MutedCharcoalOlive,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Interactive Box Breathing Circle
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .border(1.5.dp, WhisperBorder, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(animatedScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AntiqueHoneyBrass.copy(alpha = 0.35f),
                            BritishRacingSage.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .border(2.dp, AntiqueHoneyBrass, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentPhase.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
                Text(
                    text = "${phaseSecondsRemaining}s",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AntiqueHoneyBrass
                )
            }
        }
    }

    Text(
        text = currentPhase.instruction,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = BritishRacingSage,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )

    Text(
        text = "Completed Breath Cycles: $completedCycles",
        fontSize = 11.sp,
        color = MutedCharcoalOlive,
        modifier = Modifier.padding(top = 4.dp)
    )

    // 432Hz Somatic Tone Generator Button
    var isAudioPlaying by remember { mutableStateOf(com.focuscrew.app.audio.BinauralAudioGenerator.isAudioPlaying()) }

    OutlinedButton(
        onClick = {
            if (isAudioPlaying) {
                com.focuscrew.app.audio.BinauralAudioGenerator.stop()
                isAudioPlaying = false
            } else {
                com.focuscrew.app.audio.BinauralAudioGenerator.play432Hz()
                isAudioPlaying = true
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isAudioPlaying) BritishRacingSage.copy(alpha = 0.12f) else WarmLinen,
            contentColor = if (isAudioPlaying) BritishRacingSage else SoftEspressoInk
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAudioPlaying) BritishRacingSage else WhisperBorder
        ),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isAudioPlaying) "🔊" else "🎧", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAudioPlaying) "432Hz Tone Playing (Tap to Mute)" else "Play 432Hz Somatic Frequency",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Dopamine Surge Clock Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏳", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dopamine Reality Clock",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftEspressoInk
                )
                val minutes = surgeSecondsLeft / 60
                val seconds = surgeSecondsLeft % 60
                Text(
                    text = String.format("%02d:%02d until craving baseline resets", minutes, seconds),
                    fontSize = 12.sp,
                    color = MutedCharcoalOlive
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Somatic Grounding Checklist
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Physical Grounding Checklist",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(8.dp))

            val steps = listOf(
                "Put phone face-down on table right now",
                "Drink 1 tall glass of ice-cold water",
                "Drop and do 10 pushups or 20 squats",
                "Splash cold water directly over eyes and face"
            )

            steps.forEachIndexed { index, step ->
                val isChecked = checkedGrounding.contains(index)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleGrounding(index) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                if (isChecked) BritishRacingSage else WarmLinen,
                                RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, WhisperBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        fontSize = 12.5.sp,
                        color = if (isChecked) MutedCharcoalOlive else SoftEspressoInk,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Action 1: Urge Neutralized (Victory)
    Button(
        onClick = onUrgeCrushed,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
    ) {
        Text(
            text = "🟢 Urge Neutralized — I Am In Control",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Action 2: Urge Still Strong (Escalate to Round 2)
    Button(
        onClick = onEscalateToRound2,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
    ) {
        Text(
            text = "🔴 Urge Still Calling Me ➔ Enter Round 2",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Text("Exit to Safety", fontSize = 13.sp, color = MutedCharcoalOlive)
    }
}

/**
 * ROUND 2: Mental Mirage Shatterer (H.A.L.T. Scanner, 10s vs 24h Reality Split, Cognitive Typing Oath)
 */
@Composable
private fun Round2MentalMirageView(
    selectedHalt: String?,
    onSelectHalt: (String) -> Unit,
    typedOath: String,
    onOathChange: (String) -> Unit,
    isOathMatched: Boolean,
    whyIStartedNote: String,
    onUrgeCrushed: () -> Unit,
    onEscalateToRound3: () -> Unit,
    onDismiss: () -> Unit
) {
    // Header Badge
    Box(
        modifier = Modifier
            .background(TerracottaClay.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "ROUND 2: THE MENTAL MIRAGE SHATTERER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TerracottaClay,
            letterSpacing = 1.1.sp
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "Expose The Dopamine Lie",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = SoftEspressoInk,
        textAlign = TextAlign.Center
    )

    Text(
        text = "Your brain is attempting to rationalize a fake reward. Let's dismantle the illusion piece by piece.",
        fontSize = 13.sp,
        color = MutedCharcoalOlive,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(18.dp))

    // H.A.L.T. Diagnostic
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "What is your brain actually screaming for?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(10.dp))

            val triggers = listOf("Boredom", "Loneliness", "Stress / Anxiety", "Exhaustion")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                triggers.forEach { trigger ->
                    val isSelected = selectedHalt == trigger
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) TerracottaClay else WarmLinen,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectHalt(trigger) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trigger,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else SoftEspressoInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val explanation = when (selectedHalt) {
                "Boredom" -> "💡 You are not horny. Your brain is under-stimulated. Porn will not cure boredom; it will fry your dopamine receptors so ordinary life becomes 10x more boring."
                "Loneliness" -> "💡 Watching two strangers on glass will leave you feeling completely isolated the second you finish. Turn the screen off and connect with real people."
                "Stress / Anxiety" -> "💡 Escaping into pixels will multiply your cortisol and guilt tomorrow morning. Face the stress like a sovereign warrior."
                "Exhaustion" -> "💡 Your prefrontal cortex is depleted and dropping its guard. What you need is deep sleep or a 20-min power nap, not synthetic hyper-stimulation."
                else -> "Identify the emotion behind the craving to take away its power."
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmLinen, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = explanation,
                    fontSize = 12.sp,
                    color = SoftEspressoInk,
                    lineHeight = 17.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 10-Second vs 24-Hour Reality Split Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "The Unequal Trade",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mirage side
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(TerracottaClay.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("The Trap 🎭", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TerracottaClay)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 10 seconds of cheap rush\n• Alone in a room\n• Empty hands\n• Zero accomplishment",
                        fontSize = 11.sp,
                        color = SoftEspressoInk,
                        lineHeight = 16.sp
                    )
                }

                // Reality side
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(BritishRacingSage.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text("The Price 💥", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BritishRacingSage)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 24 hrs brain fog\n• Heavy social anxiety\n• Shattered confidence\n• Looking away in shame",
                        fontSize = 11.sp,
                        color = SoftEspressoInk,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Cognitive Typing Oath (Reactivates Prefrontal Cortex)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOathMatched) BritishRacingSage else WhisperBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isOathMatched) "✅" else "✍️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Type Your Mental Anchor",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOathMatched) BritishRacingSage else SoftEspressoInk
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Type this exact sentence to engage your executive prefrontal brain:",
                fontSize = 11.sp,
                color = MutedCharcoalOlive
            )
            Text(
                text = "\"${UrgeDestroyerState.REQUIRED_OATH}\"",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AntiqueHoneyBrass,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            OutlinedTextField(
                value = typedOath,
                onValueChange = onOathChange,
                placeholder = { Text("Type the oath here...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isOathMatched) BritishRacingSage else AntiqueHoneyBrass,
                    unfocusedBorderColor = if (isOathMatched) BritishRacingSage else WhisperBorder
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Action 1: Urge Crushed
    Button(
        onClick = onUrgeCrushed,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
    ) {
        Text(
            text = "🟢 My Mind Is Clear — Urge Crushed",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Action 2: Escalate to Round 3 Strict
    Button(
        onClick = onEscalateToRound3,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E)) // Intense Crimson
    ) {
        Text(
            text = "⚡ STILL SLIPPING ➔ UNLEASH ROUND 3 (LOCKDOWN)",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Text("Exit to Safety", fontSize = 13.sp, color = MutedCharcoalOlive)
    }
}

/**
 * ROUND 3: The Nuclear Truth & Strict Lockdown
 * (Obsidian Dark mode, Post-Nut Clarity Simulator, Industry Demystification, Unskippable 45s Stare-Down)
 */
@Composable
private fun Round3NuclearLockdownView(
    lockdownSecondsLeft: Int,
    isLockdownComplete: Boolean,
    whyIStartedNote: String,
    onUrgeObliterated: () -> Unit
) {
    // Top Warning Badge
    Box(
        modifier = Modifier
            .background(Color(0xFF8B1E1E).copy(alpha = 0.25f), CircleShape)
            .border(1.dp, Color(0xFFC5221F), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "ROUND 3: TITAN NUCLEAR LOCKDOWN",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFF6B6B),
            letterSpacing = 1.2.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Face The Brutal Truth",
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFFF6ED),
        textAlign = TextAlign.Center
    )

    Text(
        text = "The illusion ends right now. Look at what you are about to do and feel the disgust before it happens.",
        fontSize = 13.sp,
        color = Color(0xFFC4B8A9),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Pre-Loaded Post-Nut Clarity Simulator Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221E1B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3E38))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👁️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "POST-NUT CLARITY SIMULATOR",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF9B71),
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Fast-forward 60 seconds after giving in:\n\n• You are sitting alone in the dark.\n• Your hand is sticky. Your phone feels filthy.\n• A cold wave of guilt and self-disgust hits your stomach.\n• You hastily close 10 browser tabs, dreading someone seeing.\n• You look in the mirror and see a tired, defeated man who traded his dignity for pixel dust.\n\nYOU KNOW THIS FEELING. YOU HATE THIS FEELING. WHY PAY FOR IT AGAIN?",
                fontSize = 12.5.sp,
                color = Color(0xFFE8DFD8),
                lineHeight = 19.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // The Raw Truth / Industry Demystification Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF221E1B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3E38))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "The Reality of the Trap ⛓️",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE5A93C)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Porn is a digital slaughterhouse for your masculine drive and ambition. Strangers who don't care about you are profiting off keeping you isolated, docile, and weak.\n\nAre you a spectator in other people's lives, or the king of your own?",
                fontSize = 12.5.sp,
                color = Color(0xFFE8DFD8),
                lineHeight = 18.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Sacred Anchor Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2520)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE5A93C))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚓", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "YOUR SACRED OATH",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE5A93C),
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"$whyIStartedNote\"",
                fontSize = 13.5.sp,
                fontStyle = FontStyle.Italic,
                color = Color.White,
                lineHeight = 19.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Mandatory 45-Second Stare-Down Timer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1815)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isLockdownComplete) Color(0xFF2E7D32) else Color(0xFF8B1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLockdownComplete) "LOCKDOWN DISCHARGED — YOU HELD THE LINE" else "UNSKIPPABLE STARE-DOWN IN PROGRESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isLockdownComplete) Color(0xFF81C784) else Color(0xFFFF6B6B),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isLockdownComplete) "0s" else "${lockdownSecondsLeft}s",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = if (isLockdownComplete) Color(0xFF81C784) else Color(0xFFFF9B71)
            )
            Text(
                text = if (isLockdownComplete)
                    "The urge has starved. Your mind has regained sovereignty."
                else
                    "Stare into your screen. Breathe through the friction. The urge is screaming because it is DYING.",
                fontSize = 12.sp,
                color = Color(0xFFC4B8A9),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // The Victory Button (Locked until timer hits 0)
    Button(
        onClick = onUrgeObliterated,
        enabled = isLockdownComplete,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32),
            disabledContainerColor = Color(0xFF332B25)
        )
    ) {
        Text(
            text = if (isLockdownComplete)
                "🛡️ I STOOD IN THE FIRE AND WON — URGE OBLITERATED"
            else
                "HOLDING THE LINE (${lockdownSecondsLeft}s remaining)",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isLockdownComplete) Color.White else Color(0xFF8C7D73)
        )
    }
}

/**
 * VICTORY REWARD & PRAISE
 * (Golden celebratory aura, rotating dopamine-healthy psychological praise, victory armor stats)
 */
@Composable
private fun VictoryRewardView(
    compliment: String,
    cleanStreak: Int,
    urgesCount: Int,
    onClaimAndClose: () -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp))

    // Radiant Crown Icon
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AntiqueHoneyBrass.copy(alpha = 0.35f),
                        BritishRacingSage.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
            .border(2.5.dp, AntiqueHoneyBrass, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("👑", fontSize = 38.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "URGE DESTROYED!",
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        color = SoftEspressoInk,
        letterSpacing = 1.sp
    )

    Text(
        text = "You Claimed Victory Over The Beast",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = BritishRacingSage
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Hero's Psychological Praise Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AntiqueHoneyBrass.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚔️ A WORD OF HONOUR ⚔️", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = AntiqueHoneyBrass)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\"$compliment\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoftEspressoInk,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Battle Stats Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmLinen),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 CLEAN STREAK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedCharcoalOlive)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$cleanStreak Days",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftEspressoInk
                )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(WhisperBorder))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡️ URGES CRUSHED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedCharcoalOlive)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$urgesCount Won",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BritishRacingSage
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Healthy Dopamine Replacement Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Next Steps For Real Dopamine ✨",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Put your phone in another room or face-down.\n2. Drink 1 cold glass of water.\n3. Take a 5-minute walk outside or wash your face.\n4. Walk back into life with your head held high.",
                fontSize = 12.sp,
                color = MutedCharcoalOlive,
                lineHeight = 18.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onClaimAndClose,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
    ) {
        Text(
            text = "🏆 Claim Victory & Return to Freedom",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
