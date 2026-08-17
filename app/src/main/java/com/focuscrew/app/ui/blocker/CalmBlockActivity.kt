package com.focuscrew.app.ui.blocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.ui.MainActivity
import com.focuscrew.app.ui.theme.FocusCrewTheme
import com.focuscrew.app.ui.timer.TimerManager

class CalmBlockActivity : ComponentActivity() {

    private val blockedLabelState = mutableStateOf("This app")
    private val blockTypeState = mutableStateOf(BLOCK_TYPE_TIMER_APP)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "This app"
        val rawLabel = intent.getStringExtra(EXTRA_BLOCKED_LABEL) ?: blockedPackage
        blockedLabelState.value = rawLabel
        blockTypeState.value = intent.getStringExtra(EXTRA_BLOCK_TYPE) ?: inferBlockType(rawLabel)

        // When user taps system back button, take them safely to the Android Home screen
        onBackPressedDispatcher.addCallback(this) {
            goToHomeScreen()
        }

        setContent {
            FocusCrewTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) { innerPadding ->
                    CalmBlockScreen(
                        blockedAppLabel = blockedLabelState.value,
                        blockType = blockTypeState.value,
                        onReturnToFocus = {
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(mainIntent)
                            finish()
                        },
                        onGoHome = {
                            goToHomeScreen()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: "This app"
        val rawLabel = intent.getStringExtra(EXTRA_BLOCKED_LABEL) ?: blockedPackage
        blockedLabelState.value = rawLabel
        blockTypeState.value = intent.getStringExtra(EXTRA_BLOCK_TYPE) ?: inferBlockType(rawLabel)
    }

    private fun inferBlockType(label: String): String {
        val lower = label.lowercase()
        return when {
            lower.contains("safesearch") || lower.contains("dns") -> BLOCK_TYPE_SAFESEARCH
            lower.contains("adult") || lower.contains("wrong thing") || lower.contains("domain") ||
            lower.contains("curfew") || lower.contains("keyword") || lower.contains("incognito") ||
            lower.contains("leet") || lower.contains("squashed") || lower.contains("obfuscated") ||
            lower.contains("phonetic") || lower.contains("ngram") || lower.contains("compound") -> BLOCK_TYPE_ADULT_CONTENT
            else -> BLOCK_TYPE_TIMER_APP
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCKED_LABEL = "extra_blocked_label"
        const val EXTRA_BLOCK_TYPE = "extra_block_type"

        const val BLOCK_TYPE_TIMER_APP = "timer_app"
        const val BLOCK_TYPE_ADULT_CONTENT = "adult_content"
        const val BLOCK_TYPE_SAFESEARCH = "safesearch"
    }
}

@Composable
fun CalmBlockScreen(
    blockedAppLabel: String,
    blockType: String = CalmBlockActivity.BLOCK_TYPE_TIMER_APP,
    onReturnToFocus: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot by TimerManager.snapshot.collectAsState()
    val isAdultOrWrongSearch = blockType == CalmBlockActivity.BLOCK_TYPE_ADULT_CONTENT
    val isSafeSearchLock = blockType == CalmBlockActivity.BLOCK_TYPE_SAFESEARCH

    val quotes = remember {
        listOf(
            "Discipline is choosing between what you want now and what you want most." to "Abraham Lincoln",
            "The pain of discipline weighs ounces; the pain of regret weighs tons." to "Jim Rohn",
            "Urges are like waves: they rise, crest, and break. You are stronger than the urge." to "Mindful Focus",
            "Deep work is the superpower of the 21st century." to "Cal Newport",
            "Small daily disciplines repeated consistently lead to monumental achievements." to "John C. Maxwell"
        )
    }
    val currentQuote = remember { quotes.random() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual badge
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isAdultOrWrongSearch || isSafeSearchLock) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                }
            ),
            modifier = Modifier.size(76.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isAdultOrWrongSearch) "🚫" else if (isSafeSearchLock) "🔒" else "🌱",
                    fontSize = 36.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = when {
                isAdultOrWrongSearch -> "You Are Searching the Wrong Thing!"
                isSafeSearchLock -> if (blockedAppLabel.contains("DNS", ignoreCase = true)) "DNS Shield Is Locked" else "Safe Search Is Locked"
                else -> "Take a Breath"
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (isAdultOrWrongSearch) 23.sp else if (isSafeSearchLock) 24.sp else 30.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = if (isAdultOrWrongSearch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                isAdultOrWrongSearch -> "FocusCrew intercepted explicit search terms to protect your discipline & dopamine."
                isSafeSearchLock -> if (blockedAppLabel.contains("DNS", ignoreCase = true)) "DNS tampering is locked to preserve Nuclear Shield SafeSearch." else "Safe Search is locked on to protect your focus."
                else -> "You chose to focus on \"${snapshot.activeSubject}\" right now."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Intercept Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isAdultOrWrongSearch -> "⚠️ You Are Searching the Wrong Thing"
                        isSafeSearchLock -> "Safe Search Mode Locked"
                        else -> blockedAppLabel
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdultOrWrongSearch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isAdultOrWrongSearch -> "You are searching the wrong thing.\nSafeSearch and Focus Shield have blocked this content to protect your discipline and focus."
                        isSafeSearchLock -> "Safe Search mode is permanently locked on. Distracting and unhealthy content cannot be unblurred or turned off."
                        else -> "is gently blocked during your focus session"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (!isAdultOrWrongSearch && !isSafeSearchLock && snapshot.remainingMs > 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "⏳ ${snapshot.formattedTime} Remaining",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Philosophical Discipline Quote Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "“${currentQuote.first}”",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "— ${currentQuote.second}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onReturnToFocus,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Open FocusCrew",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Back to Home Screen",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
