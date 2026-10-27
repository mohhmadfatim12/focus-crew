package com.focuscrew.app.ui.freedom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.domain.LifeReclaimedCalculator
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun LifeReclaimedCard(
    cleanDays: Int,
    searchesIntercepted: Int,
    urgesConquered: Int,
    hourlyRate: Int = LifeReclaimedCalculator.DEFAULT_HOURLY_RATE,
    modifier: Modifier = Modifier
) {
    val stats = remember(cleanDays, searchesIntercepted, urgesConquered, hourlyRate) {
        LifeReclaimedCalculator.calculate(
            cleanDays = cleanDays,
            searchesIntercepted = searchesIntercepted,
            urgesConquered = urgesConquered,
            hourlyRate = hourlyRate
        )
    }

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
                            .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏳", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Life Reclaimed",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                }

                Box(
                    modifier = Modifier
                        .background(AntiqueHoneyBrass.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "+$${stats.focusCapitalDollars} Value",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueHoneyBrass
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact 3-Column Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReclaimedStatItem(
                    icon = "⏱️",
                    value = "${stats.totalHoursSaved}h",
                    label = "Time Saved",
                    modifier = Modifier.weight(1f)
                )
                ReclaimedStatItem(
                    icon = "💪",
                    value = "${stats.workoutsEquivalent}",
                    label = "Workouts",
                    modifier = Modifier.weight(1f)
                )
                ReclaimedStatItem(
                    icon = "⚡",
                    value = "${stats.deepWorkDaysEquivalent}d",
                    label = "Deep Flow",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReclaimedStatItem(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(WarmLinen, RoundedCornerShape(10.dp))
            .border(1.dp, WhisperBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SoftEspressoInk
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MutedCharcoalOlive,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
