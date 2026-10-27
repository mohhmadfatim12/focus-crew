package com.focuscrew.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focuscrew.app.R
import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskAlarmAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_TASK_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TaskAlarmAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FocusDatabase.getInstance(context)
                val activeTasks = db.taskAlarmDao().getActivePendingTasks()
                val nextTask = activeTasks.minByOrNull { it.scheduledTimeMillis }

                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                val todayYear = cal.get(Calendar.YEAR)
                val todayDay = cal.get(Calendar.DAY_OF_YEAR)

                val todayTasks = activeTasks.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.scheduledTimeMillis }
                    c.get(Calendar.YEAR) == todayYear && c.get(Calendar.DAY_OF_YEAR) == todayDay
                }

                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_task_alarm)

                    // Open TaskAlarm section in MainActivity
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "TASK_ALARM")
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_task_alarm_root, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_btn_add, pendingIntent)

                    if (nextTask != null) {
                        views.setTextViewText(R.id.widget_task_title, nextTask.title)
                        val formattedTime = timeFormat.format(Date(nextTask.scheduledTimeMillis))
                        views.setTextViewText(R.id.widget_task_time, "⏰ $formattedTime • ${nextTask.category}")
                    } else {
                        views.setTextViewText(R.id.widget_task_title, "No pending tasks")
                        views.setTextViewText(R.id.widget_task_time, "Tap + Add to set alarm")
                    }

                    views.setTextViewText(R.id.widget_today_count, "Today: ${todayTasks.size}")

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_TASK_WIDGET = "com.focuscrew.app.action.UPDATE_TASK_WIDGET"

        fun notifyUpdate(context: Context) {
            val intent = Intent(context, TaskAlarmAppWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_TASK_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
