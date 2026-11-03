package com.focuscrew.app.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

/**
 * Android 13+ (API 33+) Restricted Settings & Sideloaded APK Permission Helper.
 *
 * When an APK is downloaded directly (sideloaded) outside of Google Play or ADB:
 * Android 13, 14, 15, and 16 automatically enforce "Restricted setting" on Accessibility
 * Services. Tapping the service in system settings shows:
 * "Restricted setting - For your security, this setting is currently unavailable."
 *
 * This helper provides 1-tap navigation to:
 * 1. App Info (where the user taps ⋮ -> "Allow restricted settings" with PIN/fingerprint)
 * 2. Accessibility Settings (to turn the blocker switch ON)
 * 3. Battery Optimization Exclusion (preventing OEM killers from stopping the service)
 */
object RestrictedSettingsHelper {

    private const val TAG = "RestrictedSettings"

    /**
     * Checks if BlockerAccessibilityService is currently enabled and active in Android Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val expectedServiceName = "${context.packageName}/com.focuscrew.app.service.BlockerAccessibilityService"
            val simpleServiceName = "${context.packageName}/.service.BlockerAccessibilityService"

            for (service in enabledServices) {
                val id = service.id
                if (id.equals(expectedServiceName, ignoreCase = true) ||
                    id.equals(simpleServiceName, ignoreCase = true) ||
                    id.contains("BlockerAccessibilityService", ignoreCase = true)
                ) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking accessibility service status: ${e.message}")
            false
        }
    }

    /**
     * Opens the FocusCrew App Info page in Android Settings so the user can tap
     * the top-right 3-dots (⋮) and choose "Allow restricted settings".
     */
    fun openAppInfoForRestrictedSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open App Details settings: ${e.message}")
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Opens the system Accessibility settings list.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
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
     * Checks if battery optimizations are currently ignored for FocusCrew.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    /**
     * Requests exclusion from battery optimization so the blocker runs 24/7
     * even when the phone is unplugged and IDE is disconnected.
     */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not launch battery optimization intent: ${e.message}")
            }
        }
    }
}
