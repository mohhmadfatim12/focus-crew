package com.focuscrew.app.service

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * SystemPrivateDnsManager — Manages Android's native system-level Private DNS settings.
 *
 * Allows 1-Click activation of adult-filter-dns.cleanbrowsing.org directly inside
 * Android Settings (Settings.Global.PRIVATE_DNS_MODE and PRIVATE_DNS_SPECIFIER).
 */
object SystemPrivateDnsManager {
    private const val TAG = "SystemPrivateDns"
    const val ADULT_FILTER_DNS_HOST = "adult-filter-dns.cleanbrowsing.org"

    /**
     * Checks whether Android's system-level Private DNS is currently set to the adult filter.
     */
    fun isAdultPrivateDnsActive(context: Context): Boolean {
        return try {
            val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
            val specifier = Settings.Global.getString(context.contentResolver, "private_dns_specifier")
            mode == "hostname" && (specifier == ADULT_FILTER_DNS_HOST || specifier?.contains("cleanbrowsing") == true)
        } catch (e: Exception) {
            Log.w(TAG, "Error reading private_dns: ${e.message}")
            false
        }
    }

    /**
     * Reads current DNS hostname configured in Android Settings.
     */
    fun getCurrentSpecifier(context: Context): String? {
        return try {
            Settings.Global.getString(context.contentResolver, "private_dns_specifier")
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 1-Click System DNS Enabler:
     * Programmatically writes 'adult-filter-dns.cleanbrowsing.org' and 'hostname'
     * directly into Android's Global system settings.
     *
     * IMPORTANT: In AOSP/Android Settings, 'private_dns_specifier' MUST be written FIRST
     * before 'private_dns_mode = "hostname"'. If mode is written first, ConnectivityService
     * evaluates the empty/previous specifier and falls back to opportunistic (Automatic).
     */
    fun setAdultPrivateDnsEnabled(context: Context, enabled: Boolean): Boolean {
        return try {
            if (enabled) {
                // 1. SPECIFIER FIRST (crucial for Android ConnectivityService validation)
                Settings.Global.putString(context.contentResolver, "private_dns_specifier", ADULT_FILTER_DNS_HOST)
                // 2. MODE SECOND (triggers network reload with the new specifier active)
                Settings.Global.putString(context.contentResolver, "private_dns_mode", "hostname")
                Log.d(TAG, "Successfully set Android system Private DNS to $ADULT_FILTER_DNS_HOST")
                true
            } else {
                Settings.Global.putString(context.contentResolver, "private_dns_mode", "opportunistic")
                Settings.Global.putString(context.contentResolver, "private_dns_specifier", "")
                Log.d(TAG, "Reset Android system Private DNS to Automatic (opportunistic)")
                true
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Private DNS: ${e.message}")
            false
        }
    }

    /**
     * Opens Android's native Network & Internet settings page so the user
     * can verify the Private DNS provider hostname with their own eyes.
     */
    fun openNetworkSettings(context: Context) {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (err: Exception) {
                Log.w(TAG, "Could not open settings: ${err.message}")
            }
        }
    }
}
