package com.focuscrew.app.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.focuscrew.app.service.ReelCounterManager
import java.util.Calendar

/**
 * Authoritative OS-level Screen Time Query Utility.
 * Fetches exact millisecond screen time for Instagram & YouTube directly from
 * Android's hardware/OS-level UsageStatsManager (zero estimates, zero fake numbers).
 */
object UsageStatsHelper {

    private const val TAG = "UsageStatsHelper"
    const val PACKAGE_INSTAGRAM = "com.instagram.android"
    const val PACKAGE_YOUTUBE = "com.google.android.youtube"

    /**
     * Checks if the user has granted Usage Access permission to FocusCrew.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.w(TAG, "Error checking usage stats permission: ${e.message}")
            false
        }
    }

    /**
     * Launches the system Usage Access settings page.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Calculates the start timestamp of today (midnight 00:00:00.000 local time).
     */
    fun getStartOfTodayMs(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Queries the exact foreground screen time for a given package name today from midnight to now.
     * Returns time in milliseconds.
     */
    fun getExactTodayAppTimeMs(context: Context, packageName: String): Long {
        if (!hasUsageAccessPermission(context)) {
            return 0L
        }

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0L

            val startOfToday = getStartOfTodayMs()
            val now = System.currentTimeMillis()

            val statsList: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfToday,
                now
            ) ?: emptyList()

            var totalTimeMs = 0L
            for (stat in statsList) {
                if (stat.packageName == packageName) {
                    val time = stat.totalTimeInForeground
                    if (time > totalTimeMs) {
                        totalTimeMs = time
                    }
                }
            }

            totalTimeMs
        } catch (e: Exception) {
            Log.w(TAG, "Error querying usage stats for $packageName: ${e.message}")
            0L
        }
    }

    /**
     * Synchronizes authoritative OS screen time into ReelCounterManager.
     * Guarantees that time spent shown in the app reflects exact system reality.
     */
    fun syncExactTimesIfPermitted(context: Context) {
        if (!hasUsageAccessPermission(context)) return

        try {
            val igTimeMs = getExactTodayAppTimeMs(context, PACKAGE_INSTAGRAM)
            val ytTimeMs = getExactTodayAppTimeMs(context, PACKAGE_YOUTUBE)

            if (igTimeMs > 0L || ytTimeMs > 0L) {
                ReelCounterManager.syncAuthoritativeTimes(
                    instagramTimeMs = igTimeMs,
                    youtubeTimeMs = ytTimeMs
                )
                Log.d(TAG, "Synchronized exact OS screen time: IG=${igTimeMs}ms, YT=${ytTimeMs}ms")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync exact times: ${e.message}")
        }
    }
}
