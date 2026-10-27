package com.focuscrew.app.data.repository

import android.content.Context
import android.util.Log
import com.focuscrew.app.data.local.dao.TaskAlarmDao
import com.focuscrew.app.data.local.entity.TaskAlarmEntity
import com.focuscrew.app.service.taskalarm.TaskAlarmNotificationService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskAlarmRepository(
    private val dao: TaskAlarmDao,
    private val context: Context
) {
    private val TAG = "TaskAlarmRepository"

    val allTasks: Flow<List<TaskAlarmEntity>> = dao.getAllTasksFlow()

    suspend fun getTaskById(id: String): TaskAlarmEntity? = dao.getTaskById(id)

    suspend fun insertTask(task: TaskAlarmEntity) {
        val calculatedTask = if (task.scheduledTimeMillis <= System.currentTimeMillis() && task.recurrence != "NONE") {
            task.copy(scheduledTimeMillis = calculateNextOccurrence(task.scheduledTimeMillis, task.recurrence, task.customWeekdays))
        } else {
            task
        }
        dao.insertTask(calculatedTask)
        if (calculatedTask.isEnabled && !calculatedTask.isCompleted) {
            scheduleNotificationForTask(calculatedTask)
        }
        com.focuscrew.app.widget.TaskAlarmAppWidgetProvider.notifyUpdate(context)
    }

    suspend fun updateTask(task: TaskAlarmEntity) {
        TaskAlarmNotificationService.cancelAlarm(context, task.id)
        val calculatedTask = if (task.scheduledTimeMillis <= System.currentTimeMillis() && task.recurrence != "NONE") {
            task.copy(scheduledTimeMillis = calculateNextOccurrence(task.scheduledTimeMillis, task.recurrence, task.customWeekdays))
        } else {
            task
        }
        dao.updateTask(calculatedTask)
        if (calculatedTask.isEnabled && !calculatedTask.isCompleted) {
            scheduleNotificationForTask(calculatedTask)
        }
        com.focuscrew.app.widget.TaskAlarmAppWidgetProvider.notifyUpdate(context)
    }

    suspend fun deleteTask(id: String) {
        TaskAlarmNotificationService.cancelAlarm(context, id)
        dao.deleteTaskById(id)
        com.focuscrew.app.widget.TaskAlarmAppWidgetProvider.notifyUpdate(context)
    }

    suspend fun setTaskCompleted(id: String, completed: Boolean) {
        val task = dao.getTaskById(id) ?: return
        if (completed) {
            TaskAlarmNotificationService.cancelAlarm(context, id)
            dao.setTaskCompleted(id, true, System.currentTimeMillis())
        } else {
            val now = System.currentTimeMillis()
            val newScheduledTime = if (task.scheduledTimeMillis <= now) {
                if (task.recurrence != "NONE") {
                    calculateNextOccurrence(task.scheduledTimeMillis, task.recurrence, task.customWeekdays)
                } else {
                    now + (60 * 60 * 1000) // Default 1 hr from now if in past
                }
            } else {
                task.scheduledTimeMillis
            }
            dao.updateSchedule(id, newScheduledTime, 0, now)
            dao.setTaskCompleted(id, false, null)
            val updated = dao.getTaskById(id)
            if (updated != null && updated.isEnabled) {
                scheduleNotificationForTask(updated)
            }
        }
        com.focuscrew.app.widget.TaskAlarmAppWidgetProvider.notifyUpdate(context)
    }

    suspend fun setTaskEnabled(id: String, enabled: Boolean) {
        dao.setTaskEnabled(id, enabled)
        val task = dao.getTaskById(id) ?: return
        if (enabled && !task.isCompleted) {
            scheduleNotificationForTask(task)
        } else {
            TaskAlarmNotificationService.cancelAlarm(context, id)
        }
    }

    suspend fun snoozeTask(id: String, minutes: Int) {
        val task = dao.getTaskById(id) ?: return
        val newTime = System.currentTimeMillis() + (minutes.toLong() * 60 * 1000)
        dao.snoozeTask(id, newTime, 1)
        val updated = task.copy(
            scheduledTimeMillis = newTime,
            snoozeCount = task.snoozeCount + 1,
            isEnabled = true,
            isCompleted = false
        )
        scheduleNotificationForTask(updated)
    }

    suspend fun handleAlarmTriggered(id: String) {
        val task = dao.getTaskById(id) ?: return
        if (task.recurrence != "NONE") {
            val nextTime = calculateNextOccurrence(task.scheduledTimeMillis, task.recurrence, task.customWeekdays)
            val nextTask = task.copy(
                scheduledTimeMillis = nextTime,
                snoozeCount = 0,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.updateTask(nextTask)
            scheduleNotificationForTask(nextTask)
            Log.d(TAG, "Recurring task $id rescheduled for next occurrence: $nextTime")
        }
    }

    suspend fun rescheduleAllActiveAlarms() {
        val activeTasks = dao.getActivePendingTasks()
        val now = System.currentTimeMillis()
        for (task in activeTasks) {
            if (task.scheduledTimeMillis <= now && task.recurrence != "NONE") {
                val nextTime = calculateNextOccurrence(task.scheduledTimeMillis, task.recurrence, task.customWeekdays)
                val updated = task.copy(scheduledTimeMillis = nextTime)
                dao.updateTask(updated)
                scheduleNotificationForTask(updated)
            } else if (task.scheduledTimeMillis > now) {
                scheduleNotificationForTask(task)
            }
        }
        Log.d(TAG, "Rescheduled ${activeTasks.size} active tasks after reboot")
    }

    private fun scheduleNotificationForTask(task: TaskAlarmEntity) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(task.scheduledTimeMillis))
        val body = "It's $formattedTime — time to do your ${task.title} task."

        TaskAlarmNotificationService.scheduleAlarm(
            context = context,
            id = task.id,
            triggerAtMillis = task.scheduledTimeMillis,
            title = task.title,
            body = body,
            isExact = task.isExactAlarm,
            payload = task.id
        )
    }

    fun calculateNextOccurrence(baseTimeMillis: Long, recurrence: String, customWeekdays: String = ""): Long {
        val calendar = Calendar.getInstance()
        val baseCal = Calendar.getInstance().apply { timeInMillis = baseTimeMillis }

        val targetHour = baseCal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = baseCal.get(Calendar.MINUTE)
        val targetSecond = 0

        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, targetMinute)
        calendar.set(Calendar.SECOND, targetSecond)
        calendar.set(Calendar.MILLISECOND, 0)

        val now = System.currentTimeMillis()

        when (recurrence) {
            "DAILY" -> {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            "WEEKLY" -> {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            "WEEKDAYS" -> { // Monday (2) to Friday (6)
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                ) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            "CUSTOM_WEEKDAYS" -> {
                val allowedDays = customWeekdays.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()

                if (allowedDays.isNotEmpty()) {
                    if (calendar.timeInMillis <= now || !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        var safetyCounter = 0
                        while (!allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK)) && safetyCounter < 14) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                            safetyCounter++
                        }
                    }
                } else {
                    if (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }

            else -> {
                // One-time reminder
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return calendar.timeInMillis
    }

    suspend fun exportToJson(): String {
        val tasks = dao.getActivePendingTasks()
        val jsonArray = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("scheduledTimeMillis", task.scheduledTimeMillis)
                put("recurrence", task.recurrence)
                put("customWeekdays", task.customWeekdays)
                put("isEnabled", task.isEnabled)
                put("isCompleted", task.isCompleted)
                put("isExactAlarm", task.isExactAlarm)
                put("snoozeMinutes", task.snoozeMinutes)
                put("category", task.category)
                put("priority", task.priority)
            }
            jsonArray.put(obj)
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("tasks", jsonArray)
        }.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Int {
        val root = JSONObject(jsonString)
        val array = root.getJSONArray("tasks")
        val importedList = mutableListOf<TaskAlarmEntity>()

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val task = TaskAlarmEntity(
                id = item.optString("id", java.util.UUID.randomUUID().toString()),
                title = item.getString("title"),
                description = item.optString("description", ""),
                scheduledTimeMillis = item.getLong("scheduledTimeMillis"),
                recurrence = item.optString("recurrence", "NONE"),
                customWeekdays = item.optString("customWeekdays", ""),
                isEnabled = item.optBoolean("isEnabled", true),
                isCompleted = item.optBoolean("isCompleted", false),
                isExactAlarm = item.optBoolean("isExactAlarm", true),
                snoozeMinutes = item.optInt("snoozeMinutes", 10),
                category = item.optString("category", "Coding"),
                priority = item.optString("priority", "HIGH")
            )
            importedList.add(task)
        }

        dao.insertAll(importedList)
        for (t in importedList) {
            if (t.isEnabled && !t.isCompleted && t.scheduledTimeMillis > System.currentTimeMillis()) {
                scheduleNotificationForTask(t)
            }
        }
        return importedList.size
    }
}
