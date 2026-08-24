package com.focuscrew.app.ui.timer

import android.content.Context
import android.content.SharedPreferences
import com.focuscrew.app.core.time.AndroidTimeProvider
import com.focuscrew.app.data.repository.FocusRepository
import com.focuscrew.app.domain.model.CommitmentMode
import com.focuscrew.app.domain.model.SessionMode
import com.focuscrew.app.domain.model.SessionTask
import com.focuscrew.app.domain.model.TimerSnapshot
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.domain.statemachine.SessionStateMachine
import com.focuscrew.app.service.BlockerManager
import com.focuscrew.app.service.TimerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Global session controller coordinating timer calculations, background updates,
 * ongoing notifications, app blocker state, and local offline Room database persistence.
 */
object TimerManager {
    private val timeProvider = AndroidTimeProvider()
    val stateMachine = SessionStateMachine(timeProvider)

    private val sessionTasksList = mutableListOf<SessionTask>()

    private val _snapshot = MutableStateFlow(stateMachine.getSnapshot())
    val snapshot: StateFlow<TimerSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null
    private var sessionStartJob: Job? = null

    private var repository: FocusRepository? = null
    private var activeSessionDbId: Long? = null
    private var activePlannedDurationMs: Long = 0L
    var activeSubject: String = "Deep Focus"
        private set

    private var appContext: Context? = null
    private var taskPrefs: SharedPreferences? = null

    private const val PREFS_TASKS = "focuscrew_session_tasks"
    private const val KEY_SAVED_TASKS = "saved_tasks_json"

    init {
        updateSnapshot()
    }

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        taskPrefs = app.getSharedPreferences(PREFS_TASKS, Context.MODE_PRIVATE)
        loadSavedTasks()
        TimerNotificationManager.createNotificationChannel(app)
        updateSnapshot()
    }

    fun initRepository(repo: FocusRepository) {
        repository = repo
    }

    fun setStrictLockdown(enabled: Boolean) {
        stateMachine.setStrictLockdown(enabled)
        updateSnapshot()
    }

    fun setCommitmentMode(mode: CommitmentMode) {
        stateMachine.setCommitmentMode(mode)
        updateSnapshot()
    }

    fun addTask(title: String) {
        if (title.isNotBlank()) {
            sessionTasksList.add(SessionTask(title = title.trim()))
            persistTasks()
            updateSnapshot()
        }
    }

    fun removeTask(taskId: String) {
        sessionTasksList.removeAll { it.id == taskId }
        persistTasks()
        updateSnapshot()
    }

    fun toggleTask(taskId: String) {
        val index = sessionTasksList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = sessionTasksList[index]
            sessionTasksList[index] = task.copy(isCompleted = !task.isCompleted)
            persistTasks()
            updateSnapshot()
        }
    }

    fun clearCompletedTasks() {
        sessionTasksList.removeAll { it.isCompleted }
        persistTasks()
        updateSnapshot()
    }

    private fun persistTasks() {
        val prefs = taskPrefs ?: return
        val array = JSONArray()
        for (task in sessionTasksList) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("isCompleted", task.isCompleted)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_TASKS, array.toString()).apply()
    }

    private fun loadSavedTasks() {
        val prefs = taskPrefs ?: return
        val jsonStr = prefs.getString(KEY_SAVED_TASKS, null) ?: return
        try {
            val array = JSONArray(jsonStr)
            sessionTasksList.clear()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val title = obj.optString("title", "")
                val isCompleted = obj.optBoolean("isCompleted", false)
                if (id.isNotEmpty() && title.isNotEmpty()) {
                    sessionTasksList.add(SessionTask(id = id, title = title, isCompleted = isCompleted))
                }
            }
        } catch (_: Exception) {}
    }

    fun unlockViaTasksCompleted() {
        val currentRemaining = snapshot.value.remainingMs
        val startJob = sessionStartJob
        val dbIdToClose = activeSessionDbId
        val plannedDuration = activePlannedDurationMs

        stateMachine.unlockViaTasksCompleted()
        BlockerManager.setBlockingActive(false)
        stopTicker()
        updateSnapshot()

        scope.launch(Dispatchers.IO) {
            startJob?.join()
            val targetId = dbIdToClose ?: activeSessionDbId
            if (targetId != null) {
                val actualMs = (plannedDuration - currentRemaining).coerceAtLeast(0L)
                repository?.finishSession(targetId, actualMs, "TASK_UNLOCKED")
                activeSessionDbId = null
            }
        }
    }

    fun startSession(
        mode: SessionMode,
        customDurationMs: Long? = null,
        isStrict: Boolean = true,
        subject: String = "Deep Focus",
        commitmentMode: CommitmentMode = stateMachine.commitmentMode,
        isStrictLockdown: Boolean = false
    ) {
        activeSubject = subject
        stateMachine.setCommitmentMode(commitmentMode)
        stateMachine.setStrictLockdown(isStrictLockdown)
        stateMachine.startSession(mode, customDurationMs, isStrictLockdown)
        val durationMs = customDurationMs ?: mode.durationMs
        activePlannedDurationMs = durationMs

        // Persist to Room database asynchronously with tracked Job
        sessionStartJob = scope.launch(Dispatchers.IO) {
            val dbId = repository?.startSession(
                mode = mode.name,
                plannedDurationMs = durationMs,
                isStrict = isStrict,
                subject = subject
            )
            activeSessionDbId = dbId
        }

        BlockerManager.setBlockingActive(true)
        updateSnapshot()
        startTicker()
    }

    fun startSessionWithElapsed(
        mode: SessionMode = SessionMode.CUSTOM,
        plannedMinutes: Int = 20,
        elapsedMinutes: Int = 4,
        isStrict: Boolean = true,
        subject: String = "Deep Focus"
    ) {
        val plannedMs = plannedMinutes * 60 * 1000L
        val elapsedMs = elapsedMinutes * 60 * 1000L
        activeSubject = subject
        activePlannedDurationMs = plannedMs

        stateMachine.startSessionWithElapsed(mode, plannedMs, elapsedMs)

        sessionStartJob = scope.launch(Dispatchers.IO) {
            val dbId = repository?.startSession(
                mode = mode.name,
                plannedDurationMs = plannedMs,
                isStrict = isStrict,
                subject = subject
            )
            activeSessionDbId = dbId
        }

        BlockerManager.setBlockingActive(true)
        updateSnapshot()
        startTicker()
    }

    fun pause() {
        stateMachine.pause()
        // USER MANDATE: Pausing the timer STILL blocks selected apps unconditionally!
        BlockerManager.setBlockingActive(true)
        stopTicker()
        updateSnapshot()
    }

    fun resume() {
        stateMachine.resume()
        BlockerManager.setBlockingActive(true)
        updateSnapshot()
        startTicker()
    }

    fun stop(isEmergency: Boolean = false) {
        val currentRemaining = snapshot.value.remainingMs
        stateMachine.stop()
        BlockerManager.setBlockingActive(false)
        stopTicker()
        updateSnapshot()

        // Update database with final session status safely awaiting start job
        val remainingMs = currentRemaining
        val startJob = sessionStartJob
        scope.launch(Dispatchers.IO) {
            startJob?.join()
            val dbId = activeSessionDbId
            if (dbId != null) {
                val actualMs = (activePlannedDurationMs - remainingMs).coerceAtLeast(0L)
                val reason = if (isEmergency) "EMERGENCY_STOP" else "CANCELLED_EARLY"
                repository?.finishSession(dbId, actualMs, reason)
                activeSessionDbId = null
            }
        }
    }

    fun calculateIntelligentBreakMinutes(plannedDurationMs: Long): Int {
        return stateMachine.calculateIntelligentBreakMinutes(plannedDurationMs)
    }

    fun calculateRestUnlockThresholdMs(plannedDurationMs: Long): Long {
        return stateMachine.calculateRestUnlockThresholdMs(plannedDurationMs)
    }

    fun getBreakScheduleDescription(plannedMinutes: Int): String {
        return stateMachine.getBreakScheduleDescription(plannedMinutes)
    }

    fun takeInSessionBreak(durationMinutes: Int) {
        stateMachine.startInSessionBreak(durationMinutes)
        BlockerManager.setBlockingActive(false)
        updateSnapshot()
        startTicker()
    }

    fun returnToFocusFromBreak() {
        stateMachine.resumeFromBreak()
        BlockerManager.setBlockingActive(true)
        updateSnapshot()
        startTicker()
    }

    fun startBreak(durationMinutes: Int = 5, breakTitle: String = "Short Break") {
        stateMachine.startBreak(durationMinutes, breakTitle)
        // CRITICAL: Suspend app blocking during scheduled breaks
        BlockerManager.setBlockingActive(false)
        updateSnapshot()
        startTicker()
    }

    fun skipBreak() {
        val wasInSessionBreak = (_snapshot.value.status as? TimerStatus.InBreak)?.pausedFocusState != null
        stateMachine.skipBreak()
        if (wasInSessionBreak) {
            BlockerManager.setBlockingActive(true)
            startTicker()
        } else {
            BlockerManager.setBlockingActive(false)
            stopTicker()
        }
        updateSnapshot()
    }

    fun reset() {
        val currentRemaining = snapshot.value.remainingMs
        val startJob = sessionStartJob
        val dbIdToClose = activeSessionDbId
        val plannedDuration = activePlannedDurationMs

        stateMachine.reset()
        BlockerManager.setBlockingActive(false)
        stopTicker()
        activeSessionDbId = null
        updateSnapshot()

        if (dbIdToClose != null || startJob != null) {
            scope.launch(Dispatchers.IO) {
                startJob?.join()
                val targetId = dbIdToClose ?: activeSessionDbId
                if (targetId != null) {
                    val actualMs = (plannedDuration - currentRemaining).coerceAtLeast(0L)
                    repository?.finishSession(targetId, actualMs, "CANCELLED_EARLY")
                }
            }
        }
    }

    fun addDistractionThought(thought: String, onSaved: () -> Unit = {}) {
        val startJob = sessionStartJob
        scope.launch(Dispatchers.IO) {
            startJob?.join()
            val dbId = activeSessionDbId
            if (dbId != null) {
                repository?.addDistraction(dbId, thought)
            }
            withContext(Dispatchers.Main) {
                onSaved()
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(500L)
                updateSnapshot()

                val status = stateMachine.currentStatus
                if (status is TimerStatus.Completed) {
                    BlockerManager.setBlockingActive(false)
                    stopTicker()

                    // Record completed session in database safely awaiting start job
                    val startJob = sessionStartJob
                    launch(Dispatchers.IO) {
                        startJob?.join()
                        val dbId = activeSessionDbId
                        if (dbId != null) {
                            repository?.finishSession(dbId, activePlannedDurationMs, "COMPLETED")
                            activeSessionDbId = null
                        }
                    }
                    break
                } else if (status is TimerStatus.Cancelled || (status is TimerStatus.Idle && _snapshot.value.isBreak)) {
                    BlockerManager.setBlockingActive(false)
                    stopTicker()
                    appContext?.let { ctx ->
                        TimerNotificationManager.cancelNotification(ctx)
                    }
                    break
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateSnapshot() {
        val tasks = sessionTasksList.toList()
        val allDone = tasks.isNotEmpty() && tasks.all { it.isCompleted }
        val snap = stateMachine.getSnapshot().copy(
            activeSubject = activeSubject,
            sessionTasks = tasks,
            areTasksCompleted = allDone
        )
        _snapshot.value = snap
        appContext?.let { ctx ->
            TimerNotificationManager.updateNotification(ctx, snap, activeSubject)
        }
    }
}
