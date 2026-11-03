package com.focuscrew.app.ui.freedom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.service.AntiPornDefenderManager
import com.focuscrew.app.ui.theme.AntiqueHoneyBrass
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.TerracottaClay
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder

@Composable
fun AntiPornDefenderCard() {
    val context = LocalContext.current
    val isEnabled by AntiPornDefenderManager.isDefenderEnabled.collectAsState()
    val blockedSearches by AntiPornDefenderManager.blockedSearchesCount.collectAsState()
    val isSafeSearch by AntiPornDefenderManager.isSafeSearchEnforced.collectAsState()

    var showPurgeConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(TerracottaClay.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚫", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dopamine Detox Defender",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = "Live Search & Focus Blocker",
                            fontSize = 12.sp,
                            color = MutedCharcoalOlive
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { AntiPornDefenderManager.setDefenderEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Module 1 Status Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isEnabled) BritishRacingSage.copy(alpha = 0.08f) else WarmLinen,
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        if (isEnabled) BritishRacingSage.copy(alpha = 0.25f) else WhisperBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnabled) "● DEFENDER SHIELD ACTIVE" else "○ SHIELD DISABLED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) BritishRacingSage else MutedCharcoalOlive
                    )
                    Text(
                        text = "$blockedSearches Interceptions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) BritishRacingSage else MutedCharcoalOlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation
            Text(
                text = "Module 1: Real-Time Keyword Interception. Filters unhealthy search triggers and distraction domains as you type in Chrome, Brave, Opera, Firefox, Edge, and Samsung Internet before pages load.",
                fontSize = 12.sp,
                color = MutedCharcoalOlive,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Module 2: SafeSearch Enforcer (Blocks Images & Pics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Module 2: SafeSearch Image Shield",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "Enforces strict SafeSearch and cleans unhealthy search thumbnails",
                        fontSize = 11.sp,
                        color = MutedCharcoalOlive
                    )
                }

                Box(
                    modifier = Modifier
                        .background(BritishRacingSage.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🔒 LOCKED ON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BritishRacingSage
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🛡️ SafeSearch is permanently enforced. Attempts to disable it in search engines or browser settings are automatically blocked.",
                fontSize = 10.5.sp,
                color = MutedCharcoalOlive,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Module 3: 1-Tap "Purge Browser Traces" (Clean Slate Button)
            Button(
                onClick = { showPurgeConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧹", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Module 3: Purge Browser Traces & History",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showPurgeConfirmation) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmation = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧹", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Purge Browser Traces?",
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "This will immediately wipe your clipboard history and launch Chrome's Clear Browsing Data screen so you can wipe history and cached files in 1 tap.",
                        fontSize = 13.sp,
                        color = MutedCharcoalOlive,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPurgeConfirmation = false
                        try {
                            val intent = AntiPornDefenderManager.executePurgeTraces(context)
                            context.startActivity(intent)
                            Toast.makeText(context, "Clipboard wiped! Opening Clear Browsing Data...", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Please clear history in Chrome Settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaClay)
                ) {
                    Text("Wipe & Open Chrome", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmation = false }) {
                    Text("Cancel", color = MutedCharcoalOlive)
                }
            },
            containerColor = DaylightPaper
        )
    }
}
