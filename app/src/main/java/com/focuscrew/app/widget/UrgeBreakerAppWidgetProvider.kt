package com.focuscrew.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focuscrew.app.R
import com.focuscrew.app.service.FreedomVaultManager
import com.focuscrew.app.ui.MainActivity

/**
 * UrgeBreakerAppWidgetProvider — 1-Tap Home Screen Emergency SOS Widget.
 *
 * Allows the user to trigger the 3-Round Urge Destroyer immediately with 1 tap
 * directly from their Android Home Screen without needing to open or navigate the app.
 */
class UrgeBreakerAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        FreedomVaultManager.initialize(context)
        val cleanDays = FreedomVaultManager.state.value.cleanDaysStreak

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_urge_breaker)

            val streakText = if (cleanDays == 1) "Day 1 Clean" else "Day $cleanDays Clean 🔥"
            views.setTextViewText(R.id.widget_streak, streakText)

            // Intent to launch MainActivity and open the Urge Breaker dialog immediately
            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_LAUNCH_URGE_BREAKER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.btn_emergency_sos, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        const val ACTION_LAUNCH_URGE_BREAKER = "com.focuscrew.app.ACTION_LAUNCH_URGE_BREAKER"
    }
}
