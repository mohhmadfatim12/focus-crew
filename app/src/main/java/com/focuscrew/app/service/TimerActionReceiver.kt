package com.focuscrew.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuscrew.app.ui.timer.TimerManager

/**
 * Handles quick Pause and Resume broadcast actions fired from the Timer ongoing notification.
 */
class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            TimerNotificationManager.ACTION_PAUSE -> {
                TimerManager.pause()
            }
            TimerNotificationManager.ACTION_RESUME -> {
                TimerManager.resume()
            }
        }
    }
}
