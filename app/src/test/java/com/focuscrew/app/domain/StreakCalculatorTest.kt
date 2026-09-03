package com.focuscrew.app.domain

import com.focuscrew.app.data.local.entity.SessionEntity
import com.focuscrew.app.domain.streak.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakCalculatorTest {

    @Test
    fun `empty sessions list returns zero streak and locked badges`() {
        val stats = StreakCalculator.calculate(emptyList(), totalDistractions = 0)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(0, stats.bestStreakDays)
        assertEquals(0, stats.totalCompletedSessions)
        assertEquals(0L, stats.totalFocusMinutes)
        assertTrue(stats.badges.none { it.isUnlocked })
    }

    @Test
    fun `completed session unlocks First Step badge`() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            SessionEntity(
                id = 1,
                subject = "Study",
                mode = "POMODORO",
                plannedDurationMs = 25 * 60 * 1000L,
                actualDurationMs = 25 * 60 * 1000L,
                startedAt = now,
                endedAt = now + 25 * 60 * 1000L,
                endReason = "COMPLETED"
            )
        )

        val stats = StreakCalculator.calculate(sessions, referenceTimeMillis = now)
        assertEquals(1, stats.currentStreakDays)
        assertEquals(1, stats.totalCompletedSessions)

        val firstStep = stats.badges.find { it.id == "first_step" }!!
        assertTrue(firstStep.isUnlocked)
    }

    @Test
    fun `50 minute session unlocks Deep Diver badge`() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            SessionEntity(
                id = 1,
                subject = "Coding",
                mode = "FOCUS_50",
                plannedDurationMs = 50 * 60 * 1000L,
                actualDurationMs = 50 * 60 * 1000L,
                startedAt = now,
                endedAt = now + 50 * 60 * 1000L,
                endReason = "COMPLETED"
            )
        )

        val stats = StreakCalculator.calculate(sessions, referenceTimeMillis = now)
        val deepDiver = stats.badges.find { it.id == "deep_diver" }!!
        assertTrue(deepDiver.isUnlocked)
    }

    @Test
    fun `consecutive three days calculate 3-day streak and unlocks Discipline Master`() {
        val dayMillis = 24 * 60 * 60 * 1000L
        val today = System.currentTimeMillis()
        val yesterday = today - dayMillis
        val twoDaysAgo = today - (2 * dayMillis)

        val sessions = listOf(
            SessionEntity(id = 1, subject = "Math", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 25 * 60000L, startedAt = twoDaysAgo, endReason = "COMPLETED"),
            SessionEntity(id = 2, subject = "Physics", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 25 * 60000L, startedAt = yesterday, endReason = "COMPLETED"),
            SessionEntity(id = 3, subject = "Coding", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 25 * 60000L, startedAt = today, endReason = "COMPLETED")
        )

        val stats = StreakCalculator.calculate(sessions, referenceTimeMillis = today)
        assertEquals(3, stats.currentStreakDays)
        assertEquals(3, stats.bestStreakDays)

        val streakBadge = stats.badges.find { it.id == "streak_3" }!!
        assertTrue(streakBadge.isUnlocked)
    }

    @Test
    fun `yesterday session keeps current streak active if today is not yet completed`() {
        val dayMillis = 24 * 60 * 60 * 1000L
        val today = System.currentTimeMillis()
        val yesterday = today - dayMillis

        val sessions = listOf(
            SessionEntity(id = 1, subject = "Study", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 25 * 60000L, startedAt = yesterday, endReason = "COMPLETED")
        )

        val stats = StreakCalculator.calculate(sessions, referenceTimeMillis = today)
        assertEquals(1, stats.currentStreakDays)
    }

    @Test
    fun `gap of two days breaks current streak but preserves best streak`() {
        val dayMillis = 24 * 60 * 60 * 1000L
        val today = System.currentTimeMillis()
        val threeDaysAgo = today - (3 * dayMillis)
        val fourDaysAgo = today - (4 * dayMillis)

        val sessions = listOf(
            SessionEntity(id = 1, subject = "Study", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 25 * 60000L, startedAt = fourDaysAgo, endReason = "COMPLETED"),
            SessionEntity(id = 2, subject = "Work", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 25 * 60000L, startedAt = threeDaysAgo, endReason = "COMPLETED")
        )

        // Today user has not studied; gap between today and 3 days ago is > 1 day
        val stats = StreakCalculator.calculate(sessions, referenceTimeMillis = today)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(2, stats.bestStreakDays)
    }

    @Test
    fun `cancelled or incomplete sessions do not count toward streaks`() {
        val today = System.currentTimeMillis()
        val sessions = listOf(
            SessionEntity(id = 1, subject = "Study", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 5 * 60000L, startedAt = today, endReason = "CANCELLED_EARLY"),
            SessionEntity(id = 2, subject = "Work", mode = "POMODORO", plannedDurationMs = 25 * 60000L, actualDurationMs = 2 * 60000L, startedAt = today, endReason = "EMERGENCY_STOP")
        )

        val stats = StreakCalculator.calculate(sessions, referenceTimeMillis = today)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(0, stats.totalCompletedSessions)
    }
}
