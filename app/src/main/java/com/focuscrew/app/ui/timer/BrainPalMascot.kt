package com.focuscrew.app.ui.timer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.R
import com.focuscrew.app.domain.model.TimerStatus

/**
 * Cute, animated BrainPal Mascot with living breathing animations,
 * floating bobbing motion, and manipulative psychological micro-copy.
 */
@Composable
fun BrainPalMascot(
    status: TimerStatus,
    isStrictLockdown: Boolean,
    isBreak: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "brainPalAnimations")

    // Smooth breathing scale
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotBreathe"
    )

    // Smooth floating vertical bobbing
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotFloat"
    )

    // Pulsing aura glow behind mascot
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotAura"
    )

    val isRunning = status is TimerStatus.Running
    val isPaused = status is TimerStatus.Paused
    val isCompleted = status is TimerStatus.Completed

    val mascotDrawable = when {
        isBreak -> R.drawable.ic_brain_state_0
        isRunning -> R.drawable.ic_brain_state_0
        isPaused -> R.drawable.ic_brain_state_1
        isCompleted -> R.drawable.ic_brain_state_0
        else -> R.drawable.ic_brain_state_0
    }

    val glowColor = when {
        isStrictLockdown -> MaterialTheme.colorScheme.error
        isBreak -> MaterialTheme.colorScheme.tertiary
        isRunning -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    val headlineText = when {
        isBreak -> "Synaptic Recharge Active"
        isRunning -> if (isStrictLockdown) "Fortress Protocol Engaged" else "Prefrontal Cortex Healing"
        isPaused -> if (isStrictLockdown) "Zero Escape: Apps Blocked" else "Dopamine Craving Detected"
        isCompleted -> "Prefrontal Circuit Upgraded!"
        isStrictLockdown -> "Willpower Shield Armed"
        else -> "Rewire Your Brain"
    }

    val manipulativeSubtext = when {
        isBreak -> "Rest your eyes. Scrolling now destroys your neuroplastic gains."
        isRunning -> if (isStrictLockdown) "0 loopholes. Every second of resistance builds unshakeable discipline." else "Dopamine receptors healing. Cheap distraction is losing its grip on you."
        isPaused -> if (isStrictLockdown) "Apps remain 100% blocked on pause. Resume when you are ready to conquer." else "Your brain wants an easy escape. Giving up now drains your focus and willpower."
        isCompleted -> "You chose discipline over instant regret. Your future self thanks you."
        isStrictLockdown -> "No early exits allowed once started. Only the strong survive."
        else -> "Choose your focus. Cheap dopamine wants you weak and distracted."
    }

    val statusPillText = when {
        isBreak -> "☕ RECHARGING"
        isRunning -> if (isStrictLockdown) "🔒 UNBREAKABLE" else "⚡ REWIRING"
        isPaused -> "⏸ RESIST THE URGE"
        isCompleted -> "🏆 CONQUERED"
        isStrictLockdown -> "🔒 STRICT ACTIVE"
        else -> "🧠 READY"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, glowColor.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Animated Mascot Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .offset(y = floatY.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, glowColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(34.dp)
                        .scale(breatheScale)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = mascotDrawable),
                            contentDescription = "BrainPal Mascot",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Punchy Headline
            Text(
                text = headlineText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sleek Status Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = glowColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, glowColor.copy(alpha = 0.35f))
            ) {
                Text(
                    text = statusPillText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = glowColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
