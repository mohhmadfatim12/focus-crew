package com.focuscrew.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WeeklyReelNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        WeeklyReelScheduleManager.initialize(context)

        when (intent.action) {
            WeeklyReelScheduleManager.ACTION_SUNDAY_RESET -> {
                val prefs = context.getSharedPreferences("focuscrew_weekly_reel_schedule", Context.MODE_PRIVATE)
                WeeklyReelScheduleManager.checkAndApplyWeekRollover(context, prefs)
                WeeklyReelScheduleManager.scheduleWeeklyAlarms(context)
            }
            WeeklyReelScheduleManager.ACTION_MONDAY_KICKOFF -> {
                WeeklyReelScheduleManager.sendMondayMorningKickoffNotification(context)
                WeeklyReelScheduleManager.scheduleWeeklyAlarms(context)
            }
        }
    }
}
