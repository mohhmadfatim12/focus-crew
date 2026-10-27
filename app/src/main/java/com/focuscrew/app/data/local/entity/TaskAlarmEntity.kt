package com.focuscrew.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "task_alarms",
    indices = [
        Index(value = ["scheduledTimeMillis"]),
        Index(value = ["isEnabled"]),
        Index(value = ["isCompleted"])
    ]
)
data class TaskAlarmEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val scheduledTimeMillis: Long,
    val recurrence: String = "NONE", // NONE, DAILY, WEEKLY, WEEKDAYS, CUSTOM_WEEKDAYS
    val customWeekdays: String = "",  // e.g. "2,3,4,5,6" (Calendar.MONDAY = 2, etc.)
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val isExactAlarm: Boolean = true,
    val snoozeCount: Int = 0,
    val snoozeMinutes: Int = 10,
    val category: String = "Coding", // Coding, Work, Study, Health, Personal, Other
    val priority: String = "HIGH",   // LOW, MEDIUM, HIGH, URGENT
    val subtasksJson: String = "[]", // JSON array of [{"id":"1","text":"Subtask","isDone":false}]
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
