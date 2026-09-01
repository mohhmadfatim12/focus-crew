package com.focuscrew.app.domain

import com.focuscrew.app.domain.model.SessionMode
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.domain.statemachine.SessionStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exhaustive Verification Test Suite for Intelligent In-Session Rest Breaks across 5 distinct duration scenarios.
 * Verifies calculation rules, unlock thresholds, state transitions, break suspensions, and focus restorations.
 */
class IntelligentBreakVerificationTest {

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

    // SCENARIO 1: Short Session (<= 15 minutes) - e.g. 15m or 10m
    // Requirement: No rest break allowed because 15m is too short to fragment focus.
    @Test
    fun `Scenario 1 - 15m session strictly rejects rest break`() {
        val planned15m = 15 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, planned15m)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned15m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned15m)

        assertEquals(0, breakMins)
        assertEquals(Long.MAX_VALUE, threshold)

        // Advance 10 minutes (5m remaining)
        testTimeProvider.advanceElapsedRealtime(10 * 60 * 1000L)
        val snapshot = stateMachine.getSnapshot()
        assertEquals(5 * 60 * 1000L, snapshot.remainingMs)

        // Elapsed time (10m) is still less than threshold (Long.MAX_VALUE)
        val elapsed = planned15m - snapshot.remainingMs
        assertFalse(elapsed >= threshold)
    }

    // SCENARIO 2: 20-Minute Custom Session (The User's specific case)
    // Requirement: 5m rest break; unlocks after 4m of focus (at 16:00 remaining).
    @Test
    fun `Scenario 2 - 20m session unlocks 5m rest at 4m focus and restores focus properly`() {
        val planned20m = 20 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, planned20m)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned20m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned20m)

        assertEquals(5, breakMins)
        assertEquals(4 * 60 * 1000L, threshold)

        // Step A: At 2m focus (18:00 remaining), rest is still locked
        testTimeProvider.advanceElapsedRealtime(2 * 60 * 1000L)
        var snapshot = stateMachine.getSnapshot()
        var elapsed = planned20m - snapshot.remainingMs
        assertEquals(18 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("18:00", snapshot.formattedTime)
        assertFalse(elapsed >= threshold)

        // Step B: At 4m focus (16:00 remaining), rest is unlocked
        testTimeProvider.advanceElapsedRealtime(2 * 60 * 1000L)
        snapshot = stateMachine.getSnapshot()
        elapsed = planned20m - snapshot.remainingMs
        assertEquals(16 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("16:00", snapshot.formattedTime)
        assertTrue(elapsed >= threshold)

        // Step C: Trigger 5-minute in-session rest break
        val breakSnapshot = stateMachine.startInSessionBreak(breakMins)
        assertTrue(breakSnapshot.isBreak)
        assertTrue(breakSnapshot.hasPausedFocusSession)
        assertEquals(5 * 60 * 1000L, breakSnapshot.remainingMs)
        assertEquals("05:00", breakSnapshot.formattedTime)

        // Step D: Rest for 3 minutes, then tap Skip Break & Resume Focus
        testTimeProvider.advanceElapsedRealtime(3 * 60 * 1000L)
        val duringBreak = stateMachine.getSnapshot()
        assertEquals(2 * 60 * 1000L, duringBreak.remainingMs)
        assertEquals("02:00", duringBreak.formattedTime)

        val resumedSnapshot = stateMachine.skipBreak()
        assertTrue(resumedSnapshot.status is TimerStatus.Running)
        assertEquals(16 * 60 * 1000L, resumedSnapshot.remainingMs)
        assertEquals("16:00", resumedSnapshot.formattedTime)
    }

    // SCENARIO 3: Pomodoro Session (25 minutes)
    // Requirement: 5m rest break; unlocks after 15m of focus (at 10:00 remaining).
    @Test
    fun `Scenario 3 - 25m Pomodoro unlocks 5m rest at 15m focus and handles expiry`() {
        val planned25m = 25 * 60 * 1000L
        stateMachine.startSession(SessionMode.POMODORO)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned25m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned25m)

        assertEquals(5, breakMins)
        assertEquals(15 * 60 * 1000L, threshold)

        // At 10m focus (15:00 remaining) -> still locked
        testTimeProvider.advanceElapsedRealtime(10 * 60 * 1000L)
        var snapshot = stateMachine.getSnapshot()
        assertFalse((planned25m - snapshot.remainingMs) >= threshold)

        // Advance another 5m to 15m elapsed (10:00 remaining) -> unlocked!
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L)
        snapshot = stateMachine.getSnapshot()
        assertEquals(10 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("10:00", snapshot.formattedTime)
        val elapsed = planned25m - snapshot.remainingMs
        assertTrue(elapsed >= threshold)

        // Take 5m rest
        stateMachine.startInSessionBreak(breakMins)

        // Let break expire completely (+ 1 second)
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L + 1000L)
        val expiredSnapshot = stateMachine.getSnapshot()

        // Rest completed, state returns to paused focus with all 10m remaining intact!
        assertTrue(expiredSnapshot.status is TimerStatus.Paused)
        assertEquals(10 * 60 * 1000L, expiredSnapshot.remainingMs)
        assertEquals("10:00", expiredSnapshot.formattedTime)
    }

    // SCENARIO 4: Standard Focus Session (50 minutes)
    // Requirement: 10m rest break; unlocks after 30m of focus (at 20:00 remaining).
    @Test
    fun `Scenario 4 - 50m Focus unlocks 10m rest at 30m focus`() {
        val planned50m = 50 * 60 * 1000L
        stateMachine.startSession(SessionMode.FOCUS_50)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned50m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned50m)

        assertEquals(10, breakMins)
        assertEquals(30 * 60 * 1000L, threshold)

        // At 25m elapsed (25:00 remaining) -> Locked
        testTimeProvider.advanceElapsedRealtime(25 * 60 * 1000L)
        var snapshot = stateMachine.getSnapshot()
        assertFalse((planned50m - snapshot.remainingMs) >= threshold)

        // At 30m elapsed (20:00 remaining) -> Unlocked
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L)
        snapshot = stateMachine.getSnapshot()
        assertEquals(20 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("20:00", snapshot.formattedTime)
        assertTrue((planned50m - snapshot.remainingMs) >= threshold)

        // Take 10m break and verify remaining time in break
        val breakSnapshot = stateMachine.startInSessionBreak(breakMins)
        assertEquals(10 * 60 * 1000L, breakSnapshot.remainingMs)
        assertEquals("10:00", breakSnapshot.formattedTime)
    }

    // SCENARIO 5: Deep Work Session (90 minutes)
    // Requirement: 15m rest break; unlocks after 15m of focus (at 75:00 remaining).
    @Test
    fun `Scenario 5 - 90m Deep Work unlocks 15m rest at 15m focus`() {
        val planned90m = 90 * 60 * 1000L
        stateMachine.startSession(SessionMode.DEEP_90)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned90m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned90m)

        assertEquals(15, breakMins)
        assertEquals(15 * 60 * 1000L, threshold)

        // Advance 15 minutes (75:00 remaining)
        testTimeProvider.advanceElapsedRealtime(15 * 60 * 1000L)
        val snapshot = stateMachine.getSnapshot()
        assertEquals(75 * 60 * 1000L, snapshot.remainingMs)
        assertEquals("75:00", snapshot.formattedTime)
        assertTrue((planned90m - snapshot.remainingMs) >= threshold)

        // Take 15m break
        val breakSnapshot = stateMachine.startInSessionBreak(breakMins)
        assertEquals(15 * 60 * 1000L, breakSnapshot.remainingMs)
        assertEquals("15:00", breakSnapshot.formattedTime)

        // Resume manually from break
        val resumed = stateMachine.resumeFromBreak()
        assertTrue(resumed.status is TimerStatus.Running)
        assertEquals(75 * 60 * 1000L, resumed.remainingMs)
        assertEquals("75:00", resumed.formattedTime)
    }

    // SCENARIO 6: 130 to 150 min Session (e.g. 140m)
    // Requirement: 25m rest break; unlocks after 40m of focus.
    @Test
    fun `Scenario 6 - 140m Session unlocks 25m rest at 40m focus`() {
        val planned140m = 140 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, planned140m)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned140m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned140m)

        assertEquals(25, breakMins)
        assertEquals(40 * 60 * 1000L, threshold)

        // At 30m elapsed -> Locked
        testTimeProvider.advanceElapsedRealtime(30 * 60 * 1000L)
        var snapshot = stateMachine.getSnapshot()
        assertFalse((planned140m - snapshot.remainingMs) >= threshold)

        // At 40m elapsed -> Unlocked
        testTimeProvider.advanceElapsedRealtime(10 * 60 * 1000L)
        snapshot = stateMachine.getSnapshot()
        assertEquals(100 * 60 * 1000L, snapshot.remainingMs)
        assertTrue((planned140m - snapshot.remainingMs) >= threshold)

        val breakSnapshot = stateMachine.startInSessionBreak(breakMins)
        assertEquals(25 * 60 * 1000L, breakSnapshot.remainingMs)
        assertEquals("25:00", breakSnapshot.formattedTime)
    }

    // SCENARIO 7: 150 to 180 min Session (e.g. 160m)
    // Requirement: 35m rest break; unlocks after 50m of focus.
    @Test
    fun `Scenario 7 - 160m Session unlocks 35m rest at 50m focus`() {
        val planned160m = 160 * 60 * 1000L
        stateMachine.startSession(SessionMode.CUSTOM, planned160m)

        val breakMins = stateMachine.calculateIntelligentBreakMinutes(planned160m)
        val threshold = stateMachine.calculateRestUnlockThresholdMs(planned160m)

        assertEquals(35, breakMins)
        assertEquals(50 * 60 * 1000L, threshold)

        // At 45m elapsed -> Locked
        testTimeProvider.advanceElapsedRealtime(45 * 60 * 1000L)
        var snapshot = stateMachine.getSnapshot()
        assertFalse((planned160m - snapshot.remainingMs) >= threshold)

        // At 50m elapsed -> Unlocked
        testTimeProvider.advanceElapsedRealtime(5 * 60 * 1000L)
        snapshot = stateMachine.getSnapshot()
        assertEquals(110 * 60 * 1000L, snapshot.remainingMs)
        assertTrue((planned160m - snapshot.remainingMs) >= threshold)

        val breakSnapshot = stateMachine.startInSessionBreak(breakMins)
        assertEquals(35 * 60 * 1000L, breakSnapshot.remainingMs)
        assertEquals("35:00", breakSnapshot.formattedTime)
    }
}
