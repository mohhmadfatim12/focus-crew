package com.focuscrew.app.ui.freedom

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.model.FreedomVaultState
import com.focuscrew.app.service.FreedomVaultManager
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.TerracottaClay
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun DangerZoneSettingsCard(
    state: FreedomVaultState,
    isCurfewActiveNow: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isCurfewActiveNow) TerracottaClay.copy(alpha = 0.12f)
                                else BritishRacingSage.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isCurfewActiveNow) "🌙" else "⏰", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Night Curfew",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = if (state.dangerZoneEnabled) "Bedtime lockdown enabled" else "Disabled",
                            fontSize = 11.sp,
                            color = MutedCharcoalOlive
                        )
                    }
                }

                Switch(
                    checked = state.dangerZoneEnabled,
                    onCheckedChange = { isEnabled ->
                        FreedomVaultManager.setDangerZone(isEnabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }

            if (state.dangerZoneEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                // Active Status Badge
                val startFormatted = formatTime(state.dangerZoneStartHour, state.dangerZoneStartMinute)
                val endFormatted = formatTime(state.dangerZoneEndHour, state.dangerZoneEndMinute)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurfewActiveNow) TerracottaClay.copy(alpha = 0.08f) else BritishRacingSage.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isCurfewActiveNow) TerracottaClay.copy(alpha = 0.25f) else BritishRacingSage.copy(alpha = 0.25f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCurfewActiveNow) "● CURFEW ACTIVE NOW" else "○ Scheduled: $startFormatted – $endFormatted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurfewActiveNow) TerracottaClay else BritishRacingSage
                        )
                        Text(
                            text = if (isCurfewActiveNow) "Lockdown" else "Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurfewActiveNow) TerracottaClay else BritishRacingSage
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Schedule Presets
                val presets = listOf(
                    Triple("11 PM – 6 AM", 23 to 0, 6 to 0),
                    Triple("10 PM – 5 AM", 22 to 0, 5 to 0),
                    Triple("12 AM – 7 AM", 0 to 0, 7 to 0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, startPair, endPair) ->
                        val isSelected = state.dangerZoneStartHour == startPair.first &&
                                state.dangerZoneEndHour == endPair.first

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) AntiqueHoneyBrass.copy(alpha = 0.12f) else WarmLinen,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) AntiqueHoneyBrass else WhisperBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    FreedomVaultManager.setDangerZone(
                                        enabled = true,
                                        startHour = startPair.first,
                                        startMinute = startPair.second,
                                        endHour = endPair.first,
                                        endMinute = endPair.second
                                    )
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AntiqueHoneyBrass else SoftEspressoInk
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Locked Apps Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Curfew Lock:",
                        fontSize = 11.sp,
                        color = MutedCharcoalOlive
                    )
                    Text(
                        text = "${state.dangerZoneTriggerPackages.size} Browsers & Media Apps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BritishRacingSage
                    )
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%d:%02d %s", h, minute, amPm)
}
