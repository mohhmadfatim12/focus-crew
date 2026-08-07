package com.focuscrew.app.data.model

enum class DayOfWeekIso(val shortName: String, val fullName: String, val isoValue: Int) {
    MON("MON", "Monday", 1),
    TUE("TUE", "Tuesday", 2),
    WED("WED", "Wednesday", 3),
    THU("THU", "Thursday", 4),
    FRI("FRI", "Friday", 5),
    SAT("SAT", "Saturday", 6),
    SUN("SUN", "Sunday", 7);

    companion object {
        fun fromIso(value: Int): DayOfWeekIso {
            return entries.firstOrNull { it.isoValue == value } ?: MON
        }

        fun fromCalendarDay(calendarDay: Int): DayOfWeekIso {
            // java.util.Calendar: SUNDAY=1, MONDAY=2, ... SATURDAY=7
            return when (calendarDay) {
                java.util.Calendar.MONDAY -> MON
                java.util.Calendar.TUESDAY -> TUE
                java.util.Calendar.WEDNESDAY -> WED
                java.util.Calendar.THURSDAY -> THU
                java.util.Calendar.FRIDAY -> FRI
                java.util.Calendar.SATURDAY -> SAT
                java.util.Calendar.SUNDAY -> SUN
                else -> MON
            }
        }
    }
}

data class DayReelEntry(
    val day: DayOfWeekIso,
    val count: Int,
    val timeSpentMs: Long = 0L,
    val dateStr: String = "",
    val isToday: Boolean = false
) {
    val formattedDuration: String
        get() = ReelStats.formatDuration(timeSpentMs)
}

data class WeeklyReelSchedule(
    val weekId: String = "",
    val days: List<DayReelEntry> = emptyList(),
    val totalWeeklyReels: Int = 0,
    val totalWeeklyTimeMs: Long = 0L,
    val dailyAverage: Int = 0,
    val dailyLimit: Int = 50,
    val lastResetTimestamp: Long = 0L
) {
    val formattedTotalDuration: String
        get() = ReelStats.formatDuration(totalWeeklyTimeMs)

    val peakDay: DayReelEntry?
        get() = days.maxByOrNull { it.count }
}

