package com.focuscrew.app.domain.statemachine

import com.focuscrew.app.core.time.TimeProvider
import com.focuscrew.app.domain.model.SessionMode
import com.focuscrew.app.domain.model.TimerSnapshot
import com.focuscrew.app.domain.model.TimerStatus

/**
 * Pure Kotlin Session Timer State Machine.
 *
 * Implements absolute target end time math rather than a decrement counter.
 * Survives screen-off / Doze mode, process suspension, and wall-clock time shifts.
 * ZERO Android framework dependencies (safe for JVM unit testing).
 */
class SessionStateMachine(
    private val timeProvider: TimeProvider
) {
    var currentStatus: TimerStatus = TimerStatus.Idle
        private set

    var commitmentMode: com.focuscrew.app.domain.model.CommitmentMode = com.focuscrew.app.domain.model.CommitmentMode.STRICT_TIMER
        private set

    var isStrictLockdown: Boolean = false
        private set

    fun setStrictLockdown(enabled: Boolean) {
        isStrictLockdown = enabled
    }

    private val _tasks = mutableListOf<com.focuscrew.app.domain.model.SessionTask>()
    val sessionTasks: List<com.focuscrew.app.domain.model.SessionTask>
        get() = _tasks.toList()

    fun setCommitmentMode(mode: com.focuscrew.app.domain.model.CommitmentMode) {
        commitmentMode = mode
    }

    fun setTasks(tasks: List<com.focuscrew.app.domain.model.SessionTask>) {
        _tasks.clear()
        _tasks.addAll(tasks)
    }

    fun addTask(title: String): com.focuscrew.app.domain.model.SessionTask {
        val task = com.focuscrew.app.domain.model.SessionTask(title = title.trim())
        _tasks.add(task)
        return task
    }

    fun removeTask(id: String) {
        _tasks.removeAll { it.id == id }
    }

    fun toggleTask(id: String): TimerSnapshot {
        val index = _tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = _tasks[index]
            _tasks[index] = existing.copy(isCompleted = !existing.isCompleted)
        }
        return getSnapshot()
    }

    fun areAllTasksCompleted(): Boolean {
        return _tasks.isNotEmpty() && _tasks.all { it.isCompleted }
    }

    fun unlockViaTasksCompleted(): TimerSnapshot {
        val running = currentStatus as? TimerStatus.Running
        val paused = currentStatus as? TimerStatus.Paused
        val mode = running?.mode ?: paused?.mode ?: SessionMode.POMODORO
        val planned = running?.plannedDurationMs ?: paused?.plannedDurationMs ?: 0L

        currentStatus = TimerStatus.Completed(
            mode = mode,
            durationMs = planned,
            completedAtWallClock = timeProvider.currentTimeMillis(),
            wasTaskUnlocked = true
        )
        return getSnapshot()
    }


    fun startSession(
        mode: SessionMode,
        customDurationMs: Long? = null,
        isStrictLockdown: Boolean = this.isStrictLockdown
    ): TimerSnapshot {
        this.isStrictLockdown = isStrictLockdown
        val durationMs = customDurationMs ?: mode.durationMs
        val nowElapsed = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val targetEnd = nowElapsed + durationMs

        currentStatus = TimerStatus.Running(
            mode = mode,
            plannedDurationMs = durationMs,
            startWallClock = nowWall,
            startElapsedRealtime = nowElapsed,
            targetElapsedEnd = targetEnd
        )

        return getSnapshot()
    }

    fun startSessionWithElapsed(
        mode: SessionMode,
        totalPlannedMs: Long,
        elapsedMs: Long
    ): TimerSnapshot {
        val nowElapsed = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val remainingMs = (totalPlannedMs - elapsedMs).coerceAtLeast(0L)
        val targetEnd = nowElapsed + remainingMs
        val startElapsed = nowElapsed - elapsedMs

        currentStatus = TimerStatus.Running(
            mode = mode,
            plannedDurationMs = totalPlannedMs,
            startWallClock = nowWall - elapsedMs,
            startElapsedRealtime = startElapsed,
            targetElapsedEnd = targetEnd
        )

        return getSnapshot()
    }

    fun pause(): TimerSnapshot {
        val running = currentStatus as? TimerStatus.Running ?: return getSnapshot()
        val nowElapsed = timeProvider.elapsedRealtime()
        val remaining = (running.targetElapsedEnd - nowElapsed).coerceAtLeast(0L)

        currentStatus = if (remaining <= 0L) {
            TimerStatus.Completed(
                mode = running.mode,
                durationMs = running.plannedDurationMs,
                completedAtWallClock = timeProvider.currentTimeMillis()
            )
        } else {
            TimerStatus.Paused(
                mode = running.mode,
                remainingMs = remaining,
                plannedDurationMs = running.plannedDurationMs,
                startWallClock = running.startWallClock,
                pausedAtElapsed = nowElapsed
            )
        }

        return getSnapshot()
    }

    fun resume(): TimerSnapshot {
        val paused = currentStatus as? TimerStatus.Paused ?: return getSnapshot()
        val nowElapsed = timeProvider.elapsedRealtime()
        val newTargetEnd = nowElapsed + paused.remainingMs

        currentStatus = TimerStatus.Running(
            mode = paused.mode,
            plannedDurationMs = paused.plannedDurationMs,
            startWallClock = paused.startWallClock,
            startElapsedRealtime = nowElapsed,
            targetElapsedEnd = newTargetEnd
        )

        return getSnapshot()
    }

    fun stop(): TimerSnapshot {
        val running = currentStatus as? TimerStatus.Running
        val paused = currentStatus as? TimerStatus.Paused

        val mode = running?.mode ?: paused?.mode ?: SessionMode.POMODORO
        val planned = running?.plannedDurationMs ?: paused?.plannedDurationMs ?: 0L
        val remaining = when {
            running != null -> (running.targetElapsedEnd - timeProvider.elapsedRealtime()).coerceAtLeast(0L)
            paused != null -> paused.remainingMs
            else -> 0L
        }
        val elapsed = (planned - remaining).coerceAtLeast(0L)

        currentStatus = TimerStatus.Cancelled(mode = mode, elapsedDurationMs = elapsed)
        return getSnapshot()
    }

    fun calculateIntelligentBreakMinutes(plannedDurationMs: Long): Int {
        val plannedMinutes = (plannedDurationMs / (60 * 1000L)).toInt()
        return when {
            plannedMinutes <= 15 -> 0
            plannedMinutes <= 35 -> 5    // 25m -> 5 min rest
            plannedMinutes <= 65 -> 10   // 50m -> 10 min rest
            plannedMinutes < 130 -> 15   // 90m -> 15 min rest
            plannedMinutes <= 150 -> 25  // 130-150m -> 25 min rest
            else -> 35                   // 150-180m -> 35 min rest
        }
    }

    fun calculateRestUnlockThresholdMs(plannedDurationMs: Long): Long {
        val plannedMinutes = (plannedDurationMs / (60 * 1000L)).toInt()
        return when {
            plannedMinutes <= 15 -> Long.MAX_VALUE
            plannedMinutes <= 20 -> 4 * 60 * 1000L  // 20m session: unlocks at 4m focus
            plannedMinutes <= 35 -> 15 * 60 * 1000L // 25m Pomodoro: 5 min rest will come after 15 minutes
            plannedMinutes <= 65 -> 30 * 60 * 1000L // 50m standard: 10 min rest occur in 30 minutes
            plannedMinutes < 130 -> 15 * 60 * 1000L // 90m deep work: 15 min rest occur in 15 minutes
            plannedMinutes <= 150 -> 40 * 60 * 1000L // 130-150m: 25 min rest comes after 40 min
            else -> 50 * 60 * 1000L                  // 150-180m: 35 min rest comes after 50 min
        }
    }

    fun getBreakScheduleDescription(plannedMinutes: Int): String {
        val breakMins = calculateIntelligentBreakMinutes(plannedMinutes * 60000L)
        val unlockMins = (calculateRestUnlockThresholdMs(plannedMinutes * 60000L) / 60000L).toInt()
        return when {
            breakMins == 0 -> "No rest break (session ≤ 15m)"
            plannedMinutes == 25 -> "5 minutes rest will come after 15 minutes"
            plannedMinutes == 50 -> "10 min rest occur in 30 minutes"
            plannedMinutes == 90 -> "15 min rest occur in 15 minutes"
            plannedMinutes in 130..150 -> "25 min rest comes after 40 min"
            plannedMinutes in 151..180 -> "35 min rest comes after 50 min"
            else -> "$breakMins min rest comes after ${unlockMins}m focus"
        }
    }

    fun startBreak(durationMinutes: Int = 5, breakTitle: String = "Short Break"): TimerSnapshot {
        val durationMs = durationMinutes * 60 * 1000L
        val nowElapsed = timeProvider.elapsedRealtime()
        val nowWall = timeProvider.currentTimeMillis()
        val targetEnd = nowElapsed + durationMs

        currentStatus = TimerStatus.InBreak(
            plannedDurationMs = durationMs,
            startWallClock = nowWall,
            startElapsedRealtime = nowElapsed,
            targetElapsedEnd = targetEnd,
            breakTitle = breakTitle,
            pausedFocusState = null
        )
        return getSnapshot()
    }

    fun startInSessionBreak(durationMinutes: Int? = null): TimerSnapshot {
        val nowElapsed = timeProvider.elapsedRealtime()
        val pausedState = when (val s = currentStatus) {
            is TimerStatus.Running -> {
                val remainingMs = (s.targetElapsedEnd - nowElapsed).coerceAtLeast(0L)
                TimerStatus.Paused(
                    mode = s.mode,
                    remainingMs = remainingMs,
                    plannedDurationMs = s.plannedDurationMs,
                    startWallClock = s.startWallClock,
                    pausedAtElapsed = nowElapsed
                )
            }
            is TimerStatus.Paused -> s
            else -> return getSnapshot()
        }

        val breakMins = durationMinutes ?: calculateIntelligentBreakMinutes(pausedState.plannedDurationMs).coerceAtLeast(5)
        val durationMs = breakMins * 60 * 1000L
        val nowWall = timeProvider.currentTimeMillis()
        val targetEnd = nowElapsed + durationMs

        currentStatus = TimerStatus.InBreak(
            plannedDurationMs = durationMs,
            startWallClock = nowWall,
            startElapsedRealtime = nowElapsed,
            targetElapsedEnd = targetEnd,
            breakTitle = "$breakMins-Minute Rest",
            pausedFocusState = pausedState
        )
        return getSnapshot()
    }

    fun resumeFromBreak(): TimerSnapshot {
        val inBreak = currentStatus as? TimerStatus.InBreak ?: return getSnapshot()
        val paused = inBreak.pausedFocusState
        return if (paused != null) {
            val nowElapsed = timeProvider.elapsedRealtime()
            val newTargetEnd = nowElapsed + paused.remainingMs

            currentStatus = TimerStatus.Running(
                mode = paused.mode,
                plannedDurationMs = paused.plannedDurationMs,
                startWallClock = paused.startWallClock,
                startElapsedRealtime = nowElapsed,
                targetElapsedEnd = newTargetEnd
            )
            getSnapshot()
        } else {
            currentStatus = TimerStatus.Idle
            getSnapshot()
        }
    }

    fun skipBreak(): TimerSnapshot {
        val inBreak = currentStatus as? TimerStatus.InBreak
        return if (inBreak?.pausedFocusState != null) {
            resumeFromBreak()
        } else {
            currentStatus = TimerStatus.Idle
            getSnapshot()
        }
    }

    fun reset(): TimerSnapshot {
        currentStatus = TimerStatus.Idle
        return getSnapshot()
    }

    fun getSnapshot(): TimerSnapshot {
        val tasks = sessionTasks
        val areDone = tasks.isNotEmpty() && tasks.all { it.isCompleted }

        val baseSnapshot = when (val state = currentStatus) {
            is TimerStatus.Idle -> {
                TimerSnapshot(
                    status = state,
                    remainingMs = 0L,
                    totalPlannedMs = 0L,
                    progressFraction = 0.0f,
                    formattedTime = "25:00"
                )
            }

            is TimerStatus.Running -> {
                val nowElapsed = timeProvider.elapsedRealtime()
                val remaining = (state.targetElapsedEnd - nowElapsed).coerceAtLeast(0L)

                if (remaining <= 0L) {
                    currentStatus = TimerStatus.Completed(
                        mode = state.mode,
                        durationMs = state.plannedDurationMs,
                        completedAtWallClock = timeProvider.currentTimeMillis()
                    )
                    TimerSnapshot(
                        status = currentStatus,
                        remainingMs = 0L,
                        totalPlannedMs = state.plannedDurationMs,
                        progressFraction = 1.0f,
                        formattedTime = "00:00"
                    )
                } else {
                    val progress = 1.0f - (remaining.toFloat() / state.plannedDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
                    TimerSnapshot(
                        status = state,
                        remainingMs = remaining,
                        totalPlannedMs = state.plannedDurationMs,
                        progressFraction = progress,
                        formattedTime = formatTime(remaining)
                    )
                }
            }

            is TimerStatus.Paused -> {
                val progress = 1.0f - (state.remainingMs.toFloat() / state.plannedDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
                TimerSnapshot(
                    status = state,
                    remainingMs = state.remainingMs,
                    totalPlannedMs = state.plannedDurationMs,
                    progressFraction = progress,
                    formattedTime = formatTime(state.remainingMs)
                )
            }

            is TimerStatus.InBreak -> {
                val nowElapsed = timeProvider.elapsedRealtime()
                val remaining = (state.targetElapsedEnd - nowElapsed).coerceAtLeast(0L)

                if (remaining <= 0L) {
                    if (state.pausedFocusState != null) {
                        currentStatus = state.pausedFocusState
                        val progress = 1.0f - (state.pausedFocusState.remainingMs.toFloat() / state.pausedFocusState.plannedDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
                        TimerSnapshot(
                            status = currentStatus,
                            remainingMs = state.pausedFocusState.remainingMs,
                            totalPlannedMs = state.pausedFocusState.plannedDurationMs,
                            progressFraction = progress,
                            formattedTime = formatTime(state.pausedFocusState.remainingMs),
                            hasPausedFocusSession = true
                        )
                    } else {
                        currentStatus = TimerStatus.Idle
                        TimerSnapshot(
                            status = currentStatus,
                            remainingMs = 0L,
                            totalPlannedMs = state.plannedDurationMs,
                            progressFraction = 1.0f,
                            formattedTime = "00:00",
                            isBreak = true,
                            breakLabel = state.breakTitle,
                            hasPausedFocusSession = false
                        )
                    }
                } else {
                    val progress = 1.0f - (remaining.toFloat() / state.plannedDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
                    TimerSnapshot(
                        status = state,
                        remainingMs = remaining,
                        totalPlannedMs = state.plannedDurationMs,
                        progressFraction = progress,
                        formattedTime = formatTime(remaining),
                        isBreak = true,
                        breakLabel = state.breakTitle,
                        hasPausedFocusSession = state.pausedFocusState != null
                    )
                }
            }

            is TimerStatus.Completed -> {
                TimerSnapshot(
                    status = state,
                    remainingMs = 0L,
                    totalPlannedMs = state.durationMs,
                    progressFraction = 1.0f,
                    formattedTime = "00:00"
                )
            }

            is TimerStatus.Cancelled -> {
                TimerSnapshot(
                    status = state,
                    remainingMs = 0L,
                    totalPlannedMs = state.elapsedDurationMs,
                    progressFraction = 0.0f,
                    formattedTime = "00:00"
                )
            }
        }

        return baseSnapshot.copy(
            commitmentMode = commitmentMode,
            sessionTasks = tasks,
            areTasksCompleted = areDone,
            isStrictLockdown = isStrictLockdown
        )
    }

    companion object {
        fun formatTime(millis: Long): String {
            val totalSeconds = (millis + 999) / 1000 // Round up so 00:01 stays visible for full second
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
