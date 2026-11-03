package com.focuscrew.app.ui.taskalarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.data.local.entity.TaskAlarmEntity
import com.focuscrew.app.data.repository.TaskAlarmRepository
import com.focuscrew.app.service.taskalarm.TaskAlarmNotificationService
import com.focuscrew.app.ui.MainActivity
import com.focuscrew.app.ui.theme.FocusCrewTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAlarmFullScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskId = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_ID) ?: ""
        val initialTitle = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_TITLE) ?: "Task Alarm"
        val initialBody = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_BODY) ?: ""

        startAlarmSoundAndVibration()

        setContent {
            FocusCrewTheme {
                var taskEntity by remember { mutableStateOf<TaskAlarmEntity?>(null) }

                LaunchedEffect(taskId) {
                    if (taskId.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            try {
                                val db = FocusDatabase.getInstance(this@TaskAlarmFullScreenActivity)
                                taskEntity = db.taskAlarmDao().getTaskById(taskId)
                            } catch (_: Exception) {}
                        }
                    }
                }

                val title = taskEntity?.title ?: initialTitle
                val description = taskEntity?.description?.takeIf { it.isNotBlank() }
                    ?: initialBody.takeIf { it.isNotBlank() && !it.startsWith("It's ") }
                val category = taskEntity?.category

                FullScreenAlarmContent(
                    taskTitle = title,
                    taskDescription = description,
                    taskCategory = category,
                    onDone = {
                        stopAlarmSoundAndVibration()
                        TaskAlarmNotificationService.dismissNotification(this, taskId)
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = FocusDatabase.getInstance(this@TaskAlarmFullScreenActivity)
                            val repo = TaskAlarmRepository(db.taskAlarmDao(), this@TaskAlarmFullScreenActivity)
                            repo.setTaskCompleted(taskId, true)
                        }
                        finish()
                    },
                    onSnooze = { minutes ->
                        stopAlarmSoundAndVibration()
                        TaskAlarmNotificationService.dismissNotification(this, taskId)
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = FocusDatabase.getInstance(this@TaskAlarmFullScreenActivity)
                            val repo = TaskAlarmRepository(db.taskAlarmDao(), this@TaskAlarmFullScreenActivity)
                            repo.snoozeTask(taskId, minutes)
                        }
                        finish()
                    },
                    onDismiss = {
                        stopAlarmSoundAndVibration()
                        TaskAlarmNotificationService.dismissNotification(this, taskId)
                        finish()
                    },
                    onStartFocus = {
                        stopAlarmSoundAndVibration()
                        TaskAlarmNotificationService.dismissNotification(this, taskId)
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("start_preset_timer", true)
                            putExtra("planned_minutes", 25)
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startAlarmSoundAndVibration()
    }

    private fun startAlarmSoundAndVibration() {
        com.focuscrew.app.service.taskalarm.AlarmSoundRinger.start(this)
    }

    private fun stopAlarmSoundAndVibration() {
        com.focuscrew.app.service.taskalarm.AlarmSoundRinger.stop()
    }

    override fun onDestroy() {
        stopAlarmSoundAndVibration()
        super.onDestroy()
    }
}

@Composable
fun FullScreenAlarmContent(
    taskTitle: String,
    taskDescription: String?,
    taskCategory: String?,
    onDone: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    onStartFocus: () -> Unit
) {
    val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B132B) // Ultra-dark executive navy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Time & Visual Alert Pulse
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(76.dp)
                        .background(Color(0xFFDC2626).copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏰", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentTime,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = currentDate,
                    fontSize = 15.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }

            // Middle Section: Task / Alarm Name and Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                // Category & Alarm Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDC2626).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🚨", fontSize = 12.sp)
                        Text(
                            text = if (!taskCategory.isNullOrBlank()) "TASK ALARM • $taskCategory" else "TASK ALARM ACTIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Task Name
                Text(
                    text = taskTitle,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Box if present
                if (!taskDescription.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "DESCRIPTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = taskDescription,
                                fontSize = 15.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Time to complete your scheduled task and stay in the flow.",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Section: Action Controls (Big Done, Snooze, Focus Timer, Dismiss)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Done Button
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text(
                        text = "✓ Mark Task Done",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Start Focus Timer Button (Pomodoro integration)
                Button(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(
                        text = "🎯 Start 25m Focus Session",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Snooze & Dismiss Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSnooze(10) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Text("💤 Snooze 10m", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
