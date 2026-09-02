package com.focuscrew.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeuroplasticityTimelineTest {

    @Test
    fun `milestones list is ordered by days required ascending`() {
        val list = NeuroplasticityTimeline.MILESTONES
        assertTrue(list.isNotEmpty())
        for (i in 0 until list.size - 1) {
            assertTrue(list[i].dayRequired < list[i + 1].dayRequired)
        }
    }

    @Test
    fun `day 0 returns day 1 milestone as baseline`() {
        val current = NeuroplasticityTimeline.getCurrentMilestone(0)
        assertEquals(1, current.dayRequired)
        assertEquals("Adrenaline & Cortisol Flush", current.title)
    }

    @Test
    fun `day 7 returns androgen and energy surge milestone`() {
        val current = NeuroplasticityTimeline.getCurrentMilestone(7)
        assertEquals(7, current.dayRequired)
        assertEquals("Androgen & Energy Surge", current.title)

        val next = NeuroplasticityTimeline.getNextMilestone(7)
        assertNotNull(next)
        assertEquals(14, next?.dayRequired)
    }

    @Test
    fun `day 90 returns final milestone with null next milestone`() {
        val current = NeuroplasticityTimeline.getCurrentMilestone(90)
        assertEquals(90, current.dayRequired)
        assertEquals("The Full Titan Reboot", current.title)

        val next = NeuroplasticityTimeline.getNextMilestone(90)
        assertNull(next)

        val progress = NeuroplasticityTimeline.getProgressToNextMilestone(90)
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun `progress calculation works accurately between milestones`() {
        // Between Day 7 and Day 14 (range = 7 days)
        // Day 7 -> 0% progress to next
        assertEquals(0.0f, NeuroplasticityTimeline.getProgressToNextMilestone(7), 0.01f)
        // Day 10 -> 3/7 = ~42.8% progress
        val p10 = NeuroplasticityTimeline.getProgressToNextMilestone(10)
        assertEquals(3.0f / 7.0f, p10, 0.01f)
    }
}
