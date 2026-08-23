package com.focuscrew.app.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.service.FocusSoundEngine
import com.focuscrew.app.service.FocusSoundType
import com.focuscrew.app.ui.theme.*

/**
 * FocusSoundCard — 100% Offline Binaural Beats & Ambient Sound Generator.
 * Gives students and deep workers instant audio flow state without needing Spotify.
 */
@Composable
fun FocusSoundCard(
    modifier: Modifier = Modifier
) {
    val activeSound by FocusSoundEngine.activeSound.collectAsState()
    val volume by FocusSoundEngine.volume.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (activeSound != FocusSoundType.OFF) BritishRacingSage.copy(alpha = 0.15f) else WarmLinen,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeSound.icon, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Flow State Sound",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = if (activeSound == FocusSoundType.OFF) "Offline Binaural Beats & Ambient Masking" else activeSound.title,
                            fontSize = 11.sp,
                            color = if (activeSound != FocusSoundType.OFF) BritishRacingSage else MutedCharcoalOlive,
                            fontWeight = if (activeSound != FocusSoundType.OFF) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Text(
                    text = if (isExpanded) "▲" else "▼",
                    color = MutedCharcoalOlive,
                    fontSize = 12.sp
                )
            }

            if (isExpanded || activeSound != FocusSoundType.OFF) {
                Spacer(modifier = Modifier.height(14.dp))

                // Sound Selector Pills
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusSoundType.values().forEach { sound ->
                        val isSelected = activeSound == sound

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) BritishRacingSage.copy(alpha = 0.1f) else WarmLinen,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) BritishRacingSage else WhisperBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    FocusSoundEngine.setSound(sound)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sound.icon, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = sound.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BritishRacingSage else SoftEspressoInk
                                    )
                                    Text(
                                        text = sound.description,
                                        fontSize = 10.sp,
                                        color = MutedCharcoalOlive
                                    )
                                }
                            }

                            if (isSelected) {
                                Text("● PLAYING", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = BritishRacingSage)
                            }
                        }
                    }
                }

                if (activeSound != FocusSoundType.OFF) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔉", fontSize = 14.sp)
                        Slider(
                            value = volume,
                            onValueChange = { FocusSoundEngine.setVolume(it) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = BritishRacingSage,
                                activeTrackColor = BritishRacingSage,
                                inactiveTrackColor = WarmLinen
                            )
                        )
                        Text("🔊", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
