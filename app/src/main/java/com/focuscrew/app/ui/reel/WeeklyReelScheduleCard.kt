package com.focuscrew.app.ui.reel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.model.DayReelEntry
import com.focuscrew.app.service.WeeklyReelScheduleManager
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.AntiqueHoneyBrassContainer
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.BritishRacingSageContainer
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.OnAntiqueHoneyBrassContainer
import com.focuscrew.app.ui.theme.SagePillBg
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.TerracottaClay
import com.focuscrew.app.ui.theme.TerracottaContainer
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WarmLinenHigh
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun WeeklyReelScheduleCard(
    modifier: Modifier = Modifier
) {
    val schedule by WeeklyReelScheduleManager.schedule.collectAsState()
    val dailyLimit = schedule.dailyLimit.coerceAtLeast(1)

    Card(
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WhisperBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BritishRacingSageContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📅", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly Reel Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Mon – Sun Scroll Rhythm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedCharcoalOlive,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Total Weekly Count & Duration Badge
                Surface(
                    color = SagePillBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WhisperBorder)
                ) {
                    Text(
                        text = "${schedule.totalWeeklyReels} Reels • ${schedule.formattedTotalDuration}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BritishRacingSage,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7 Days Columns Row (MON -> SUN)
            val maxBarCount = (schedule.days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(dailyLimit).coerceAtLeast(1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                schedule.days.forEach { dayEntry ->
                    DayColumn(
                        entry = dayEntry,
                        dailyLimit = dailyLimit,
                        maxBarCount = maxBarCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Summary & Sunday 12:01 AM Reset Notice
            Surface(
                color = WarmLinen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ Avg: ${schedule.dailyAverage}/day • ${schedule.formattedTotalDuration} total",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftEspressoInk,
                        maxLines = 1
                    )
                    Text(
                        text = "🔄 Resets Sun 12:01 AM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MutedCharcoalOlive,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    entry: DayReelEntry,
    dailyLimit: Int,
    maxBarCount: Int,
    modifier: Modifier = Modifier
) {
    val barFraction = (entry.count.toFloat() / maxBarCount.toFloat()).coerceIn(0.06f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = barFraction,
        animationSpec = tween(durationMillis = 600),
        label = "barFill"
    )

    // Status-based color coding
    val (barColor, countBadgeBg, countTextColor) = when {
        entry.count <= 0 -> Triple(WarmLinenHigh, Color.Transparent, MutedCharcoalOlive.copy(alpha = 0.5f))
        entry.count <= dailyLimit * 0.7f -> Triple(BritishRacingSage, SagePillBg, BritishRacingSage)
        entry.count <= dailyLimit -> Triple(AntiqueHoneyBrass, AntiqueHoneyBrassContainer, OnAntiqueHoneyBrassContainer)
        else -> Triple(TerracottaClay, TerracottaContainer, TerracottaClay)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (entry.isToday) AntiqueHoneyBrassContainer.copy(alpha = 0.35f) else Color.Transparent)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Count Badge
        Text(
            text = if (entry.count > 0) "${entry.count}" else "0",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (entry.count > 0) FontWeight.Bold else FontWeight.Normal,
            color = countTextColor,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Vertical Bar Track
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(65.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WarmLinenHigh),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFraction)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Exact Time Spent Badge
        Text(
            text = entry.formattedDuration,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (entry.timeSpentMs > 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (entry.timeSpentMs > 0) SoftEspressoInk else MutedCharcoalOlive.copy(alpha = 0.6f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Day of Week Label (Single-line non-wrapped Mon..Sun)
        Text(
            text = entry.day.shortName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (entry.isToday) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (entry.isToday) AntiqueHoneyBrass else MutedCharcoalOlive,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false
        )

        // Today pill indicator
        if (entry.isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(AntiqueHoneyBrass)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
