package com.focuscrew.app.data.model

data class ReelStats(
    val todayTotal: Int = 0,
    val yesterdayTotal: Int = 0,
    val todayInstagram: Int = 0,
    val todayYoutube: Int = 0,
    val yesterdayInstagram: Int = 0,
    val yesterdayYoutube: Int = 0,
    val todayInstagramTimeMs: Long = 0L,
    val todayYoutubeTimeMs: Long = 0L,
    val yesterdayInstagramTimeMs: Long = 0L,
    val yesterdayYoutubeTimeMs: Long = 0L,
    val dailyLimit: Int = 50,
    val isOverlayEnabled: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val lastSwipeTimestamp: Long = 0L,
    val countingMetric: ReelCountingMetric = ReelCountingMetric.SCROLLS,
    val watchTimeThresholdSec: Int = 5
) {
    val todayTotalTimeMs: Long
        get() = todayInstagramTimeMs + todayYoutubeTimeMs

    val yesterdayTotalTimeMs: Long
        get() = yesterdayInstagramTimeMs + yesterdayYoutubeTimeMs

    val percentageVsYesterday: Int
        get() = if (yesterdayTotal == 0) 0 else ((todayTotal - yesterdayTotal) * 100) / yesterdayTotal

    val warningStage: ReelWarningStage
        get() = when {
            todayTotal <= (dailyLimit * 0.4).toInt() -> ReelWarningStage.SAFE
            todayTotal <= dailyLimit -> ReelWarningStage.CAUTION
            else -> ReelWarningStage.DANGER
        }

    val brainState: BrainHealthState
        get() {
            val limit = if (dailyLimit > 0) dailyLimit else 50
            val ratio = todayTotal.toFloat() / limit.toFloat()
            return when {
                todayTotal <= 0 -> BrainHealthState.FRESH
                ratio <= 0.40f -> BrainHealthState.FRESH
                ratio <= 0.75f -> BrainHealthState.CONCERNED
                ratio <= 1.00f -> BrainHealthState.WEARY
                ratio <= 1.50f -> BrainHealthState.EXHAUSTED
                else -> BrainHealthState.BRAINROT
            }
        }

    /**
     * NeuroShield Dopamine Baseline Sensitivity (10% - 100%).
     * Each reel consumed drains dopamine sensitivity by ~0.75%.
     * Active recovery regenerates +12% per hour off reels.
     */
    fun calculateDopamineSensitivity(currentTime: Long = System.currentTimeMillis()): Int {
        if (todayTotal <= 0) return 100
        val baseDrain = (todayTotal * 0.75f).toInt()
        val depletedBaseline = (100 - baseDrain).coerceAtLeast(10)

        if (lastSwipeTimestamp > 0L) {
            val offReelsMs = (currentTime - lastSwipeTimestamp).coerceAtLeast(0L)
            val recoveryHours = offReelsMs / (3600 * 1000f)
            val recoveredPercent = (recoveryHours * 12f).toInt()
            return (depletedBaseline + recoveredPercent).coerceIn(10, 100)
        }
        return depletedBaseline
    }

    val dopamineStatus: DopamineBiologicalState
        get() {
            val level = calculateDopamineSensitivity()
            return when {
                level >= 80 -> DopamineBiologicalState.PEAK_SHARP
                level >= 60 -> DopamineBiologicalState.ATTENTION_FRAGMENTED
                level >= 40 -> DopamineBiologicalState.DOPAMINE_CRASH
                level >= 20 -> DopamineBiologicalState.MOTOR_ZOMBIE
                else -> DopamineBiologicalState.EXECUTIVE_DYSFUNCTION
            }
        }

    /**
     * Trance-Breaker trigger check:
     * Fires automatically at configured daily limit and critical dopamine crash milestones (10, 25, 50, 75, 100, 150+ reels).
     */
    val isTranceBreakerDue: Boolean
        get() = todayTotal > 0 && (
            (dailyLimit > 0 && todayTotal == dailyLimit) ||
            todayTotal == 10 || todayTotal == 25 || todayTotal == 50 ||
            todayTotal == 75 || todayTotal == 100 || todayTotal == 150 ||
            (todayTotal > 150 && todayTotal % 50 == 0)
        )

    companion object {
        fun formatDuration(timeMs: Long): String {
            if (timeMs <= 0L) return "0m"
            val totalSeconds = timeMs / 1000
            val totalMinutes = totalSeconds / 60
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                totalSeconds > 0 -> "${totalSeconds}s"
                else -> "0m"
            }
        }
    }
}

enum class ReelWarningStage {
    SAFE,       // 1 - ~15 reels (Teal)
    CAUTION,    // 16 - ~35 reels (Amber)
    DANGER      // 36+ reels (Red / Doom-scroll alert)
}

enum class BrainHealthState(val stageIndex: Int, val title: String, val quote: String) {
    FRESH(0, "Fresh & Focused", "Brain is sharp! Keep going strong."),
    CONCERNED(1, "Slight Fatigue", "Is this reel really worth your time?"),
    WEARY(2, "Dazed & Weary", "Dopamine receptors burning out..."),
    EXHAUSTED(3, "High Strain", "Your brain is begging for a break!"),
    BRAINROT(4, "Full Brainrot", "Meltdown! Achievement unlocked: Brain Dead 🧠🔥")
}

enum class DopamineBiologicalState(
    val title: String,
    val subtitle: String,
    val neurobiologyNotice: String
) {
    PEAK_SHARP(
        "Peak Focus Sensitivity",
        "Dopamine receptors at 100%",
        "Mental clarity is high. Willpower reserves are full. Optimal state for deep work."
    ),
    ATTENTION_FRAGMENTED(
        "Attention Fragmenting",
        "Craving fast stimulation",
        "Receptors are getting desensitized. High resistance to reading or slow tasks."
    ),
    DOPAMINE_CRASH(
        "Dopamine Crash Active",
        "Working memory down 40%",
        "You are in mild withdrawal. Normal tasks feel agonizingly boring compared to short video stimulation."
    ),
    MOTOR_ZOMBIE(
        "Hypnotic Motor Loop",
        "Prefrontal cortex offline",
        "Your thumb is swiping automatically. Zero information is being retained in long-term memory."
    ),
    EXECUTIVE_DYSFUNCTION(
        "Willpower Meltdown",
        "Complete dopamine exhaustion",
        "Brain fog maximum. You traded hours of real ambition for rapid sensory junk."
    )
}

enum class ReelCountingMetric(
    val title: String,
    val badge: String,
    val description: String
) {
    SCROLLS(
        "Every Scroll Gesture",
        "👆 Scrolls",
        "Counts every deliberate swipe/scroll gesture through reels."
    ),
    OPENED_REELS(
        "Opened Reels",
        "🎬 Unique Reels",
        "Counts unique reels opened or navigated into, ignoring micro-scrolls on the same video."
    ),
    ESTIMATED_WATCHED(
        "Estimated Watched Reels",
        "⏱️ Watched (Dwell Time)",
        "Only counts a reel if you linger and watch it for at least the watch-time threshold."
    )
}

data class PlatformAccuracy(
    val platformName: String,
    val packageName: String,
    val isSupported: Boolean,
    val accuracyLevel: String, // "100% Verified", "Experimental", "Not Supported"
    val detectionMethod: String,
    val limitationReason: String
)

object PlatformAccuracyMatrix {
    val platforms = listOf(
        PlatformAccuracy(
            platformName = "Instagram Reels",
            packageName = "com.instagram.android",
            isSupported = true,
            accuracyLevel = "100% Verified",
            detectionMethod = "Clips Viewport + Adapter Index Delta",
            limitationReason = "Fully supported with 0% text scraping."
        ),
        PlatformAccuracy(
            platformName = "YouTube Shorts",
            packageName = "com.google.android.youtube",
            isSupported = true,
            accuracyLevel = "100% Verified",
            detectionMethod = "Fullscreen Snap Pager + Momentum Debounce",
            limitationReason = "Fully supported. Legacy v7 feed strictly isolated."
        ),
        PlatformAccuracy(
            platformName = "TikTok",
            packageName = "com.zhiliaoapp.musically",
            isSupported = false,
            accuracyLevel = "Not Supported",
            detectionMethod = "None (Privacy Protected)",
            limitationReason = "Requires invasive accessibility view hierarchy scraping which FocusCrew strictly forbids for your privacy."
        ),
        PlatformAccuracy(
            platformName = "Facebook Reels",
            packageName = "com.facebook.katana",
            isSupported = false,
            accuracyLevel = "Not Supported",
            detectionMethod = "None (Multi-Feed Ambiguity)",
            limitationReason = "Cannot differentiate reels from standard newsfeed posts without inspecting user post text."
        ),
        PlatformAccuracy(
            platformName = "Snapchat Spotlight",
            packageName = "com.snapchat.android",
            isSupported = false,
            accuracyLevel = "Not Supported",
            detectionMethod = "None (Proprietary Canvas)",
            limitationReason = "Proprietary gesture pipeline does not emit standard Android scroll metadata."
        )
    )
}


