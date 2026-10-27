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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val progress = remember(cleanDays) { NeuroplasticityTimeline.getProgressToNextMilestone(cleanDays) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AntiqueHoneyBrass.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧠", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Brain Rewiring",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                }

                Box(
                    modifier = Modifier
                        .background(BritishRacingSage.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Day $cleanDays",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BritishRacingSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current Phase & Milestone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(current.iconEmoji, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${current.phaseName}: ${current.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk,
                        maxLines = 1
                    )
                }

                Text(
                    text = if (isExpanded) "Hide ▲" else "Details ▼",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AntiqueHoneyBrass,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(start = 6.dp, top = 2.dp, bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = BritishRacingSage,
                trackColor = WarmLinen
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 1-Line Biological Benefit
            Text(
                text = current.biologicalBenefit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = BritishRacingSage,
                maxLines = 1
            )

            // Collapsible Details (Hidden by default so screen stays clean!)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(WarmLinen, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = current.neuroscienceDetail,
                        fontSize = 11.sp,
                        color = SoftEspressoInk,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
