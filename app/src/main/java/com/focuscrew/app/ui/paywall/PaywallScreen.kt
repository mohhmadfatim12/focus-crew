package com.focuscrew.app.ui.paywall

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.model.SubscriptionTier
import com.focuscrew.app.service.TrialSubscriptionManager
import com.focuscrew.app.ui.theme.*

/**
 * PaywallScreen — High-Converting Premium Paywall for FocusCrew.
 * Shows value gained during 5-day trial and offers 3 subscription tiers:
 *   - $80 / year (Best Value)
 *   - $40 / 6 months
 *   - $6 / month
 */
@Composable
fun PaywallScreen(
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subState by TrialSubscriptionManager.subscriptionState.collectAsState()
    var selectedTier by remember { mutableStateOf(SubscriptionTier.ANNUAL) }
    var isPurchasing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CashmereCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Close Button (only if trial is still active)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(AntiqueHoneyBrass.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (subState.isTrialActive) "⏳ ${subState.trialDaysRemaining} DAYS LEFT IN TRIAL" else "⚠️ TRIAL COMPLETED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AntiqueHoneyBrass,
                    letterSpacing = 0.5.sp
                )
            }

            if (onDismiss != null && subState.isTrialActive) {
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 18.sp, color = MutedCharcoalOlive, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Icon & Title
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("👑", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "FOCUSCREW PRO",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SoftEspressoInk,
            letterSpacing = 1.sp
        )

        Text(
            text = "All-In-One Dopamine Detox & Deep Work Fortress",
            fontSize = 13.sp,
            color = MutedCharcoalOlive,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Value Statement Card (Social Proof & Tangible Results)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DaylightPaper),
            border = BorderStroke(1.dp, WhisperBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "YOUR TRIAL IMPACT SO FAR:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BritishRacingSage,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                ImpactRow("🛡️ Distractions Blocked", "${subState.distractionsBlockedCount} attempts stopped")
                ImpactRow("⏳ Screen Time Saved", String.format("%.1f hours reclaimed", subState.estimatedHoursSaved))
                ImpactRow("🔒 Protection Active", "SafeSearch VIP & Curfew Enforced")
                ImpactRow("⚡ Privacy Guarantee", "100% Offline • Zero Data Tracking")
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "SELECT YOUR COMMITMENT PLAN:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SoftEspressoInk,
            letterSpacing = 0.5.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Plan 1: 1 Year Elite Mastery (Best Value)
        TierCard(
            tier = SubscriptionTier.ANNUAL,
            isSelected = selectedTier == SubscriptionTier.ANNUAL,
            onClick = { selectedTier = SubscriptionTier.ANNUAL }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Plan 2: 6 Months Focus Pass
        TierCard(
            tier = SubscriptionTier.SEMI_ANNUAL,
            isSelected = selectedTier == SubscriptionTier.SEMI_ANNUAL,
            onClick = { selectedTier = SubscriptionTier.SEMI_ANNUAL }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Plan 3: 1 Month Flexible
        TierCard(
            tier = SubscriptionTier.MONTHLY,
            isSelected = selectedTier == SubscriptionTier.MONTHLY,
            onClick = { selectedTier = SubscriptionTier.MONTHLY }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Big CTA Button
        Button(
            onClick = {
                isPurchasing = true
                TrialSubscriptionManager.activateSubscription(selectedTier)
                Toast.makeText(context, "🎉 Welcome to FocusCrew Pro!", Toast.LENGTH_LONG).show()
                isPurchasing = false
                onDismiss?.invoke()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "⚡ UNLOCK FULL PRO ACCESS",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reassurance footer
        Text(
            text = "🔒 1-Click Offline Unlock • No Hidden Subscriptions • Ad-Free Forever",
            fontSize = 11.sp,
            color = MutedCharcoalOlive,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ImpactRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = SoftEspressoInk, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = BritishRacingSage, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TierCard(
    tier: SubscriptionTier,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AntiqueHoneyBrass else WhisperBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val bgColor = if (isSelected) AntiqueHoneyBrass.copy(alpha = 0.05f) else DaylightPaper

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (tier.badge != null) {
                Box(
                    modifier = Modifier
                        .background(
                            if (tier.isBestValue) AntiqueHoneyBrass else BritishRacingSage,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tier.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = tier.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "${tier.periodDisplay} • ${tier.dailyPriceDisplay}",
                        fontSize = 12.sp,
                        color = MutedCharcoalOlive
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier.priceDisplay,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) AntiqueHoneyBrass else SoftEspressoInk
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                if (isSelected) AntiqueHoneyBrass else Color.Transparent,
                                CircleShape
                            )
                            .border(1.5.dp, if (isSelected) AntiqueHoneyBrass else WhisperBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
