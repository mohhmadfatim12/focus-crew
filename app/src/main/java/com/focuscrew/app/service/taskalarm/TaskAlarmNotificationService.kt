package com.focuscrew.app.service.taskalarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focuscrew.app.ui.MainActivity

/**
 * Generic Notification & Alarm Service
 * Architecture requirement:
 * Strictly decoupled from domain/task entities. Accepts only primitive arguments:
 * id, triggerAtMillis, title, body, isExact, payload.
 */
object TaskAlarmNotificationService {

    private const val TAG = "TaskAlarmNotifService"
    const val CHANNEL_ID = "task_alarm_alerts_v2"
    private const val CHANNEL_NAME = "Task & Routine Alarms"
    private const val CHANNEL_DESC = "Rings and alerts for scheduled productivity tasks"

    const val ACTION_COMPLETE_TASK = "com.focuscrew.app.action.COMPLETE_TASK"
    const val ACTION_SNOOZE_TASK = "com.focuscrew.app.action.SNOOZE_TASK"
    const val ACTION_ALARM_TRIGGER = "com.focuscrew.app.action.ALARM_TRIGGER"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_BODY = "extra_task_body"
    const val EXTRA_TASK_PAYLOAD = "extra_task_payload"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Generic alarm scheduling primitive.
     */
    fun scheduleAlarm(
        context: Context,
        id: String,
        triggerAtMillis: Long,
        title: String,
        body: String,
        isExact: Boolean = true,
        payload: String? = null
    ) {
        initNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_TASK_ID, id)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_BODY, body)
            putExtra(EXTRA_TASK_PAYLOAD, payload)
        }

        val requestCode = id.hashCode()
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, pendingIntentFlags)

        try {
            if (isExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        val showIntent = Intent(context, MainActivity::class.java).apply {
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("navigate_to", "TASK_ALARM")
                            putExtra("task_id", id)
                        }
                        val showPendingIntent = PendingIntent.getActivity(
                            context,
                            requestCode + 100000,
                            showIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                            pendingIntent
                        )
                        Log.d(TAG, "Exact AlarmClock scheduled for task $id at $triggerAtMillis")
                    } else {
                        // Fallback when exact alarm permission is denied
                        Log.w(TAG, "SCHEDULE_EXACT_ALARM not granted. Falling back to setAndAllowWhileIdle")
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    Log.d(TAG, "setExactAndAllowWhileIdle scheduled for task $id at $triggerAtMillis")
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.d(TAG, "Inexact alarm scheduled for task $id at $triggerAtMillis")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm for $id: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (inner: Exception) {
                Log.e(TAG, "Fallback alarm schedule also failed: ${inner.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
        }
    }

    /**
     * Generic alarm cancellation primitive.
     */
    fun cancelAlarm(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val requestCode = id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm cancelled for task $id")
        }
        dismissNotification(context, id)
    }

    /**
     * Show notification with rich action buttons (Done, Snooze 10m).
     */
    fun showNotification(
        context: Context,
        id: String,
        title: String,
        body: String,
        payload: String? = null
    ) {
        initNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap content intent -> Opens TaskAlarm section in MainActivity
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "TASK_ALARM")
            putExtra("task_id", id)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Done
        val doneIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10 mins
        val snoozeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, id)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_BODY, body)
            putExtra(EXTRA_TASK_PAYLOAD, payload)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 30 mins
        val snooze30Intent = Intent(context, TaskActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, id)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_BODY, body)
            putExtra(EXTRA_TASK_PAYLOAD, payload)
            putExtra("extra_snooze_minutes", 30)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 3,
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full Screen Intent for lock screen alarm activity
        val fullScreenIntent = Intent(context, com.focuscrew.app.ui.taskalarm.TaskAlarmFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, id)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_BODY, body)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            id.hashCode() + 10,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.checkbox_on_background, "✓ Done", donePendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "⏰ 10m", snoozePendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "⏰ 30m", snooze30PendingIntent)
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }

    fun dismissNotification(context: Context, id: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(id.hashCode())
    }
}
