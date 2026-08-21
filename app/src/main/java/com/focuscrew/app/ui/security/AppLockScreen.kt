package com.focuscrew.app.ui.security

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.focuscrew.app.service.AppSecurityManager
import com.focuscrew.app.ui.theme.*

/**
 * AppLockScreen — Full-screen discrete PIN security gate.
 * Prevents anyone else from opening FocusCrew or modifying protection settings.
 */
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CashmereCream)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(BritishRacingSage.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🔒", fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "SECURITY LOCK",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SoftEspressoInk,
            letterSpacing = 1.sp
        )

        Text(
            text = "Enter your 4-digit passcode to unlock FocusCrew",
            fontSize = 12.sp,
            color = MutedCharcoalOlive,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4 PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isFilled = index < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (isFilled) BritishRacingSage else WarmLinen,
                            CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage!!,
                color = TerracottaClay,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Discrete Numeric Keypad (1 - 9, 0)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { digit ->
                        KeypadButton(
                            label = digit,
                            onClick = {
                                when (digit) {
                                    "C" -> {
                                        enteredPin = ""
                                        errorMessage = null
                                    }
                                    "⌫" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                            errorMessage = null
                                        }
                                    }
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            val newPin = enteredPin + digit
                                            enteredPin = newPin
                                            if (newPin.length == 4) {
                                                if (AppSecurityManager.verifyPin(newPin)) {
                                                    onUnlocked()
                                                } else {
                                                    errorMessage = "Incorrect passcode. Try again."
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .background(DaylightPaper, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SoftEspressoInk
        )
    }
}
