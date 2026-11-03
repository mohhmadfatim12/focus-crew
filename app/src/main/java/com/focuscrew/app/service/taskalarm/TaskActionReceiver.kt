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

class TaskActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra(TaskAlarmNotificationService.EXTRA_TASK_ID) ?: return
        val action = intent.action ?: return

        Log.d("TaskActionReceiver", "Action received: $action for task $taskId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusDatabase.getInstance(context)
                val repository = TaskAlarmRepository(db.taskAlarmDao(), context)

                // Stop ringing immediately on any user action
                AlarmSoundRinger.stop()

                when (action) {
                    TaskAlarmNotificationService.ACTION_COMPLETE_TASK -> {
                        repository.setTaskCompleted(taskId, true)
                        TaskAlarmNotificationService.dismissNotification(context, taskId)
                    }

                    TaskAlarmNotificationService.ACTION_SNOOZE_TASK -> {
                        val minutes = intent.getIntExtra("extra_snooze_minutes", 10)
                        repository.snoozeTask(taskId, minutes)
                        TaskAlarmNotificationService.dismissNotification(context, taskId)
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskActionReceiver", "Error executing action $action: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
