package com.focuscrew.app.domain.streak

import com.focuscrew.app.data.local.entity.SessionEntity

/**
 * Milestone 10: 100% Offline Streak & Milestone Badge Calculator.
 *
 * Deterministic, pure JVM logic calculating streaks, hours, and discipline achievements
 * with zero cloud dependencies.
 */
object StreakCalculator {

    fun calculate(
        sessions: List<SessionEntity>,
        totalDistractions: Int = 0,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): StreakStats {
        val completed = sessions.filter { it.endReason == "COMPLETED" }
        val totalCompletedCount = completed.size
        val totalFocusMinutes = completed.sumOf { it.actualDurationMs } / 60000L

        // Group completed session dates by normalized epoch day
        val sessionEpochDays = completed.map { toEpochDay(it.startedAt) }.toSet()

        val todayEpochDay = toEpochDay(referenceTimeMillis)
        val yesterdayEpochDay = todayEpochDay - 1

        // Current streak calculation
        var currentStreak = 0
        val streakActiveFrom = when {
            sessionEpochDays.contains(todayEpochDay) -> todayEpochDay
            sessionEpochDays.contains(yesterdayEpochDay) -> yesterdayEpochDay
            else -> null
        }

        if (streakActiveFrom != null) {
            var checkDay = streakActiveFrom
            while (sessionEpochDays.contains(checkDay)) {
                currentStreak++
                checkDay--
            }
        }

        // Best historical streak calculation
        var bestStreak = 0
        if (sessionEpochDays.isNotEmpty()) {
            val sortedDays = sessionEpochDays.sorted()
            var currentRun = 0
            var previousDay: Long? = null

            for (day in sortedDays) {
                if (previousDay == null || day == previousDay + 1) {
                    currentRun++
                } else {
                    currentRun = 1
                }
                if (currentRun > bestStreak) {
                    bestStreak = currentRun
                }
                previousDay = day
            }
        }

        // Build Milestone Badges
        val has50mSession = completed.any { (it.actualDurationMs / 60000L) >= 50L }

        val badges = listOf(
            MilestoneBadge(
                id = "first_step",
                title = "First Step",
                icon = "🥉",
                description = "Complete your 1st focus session",
                isUnlocked = totalCompletedCount >= 1,
                progressCurrent = totalCompletedCount.coerceAtMost(1),
                progressMax = 1,
                progressText = if (totalCompletedCount >= 1) "Completed!" else "$totalCompletedCount/1"
            ),
            MilestoneBadge(
                id = "deep_diver",
                title = "Deep Diver",
                icon = "🥈",
                description = "Complete a 50m+ deep session",
                isUnlocked = has50mSession,
                progressCurrent = if (has50mSession) 1 else 0,
                progressMax = 1,
                progressText = if (has50mSession) "Completed!" else "Incomplete"
            ),
            MilestoneBadge(
                id = "streak_3",
                title = "Discipline Master",
                icon = "🥇",
                description = "Build a 3-day focus streak",
                isUnlocked = currentStreak >= 3 || bestStreak >= 3,
                progressCurrent = currentStreak.coerceAtLeast(bestStreak).coerceAtMost(3),
                progressMax = 3,
                progressText = if (currentStreak >= 3 || bestStreak >= 3) "Earned!" else "${currentStreak.coerceAtLeast(bestStreak)}/3 days"
            ),
            MilestoneBadge(
                id = "iron_will",
                title = "Iron Will",
                icon = "🛡️",
                description = "Complete 5 focus sessions",
                isUnlocked = totalCompletedCount >= 5,
                progressCurrent = totalCompletedCount.coerceAtMost(5),
                progressMax = 5,
                progressText = if (totalCompletedCount >= 5) "Earned!" else "$totalCompletedCount/5"
            ),
            MilestoneBadge(
                id = "thought_master",
                title = "Thought Master",
                icon = "🧘",
                description = "Park 3 thoughts in Distraction Journal",
                isUnlocked = totalDistractions >= 3,
                progressCurrent = totalDistractions.coerceAtMost(3),
                progressMax = 3,
                progressText = if (totalDistractions >= 3) "Earned!" else "$totalDistractions/3"
            ),
            MilestoneBadge(
                id = "century_club",
                title = "Century Club",
                icon = "👑",
                description = "Log 100+ total minutes of focused work",
                isUnlocked = totalFocusMinutes >= 100L,
                progressCurrent = totalFocusMinutes.coerceAtMost(100L).toInt(),
                progressMax = 100,
                progressText = if (totalFocusMinutes >= 100L) "Earned!" else "${totalFocusMinutes}/100 min"
            )
        )

        return StreakStats(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak.coerceAtLeast(currentStreak),
            totalCompletedSessions = totalCompletedCount,
            totalFocusMinutes = totalFocusMinutes,
            totalDistractionsParked = totalDistractions,
            badges = badges
        )
    }

    private fun toEpochDay(timeMillis: Long): Long {
        return java.time.Instant.ofEpochMilli(timeMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
    }
}
