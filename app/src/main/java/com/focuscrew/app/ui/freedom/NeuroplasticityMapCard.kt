package com.focuscrew.app.ui.freedom

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.domain.NeuroplasticityTimeline
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun NeuroplasticityMapCard(
    cleanDays: Int,
    modifier: Modifier = Modifier
) {
    val current = remember(cleanDays) { NeuroplasticityTimeline.getCurrentMilestone(cleanDays) }
    val next = remember(cleanDays) { NeuroplasticityTimeline.getNextMilestone(cleanDays) }
    val progress = remember(cleanDays) { NeuroplasticityTimeline.getProgressToNextMilestone(cleanDays) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AntiqueHoneyBrass.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧠", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NEUROPLASTICITY MAP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AntiqueHoneyBrass,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Dopamine & Brain Rewiring",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(BritishRacingSage.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Day $cleanDays Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BritishRacingSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Phase Highlight Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmLinen, RoundedCornerShape(16.dp))
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = current.phaseName.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AntiqueHoneyBrass,
                            letterSpacing = 0.8.sp
                        )
                        Text(current.iconEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = current.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = current.biologicalBenefit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BritishRacingSage
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = current.neuroscienceDetail,
                        fontSize = 12.sp,
                        color = MutedCharcoalOlive,
                        lineHeight = 16.sp
                    )
                }
            }

            // Progress Bar to Next Phase
            if (next != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next: ${next.title} (Day ${next.dayRequired})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedCharcoalOlive
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueHoneyBrass
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AntiqueHoneyBrass,
                    trackColor = WarmLinen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expand / Collapse Full 90-Day Roadmap Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide Full 90-Day Roadmap ▲" else "View All 8 Brain Milestones ▼",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueHoneyBrass
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeuroplasticityTimeline.MILESTONES.forEach { milestone ->
                        val isReached = cleanDays >= milestone.dayRequired
                        val isCurrent = current.dayRequired == milestone.dayRequired

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) AntiqueHoneyBrass.copy(alpha = 0.08f)
                                    else if (isReached) WarmLinen
                                    else DaylightPaper,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) AntiqueHoneyBrass
                                    else if (isReached) WhisperBorder
                                    else WhisperBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isReached) "✅" else "🔒",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Day ${milestone.dayRequired} — ${milestone.title}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isReached) SoftEspressoInk else MutedCharcoalOlive.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = milestone.biologicalBenefit,
                                    fontSize = 11.sp,
                                    color = if (isReached) BritishRacingSage else MutedCharcoalOlive.copy(alpha = 0.5f)
                                )
                            }
                            Text(milestone.iconEmoji, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
