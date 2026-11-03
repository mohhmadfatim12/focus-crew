package com.focuscrew.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focuscrew.app.domain.model.TimerSnapshot
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.ui.MainActivity

/**
 * Manages the ongoing, persistent notification for the active focus timer.
 * Displays live countdown, strict focus status, and quick Pause/Resume actions.
 */
object TimerNotificationManager {

    const val CHANNEL_ID = "focuscrew_timer_live_session"
    const val NOTIFICATION_ID = 2002

    const val ACTION_PAUSE = "com.focuscrew.app.action.TIMER_PAUSE"
    const val ACTION_RESUME = "com.focuscrew.app.action.TIMER_RESUME"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Focus Timer Live Session"
            val descriptionText = "Displays live countdown and controls while a focus session is active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun updateNotification(context: Context, snapshot: TimerSnapshot, subject: String = "Deep Focus") {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        when (val status = snapshot.status) {
            is TimerStatus.Running -> {
                createNotificationChannel(context)

                val remainingFormatted = formatTime(snapshot.remainingMs)
                val contentTitle = "⏱️ Focus Session: $subject"
                val contentText = "$remainingFormatted remaining • Strict Focus Armed"

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigate_to", "TIMER")
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    10,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val pauseIntent = Intent(context, TimerActionReceiver::class.java).apply {
                    action = ACTION_PAUSE
                }
                val pausePendingIntent = PendingIntent.getBroadcast(
                    context,
                    11,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSubText("FocusCrew Timer")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
                    .build()

                try {
                    manager.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {}
            }

            is TimerStatus.Paused -> {
                createNotificationChannel(context)

                val remainingFormatted = formatTime(snapshot.remainingMs)
                val contentTitle = "⏸️ Focus Paused: $subject"
                val contentText = "$remainingFormatted remaining • Apps Remain Blocked"

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigate_to", "TIMER")
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    10,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val resumeIntent = Intent(context, TimerActionReceiver::class.java).apply {
                    action = ACTION_RESUME
                }
                val resumePendingIntent = PendingIntent.getBroadcast(
                    context,
                    12,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_pause)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSubText("Focus Paused")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
                    .build()

                try {
                    manager.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {}
            }

            else -> {
                cancelNotification(context)
            }
        }
    }

    fun notifySessionCompleted(context: Context, subject: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        cancelNotification(context)

        val completionChannelId = "focuscrew_timer_completion_v2"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                completionChannelId,
                "Focus Session Completed",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud chime and alert when a focus session finishes"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "TIMER")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            2003,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, completionChannelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🎉 Focus Session Completed!")
            .setContentText("You completed \"$subject\". Discipline maintained!")
            .setSubText("Streak Protected")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            manager.notify(NOTIFICATION_ID + 1, notification)
        } catch (_: Exception) {}

        // Also play completion chime through audio manager & ringtone directly
        playCompletionChime(context)
    }

    fun playCompletionChime(context: Context) {
        try {
            val chimeUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            if (chimeUri != null) {
                val ringtone = android.media.RingtoneManager.getRingtone(context.applicationContext, chimeUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone?.audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone?.play()
            }
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
            }
        } catch (_: Exception) {}
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }

    private fun formatTime(millis: Long): String {
        val totalSec = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
