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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                            .background(BritishRacingSage.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏳", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LIFE RECLAIMED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BritishRacingSage,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Time & Capital Saved",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(AntiqueHoneyBrass.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+$${stats.focusCapitalDollars}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AntiqueHoneyBrass
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Metric: Hours Saved
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmLinen, RoundedCornerShape(16.dp))
                    .border(1.dp, WhisperBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TOTAL HOURS SAVED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedCharcoalOlive,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${stats.totalHoursSaved}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = " hrs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedCharcoalOlive,
                            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "FOCUS VALUE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedCharcoalOlive,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${stats.focusCapitalDollars}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BritishRacingSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Equivalency Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Books Read
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DaylightPaper, RoundedCornerShape(12.dp))
                        .border(1.dp, WhisperBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("📚 Books Read", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedCharcoalOlive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.booksReadEquivalent}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SoftEspressoInk)
                        Text("full books", fontSize = 10.sp, color = MutedCharcoalOlive)
                    }
                }

                // Workouts
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DaylightPaper, RoundedCornerShape(12.dp))
                        .border(1.dp, WhisperBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("💪 Workouts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedCharcoalOlive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.workoutsEquivalent}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SoftEspressoInk)
                        Text("gym sessions", fontSize = 10.sp, color = MutedCharcoalOlive)
                    }
                }

                // Deep Work Days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DaylightPaper, RoundedCornerShape(12.dp))
                        .border(1.dp, WhisperBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("⚡ Deep Flow", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedCharcoalOlive)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${stats.deepWorkDaysEquivalent}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = SoftEspressoInk)
                        Text("work days", fontSize = 10.sp, color = MutedCharcoalOlive)
                    }
                }
            }
        }
    }
}
