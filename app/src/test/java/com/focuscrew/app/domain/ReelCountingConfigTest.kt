package com.focuscrew.app.domain

import com.focuscrew.app.data.model.PlatformAccuracyMatrix
import com.focuscrew.app.data.model.ReelCountingMetric
import com.focuscrew.app.data.model.ReelStats
import org.junit.Assert.*
import org.junit.Test

class ReelCountingConfigTest {

    @Test
    fun testDefaultReelCountingConfig() {
        val stats = ReelStats()
        assertEquals(ReelCountingMetric.SCROLLS, stats.countingMetric)
        assertEquals(5, stats.watchTimeThresholdSec)
    }

    @Test
    fun testReelCountingMetricModes() {
        val scrollsMode = ReelStats(countingMetric = ReelCountingMetric.SCROLLS)
        assertEquals(ReelCountingMetric.SCROLLS, scrollsMode.countingMetric)

        val watchedMode = ReelStats(countingMetric = ReelCountingMetric.ESTIMATED_WATCHED, watchTimeThresholdSec = 10)
        assertEquals(ReelCountingMetric.ESTIMATED_WATCHED, watchedMode.countingMetric)
        assertEquals(10, watchedMode.watchTimeThresholdSec)
    }

    @Test
    fun testPlatformAccuracyMatrixIntegrity() {
        val platforms = PlatformAccuracyMatrix.platforms
        assertEquals(5, platforms.size)

        // Instagram Reels must be supported
        val ig = platforms.first { it.platformName.contains("Instagram") }
        assertTrue(ig.isSupported)
        assertEquals("100% Verified", ig.accuracyLevel)

        // YouTube Shorts must be supported
        val yt = platforms.first { it.platformName.contains("YouTube") }
        assertTrue(yt.isSupported)
        assertEquals("100% Verified", yt.accuracyLevel)

        // TikTok must be clearly flagged as NOT supported due to zero-scraping privacy
        val tiktok = platforms.first { it.platformName.contains("TikTok") }
        assertFalse(tiktok.isSupported)
        assertEquals("Not Supported", tiktok.accuracyLevel)
        assertTrue(tiktok.limitationReason.contains("privacy") || tiktok.limitationReason.contains("scraping"))

        // Facebook Reels must be flagged
        val fb = platforms.first { it.platformName.contains("Facebook") }
        assertFalse(fb.isSupported)

        // Snapchat Spotlight must be flagged
        val snap = platforms.first { it.platformName.contains("Snapchat") }
        assertFalse(snap.isSupported)
    }

    @Test
    fun testSwipeClassifierDeduplication() {
        // Test that identical pageIndex within cooldown is strictly rejected
        val isCooldownBlocked = ReelSwipeClassifier.shouldCountSwipe(
            lastSwipeTimestamp = 1000L,
            currentTime = 1200L, // only 200ms elapsed (< 400ms debounce)
            currentPageIndex = 1,
            lastPageIndex = 0
        )
        assertFalse("Settling frames within 400ms must be debounced", isCooldownBlocked)

        val isAccepted = ReelSwipeClassifier.shouldCountSwipe(
            lastSwipeTimestamp = 1000L,
            currentTime = 1500L, // 500ms elapsed (>= 400ms debounce)
            currentPageIndex = 1,
            lastPageIndex = 0
        )
        assertTrue("Deliberate new swipe after cooldown must be accepted", isAccepted)
    }
}
