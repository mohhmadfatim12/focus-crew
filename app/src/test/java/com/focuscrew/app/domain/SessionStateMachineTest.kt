package com.focuscrew.app.domain

import com.focuscrew.app.core.time.TimeProvider
import com.focuscrew.app.domain.model.SessionMode
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.domain.statemachine.SessionStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionStateMachineTest {

    private lateinit var testTimeProvider: MutableTestTimeProvider
    private lateinit var stateMachine: SessionStateMachine

    @Before
    fun setUp() {
        testTimeProvider = MutableTestTimeProvider(
            initialWallClock = 1700000000000L,
            initialElapsedRealtime = 100000L
        )
        stateMachine = SessionStateMachine(testTimeProvider)
    }

    @Test
    fun `starting Pomodoro sets correct initial planned and remaining time`() {
        val snapshot = stateMachine.startSession(SessionMode.POMODORO)

        assertTrue(snapshot.status is TimerStatus.Running)
        assertEquals(25 * 60 * 1000L, snapshot.remainingMs)
        assertEquals(25 * 60 * 1000L, snapshot.totalPlannedMs)
        assertEquals("25:00", snapshot.formattedTime)
        assertEquals(0.0f, snapshot.progressFraction, 0.001f)
    }

    @Test
    fun `advancing monotonic time accurately updates remaining time and progress`() {
        stateMachine.startSession(SessionMode.POMODORO)

        // Advance simulated time by 10 minutes (600,000 ms)
        testTimeProvider.advanceElapsedRealtime(10 * 60 * 1000L)

        val snapshot = stateMachine.getSnapshot()
        assertEquals(15 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("15:00", snapshot.formattedTime)
        // Progress should be 10 / 25 = 0.4
        assertEquals(0.4f, snapshot.progressFraction, 0.001f)
    }

    @Test
    fun `timer is immune to manual wall clock changes`() {
        stateMachine.startSession(SessionMode.POMODORO)

        // Simulate user tampering with phone time: advance wall clock by 5 hours,
        // but monotonic hardware elapsedRealtime only advances by 2 minutes
        testTimeProvider.advanceWallClock(5 * 60 * 60 * 1000L)
        testTimeProvider.advanceElapsedRealtime(2 * 60 * 1000L)

        val snapshot = stateMachine.getSnapshot()
        // Remaining time must be 23 minutes (25 - 2), unaffected by the 5-hour wall clock jump
        assertEquals(23 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("23:00", snapshot.formattedTime)
    }

    @Test
    fun `pausing and resuming retains remaining duration correctly`() {
        stateMachine.startSession(SessionMode.POMODORO)

        // Run for 5 minutes
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L)
        val paused = stateMachine.pause()

        assertTrue(paused.status is TimerStatus.Paused)
        assertEquals(20 * 60 * 1000L, paused.remainingMs)

        // Phone stays paused for 30 minutes
        testTimeProvider.advanceElapsedRealtime(30 * 60 * 1000L)

        // Resume: remaining time should still be 20 minutes
        val resumed = stateMachine.resume()
        assertTrue(resumed.status is TimerStatus.Running)
        assertEquals(20 * 60 * 1000L, resumed.remainingMs)

        // Run for another 5 minutes
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L)
        val afterResume = stateMachine.getSnapshot()
        assertEquals(15 * 60 * 1000L, afterResume.remainingMs)
        assertEquals("15:00", afterResume.formattedTime)
    }

    @Test
    fun `reaching target duration transitions to Completed status`() {
        stateMachine.startSession(SessionMode.POMODORO)

        // Advance time past planned duration (25 minutes)
        testTimeProvider.advanceElapsedRealtime(25 * 60 * 1000L + 500L)

        val snapshot = stateMachine.getSnapshot()
        assertTrue(snapshot.status is TimerStatus.Completed)
        assertEquals(0L, snapshot.remainingMs)
        assertEquals(1.0f, snapshot.progressFraction, 0.001f)
        assertEquals("00:00", snapshot.formattedTime)
    }

    @Test
    fun `custom duration calculates correctly`() {
        val customDurationMs = 12 * 60 * 1000L // 12 minutes
        val snapshot = stateMachine.startSession(SessionMode.CUSTOM, customDurationMs)

        assertEquals(12 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("12:00", snapshot.formattedTime)

        testTimeProvider.advanceElapsedRealtime(2 * 60 * 1000L)
        val after2Min = stateMachine.getSnapshot()
        assertEquals(10 * 60 * 1000L, after2Min.remainingMs)
        assertEquals("10:00", after2Min.formattedTime)
    }

    @Test
    fun `break mode countdown accurately advances and finishes at zero`() {
        val snapshot = stateMachine.startBreak(durationMinutes = 5, breakTitle = "Short Break")

        assertTrue(snapshot.isBreak)
        assertEquals("Short Break", snapshot.breakLabel)
        assertEquals(5 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("05:00", snapshot.formattedTime)

        // Advance 3 minutes
        testTimeProvider.advanceElapsedRealtime(3 * 60 * 1000L)
        val after3Min = stateMachine.getSnapshot()
        assertEquals(2 * 60 * 1000L, after3Min.remainingMs)
        assertEquals("02:00", after3Min.formattedTime)

        // Advance past 5 minutes
        testTimeProvider.advanceElapsedRealtime(2 * 60 * 1000L + 500L)
        val afterFinish = stateMachine.getSnapshot()
        assertEquals(0L, afterFinish.remainingMs)
        assertEquals("00:00", afterFinish.formattedTime)
    }

    @Test
    fun `skipping break resets state to Idle immediately`() {
        stateMachine.startBreak(durationMinutes = 15, breakTitle = "Long Break")
        val skipped = stateMachine.skipBreak()

        assertTrue(skipped.status is TimerStatus.Idle)
        assertEquals(0L, skipped.remainingMs)
    }

    @Test
    fun `intelligent break duration accurately scales by session duration and rejects 15 min or less`() {
        assertEquals(0, stateMachine.calculateIntelligentBreakMinutes(10 * 60 * 1000L))
        assertEquals(0, stateMachine.calculateIntelligentBreakMinutes(15 * 60 * 1000L))
        assertEquals(5, stateMachine.calculateIntelligentBreakMinutes(20 * 60 * 1000L))
        assertEquals(5, stateMachine.calculateIntelligentBreakMinutes(25 * 60 * 1000L))
        assertEquals(5, stateMachine.calculateIntelligentBreakMinutes(30 * 60 * 1000L))
        assertEquals(10, stateMachine.calculateIntelligentBreakMinutes(50 * 60 * 1000L))
        assertEquals(10, stateMachine.calculateIntelligentBreakMinutes(60 * 60 * 1000L))
        assertEquals(15, stateMachine.calculateIntelligentBreakMinutes(90 * 60 * 1000L))
        assertEquals(25, stateMachine.calculateIntelligentBreakMinutes(140 * 60 * 1000L))
        assertEquals(35, stateMachine.calculateIntelligentBreakMinutes(160 * 60 * 1000L))
    }

    @Test
    fun `break schedule description matches exact user specifications across durations`() {
        assertEquals("5 minutes rest will come after 15 minutes", stateMachine.getBreakScheduleDescription(25))
        assertEquals("10 min rest occur in 30 minutes", stateMachine.getBreakScheduleDescription(50))
        assertEquals("15 min rest occur in 15 minutes", stateMachine.getBreakScheduleDescription(90))
        assertEquals("25 min rest comes after 40 min", stateMachine.getBreakScheduleDescription(140))
        assertEquals("35 min rest comes after 50 min", stateMachine.getBreakScheduleDescription(160))
    }

    @Test
    fun `in-session break preserves remaining focus time and skips back to running focus`() {
        // Start 20-minute session
        val duration20m = 20 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, duration20m)

        // Advance 6 minutes: 14 minutes remaining (6m >= 5m requirement)
        testTimeProvider.advanceElapsedRealtime(6 * 60 * 1000L)
        val snapshotBeforeBreak = stateMachine.getSnapshot()
        assertEquals(14 * 60 * 1000L, snapshotBeforeBreak.remainingMs)

        // Take 5-minute intelligent break
        val breakSnapshot = stateMachine.startInSessionBreak(5)
        assertTrue(breakSnapshot.isBreak)
        assertTrue(breakSnapshot.hasPausedFocusSession)
        assertEquals(5 * 60 * 1000L, breakSnapshot.remainingMs)
        assertEquals("05:00", breakSnapshot.formattedTime)

        // User rests for 2 minutes
        testTimeProvider.advanceElapsedRealtime(2 * 60 * 1000L)
        val duringBreak = stateMachine.getSnapshot()
        assertEquals(3 * 60 * 1000L, duringBreak.remainingMs)

        // User taps "Skip Break & Resume Focus"
        val resumedSnapshot = stateMachine.skipBreak()
        assertTrue(resumedSnapshot.status is TimerStatus.Running)
        // Focus session is intact with all 14 minutes preserved!
        assertEquals(14 * 60 * 1000L, resumedSnapshot.remainingMs)
        assertEquals("14:00", resumedSnapshot.formattedTime)
    }

    @Test
    fun `in-session break expiring restores paused focus state with remaining time`() {
        val duration20m = 20 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, duration20m)

        // Advance 6 minutes: 14 minutes remaining
        testTimeProvider.advanceElapsedRealtime(6 * 60 * 1000L)
        stateMachine.startInSessionBreak(5)

        // Rest for full 5 minutes + 1 second
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L + 1000L)
        val afterBreakExpired = stateMachine.getSnapshot()

        // Rest completed, state restored to paused focus with 14 minutes remaining
        assertTrue(afterBreakExpired.status is TimerStatus.Paused)
        assertEquals(14 * 60 * 1000L, afterBreakExpired.remainingMs)
        assertEquals("14:00", afterBreakExpired.formattedTime)

        // User resumes
        val resumed = stateMachine.resume()
        assertTrue(resumed.status is TimerStatus.Running)
        assertEquals(14 * 60 * 1000L, resumed.remainingMs)
    }

    @Test
    fun `in-session break can be started while session is paused`() {
        val duration20m = 20 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, duration20m)

        // Advance 6 minutes
        testTimeProvider.advanceElapsedRealtime(6 * 60 * 1000L)
        // User pauses
        val paused = stateMachine.pause()
        assertTrue(paused.status is TimerStatus.Paused)
        assertEquals(14 * 60 * 1000L, paused.remainingMs)

        // Start in-session break directly from paused state
        val breakSnapshot = stateMachine.startInSessionBreak(5)
        assertTrue(breakSnapshot.isBreak)
        assertTrue(breakSnapshot.hasPausedFocusSession)
        assertEquals(5 * 60 * 1000L, breakSnapshot.remainingMs)

        // Resume from break
        val resumed = stateMachine.resumeFromBreak()
        assertTrue(resumed.status is TimerStatus.Running)
        assertEquals(14 * 60 * 1000L, resumed.remainingMs)
    }

    @Test
    fun `startSessionWithElapsed accurately sets 16 min out of 20 with unlocked rest threshold`() {
        val total20m = 20 * 60 * 1000L
        val elapsed4m = 4 * 60 * 1000L

        // Fast-forward start with 4 minutes already elapsed out of 20
        val snapshot = stateMachine.startSessionWithElapsed(
            mode = SessionMode.CUSTOM,
            totalPlannedMs = total20m,
            elapsedMs = elapsed4m
        )

        assertTrue(snapshot.status is TimerStatus.Running)
        assertEquals(16 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("16:00", snapshot.formattedTime)
        assertEquals(0.20f, snapshot.progressFraction, 0.001f)

        // Threshold for 20m session is exactly 4m (so 16m remaining is eligible for rest!)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(total20m)
        assertEquals(4 * 60 * 1000L, threshold)
        assertTrue(elapsed4m >= threshold)

        // Taking intelligent break immediately works
        val breakSnap = stateMachine.startInSessionBreak(5)
        assertTrue(breakSnap.isBreak)
        assertEquals(5 * 60 * 1000L, breakSnap.remainingMs)
    }
}

/**
 * Deterministic in-memory time provider for unit testing.
 */
class MutableTestTimeProvider(
    private var initialWallClock: Long,
    private var initialElapsedRealtime: Long
) : TimeProvider {
    override fun currentTimeMillis(): Long = initialWallClock
    override fun elapsedRealtime(): Long = initialElapsedRealtime

    fun advanceElapsedRealtime(ms: Long) {
        initialElapsedRealtime += ms
    }

    fun advanceWallClock(ms: Long) {
        initialWallClock += ms
    }
}
