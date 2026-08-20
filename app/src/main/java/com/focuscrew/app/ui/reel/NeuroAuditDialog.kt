package com.focuscrew.app.ui.reel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.R
import com.focuscrew.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * NeuroAuditScreen: 5-Stage High-Converting Socratic Diagnostic & Soft-Persuasion Funnel.
 * 
 * 1. Empathetic Hook & Micro-Commitment Questions (Removing guilt, establishing aspiration & triggers).
 * 2. Emotional Reality & Prefrontal Amnesia Check (Confronting working memory blackout).
 * 3. The Contrast Shock & Trajectory Model (Side-by-side Path A vs. Path B with live odometers).
 * 4. Value Construction Dynamic Scanner (Anticipation generation).
 * 5. Soft-Persuasion Value-Anchor Screen (Softly convinces user to commit to their personalized protocol).
 */
@Composable
fun NeuroAuditDialog(
    onDismiss: () -> Unit,
    onComplete: (targetDailyLimit: Int) -> Unit
) {
    NeuroAuditScreen(onDismiss = onDismiss, onComplete = onComplete)
}

@Composable
fun NeuroAuditScreen(
    onDismiss: () -> Unit,
    onComplete: (targetDailyLimit: Int) -> Unit
) {
    BackHandler { onDismiss() }
    val haptic = LocalHapticFeedback.current
    var currentStep by remember { mutableIntStateOf(1) } // Steps 1 to 5

    // Diagnostic State Collected Across Funnel
    var selectedAspiration by remember { mutableStateOf<String?>(null) }
    var selectedTrigger by remember { mutableStateOf<String?>(null) }
    var selectedFeeling by remember { mutableStateOf<String?>(null) }
    var amnesiaResponse by remember { mutableStateOf<Boolean?>(null) }
    var dailyHours by remember { mutableFloatStateOf(2.5f) }
    var hourlyRate by remember { mutableFloatStateOf(30f) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(CashmereCream)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = CashmereCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation / Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧠 NEURO-DIAGNOSTIC FUNNEL",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = AntiqueHoneyBrass,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )

                Surface(
                    color = WarmLinen,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, WhisperBorder)
                ) {
                    Text(
                        text = "STEP $currentStep/5",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueHoneyBrass,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Smooth Progress Bar
            val animatedProgress by animateFloatAsState(
                targetValue = currentStep / 5f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "audit_progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AntiqueHoneyBrass,
                trackColor = WarmLinen
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step Content Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> Step1EmpatheticHookAndAspiration(
                        selectedAspiration = selectedAspiration,
                        onSelectAspiration = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAspiration = it
                        },
                        selectedTrigger = selectedTrigger,
                        onSelectTrigger = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTrigger = it
                        }
                    )
                    2 -> Step2EmotionalRealityAndAmnesia(
                        selectedFeeling = selectedFeeling,
                        onSelectFeeling = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFeeling = it
                        },
                        amnesiaResponse = amnesiaResponse,
                        onSelectResponse = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            amnesiaResponse = it
                        }
                    )
                    3 -> Step3ContrastShockAndCalculator(
                        dailyHours = dailyHours,
                        onHoursChange = { dailyHours = it },
                        hourlyRate = hourlyRate,
                        onRateChange = { hourlyRate = it }
                    )
                    4 -> Step4SynthesizingBlueprintScan(
                        onScanComplete = {
                            currentStep = 5
                        }
                    )
                    5 -> Step5SoftPersuasionConversion(
                        selectedAspiration = selectedAspiration ?: "High-Income Career Mastery",
                        selectedTrigger = selectedTrigger ?: "Late Night in Bed",
                        selectedFeeling = selectedFeeling ?: "Mentally Drained & Guilty",
                        dailyHours = dailyHours,
                        hourlyRate = hourlyRate,
                        onCommit = { targetLimit ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete(targetLimit)
                        }
                    )
                }
            }

            // Bottom Action Navigation Buttons (for steps 1-3)
            if (currentStep in 1..3) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WhisperBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedCharcoalOlive)
                        ) {
                            Text("Back")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, WhisperBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedCharcoalOlive)
                        ) {
                            Text("Skip")
                        }
                    }

                    val canProceed = when (currentStep) {
                        1 -> selectedAspiration != null && selectedTrigger != null
                        2 -> selectedFeeling != null && amnesiaResponse != null
                        3 -> true
                        else -> false
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentStep++
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD9480F),
                            contentColor = Color.White,
                            disabledContainerColor = WarmLinen,
                            disabledContentColor = MutedCharcoalOlive.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (currentStep == 3) "Synthesize My Plan 🔬" else "Continue ➔",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STAGE 1: EMPATHETIC HOOK & ASPIRATION
// ==========================================
@Composable
private fun Step1EmpatheticHookAndAspiration(
    selectedAspiration: String?,
    onSelectAspiration: (String) -> Unit,
    selectedTrigger: String?,
    onSelectTrigger: (String) -> Unit
) {
    val aspirations = listOf(
        "🚀 Launching a business or side hustle",
        "🎓 Acing competitive exams & top grades",
        "💼 Mastering high-income skills & career growth",
        "🏋️ Peak health, physical energy & deep sleep"
    )

    val triggers = listOf(
        "🌙 Late night in bed (Trading sleep & recovery)",
        "🌅 Morning wake-up (Cortisol spike into dopamine loop)",
        "💼 Work/study avoidance (Fleeing mental resistance)",
        "🚽 Idle moments & breaks (The autopilot reflex)"
    )

    // Mascot Squash & Stretch Spring Physics
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_squash")
    val mascotScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_scale"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Empathetic Validation Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DaylightPaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .scale(mascotScale)
                            .background(WarmLinen, CircleShape)
                            .border(1.5.dp, WhisperBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_brain_state_0),
                            contentDescription = "Living Brain Mascot",
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\"It is not your fault...\"",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = SoftEspressoInk
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Trillion-dollar algorithms employ 4,000+ behavioral neuroscientists to exploit your dopamine system. Answer 4 quick questions to see your personalized recovery plan.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                            color = MutedCharcoalOlive
                        )
                    }
                }
            }
        }

        // Question 1: Primary Dream
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1. What is the #1 dream you're putting off?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                ),
                color = SoftEspressoInk
            )
        }

        items(aspirations.size) { index ->
            val aspiration = aspirations[index]
            val isSelected = selectedAspiration == aspiration
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) WarmLinenHigh else DaylightPaper
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectAspiration(aspiration) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AntiqueHoneyBrass else WhisperBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = if (isSelected) AntiqueHoneyBrass else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(2.dp, AntiqueHoneyBrass, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = aspiration,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = if (isSelected) SoftEspressoInk else MutedCharcoalOlive
                    )
                }
            }
        }

        // Question 2: Trigger
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "2. When does the scroll trance hijack you hardest?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                ),
                color = SoftEspressoInk
            )
        }

        items(triggers.size) { index ->
            val trigger = triggers[index]
            val isSelected = selectedTrigger == trigger
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) WarmLinenHigh else DaylightPaper
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTrigger(trigger) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AntiqueHoneyBrass else WhisperBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = if (isSelected) AntiqueHoneyBrass else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(2.dp, AntiqueHoneyBrass, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = trigger,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = if (isSelected) SoftEspressoInk else MutedCharcoalOlive
                    )
                }
            }
        }
    }
}

// ==========================================
// STAGE 2: EMOTIONAL REALITY & AMNESIA CHECK
// ==========================================
@Composable
private fun Step2EmotionalRealityAndAmnesia(
    selectedFeeling: String?,
    onSelectFeeling: (String) -> Unit,
    amnesiaResponse: Boolean?,
    onSelectResponse: (Boolean) -> Unit
) {
    val feelings = listOf(
        "😔 Mentally drained, guilty & unmotivated",
        "🤯 Anxious, overstimulated & distracted",
        "😶 Numb autopilot, lost track of precious time",
        "⚡ Rarely energized or fulfilled"
    )

    var showTypingDots by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(350)
        showTypingDots = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Question 3: Post-Scroll Feeling
        item {
            Text(
                text = "3. How do you honestly feel 30 mins after reels?",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                ),
                color = SoftEspressoInk
            )
        }

        items(feelings.size) { index ->
            val feeling = feelings[index]
            val isSelected = selectedFeeling == feeling
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) WarmLinenHigh else DaylightPaper
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFeeling(feeling) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AntiqueHoneyBrass else WhisperBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                color = if (isSelected) AntiqueHoneyBrass else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(2.dp, AntiqueHoneyBrass, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feeling,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = if (isSelected) SoftEspressoInk else MutedCharcoalOlive
                    )
                }
            }
        }

        // Question 4: Amnesia Confrontation
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DaylightPaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREFRONTAL CORTEX TEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = AntiqueHoneyBrass,
                            letterSpacing = 1.sp
                        )

                        if (showTypingDots) {
                            TypingDotsIndicator()
                        }
                    }

                    Text(
                        text = "Without opening Instagram or YouTube:\nCan you clearly name the creator, topic, or lesson of the last 3 reels you watched?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            fontSize = 15.5.sp
                        ),
                        color = SoftEspressoInk
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSelectResponse(false) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (amnesiaResponse == false) TerracottaClay else TerracottaContainer,
                        contentColor = if (amnesiaResponse == false) DaylightPaper else TerracottaClay
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (amnesiaResponse == false) TerracottaClay else WhisperBorder,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Text(
                        text = "🤯 Total Blank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = { onSelectResponse(true) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (amnesiaResponse == true) BritishRacingSage else SagePillBg,
                        contentColor = if (amnesiaResponse == true) DaylightPaper else BritishRacingSage
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (amnesiaResponse == true) BritishRacingSage else WhisperBorder,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Text(
                        text = "🧠 Remember All 3",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = amnesiaResponse != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (amnesiaResponse == false) TerracottaTint else SagePillBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (amnesiaResponse == false) TerracottaClay.copy(alpha = 0.3f) else BritishRacingSage.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (amnesiaResponse == false) "💥 NEURO-DIAGNOSIS: PREFRONTAL BLACKOUT" else "⚖️ THE COGNITIVE AUDIT CHALLENGE",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (amnesiaResponse == false) TerracottaClay else BritishRacingSage
                        )
                        Text(
                            text = if (amnesiaResponse == false)
                                "You just traded your finite human attention for content your working memory didn't even preserve. The algorithm hijacked your dopamine motor reflex, leaving you mentally exhausted with zero cognitive return."
                            else
                                "Say the actionable lesson out loud right now. If it didn't increase your income, deepen relationships, or improve your health, you traded valuable neural bandwidth for digital candy disguised as information.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                            color = SoftEspressoInk
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STAGE 3: THE CONTRAST SHOCK & CALCULATOR
// ==========================================
@Composable
private fun Step3ContrastShockAndCalculator(
    dailyHours: Float,
    onHoursChange: (Float) -> Unit,
    hourlyRate: Float,
    onRateChange: (Float) -> Unit
) {
    val annualHours = (dailyHours * 365).toInt()
    val rawYearsLost = (annualHours * 35f) / (24f * 365f)
    val animatedYears by animateFloatAsState(
        targetValue = rawYearsLost,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "years_anim"
    )

    val targetWealth = (annualHours * hourlyRate).toInt()
    val animatedWealth by animateIntAsState(
        targetValue = targetWealth,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "wealth_anim"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Your Lifetime & Wealth Divergence",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                ),
                color = SoftEspressoInk
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Every hour spent swiping is permanently subtracted from your financial and cognitive prime.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedCharcoalOlive
            )
        }

        // Slider 1: Daily Scroll Hours
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daily Screen Scrolling Time:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoalOlive,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${String.format("%.1f", dailyHours)} hrs / day",
                        style = MaterialTheme.typography.bodySmall,
                        color = AntiqueHoneyBrass,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = dailyHours,
                    onValueChange = onHoursChange,
                    valueRange = 0.5f..6.0f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = AntiqueHoneyBrass,
                        activeTrackColor = AntiqueHoneyBrass,
                        inactiveTrackColor = WarmLinenHigh
                    )
                )
            }
        }

        // Slider 2: Target Hourly Rate
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Target Hourly Earning Potential:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedCharcoalOlive,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$${hourlyRate.toInt()} / hr",
                        style = MaterialTheme.typography.bodySmall,
                        color = BritishRacingSage,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = hourlyRate,
                    onValueChange = onRateChange,
                    valueRange = 10f..150f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = BritishRacingSage,
                        activeTrackColor = BritishRacingSage,
                        inactiveTrackColor = WarmLinenHigh
                    )
                )
            }
        }

        // Slot-Machine Ticker Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TerracottaTint),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, TerracottaClay.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⏳ LIFETIME LOST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = TerracottaClay
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", animatedYears)} YRS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "staring at glass 24/7",
                            fontSize = 10.sp,
                            color = MutedCharcoalOlive
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SagePillBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BritishRacingSage.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💸 ANNUAL WEALTH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = BritishRacingSage
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${animatedWealth}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "sacrificed / year",
                            fontSize = 10.sp,
                            color = MutedCharcoalOlive
                        )
                    }
                }
            }
        }

        // Side-by-Side Trajectory Comparison (Path A vs Path B)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DaylightPaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚖️ THE 1-YEAR TRAJECTORY CONTRAST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AntiqueHoneyBrass
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Path A
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(TerracottaTint, RoundedCornerShape(10.dp))
                                .border(1.dp, TerracottaClay.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("PATH A: NO SHIELD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TerracottaClay)
                                Text("• +$annualHours hrs surrendered", fontSize = 10.5.sp, color = SoftEspressoInk)
                                Text("• 42-sec attention span", fontSize = 10.5.sp, color = SoftEspressoInk)
                                Text("• Competitors pull ahead", fontSize = 10.5.sp, color = SoftEspressoInk)
                            }
                        }

                        // Path B
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SagePillBg, RoundedCornerShape(10.dp))
                                .border(1.dp, BritishRacingSage.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("PATH B: FOCUS PROTOCOL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = BritishRacingSage)
                                Text("• +$annualHours hrs to your dream", fontSize = 10.5.sp, color = SoftEspressoInk)
                                Text("• Clean dopamine recovery", fontSize = 10.5.sp, color = SoftEspressoInk)
                                Text("• Deep focus advantage", fontSize = 10.5.sp, color = SoftEspressoInk)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STAGE 4: SYNTHESIZING CUSTOM BLUEPRINT SCAN
// ==========================================
@Composable
private fun Step4SynthesizingBlueprintScan(
    onScanComplete: () -> Unit
) {
    var localProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Synthesizing your dopamine profile...") }

    // Pulsing Scanner Rings Animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LaunchedEffect(Unit) {
        delay(350)
        localProgress = 0.25f
        statusText = "Measuring prefrontal working memory attenuation..."
        delay(500)
        localProgress = 0.55f
        statusText = "Analyzing short-video novelty trigger frequency..."
        delay(500)
        localProgress = 0.85f
        statusText = "Calibrating autonomous swipe cap & friction shields..."
        delay(500)
        localProgress = 1.0f
        statusText = "Custom Focus Protocol successfully generated."
        delay(300)
        onScanComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(pulseScale)
                .background(AntiqueHoneyBrassContainer, CircleShape)
                .border(3.dp, AntiqueHoneyBrass, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brain_state_3),
                contentDescription = "Neural Scan",
                modifier = Modifier.size(76.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "GENERATING YOUR CUSTOM FOCUS BLUEPRINT",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            color = AntiqueHoneyBrass,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedCharcoalOlive,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        LinearProgressIndicator(
            progress = { localProgress },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(7.dp)
                .clip(RoundedCornerShape(3.5.dp)),
            color = AntiqueHoneyBrass,
            trackColor = WarmLinenHigh
        )
    }
}

// ==========================================
// STAGE 5: SOFT-PERSUASION COMMITMENT CONVERSION
// ==========================================
@Composable
private fun Step5SoftPersuasionConversion(
    selectedAspiration: String,
    selectedTrigger: String,
    selectedFeeling: String,
    dailyHours: Float,
    hourlyRate: Float,
    onCommit: (targetLimit: Int) -> Unit
) {
    val severityScore = (dailyHours * 20f).coerceIn(45f, 96f).toInt()
    val annualWealth = (dailyHours * 365 * hourlyRate).toInt()
    val recommendedLimit = when {
        dailyHours >= 3.5f -> 30
        dailyHours >= 2.0f -> 40
        else -> 50
    }

    val morphMascotRes = if (severityScore >= 75) {
        R.drawable.ic_brain_state_4 // Melting zombie brainrot
    } else {
        R.drawable.ic_brain_state_3 // Heavy strain exhausted
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Blueprint Announcement Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(AntiqueHoneyBrass, AntiqueHoneyBrassHover)),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✨ YOUR CUSTOM FOCUS BLUEPRINT IS READY",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Diagnosis & Profile Summary
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DaylightPaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = morphMascotRes),
                        contentDescription = "Degraded Brain",
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Brainrot Exposure: $severityScore%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TerracottaClay
                        )
                        Text(
                            text = "Primary Goal: $selectedAspiration",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AntiqueHoneyBrass
                        )
                        Text(
                            text = "Identified Drain: $selectedTrigger",
                            fontSize = 11.sp,
                            color = MutedCharcoalOlive
                        )
                    }
                }
            }
        }

        // The 3 Tailored Protocol Shields
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DaylightPaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, WhisperBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🛡️ INCLUDED IN YOUR CUSTOM PROTOCOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AntiqueHoneyBrass
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1️⃣", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Autonomous Daily Limiter ($recommendedLimit Reels/Day)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SoftEspressoInk)
                            Text("Hard stops mindless bingeing when your neural cap is reached.", fontSize = 10.5.sp, color = MutedCharcoalOlive)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("2️⃣", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Living Capsule Mascot HUD", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SoftEspressoInk)
                            Text("Visual accountability floating over feeds showing live brain state.", fontSize = 10.5.sp, color = MutedCharcoalOlive)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("3️⃣", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Predictive Deep-Work Rest Breaks", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SoftEspressoInk)
                            Text("Blocks social apps during sessions with earned recharge unlocks.", fontSize = 10.5.sp, color = MutedCharcoalOlive)
                        }
                    }
                }
            }
        }

        // Soft-Persuasion Value Anchor Box
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SagePillBg, RoundedCornerShape(14.dp))
                    .border(1.dp, BritishRacingSage.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "💎 THE VALUE ANCHOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = BritishRacingSage
                    )
                    Text(
                        text = "• Other brain apps charge: $9.99/mo ($120/year)\n" +
                               "• FocusCrew is: $0.00 FREE FOREVER • NO ADS • 100% OFFLINE\n" +
                               "• Annual Opportunity Reclaimed: $$annualWealth / year\n\n" +
                               "Your only cost: Commit to stopping at your daily limit.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = SoftEspressoInk
                    )
                }
            }
        }

        // Final High-Converting Activation CTA
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = { onCommit(recommendedLimit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD9480F),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "🔥 ACTIVATE MY CUSTOM PROTOCOL ➔",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

// Bouncing 3-dot typing indicator
@Composable
private fun TypingDotsIndicator() {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 150, easing = LinearEasing), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, delayMillis = 300, easing = LinearEasing), RepeatMode.Reverse),
        label = "d3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFA94D).copy(alpha = dot1Alpha), CircleShape))
        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFA94D).copy(alpha = dot2Alpha), CircleShape))
        Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFA94D).copy(alpha = dot3Alpha), CircleShape))
    }
}
