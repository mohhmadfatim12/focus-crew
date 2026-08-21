package com.focuscrew.app.ui.security

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.service.AppSecurityManager
import com.focuscrew.app.ui.theme.*

/**
 * SecuritySettingsCard — Manage PIN Passcode, Settings Anti-Tamper, and Clock Guard.
 */
@Composable
fun SecuritySettingsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPinSet by remember { mutableStateOf(AppSecurityManager.isPinEnabled()) }
    var antiTamperEnabled by remember { mutableStateOf(AppSecurityManager.isSettingsAntiTamperActive.value) }
    var showSetPinDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DaylightPaper),
        border = BorderStroke(1.dp, WhisperBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "App Privacy & Security Lock",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "PIN Passcode & Android Settings Anti-Tamper",
                        fontSize = 11.sp,
                        color = MutedCharcoalOlive
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: Passcode Protection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "4-Digit PIN Passcode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = if (isPinSet) "Passcode is active. App is locked on exit." else "Lock FocusCrew with a secret 4-digit code",
                        fontSize = 11.sp,
                        color = if (isPinSet) BritishRacingSage else MutedCharcoalOlive
                    )
                }

                Switch(
                    checked = isPinSet,
                    onCheckedChange = { enable ->
                        if (enable) {
                            showSetPinDialog = true
                        } else {
                            AppSecurityManager.clearPin()
                            isPinSet = false
                            Toast.makeText(context, "Passcode lock disabled", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Android Settings Anti-Tamper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Settings Anti-Tamper Guard",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftEspressoInk
                    )
                    Text(
                        text = "Prevents Force-Stop & Uninstall in phone settings during active focus",
                        fontSize = 11.sp,
                        color = MutedCharcoalOlive
                    )
                }

                Switch(
                    checked = antiTamperEnabled,
                    onCheckedChange = { enable ->
                        AppSecurityManager.setSettingsAntiTamper(enable)
                        antiTamperEnabled = enable
                        Toast.makeText(
                            context,
                            if (enable) "🛡️ Settings Anti-Tamper ON" else "Anti-Tamper OFF",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                        checkedTrackColor = BritishRacingSage,
                        uncheckedThumbColor = MutedCharcoalOlive,
                        uncheckedTrackColor = WarmLinen
                    )
                )
            }
        }
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        var tempPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = {
                Text("Set 4-Digit Passcode", fontWeight = FontWeight.Bold, color = SoftEspressoInk)
            },
            text = {
                Column {
                    Text(
                        "Enter a 4-digit PIN to lock FocusCrew from unauthorized eyes.",
                        fontSize = 12.sp,
                        color = MutedCharcoalOlive
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) tempPin = it },
                        label = { Text("4 Digits") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BritishRacingSage,
                            unfocusedBorderColor = WhisperBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPin.length == 4) {
                            AppSecurityManager.setPin(tempPin)
                            isPinSet = true
                            showSetPinDialog = false
                            Toast.makeText(context, "✅ PIN Passcode Set!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BritishRacingSage)
                ) {
                    Text("Save PIN", color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = MutedCharcoalOlive)
                }
            },
            containerColor = DaylightPaper
        )
    }
}
