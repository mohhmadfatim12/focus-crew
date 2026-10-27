package com.focuscrew.app.service.taskalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.data.repository.TaskAlarmRepository
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

        // 1. Show notification with alarm sound, vibration, and Done/Snooze action buttons
        TaskAlarmNotificationService.showNotification(
            context = context,
            id = taskId,
            title = "⏰ $title",
            body = body,
            payload = payload
        )

        // 2. Advance recurrence if recurring, using repository
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusDatabase.getInstance(context)
                val repository = TaskAlarmRepository(db.taskAlarmDao(), context)
                repository.handleAlarmTriggered(taskId)
            } catch (e: Exception) {
                Log.e("TaskAlarmReceiver", "Error processing alarm trigger: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
