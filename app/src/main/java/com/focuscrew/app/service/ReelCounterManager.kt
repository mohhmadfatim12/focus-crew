package com.focuscrew.app.service

import android.content.Context
import android.content.SharedPreferences
import com.focuscrew.app.data.model.ReelStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global controller tracking Instagram Reels and YouTube Shorts swipes.
 * Handles 1.4s swipe debouncing, ViewPager2 page index validation,
 * daily rollovers, offline persistence, and live reactive stats.
 */
object ReelCounterManager {

    private const val PREFS_NAME = "focuscrew_reel_stats"
    private const val KEY_LAST_DATE = "last_recorded_date"
    private const val KEY_TODAY_TOTAL = "today_total_reels"
    private const val KEY_TODAY_IG = "today_ig_reels"
    private const val KEY_TODAY_YT = "today_yt_reels"
    private const val KEY_YESTERDAY_TOTAL = "yesterday_total_reels"
    private const val KEY_YESTERDAY_IG = "yesterday_ig_reels"
    private const val KEY_YESTERDAY_YT = "yesterday_yt_reels"
    private const val KEY_TODAY_IG_TIME = "today_ig_time_ms"
    private const val KEY_TODAY_YT_TIME = "today_yt_time_ms"
    private const val KEY_YESTERDAY_IG_TIME = "yesterday_ig_time_ms"
    private const val KEY_YESTERDAY_YT_TIME = "yesterday_yt_time_ms"
    private const val KEY_DAILY_LIMIT = "daily_reel_limit"
    private const val KEY_OVERLAY_ENABLED = "is_overlay_enabled"
    private const val KEY_NOTIFICATION_ENABLED = "is_notification_enabled"
    private const val KEY_LAST_SWIPE_TIMESTAMP = "last_swipe_timestamp"
    private const val KEY_COUNTING_METRIC = "reel_counting_metric"
    private const val KEY_WATCH_TIME_THRESHOLD = "reel_watch_time_threshold_sec"

    // Debounce is now handled adaptively by ReelSwipeClassifier.shouldCountSwipe()
    private var lastSwipeTimestamp = 0L
    private var lastPageIndex = -1
    private var currentReelStartTime = 0L
    private var currentReelPackage: String? = null

    // Trance-Breaker Psychological Interruption state
    private var lastTranceBreakerTriggeredCount = -1
    private var graceReelsRemaining = 0

    fun grantGraceReels(count: Int = 5) {
        graceReelsRemaining = count
        lastTranceBreakerTriggeredCount = _stats.value.todayTotal
    }

    fun shouldTriggerTranceBreaker(): Boolean {
        val current = _stats.value
        if (current.todayTotal <= 0) return false
        if (graceReelsRemaining > 0) return false
        if (current.todayTotal == lastTranceBreakerTriggeredCount) return false

        val isMilestone = current.isTranceBreakerDue
        if (isMilestone) {
            lastTranceBreakerTriggeredCount = current.todayTotal
            return true
        }
        return false
    }

    // Active Reel Viewing Session tracking (only tracks time during verified reel scrolling)
    private var activeReelPackage: String? = null
    private var lastReelActiveTimestamp = 0L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _stats = MutableStateFlow(ReelStats())
    val stats: StateFlow<ReelStats> = _stats.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        checkAndApplyDateRollover(prefs)
        loadFromPrefs(prefs)
        WeeklyReelScheduleManager.initialize(context)
    }

    @Synchronized
    fun recordSwipe(packageName: String, pageIndex: Int = -1): Boolean {
        val now = System.currentTimeMillis()

        val isInstagram = packageName.contains("instagram")
        val isYoutube = packageName.contains("youtube")
        if (!isInstagram && !isYoutube) {
            return false
        }

        // Validate cooldown and page index deduplication
        if (!com.focuscrew.app.domain.ReelSwipeClassifier.shouldCountSwipe(
                lastSwipeTimestamp = lastSwipeTimestamp,
                currentTime = now,
                currentPageIndex = pageIndex,
                lastPageIndex = lastPageIndex
            )
        ) {
            return false
        }

        val current = _stats.value
        val shouldIncrement = true
        currentReelStartTime = now

        lastSwipeTimestamp = now
        if (pageIndex != -1) {
            lastPageIndex = pageIndex
        }

        // Accurately accumulate active reel viewing session time
        onReelSwipeDetected(packageName, now)

        if (!shouldIncrement) {
            return false
        }

        // Consume 1 grace reel if active
        if (graceReelsRemaining > 0) {
            graceReelsRemaining--
        }

        val prefs = sharedPreferences ?: return false
        checkAndApplyDateRollover(prefs)

        val newTodayTotal = current.todayTotal + 1
        val newTodayIg = if (isInstagram) current.todayInstagram + 1 else current.todayInstagram
        val newTodayYt = if (isYoutube) current.todayYoutube + 1 else current.todayYoutube

        val updated = current.copy(
            todayTotal = newTodayTotal,
            todayInstagram = newTodayIg,
            todayYoutube = newTodayYt,
            lastSwipeTimestamp = now
        )
        _stats.value = updated

        // Persist immediately
        prefs.edit().apply {
            putInt(KEY_TODAY_TOTAL, newTodayTotal)
            putInt(KEY_TODAY_IG, newTodayIg)
            putInt(KEY_TODAY_YT, newTodayYt)
            putLong(KEY_LAST_SWIPE_TIMESTAMP, now)
            apply()
        }

        WeeklyReelScheduleManager.recordSwipeForToday(now)

        return true
    }

    /**
     * Accurately tracks screen time ONLY during active, verified reel viewing sessions.
     * When user is watching a normal video or chatting, this is NEVER invoked.
     */
    @Synchronized
    fun onReelSwipeDetected(packageName: String, now: Long = System.currentTimeMillis()) {
        val isInstagram = packageName.contains("instagram")
        val isYoutube = packageName.contains("youtube")
        if (!isInstagram && !isYoutube) return

        val normalizedPkg = if (isInstagram) "com.instagram.android" else "com.google.android.youtube"

        if (activeReelPackage == normalizedPkg && lastReelActiveTimestamp > 0L) {
            val elapsed = now - lastReelActiveTimestamp
            // Genuine elapsed wall-clock time between swipes (up to 10 minutes of continuous viewing)
            if (elapsed in 500L..600_000L) {
                addTimeSpent(normalizedPkg, elapsed)
            }
        }

        activeReelPackage = normalizedPkg
        lastReelActiveTimestamp = now
    }

    /**
     * Flushes any remaining active reel time and resets session state.
     * Called when the user exits Instagram/YouTube or returns to FocusCrew.
     */
    @Synchronized
    fun endReelSession(now: Long = System.currentTimeMillis()) {
        val pkg = activeReelPackage
        if (pkg != null && lastReelActiveTimestamp > 0L) {
            val elapsed = now - lastReelActiveTimestamp
            if (elapsed in 500L..600_000L) {
                addTimeSpent(pkg, elapsed)
            }
        }
        activeReelPackage = null
        lastReelActiveTimestamp = 0L
        lastPageIndex = -1
        lastSwipeTimestamp = 0L
    }

    fun resetSessionIndex() {
        endReelSession()
    }

    /**
     * Pulses continuous 1-second active watch time while user is inside Instagram Reels or YouTube Shorts.
     */
    @Synchronized
    fun pulseActiveWatchTime(packageName: String, durationMs: Long) {
        addTimeSpent(packageName, durationMs)
        lastReelActiveTimestamp = System.currentTimeMillis()
    }

    @Synchronized
    fun addTimeSpent(packageName: String, durationMs: Long) {
        if (durationMs <= 0L || durationMs > 12 * 3600 * 1000L) {
            return
        }
        val prefs = sharedPreferences ?: return
        checkAndApplyDateRollover(prefs)

        val isInstagram = packageName.contains("instagram")
        val isYoutube = packageName.contains("youtube")

        if (!isInstagram && !isYoutube) {
            return
        }

        val current = _stats.value
        val newTodayIgTime = if (isInstagram) current.todayInstagramTimeMs + durationMs else current.todayInstagramTimeMs
        val newTodayYtTime = if (isYoutube) current.todayYoutubeTimeMs + durationMs else current.todayYoutubeTimeMs

        val updated = current.copy(
            todayInstagramTimeMs = newTodayIgTime,
            todayYoutubeTimeMs = newTodayYtTime
        )
        _stats.value = updated

        prefs.edit().apply {
            putLong(KEY_TODAY_IG_TIME, newTodayIgTime)
            putLong(KEY_TODAY_YT_TIME, newTodayYtTime)
            apply()
        }

        WeeklyReelScheduleManager.recordTimeForToday(durationMs)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        _stats.value = _stats.value.copy(isOverlayEnabled = enabled)
        sharedPreferences?.edit()?.putBoolean(KEY_OVERLAY_ENABLED, enabled)?.apply()
    }

    fun setNotificationEnabled(enabled: Boolean) {
        _stats.value = _stats.value.copy(isNotificationEnabled = enabled)
        sharedPreferences?.edit()?.putBoolean(KEY_NOTIFICATION_ENABLED, enabled)?.apply()
    }

    fun setDailyLimit(limit: Int) {
        _stats.value = _stats.value.copy(dailyLimit = limit)
        sharedPreferences?.edit()?.putInt(KEY_DAILY_LIMIT, limit)?.apply()
    }

    fun resetCounts() {
        lastSwipeTimestamp = 0L
        lastPageIndex = -1
        lastTranceBreakerTriggeredCount = -1
        graceReelsRemaining = 0
        val updated = _stats.value.copy(
            todayTotal = 0,
            todayInstagram = 0,
            todayYoutube = 0,
            todayInstagramTimeMs = 0L,
            todayYoutubeTimeMs = 0L,
            lastSwipeTimestamp = 0L
        )
        _stats.value = updated
        sharedPreferences?.edit()?.apply {
            putInt(KEY_TODAY_TOTAL, 0)
            putInt(KEY_TODAY_IG, 0)
            putInt(KEY_TODAY_YT, 0)
            putLong(KEY_TODAY_IG_TIME, 0L)
            putLong(KEY_TODAY_YT_TIME, 0L)
            putLong(KEY_LAST_SWIPE_TIMESTAMP, 0L)
            apply()
        }
    }

    private fun checkAndApplyDateRollover(prefs: SharedPreferences) {
        val todayStr = dateFormat.format(Date())
        val lastDateStr = prefs.getString(KEY_LAST_DATE, todayStr) ?: todayStr

        if (todayStr != lastDateStr) {
            val prevTodayTotal = prefs.getInt(KEY_TODAY_TOTAL, 0)
            val prevTodayIg = prefs.getInt(KEY_TODAY_IG, 0)
            val prevTodayYt = prefs.getInt(KEY_TODAY_YT, 0)
            val prevTodayIgTime = prefs.getLong(KEY_TODAY_IG_TIME, 0L)
            val prevTodayYtTime = prefs.getLong(KEY_TODAY_YT_TIME, 0L)

            val parsedLastDate = try { dateFormat.parse(lastDateStr) } catch (_: Exception) { null }
            val parsedToday = try { dateFormat.parse(todayStr) } catch (_: Exception) { null }
            val daysDifference = if (parsedLastDate != null && parsedToday != null) {
                ((parsedToday.time - parsedLastDate.time) / (1000L * 60 * 60 * 24)).coerceAtLeast(1L)
            } else {
                1L
            }

            // If exactly 1 day passed, yesterday's count was prevTodayTotal.
            // If more than 1 day skipped, yesterday's count was 0.
            val newYestTotal = if (daysDifference == 1L) prevTodayTotal else 0
            val newYestIg = if (daysDifference == 1L) prevTodayIg else 0
            val newYestYt = if (daysDifference == 1L) prevTodayYt else 0
            val newYestIgTime = if (daysDifference == 1L) prevTodayIgTime else 0L
            val newYestYtTime = if (daysDifference == 1L) prevTodayYtTime else 0L

            prefs.edit().apply {
                putString(KEY_LAST_DATE, todayStr)
                putInt(KEY_YESTERDAY_TOTAL, newYestTotal)
                putInt(KEY_YESTERDAY_IG, newYestIg)
                putInt(KEY_YESTERDAY_YT, newYestYt)
                putLong(KEY_YESTERDAY_IG_TIME, newYestIgTime)
                putLong(KEY_YESTERDAY_YT_TIME, newYestYtTime)
                putInt(KEY_TODAY_TOTAL, 0)
                putInt(KEY_TODAY_IG, 0)
                putInt(KEY_TODAY_YT, 0)
                putLong(KEY_TODAY_IG_TIME, 0L)
                putLong(KEY_TODAY_YT_TIME, 0L)
                putLong(KEY_LAST_SWIPE_TIMESTAMP, 0L)
                apply()
            }

            lastTranceBreakerTriggeredCount = -1
            graceReelsRemaining = 0

            _stats.value = _stats.value.copy(
                todayTotal = 0,
                todayInstagram = 0,
                todayYoutube = 0,
                todayInstagramTimeMs = 0L,
                todayYoutubeTimeMs = 0L,
                lastSwipeTimestamp = 0L,
                yesterdayTotal = newYestTotal,
                yesterdayInstagram = newYestIg,
                yesterdayYoutube = newYestYt,
                yesterdayInstagramTimeMs = newYestIgTime,
                yesterdayYoutubeTimeMs = newYestYtTime
            )
        }
    }

    private fun loadFromPrefs(prefs: SharedPreferences) {
        val todayTotal = prefs.getInt(KEY_TODAY_TOTAL, 0)
        val todayIg = prefs.getInt(KEY_TODAY_IG, 0)
        val todayYt = prefs.getInt(KEY_TODAY_YT, 0)
        val yestTotal = prefs.getInt(KEY_YESTERDAY_TOTAL, 0)
        val yestIg = prefs.getInt(KEY_YESTERDAY_IG, 0)
        val yestYt = prefs.getInt(KEY_YESTERDAY_YT, 0)
        val todayIgTime = prefs.getLong(KEY_TODAY_IG_TIME, 0L)
        val todayYtTime = prefs.getLong(KEY_TODAY_YT_TIME, 0L)
        val yestIgTime = prefs.getLong(KEY_YESTERDAY_IG_TIME, 0L)
        val yestYtTime = prefs.getLong(KEY_YESTERDAY_YT_TIME, 0L)
        val dailyLimit = prefs.getInt(KEY_DAILY_LIMIT, 50)
        val overlayEnabled = prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
        val notifEnabled = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        val lastSwipe = prefs.getLong(KEY_LAST_SWIPE_TIMESTAMP, 0L)

        val metricStr = prefs.getString(KEY_COUNTING_METRIC, com.focuscrew.app.data.model.ReelCountingMetric.SCROLLS.name)
            ?: com.focuscrew.app.data.model.ReelCountingMetric.SCROLLS.name
        val metric = try {
            val parsed = com.focuscrew.app.data.model.ReelCountingMetric.valueOf(metricStr)
            if (parsed == com.focuscrew.app.data.model.ReelCountingMetric.ESTIMATED_WATCHED) {
                com.focuscrew.app.data.model.ReelCountingMetric.SCROLLS
            } else {
                parsed
            }
        } catch (e: Exception) {
            com.focuscrew.app.data.model.ReelCountingMetric.SCROLLS
        }
        val watchTimeThreshold = prefs.getInt(KEY_WATCH_TIME_THRESHOLD, 5)

        _stats.value = ReelStats(
            todayTotal = todayTotal,
            yesterdayTotal = yestTotal,
            todayInstagram = todayIg,
            todayYoutube = todayYt,
            yesterdayInstagram = yestIg,
            yesterdayYoutube = yestYt,
            todayInstagramTimeMs = todayIgTime,
            todayYoutubeTimeMs = todayYtTime,
            yesterdayInstagramTimeMs = yestIgTime,
            yesterdayYoutubeTimeMs = yestYtTime,
            dailyLimit = dailyLimit,
            isOverlayEnabled = overlayEnabled,
            isNotificationEnabled = notifEnabled,
            lastSwipeTimestamp = lastSwipe,
            countingMetric = metric,
            watchTimeThresholdSec = watchTimeThreshold
        )
    }

    fun setCountingMetric(metric: com.focuscrew.app.data.model.ReelCountingMetric) {
        sharedPreferences?.edit()?.putString(KEY_COUNTING_METRIC, metric.name)?.apply()
        _stats.value = _stats.value.copy(countingMetric = metric)
    }

    fun setWatchTimeThreshold(seconds: Int) {
        val clamped = seconds.coerceIn(3, 30)
        sharedPreferences?.edit()?.putInt(KEY_WATCH_TIME_THRESHOLD, clamped)?.apply()
        _stats.value = _stats.value.copy(watchTimeThresholdSec = clamped)
    }

    fun resetAllStats() {
        sharedPreferences?.edit()?.apply {
            putInt(KEY_TODAY_TOTAL, 0)
            putInt(KEY_TODAY_IG, 0)
            putInt(KEY_TODAY_YT, 0)
            putLong(KEY_TODAY_IG_TIME, 0L)
            putLong(KEY_TODAY_YT_TIME, 0L)
            putInt(KEY_YESTERDAY_TOTAL, 0)
            putInt(KEY_YESTERDAY_IG, 0)
            putInt(KEY_YESTERDAY_YT, 0)
            putLong(KEY_YESTERDAY_IG_TIME, 0L)
            putLong(KEY_YESTERDAY_YT_TIME, 0L)
            putLong(KEY_LAST_SWIPE_TIMESTAMP, 0L)
            apply()
        }
        lastSwipeTimestamp = 0L
        lastPageIndex = -1
        currentReelStartTime = 0L
        _stats.value = _stats.value.copy(
            todayTotal = 0,
            todayInstagram = 0,
            todayYoutube = 0,
            todayInstagramTimeMs = 0L,
            todayYoutubeTimeMs = 0L,
            yesterdayTotal = 0,
            yesterdayInstagram = 0,
            yesterdayYoutube = 0,
            yesterdayInstagramTimeMs = 0L,
            yesterdayYoutubeTimeMs = 0L,
            lastSwipeTimestamp = 0L
        )
    }
}

