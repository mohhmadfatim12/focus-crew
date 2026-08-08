package com.focuscrew.app.domain

import kotlin.math.roundToInt

/**
 * LifeReclaimedCalculator — Quantifies the tangible life and capital saved
 * by avoiding digital brain rot and adult content binges.
 */
data class LifeReclaimedStats(
    val totalHoursSaved: Double,
    val focusCapitalDollars: Int,
    val booksReadEquivalent: Double,
    val workoutsEquivalent: Int,
    val deepWorkDaysEquivalent: Double
)

object LifeReclaimedCalculator {

    // Conservative estimates based on behavioral addiction research
    const val DEFAULT_HOURLY_RATE = 25 // $25/hr default value of focus
    const val HOURS_SAVED_PER_CLEAN_DAY = 2.5 // Average time lost to porn + recovery brain fog
    const val HOURS_SAVED_PER_SEARCH_INTERCEPTED = 0.75 // 45 minutes of rabbit-hole averted
    const val HOURS_SAVED_PER_URGE_CONQUERED = 1.0 // 1 hour of binging and regret prevented
    const val HOURS_PER_BOOK = 6.0 // Average time to read a non-fiction book
    const val HOURS_PER_WORKOUT = 1.0
    const val HOURS_PER_DEEP_WORK_DAY = 4.0 // 4 hours of pure undistracted deep work

    /**
     * Computes real-world reclaimed metrics.
     */
    fun calculate(
        cleanDays: Int,
        searchesIntercepted: Int,
        urgesConquered: Int,
        hourlyRate: Int = DEFAULT_HOURLY_RATE
    ): LifeReclaimedStats {
        val days = cleanDays.coerceAtLeast(0)
        val intercepts = searchesIntercepted.coerceAtLeast(0)
        val urges = urgesConquered.coerceAtLeast(0)
        val rate = hourlyRate.coerceAtLeast(1)

        val hoursFromDays = days * HOURS_SAVED_PER_CLEAN_DAY
        val hoursFromIntercepts = intercepts * HOURS_SAVED_PER_SEARCH_INTERCEPTED
        val hoursFromUrges = urges * HOURS_SAVED_PER_URGE_CONQUERED

        val totalHours = hoursFromDays + hoursFromIntercepts + hoursFromUrges
        val capital = (totalHours * rate).roundToInt()
        val books = totalHours / HOURS_PER_BOOK
        val workouts = (totalHours / HOURS_PER_WORKOUT).toInt()
        val deepWorkDays = totalHours / HOURS_PER_DEEP_WORK_DAY

        return LifeReclaimedStats(
            totalHoursSaved = (totalHours * 10.0).roundToInt() / 10.0,
            focusCapitalDollars = capital,
            booksReadEquivalent = (books * 10.0).roundToInt() / 10.0,
            workoutsEquivalent = workouts,
            deepWorkDaysEquivalent = (deepWorkDays * 10.0).roundToInt() / 10.0
        )
    }
}
