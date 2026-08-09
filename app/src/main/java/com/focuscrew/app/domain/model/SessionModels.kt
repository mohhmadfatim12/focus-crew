package com.focuscrew.app.domain.model

enum class SessionMode(val durationMs: Long, val displayName: String, val minutes: Int) {
    POMODORO(25 * 60 * 1000L, "Pomodoro (25m)", 25),
    FOCUS_50(50 * 60 * 1000L, "Focus (50m)", 50),
    DEEP_90(90 * 60 * 1000L, "Deep Work (90m)", 90),
    CUSTOM(25 * 60 * 1000L, "Custom", 25)
}

/**
 * Commitment Timer Mode:
 * STRICT_TIMER: Strict timer-based blocking. Apps remain blocked for the full duration.
 * TASK_BASED: Optional task-based unlocking. User declares tasks and can unlock once completed or timer finishes.
 * Neither silently replaces the other; users choose explicitly.
 */
enum class CommitmentMode(val displayName: String, val badge: String, val description: String) {
    STRICT_TIMER(
        "Strict Timer",
        "⏱️ Time-Locked",
        "Apps remain strictly blocked until the timer expires. Early stop requires friction."
    ),
    TASK_BASED(
        "Task-Based Unlocking",
        "✅ Task-Unlocked",
        "Declare your session goals. Apps unlock as soon as all tasks are completed or time finishes."
    )
}

data class SessionTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

sealed interface TimerStatus {
    object Idle : TimerStatus

    data class Running(
        val mode: SessionMode,
        val plannedDurationMs: Long,
        val startWallClock: Long,
        val startElapsedRealtime: Long,
        val targetElapsedEnd: Long
    ) : TimerStatus

    data class Paused(
        val mode: SessionMode,
        val remainingMs: Long,
        val plannedDurationMs: Long,
        val startWallClock: Long,
        val pausedAtElapsed: Long
    ) : TimerStatus

    data class InBreak(
        val plannedDurationMs: Long,
        val startWallClock: Long,
        val startElapsedRealtime: Long,
        val targetElapsedEnd: Long,
        val breakTitle: String = "Short Break",
        val pausedFocusState: Paused? = null
    ) : TimerStatus

    data class Completed(
        val mode: SessionMode,
        val durationMs: Long,
        val completedAtWallClock: Long,
        val wasTaskUnlocked: Boolean = false
    ) : TimerStatus

    data class Cancelled(
        val mode: SessionMode,
        val elapsedDurationMs: Long
    ) : TimerStatus
}

data class TimerSnapshot(
    val status: TimerStatus,
    val remainingMs: Long,
    val totalPlannedMs: Long,
    val progressFraction: Float, // 0.0f (full time remaining) to 1.0f (completed)
    val formattedTime: String,
    val activeSubject: String = "Deep Focus",
    val isBreak: Boolean = false,
    val breakLabel: String = "",
    val hasPausedFocusSession: Boolean = false,
    val commitmentMode: CommitmentMode = CommitmentMode.STRICT_TIMER,
    val sessionTasks: List<SessionTask> = emptyList(),
    val areTasksCompleted: Boolean = false,
    val isStrictLockdown: Boolean = false
)
