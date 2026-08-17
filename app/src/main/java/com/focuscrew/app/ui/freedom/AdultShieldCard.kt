package com.focuscrew.app.ui.freedom

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
fun AdultShieldCard(
    state: FreedomVaultState,
    onRequireAntiImpulseDelay: () -> Unit
) {

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Zero-Leak Habit Guard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "Incognito Blocker & Anti-Impulse Delay",
                        fontSize = 12.sp,
                        color = MutedCharcoalOlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Closes private browsing loopholes and enforces a mandatory 15-minute reflection delay before any protection settings can be weakened.",
                fontSize = 12.sp,
                color = MutedCharcoalOlive,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Incognito & Browser Blocker Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Incognito Mode Blocker",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "Intercept private browsing loophole attempts",
                        fontSize = 11.sp,
                        color = MutedCharcoalOlive
                    )
                }

                Switch(
                    checked = state.blockIncognitoEnabled,
                    onCheckedChange = { isEnabled ->
                        if (!isEnabled) {
                            // Enforce anti-impulse friction delay
                            onRequireAntiImpulseDelay()
                        } else {
                            FreedomVaultManager.setBlockIncognito(true)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }

            // Anti-Impulse status indicator
            if (state.antiImpulseCooldownActive && state.antiImpulseCooldownEndsAt > System.currentTimeMillis()) {
                val remainingSec = ((state.antiImpulseCooldownEndsAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0)
                val m = remainingSec / 60
                val s = remainingSec % 60

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerracottaClay.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .border(1.dp, TerracottaClay.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⏳ Anti-Impulse Cooldown Active: ${String.format("%02d:%02d", m, s)} remaining before shields can be edited. Take a deep breath.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TerracottaClay,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
