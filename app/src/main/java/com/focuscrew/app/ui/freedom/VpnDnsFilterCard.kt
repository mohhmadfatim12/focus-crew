package com.focuscrew.app.ui.freedom

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.service.AntiPornDefenderManager
import com.focuscrew.app.service.FocusCrewVpnService
import com.focuscrew.app.ui.theme.BritishRacingSage
import com.focuscrew.app.ui.theme.DaylightPaper
import com.focuscrew.app.ui.theme.MutedCharcoalOlive
import com.focuscrew.app.ui.theme.SoftEspressoInk
import com.focuscrew.app.ui.theme.TerracottaClay
import com.focuscrew.app.ui.theme.WarmLinen
import com.focuscrew.app.ui.theme.WhisperBorder

import com.focuscrew.app.service.SystemPrivateDnsManager

/**
 * 1-Click Strict DNS & Adult Shield Card.
 *
 * Provides a single, 1-click activation that:
 *  1. Launches split-tunnel local DNS VPN (blocks 1000+ porn sites to 0.0.0.0).
 *  2. Directly configures Android System Private DNS to adult-filter-dns.cleanbrowsing.org.
 *  3. Server-locks Google & Bing SafeSearch VIP (216.239.38.120) so adult pictures/previews cannot load.
 *  4. Enables real-time browser keyword interception.
 *  5. Keeps all normal web searches, YouTube, and apps running at 100% full native speed.
 */
@Composable
fun VpnDnsFilterCard() {
    val context = LocalContext.current
    val isVpnRunning by FocusCrewVpnService.isVpnRunning.collectAsState()
    val domainsBlocked by FocusCrewVpnService.domainsBlockedCount.collectAsState()

    // Activity Result Launcher for 1-Click VPN Permission Dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FocusCrewVpnService.setVpnEnabled(true)
            AntiPornDefenderManager.setDefenderEnabled(true)
            AntiPornDefenderManager.setSafeSearchEnforced(true)
            SystemPrivateDnsManager.setAdultPrivateDnsEnabled(context, true)
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
            Toast.makeText(context, "🛡️ Dopamine Detox Shield ACTIVATED!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Shield setup was cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    val trigger1ClickActivation: () -> Unit = {
        // 1. Immediately set Android System Private DNS
        SystemPrivateDnsManager.setAdultPrivateDnsEnabled(context, true)

        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            // Android OS prompt: "FocusCrew wants to set up a VPN connection"
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            // Permission already granted — start immediately with 1 tap!
            FocusCrewVpnService.setVpnEnabled(true)
            AntiPornDefenderManager.setDefenderEnabled(true)
            AntiPornDefenderManager.setSafeSearchEnforced(true)
            val vpnIntent = Intent(context, FocusCrewVpnService::class.java)
            context.startService(vpnIntent)
            Toast.makeText(context, "🛡️ Dopamine Detox Shield ACTIVATED!", Toast.LENGTH_SHORT).show()
        }
    }

    val deactivateShield: () -> Unit = {
        FocusCrewVpnService.setVpnEnabled(false)
        SystemPrivateDnsManager.setAdultPrivateDnsEnabled(context, false)
        val vpnIntent = Intent(context, FocusCrewVpnService::class.java).apply {
            action = "STOP_VPN"
        }
        context.startService(vpnIntent)
        Toast.makeText(context, "Dopamine Detox Shield deactivated", Toast.LENGTH_SHORT).show()
    }

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
                            .size(46.dp)
                            .background(
                                if (isVpnRunning) BritishRacingSage.copy(alpha = 0.15f)
                                else TerracottaClay.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isVpnRunning) "🛡️" else "⚡",
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Strict Focus DNS Shield",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftEspressoInk
                        )
                        Text(
                            text = if (isVpnRunning) "100% Enforced • SafeSearch Locked" else "1-Click Full System Protection",
                            fontSize = 12.sp,
                            color = if (isVpnRunning) BritishRacingSage else MutedCharcoalOlive,
                            fontWeight = if (isVpnRunning) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Switch(
                    checked = isVpnRunning,
                    onCheckedChange = { shouldEnable ->
                        if (shouldEnable) trigger1ClickActivation() else deactivateShield()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1-CLICK ACTION BUTTON (WHEN OFF)
            if (!isVpnRunning) {
                Button(
                    onClick = trigger1ClickActivation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text(
                        text = "⚡ ACTIVATE SHIELD (1-CLICK)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No manual copying or settings needed. 1 click blocks 1000+ distraction sites, locks Google SafeSearch & filters media. Normal browsing stays full speed.",
                    fontSize = 11.sp,
                    color = MutedCharcoalOlive,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val ok = SystemPrivateDnsManager.setAdultPrivateDnsEnabled(context, true)
                            if (ok) {
                                Toast.makeText(context, "✅ Private DNS locked to cleanbrowsing.org!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
                    ) {
                        Text("⚡ Set Private DNS", fontSize = 11.sp, color = BritishRacingSage, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { SystemPrivateDnsManager.openNetworkSettings(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
                    ) {
                        Text("⚙️ Phone Settings", fontSize = 11.sp, color = SoftEspressoInk, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // ACTIVE METRICS CARD (WHEN RUNNING)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BritishRacingSage.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, BritishRacingSage.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "● SHIELD ACTIVE & ENFORCING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BritishRacingSage
                            )
                            Text(
                                text = "$domainsBlocked Domains Blocked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BritishRacingSage
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Features active list
                        Text("✓ Android System DNS: CleanBrowsing Filter", fontSize = 11.sp, color = BritishRacingSage, fontWeight = FontWeight.SemiBold)
                        Text("✓ 1000+ Distraction Domains Blackholed", fontSize = 11.sp, color = SoftEspressoInk)
                        Text("✓ Google & Bing SafeSearch Server-Locked", fontSize = 11.sp, color = SoftEspressoInk)
                        Text("✓ Unhealthy Images, Previews & Videos Filtered", fontSize = 11.sp, color = SoftEspressoInk)
                        Text("✓ All Normal Searches & Apps: 100% Full Speed", fontSize = 11.sp, color = SoftEspressoInk)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { SystemPrivateDnsManager.openNetworkSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BritishRacingSage.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "⚙️ Verify Private DNS in Phone Settings",
                        fontSize = 12.sp,
                        color = BritishRacingSage,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = deactivateShield,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhisperBorder)
                ) {
                    Text(
                        text = "Turn Off Shield",
                        fontSize = 12.sp,
                        color = TerracottaClay,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Coverage Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoverageBadge("All Browsers", isVpnRunning)
                CoverageBadge("Incognito Tabs", isVpnRunning)
                CoverageBadge("SafeSearch VIP", isVpnRunning)
                CoverageBadge("Full Speed", isVpnRunning)
            }
        }
    }
}

@Composable
private fun CoverageBadge(label: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (isActive) BritishRacingSage.copy(alpha = 0.1f) else WarmLinen,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isActive) BritishRacingSage.copy(alpha = 0.3f) else WhisperBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (isActive) "✓ $label" else "○ $label",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) BritishRacingSage else MutedCharcoalOlive
        )
    }
}
