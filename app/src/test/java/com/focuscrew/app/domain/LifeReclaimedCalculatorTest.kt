package com.focuscrew.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LifeReclaimedCalculatorTest {

    @Test
    fun `zero stats returns zero metrics`() {
        val stats = LifeReclaimedCalculator.calculate(0, 0, 0)
        assertEquals(0.0, stats.totalHoursSaved, 0.01)
        assertEquals(0, stats.focusCapitalDollars)
        assertEquals(0.0, stats.booksReadEquivalent, 0.01)
        assertEquals(0, stats.workoutsEquivalent)
    }

    @Test
    fun `calculates 10 clean days accurately`() {
        // 10 clean days = 25 hours saved
        // 25 hrs * $25 = $625
        // 25 / 6 = ~4.2 books
        // 25 workouts
        val stats = LifeReclaimedCalculator.calculate(cleanDays = 10, searchesIntercepted = 0, urgesConquered = 0)
        assertEquals(25.0, stats.totalHoursSaved, 0.01)
        assertEquals(625, stats.focusCapitalDollars)
        assertEquals(4.2, stats.booksReadEquivalent, 0.01)
        assertEquals(25, stats.workoutsEquivalent)
        assertEquals(6.3, stats.deepWorkDaysEquivalent, 0.01)
    }

    @Test
    fun `combines days, search intercepts, and urges`() {
        // 2 days = 5.0 hrs
        // 4 intercepts * 0.75 = 3.0 hrs
        // 2 urges * 1.0 = 2.0 hrs
        // Total = 10.0 hrs
        // Capital at $30/hr = $300
        val stats = LifeReclaimedCalculator.calculate(
            cleanDays = 2,
            searchesIntercepted = 4,
            urgesConquered = 2,
            hourlyRate = 30
        )
        assertEquals(10.0, stats.totalHoursSaved, 0.01)
        assertEquals(300, stats.focusCapitalDollars)
        assertEquals(10, stats.workoutsEquivalent)
    }
}
