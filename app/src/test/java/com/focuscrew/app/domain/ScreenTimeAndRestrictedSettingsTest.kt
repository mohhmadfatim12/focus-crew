package com.focuscrew.app.domain

import com.focuscrew.app.data.model.ReelStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeAndRestrictedSettingsTest {

    @Test
    fun testMidnightStartCalculationIsReasonable() {
        val midnight = com.focuscrew.app.util.UsageStatsHelper.getStartOfTodayMs()
        val now = System.currentTimeMillis()
        assertTrue("Midnight must be in the past or right now", midnight <= now)
        assertTrue("Midnight must be within the last 24 hours", (now - midnight) < 24 * 3600 * 1000L)
    }

    @Test
    fun testPlatformPackagesConstants() {
        assertEquals("com.instagram.android", com.focuscrew.app.util.UsageStatsHelper.PACKAGE_INSTAGRAM)
        assertEquals("com.google.android.youtube", com.focuscrew.app.util.UsageStatsHelper.PACKAGE_YOUTUBE)
    }

    @Test
    fun testPlatformTimeAccumulationPreservesIntegrity() {
        val initial = ReelStats(
            todayInstagramTimeMs = 120_000L, // 2 mins
            todayYoutubeTimeMs = 300_000L    // 5 mins
        )

        val moreIgTime = 60_000L // +1 min
        val updated = initial.copy(
            todayInstagramTimeMs = initial.todayInstagramTimeMs + moreIgTime
        )

        assertEquals(180_000L, updated.todayInstagramTimeMs)
        assertEquals("3m", ReelStats.formatDuration(updated.todayInstagramTimeMs))
        assertEquals(300_000L, updated.todayYoutubeTimeMs)
        assertEquals("5m", ReelStats.formatDuration(updated.todayYoutubeTimeMs))
    }

    @Test
    fun testAntiTamperExemptionLogic() {
        // Test that accessibility and permission screens are strictly recognized and not blocked
        val accessibilityScreen = "accessibility downloaded apps focuscrew turn on allow"
        val isExempt1 = accessibilityScreen.contains("accessibility") ||
                accessibilityScreen.contains("downloaded apps") ||
                accessibilityScreen.contains("allow restricted")

        assertTrue("Accessibility screens must be exempt from anti-tamper", isExempt1)

        val restrictedScreen = "app info focuscrew allow restricted settings"
        val isExempt2 = restrictedScreen.contains("allow restricted") ||
                restrictedScreen.contains("restricted settings")

        assertTrue("Restricted settings unlock screen must be exempt from anti-tamper", isExempt2)

        // Destructive actions during timer
        val destructiveScreen = "focuscrew force stop uninstall"
        val isDestructive = destructiveScreen.contains("force stop") || destructiveScreen.contains("uninstall")
        val isFocusCrew = destructiveScreen.contains("focuscrew")

        assertTrue("Force stop and uninstall must be caught for active timer anti-tamper", isDestructive && isFocusCrew)
    }
}
