package com.focuscrew.app.service.taskalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.data.repository.TaskAlarmRepository
import com.focuscrew.app.ui.taskalarm.TaskAlarmFullScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val body = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_BODY)
            ?: run {
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                "It's $timeStr — time to do your $title task."
            }
        val payload = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_PAYLOAD)

        Log.d("TaskAlarmReceiver", "Alarm triggered for task: $taskId - $title")

        // Acquire WakeLock to ensure device stays awake to sound the alarm on time
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "FocusCrew:TaskAlarmWakeLock"
        )
        try {
            wakeLock?.acquire(45_000L) // 45 seconds wake lock
        } catch (_: Exception) {}

        // 0. Start continuous loud alarm sound & vibration immediately
        AlarmSoundRinger.start(context)

        // 1. Show notification with alarm sound, vibration, and Done/Snooze action buttons
        TaskAlarmNotificationService.showNotification(
            context = context,
            id = taskId,
            title = "⏰ $title",
            body = body,
            payload = payload
        )

        // 2. Also directly launch full-screen lockscreen alarm activity
        try {
            val fullScreenIntent = Intent(context, TaskAlarmFullScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TaskAlarmNotificationService.EXTRA_TASK_ID, taskId)
                putExtra(TaskAlarmNotificationService.EXTRA_TASK_TITLE, title)
                putExtra(TaskAlarmNotificationService.EXTRA_TASK_BODY, body)
            }
            context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("TaskAlarmReceiver", "Could not start fullScreenActivity directly: ${e.message}")
        }

        // 3. Advance recurrence if recurring, using repository
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusDatabase.getInstance(context)
                val repository = TaskAlarmRepository(db.taskAlarmDao(), context)
                repository.handleAlarmTriggered(taskId)
            } catch (e: Exception) {
                Log.e("TaskAlarmReceiver", "Error processing alarm trigger: ${e.message}")
            } finally {
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (_: Exception) {}
                pendingResult.finish()
            }
        }
    }
}
