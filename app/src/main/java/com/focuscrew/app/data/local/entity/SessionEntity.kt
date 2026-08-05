package com.focuscrew.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String = "Deep Focus",
    val mode: String, // POMODORO, FOCUS_50, DEEP_90, CUSTOM
    val plannedDurationMs: Long,
    val actualDurationMs: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val endReason: String? = null, // COMPLETED, EMERGENCY_STOP, CANCELLED_EARLY
    val isStrict: Boolean = true,
    val distractionCount: Int = 0
)
