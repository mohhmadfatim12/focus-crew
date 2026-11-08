package com.focuscrew.app.service.taskalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.data.repository.TaskAlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("TaskBootReceiver", "Boot completed / package replaced. Rescheduling active task alarms...")

            // Auto-start SafeSearch VIP Shield on device boot if permitted
            try {
                com.focuscrew.app.service.FocusCrewVpnService.initialize(context)
                com.focuscrew.app.service.FocusCrewVpnService.autoStartIfPermitted(context)
            } catch (e: Exception) {
                Log.w("TaskBootReceiver", "Failed to auto-start VPN on boot: ${e.message}")
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FocusDatabase.getInstance(context)
                    val repository = TaskAlarmRepository(db.taskAlarmDao(), context)
                    repository.rescheduleAllActiveAlarms()
                } catch (e: Exception) {
                    Log.e("TaskBootReceiver", "Failed to reschedule active alarms: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
