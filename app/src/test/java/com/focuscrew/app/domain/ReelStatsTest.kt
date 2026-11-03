package com.focuscrew.app.domain

import com.focuscrew.app.data.model.ReelStats
import com.focuscrew.app.data.model.ReelWarningStage
import org.junit.Assert.assertEquals
import org.junit.Test

class ReelStatsTest {

    @Test
    fun testDefaultStatsAreZeroAndSafe() {
        val stats = ReelStats()
        assertEquals(0, stats.todayTotal)
        assertEquals(0, stats.yesterdayTotal)
        assertEquals(0, stats.todayInstagram)
        assertEquals(0, stats.todayYoutube)
        assertEquals(50, stats.dailyLimit)
        assertEquals(ReelWarningStage.SAFE, stats.warningStage)
    }

    @Test
    fun testWarningStageTransitionsAccurately() {
        val limit = 50
        // Under 40% (0..20) -> SAFE
        val safeStats = ReelStats(todayTotal = 20, dailyLimit = limit)
        assertEquals(ReelWarningStage.SAFE, safeStats.warningStage)

        // Between 41% and 100% (21..50) -> CAUTION
        val cautionStatsStart = ReelStats(todayTotal = 21, dailyLimit = limit)
        assertEquals(ReelWarningStage.CAUTION, cautionStatsStart.warningStage)

        val cautionStatsEnd = ReelStats(todayTotal = 50, dailyLimit = limit)
        assertEquals(ReelWarningStage.CAUTION, cautionStatsEnd.warningStage)

        // Over limit (51+) -> DANGER
        val dangerStats = ReelStats(todayTotal = 51, dailyLimit = limit)
        assertEquals(ReelWarningStage.DANGER, dangerStats.warningStage)

        val extremeDanger = ReelStats(todayTotal = 150, dailyLimit = limit)
        assertEquals(ReelWarningStage.DANGER, extremeDanger.warningStage)
    }

    @Test
    fun testPercentageVsYesterdayCalculation() {
        // Reduced from 50 to 25 reels -> -50%
        val reducedDay = ReelStats(todayTotal = 25, yesterdayTotal = 50)
        assertEquals(-50, reducedDay.percentageVsYesterday)

        // Increased from 40 to 60 reels -> +50%
        val increasedDay = ReelStats(todayTotal = 60, yesterdayTotal = 40)
        assertEquals(50, increasedDay.percentageVsYesterday)

        // Yesterday was 0 -> 0% (avoids divide-by-zero)
        val firstDay = ReelStats(todayTotal = 30, yesterdayTotal = 0)
        assertEquals(0, firstDay.percentageVsYesterday)
    }

    @Test
    fun testPlatformBreakdownSumMatchesTotal() {
        val stats = ReelStats(
            todayTotal = 45,
            todayInstagram = 30,
            todayYoutube = 15
        )
        assertEquals(45, stats.todayInstagram + stats.todayYoutube)
    }

    @Test
    fun testBrainHealthStateTransitions() {
        val limit = 50

        // 1. Fresh state: 0 to 40% of limit (0..20 reels)
        val startOfDay = ReelStats(todayTotal = 5, dailyLimit = limit)
        assertEquals(com.focuscrew.app.data.model.BrainHealthState.FRESH, startOfDay.brainState)
        assertEquals(0, startOfDay.brainState.stageIndex)

        val upperFresh = ReelStats(todayTotal = 20, dailyLimit = limit)
        assertEquals(com.focuscrew.app.data.model.BrainHealthState.FRESH, upperFresh.brainState)

        // 2. Concerned state: 41% to 75% of limit (21..37 reels)
        val concerned = ReelStats(todayTotal = 25, dailyLimit = limit)
        assertEquals(com.focuscrew.app.data.model.BrainHealthState.CONCERNED, concerned.brainState)
        assertEquals(1, concerned.brainState.stageIndex)

        // 3. Weary state: 76% to 100% of limit (38..50 reels)
        val weary = ReelStats(todayTotal = 45, dailyLimit = limit)
        assertEquals(com.focuscrew.app.data.model.BrainHealthState.WEARY, weary.brainState)
        assertEquals(2, weary.brainState.stageIndex)

        // 4. Exhausted state: 101% to 150% of limit (51..75 reels)
        val exhausted = ReelStats(todayTotal = 60, dailyLimit = limit)
        assertEquals(com.focuscrew.app.data.model.BrainHealthState.EXHAUSTED, exhausted.brainState)
        assertEquals(3, exhausted.brainState.stageIndex)

        // 5. Full Brainrot state: 151%+ of limit (76..700+ reels)
        val severeBinge = ReelStats(todayTotal = 700, dailyLimit = limit)
        assertEquals(com.focuscrew.app.data.model.BrainHealthState.BRAINROT, severeBinge.brainState)
        assertEquals(4, severeBinge.brainState.stageIndex)
    }

    @Test
    fun testDopamineSensitivityCalculations() {
        // Zero reels = 100% sensitivity
        val freshDay = ReelStats(todayTotal = 0)
        assertEquals(100, freshDay.calculateDopamineSensitivity())
        assertEquals(com.focuscrew.app.data.model.DopamineBiologicalState.PEAK_SHARP, freshDay.dopamineStatus)

        // 40 reels = 100 - (40 * 0.75) = 70%
        val moderateDay = ReelStats(todayTotal = 40)
        assertEquals(70, moderateDay.calculateDopamineSensitivity())
        assertEquals(com.focuscrew.app.data.model.DopamineBiologicalState.ATTENTION_FRAGMENTED, moderateDay.dopamineStatus)

        // 80 reels = 100 - (80 * 0.75) = 40% (Dopamine crash)
        val bingeDay = ReelStats(todayTotal = 80)
        assertEquals(40, bingeDay.calculateDopamineSensitivity())
        assertEquals(com.focuscrew.app.data.model.DopamineBiologicalState.DOPAMINE_CRASH, bingeDay.dopamineStatus)

        // Floor cap test (never goes below 10%)
        val extremeBinge = ReelStats(todayTotal = 300)
        assertEquals(10, extremeBinge.calculateDopamineSensitivity())
    }

    @Test
    fun testDopamineActiveRecoveryRegeneratesSensitivity() {
        val t0 = 1_000_000L
        // 80 reels -> 40% sensitivity at t0
        val depleted = ReelStats(todayTotal = 80, lastSwipeTimestamp = t0)
        assertEquals(40, depleted.calculateDopamineSensitivity(currentTime = t0))

        // 1 hour off reels (+12%) -> 52%
        val oneHourLater = t0 + 3600 * 1000L
        assertEquals(52, depleted.calculateDopamineSensitivity(currentTime = oneHourLater))

        // 2 hours off reels (+24%) -> 64%
        val twoHoursLater = t0 + 2 * 3600 * 1000L
        assertEquals(64, depleted.calculateDopamineSensitivity(currentTime = twoHoursLater))

        // 5 hours off reels (+60%) -> 100% (capped at 100%)
        val fiveHoursLater = t0 + 5 * 3600 * 1000L
        assertEquals(100, depleted.calculateDopamineSensitivity(currentTime = fiveHoursLater))
    }

    @Test
    fun testTranceBreakerMilestonesTriggerExpectedly() {
        // Milestones: 10, 25, 50, 75, 100, 150, 200...
        val r9 = ReelStats(todayTotal = 9, dailyLimit = 50)
        org.junit.Assert.assertFalse(r9.isTranceBreakerDue)

        val r10 = ReelStats(todayTotal = 10, dailyLimit = 50)
        org.junit.Assert.assertTrue(r10.isTranceBreakerDue)

        val r24 = ReelStats(todayTotal = 24, dailyLimit = 50)
        org.junit.Assert.assertFalse(r24.isTranceBreakerDue)

        val r25 = ReelStats(todayTotal = 25, dailyLimit = 50)
        org.junit.Assert.assertTrue(r25.isTranceBreakerDue)

        val r49 = ReelStats(todayTotal = 49, dailyLimit = 50)
        org.junit.Assert.assertFalse(r49.isTranceBreakerDue)

        val r50 = ReelStats(todayTotal = 50, dailyLimit = 50)
        org.junit.Assert.assertTrue(r50.isTranceBreakerDue)

        val r75 = ReelStats(todayTotal = 75, dailyLimit = 50)
        org.junit.Assert.assertTrue(r75.isTranceBreakerDue)

        val r100 = ReelStats(todayTotal = 100, dailyLimit = 50)
        org.junit.Assert.assertTrue(r100.isTranceBreakerDue)

        // Custom daily limit test
        val customLimit = ReelStats(todayTotal = 35, dailyLimit = 35)
        org.junit.Assert.assertTrue(customLimit.isTranceBreakerDue)

        // Recurring milestone beyond 150
        val r200 = ReelStats(todayTotal = 200, dailyLimit = 50)
        org.junit.Assert.assertTrue(r200.isTranceBreakerDue)
    }

    @Test
    fun testFormatDurationPrecision() {
        // Zero time
        assertEquals("0s", ReelStats.formatDuration(0L))
        assertEquals("0s", ReelStats.formatDuration(-100L))

        // Seconds only
        assertEquals("45s", ReelStats.formatDuration(45_000L))

        // Minutes and seconds
        assertEquals("2m 30s", ReelStats.formatDuration(150_000L))
        assertEquals("5m", ReelStats.formatDuration(300_000L))

        // Hours and minutes
        assertEquals("1h 15m", ReelStats.formatDuration(4500_000L))
        assertEquals("2h", ReelStats.formatDuration(7200_000L))
    }
}
