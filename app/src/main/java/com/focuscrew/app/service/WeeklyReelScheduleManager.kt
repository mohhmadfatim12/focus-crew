package com.focuscrew.app.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focuscrew.app.data.model.DayOfWeekIso
import com.focuscrew.app.data.model.DayReelEntry
import com.focuscrew.app.data.model.WeeklyReelSchedule
import com.focuscrew.app.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Autonomous engine tracking the 7-day (Monday - Sunday) reel schedule,
 * handling automatic Sunday midnight reset & refresh, and firing weekly notifications.
 */
object WeeklyReelScheduleManager {

    private const val PREFS_NAME = "focuscrew_weekly_reel_schedule"
    private const val KEY_CURRENT_WEEK_ID = "current_week_id"
    private const val KEY_LAST_RESET_TIMESTAMP = "last_reset_timestamp"
    private const val KEY_LAST_NOTIFIED_SUNDAY_WEEK = "last_notified_sunday_week"
    private const val KEY_LAST_NOTIFIED_MONDAY_WEEK = "last_notified_monday_week"
    private const val KEY_ARCHIVED_PREFIX = "archived_week_"

    const val WEEKLY_NOTIF_CHANNEL_ID = "focuscrew_weekly_reel_reports"
    const val NOTIF_ID_SUNDAY_RESET = 2101
    const val NOTIF_ID_MONDAY_KICKOFF = 2102

    const val ACTION_SUNDAY_RESET = "com.focuscrew.app.action.WEEKLY_SUNDAY_RESET"
    const val ACTION_MONDAY_KICKOFF = "com.focuscrew.app.action.WEEKLY_MONDAY_KICKOFF"

    private val _schedule = MutableStateFlow(WeeklyReelSchedule())
    val schedule: StateFlow<WeeklyReelSchedule> = _schedule.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        createNotificationChannel(context)
        checkAndApplyWeekRollover(context, prefs)
        loadSchedule(prefs)
        scheduleWeeklyAlarms(context)
    }

    /**
     * Increments today's day-of-week reel counter by 1.
     * Invoked automatically whenever ReelCounterManager registers a reel swipe.
     */
    @Synchronized
    fun recordSwipeForToday(now: Long = System.currentTimeMillis()) {
        val prefs = sharedPreferences ?: return
        checkAndApplyWeekRollover(null, prefs, now)

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val todayIso = DayOfWeekIso.fromCalendarDay(cal.get(Calendar.DAY_OF_WEEK))

        val key = "count_${todayIso.name}"
        val currentCount = prefs.getInt(key, 0)
        val liveToday = ReelCounterManager.stats.value.todayTotal
        val newCount = maxOf(currentCount + 1, liveToday)

        prefs.edit().putInt(key, newCount).apply()
        loadSchedule(prefs, now)
    }

    /**
     * Checks if the calendar week has transitioned (Sunday midnight into Monday).
     * If so, archives the past week and wipes the 7-day schedule to 0.
     */
    @Synchronized
    fun checkAndApplyWeekRollover(context: Context?, prefs: SharedPreferences, now: Long = System.currentTimeMillis()) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentWeekId = getWeekId(cal)
        val savedWeekId = prefs.getString(KEY_CURRENT_WEEK_ID, null)

        if (savedWeekId == null) {
            // First run: initialize week ID
            prefs.edit().putString(KEY_CURRENT_WEEK_ID, currentWeekId).apply()
            return
        }

        if (savedWeekId != currentWeekId) {
            // The week has transitioned! Compute previous week's totals before wipe
            val prevMon = prefs.getInt("count_${DayOfWeekIso.MON.name}", 0)
            val prevTue = prefs.getInt("count_${DayOfWeekIso.TUE.name}", 0)
            val prevWed = prefs.getInt("count_${DayOfWeekIso.WED.name}", 0)
            val prevThu = prefs.getInt("count_${DayOfWeekIso.THU.name}", 0)
            val prevFri = prefs.getInt("count_${DayOfWeekIso.FRI.name}", 0)
            val prevSat = prefs.getInt("count_${DayOfWeekIso.SAT.name}", 0)
            val prevSun = prefs.getInt("count_${DayOfWeekIso.SUN.name}", 0)
            val prevTotal = prevMon + prevTue + prevWed + prevThu + prevFri + prevSat + prevSun

            val prevMonTime = prefs.getLong("time_${DayOfWeekIso.MON.name}", 0L)
            val prevTueTime = prefs.getLong("time_${DayOfWeekIso.TUE.name}", 0L)
            val prevWedTime = prefs.getLong("time_${DayOfWeekIso.WED.name}", 0L)
            val prevThuTime = prefs.getLong("time_${DayOfWeekIso.THU.name}", 0L)
            val prevFriTime = prefs.getLong("time_${DayOfWeekIso.FRI.name}", 0L)
            val prevSatTime = prefs.getLong("time_${DayOfWeekIso.SAT.name}", 0L)
            val prevSunTime = prefs.getLong("time_${DayOfWeekIso.SUN.name}", 0L)
            val prevTotalTime = prevMonTime + prevTueTime + prevWedTime + prevThuTime + prevFriTime + prevSatTime + prevSunTime

            // Archive previous week
            val archiveSummary = "total=$prevTotal;timeMs=$prevTotalTime;mon=$prevMon;tue=$prevTue;wed=$prevWed;thu=$prevThu;fri=$prevFri;sat=$prevSat;sun=$prevSun"
            prefs.edit()
                .putString("$KEY_ARCHIVED_PREFIX$savedWeekId", archiveSummary)
                .putString(KEY_CURRENT_WEEK_ID, currentWeekId)
                .putLong(KEY_LAST_RESET_TIMESTAMP, now)
                .putInt("count_${DayOfWeekIso.MON.name}", 0)
                .putInt("count_${DayOfWeekIso.TUE.name}", 0)
                .putInt("count_${DayOfWeekIso.WED.name}", 0)
                .putInt("count_${DayOfWeekIso.THU.name}", 0)
                .putInt("count_${DayOfWeekIso.FRI.name}", 0)
                .putInt("count_${DayOfWeekIso.SAT.name}", 0)
                .putInt("count_${DayOfWeekIso.SUN.name}", 0)
                .putLong("time_${DayOfWeekIso.MON.name}", 0L)
                .putLong("time_${DayOfWeekIso.TUE.name}", 0L)
                .putLong("time_${DayOfWeekIso.WED.name}", 0L)
                .putLong("time_${DayOfWeekIso.THU.name}", 0L)
                .putLong("time_${DayOfWeekIso.FRI.name}", 0L)
                .putLong("time_${DayOfWeekIso.SAT.name}", 0L)
                .putLong("time_${DayOfWeekIso.SUN.name}", 0L)
                .apply()

            // Trigger reset notification if context is available
            context?.let { ctx ->
                sendSundayMidnightNotification(ctx, prevTotal)
            }
        }
    }

    /**
     * Records elapsed reel viewing milliseconds for today.
     */
    @Synchronized
    fun recordTimeForToday(durationMs: Long, now: Long = System.currentTimeMillis()) {
        if (durationMs <= 0L) return
        val prefs = sharedPreferences ?: return
        checkAndApplyWeekRollover(null, prefs, now)

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val todayIso = DayOfWeekIso.fromCalendarDay(cal.get(Calendar.DAY_OF_WEEK))

        val key = "time_${todayIso.name}"
        val currentMs = prefs.getLong(key, 0L)
        val liveTotalMs = ReelCounterManager.stats.value.todayTotalTimeMs
        val newMs = maxOf(currentMs + durationMs, liveTotalMs)

        prefs.edit().putLong(key, newMs).apply()
        loadSchedule(prefs, now)
    }

    /**
     * Builds and updates the reactive StateFlow containing all 7 days for Compose UI.
     */
    fun loadSchedule(prefs: SharedPreferences, now: Long = System.currentTimeMillis()) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentWeekId = getWeekId(cal)
        val days = getDaysForWeek(cal, prefs)
        val total = days.sumOf { it.count }
        val totalTime = days.sumOf { it.timeSpentMs }
        val activeDaysCount = days.count { it.count > 0 }.coerceAtLeast(1)
        val avg = total / activeDaysCount
        val lastReset = prefs.getLong(KEY_LAST_RESET_TIMESTAMP, 0L)
        val dailyLimit = ReelCounterManager.stats.value.dailyLimit

        _schedule.value = WeeklyReelSchedule(
            weekId = currentWeekId,
            days = days,
            totalWeeklyReels = total,
            totalWeeklyTimeMs = totalTime,
            dailyAverage = avg,
            dailyLimit = dailyLimit,
            lastResetTimestamp = lastReset
        )
    }

    fun getDaysForWeek(cal: Calendar, prefs: SharedPreferences): List<DayReelEntry> {
        val workingCal = cal.clone() as Calendar
        workingCal.firstDayOfWeek = Calendar.MONDAY

        val currentDayOfWeek = workingCal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (currentDayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> currentDayOfWeek - Calendar.MONDAY
        }
        workingCal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

        val result = mutableListOf<DayReelEntry>()
        val isoDays = listOf(
            DayOfWeekIso.MON,
            DayOfWeekIso.TUE,
            DayOfWeekIso.WED,
            DayOfWeekIso.THU,
            DayOfWeekIso.FRI,
            DayOfWeekIso.SAT,
            DayOfWeekIso.SUN
        )

        for (isoDay in isoDays) {
            val dateStr = dateFormat.format(workingCal.time)
            val isToday = workingCal.get(Calendar.YEAR) == todayYear &&
                          workingCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
            var count = prefs.getInt("count_${isoDay.name}", 0)
            var timeMs = prefs.getLong("time_${isoDay.name}", 0L)
            if (isToday) {
                val liveToday = ReelCounterManager.stats.value.todayTotal
                if (liveToday > count) {
                    count = liveToday
                    prefs.edit().putInt("count_${isoDay.name}", count).apply()
                }
                val liveTodayTime = ReelCounterManager.stats.value.todayTotalTimeMs
                if (liveTodayTime > timeMs) {
                    timeMs = liveTodayTime
                    prefs.edit().putLong("time_${isoDay.name}", timeMs).apply()
                }
            }
            result.add(
                DayReelEntry(
                    day = isoDay,
                    count = count,
                    timeSpentMs = timeMs,
                    dateStr = dateStr,
                    isToday = isToday
                )
            )
            workingCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
    }

    fun getWeekId(calendar: Calendar): String {
        val cal = calendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weekly Reel Reports & Reset"
            val descriptionText = "Weekly reel scroll schedule summaries and Sunday midnight reset alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(WEEKLY_NOTIF_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendSundayMidnightNotification(context: Context, totalReels: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "REEL_COUNTER")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WEEKLY_NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("🔄 Weekly Reel Reset Complete")
            .setContentText("You scrolled $totalReels reels this week. The 7-day schedule has been refreshed for a fresh start!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("You scrolled $totalReels reels this week (Mon-Sun). The board has been wiped clean. Aim for fewer reels and protect your focus this week!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(NOTIF_ID_SUNDAY_RESET, notification)
        } catch (_: Exception) {}
    }

    fun sendMondayMorningKickoffNotification(context: Context) {
        val prefs = sharedPreferences ?: return
        val currentWeekId = getWeekId(Calendar.getInstance())
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_MONDAY_WEEK, null)
        if (lastNotified == currentWeekId) return // Already sent for this week

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "REEL_COUNTER")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            102,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WEEKLY_NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌅 New Week, Clean Slate")
            .setContentText("Your 7-day reel counter is ready. Set your daily limit and reclaim your deep work hours today!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Clean slate! Your 7-day reel schedule is refreshed. Stay mindful of your scrolling habits and keep your mind sharp this week."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(NOTIF_ID_MONDAY_KICKOFF, notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_MONDAY_WEEK, currentWeekId).apply()
        } catch (_: Exception) {}
    }

    /**
     * Schedules alarms for Sunday 12:01 AM and Monday 8:00 AM.
     */
    fun scheduleWeeklyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Sunday Midnight (12:01 AM Sunday night / Monday morning rollover)
        val sundayMidnightCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 24) // 12:00 midnight
            set(Calendar.MINUTE, 1)        // 12:01 AM
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 7)
            }
        }

        val sundayIntent = Intent(context, WeeklyReelNotificationReceiver::class.java).apply {
            action = ACTION_SUNDAY_RESET
        }
        val sundayPendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            sundayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                sundayMidnightCal.timeInMillis,
                sundayPendingIntent
            )
        } catch (_: Exception) {}

        // 2. Monday Morning (8:00 AM)
        val mondayMorningCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 7)
            }
        }

        val mondayIntent = Intent(context, WeeklyReelNotificationReceiver::class.java).apply {
            action = ACTION_MONDAY_KICKOFF
        }
        val mondayPendingIntent = PendingIntent.getBroadcast(
            context,
            202,
            mondayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                mondayMorningCal.timeInMillis,
                mondayPendingIntent
            )
        } catch (_: Exception) {}
    }

    /**
     * Testing / Mock utility to set a specific day's count.
     */
    fun setDayCount(day: DayOfWeekIso, count: Int) {
        val prefs = sharedPreferences ?: return
        prefs.edit().putInt("count_${day.name}", count).apply()
        loadSchedule(prefs)
    }

    /**
     * Resets current week schedule to 0.
     */
    fun resetCurrentWeek() {
        val prefs = sharedPreferences ?: return
        prefs.edit()
            .putInt("count_${DayOfWeekIso.MON.name}", 0)
            .putInt("count_${DayOfWeekIso.TUE.name}", 0)
            .putInt("count_${DayOfWeekIso.WED.name}", 0)
            .putInt("count_${DayOfWeekIso.THU.name}", 0)
            .putInt("count_${DayOfWeekIso.FRI.name}", 0)
            .putInt("count_${DayOfWeekIso.SAT.name}", 0)
            .putInt("count_${DayOfWeekIso.SUN.name}", 0)
            .putLong("time_${DayOfWeekIso.MON.name}", 0L)
            .putLong("time_${DayOfWeekIso.TUE.name}", 0L)
            .putLong("time_${DayOfWeekIso.WED.name}", 0L)
            .putLong("time_${DayOfWeekIso.THU.name}", 0L)
            .putLong("time_${DayOfWeekIso.FRI.name}", 0L)
            .putLong("time_${DayOfWeekIso.SAT.name}", 0L)
            .putLong("time_${DayOfWeekIso.SUN.name}", 0L)
            .putLong(KEY_LAST_RESET_TIMESTAMP, System.currentTimeMillis())
            .apply()
        loadSchedule(prefs)
    }
}
