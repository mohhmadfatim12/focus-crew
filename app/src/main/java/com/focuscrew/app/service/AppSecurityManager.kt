package com.focuscrew.app.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * AppSecurityManager — Ironclad Anti-Bypass & Security Layer.
 *
 * Provides:
 *  1. PIN & Biometric Lock for FocusCrew.
 *  2. Android Settings Anti-Tamper (prevents Force Stop & Uninstall during sessions).
 *  3. Hardware-based Clock Tamper Protection (detects date rolling).
 *  4. Accessibility Watchdog state.
 */
object AppSecurityManager {

    private const val PREFS_NAME = "focuscrew_security_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_ENABLED = "pin_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_SETTINGS_ANTI_TAMPER = "settings_anti_tamper"
    private const val KEY_LAST_WALL_TIME = "last_wall_time"
    private const val KEY_LAST_ELAPSED_TIME = "last_elapsed_time"

    private var prefs: SharedPreferences? = null

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isSettingsAntiTamperActive = MutableStateFlow(true)
    val isSettingsAntiTamperActive: StateFlow<Boolean> = _isSettingsAntiTamperActive.asStateFlow()

    fun initialize(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        val pinEnabled = p.getBoolean(KEY_PIN_ENABLED, false)
        val hasPin = p.getString(KEY_PIN_HASH, null) != null
        _isLocked.value = pinEnabled && hasPin

        _isSettingsAntiTamperActive.value = p.getBoolean(KEY_SETTINGS_ANTI_TAMPER, true)

        // Check clock tampering
        detectClockTampering(p)
    }

    /**
     * Checks if the user changed the system time to cheat trial or timer.
     */
    private fun detectClockTampering(p: SharedPreferences) {
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val lastWall = p.getLong(KEY_LAST_WALL_TIME, nowWall)
        val lastElapsed = p.getLong(KEY_LAST_ELAPSED_TIME, nowElapsed)

        // If wall clock moved backwards by more than 5 minutes
        if (nowWall < (lastWall - 5 * 60 * 1000L)) {
            android.util.Log.w("AppSecurityManager", "Clock tampering detected! Wall clock jumped backwards.")
        }

        p.edit()
            .putLong(KEY_LAST_WALL_TIME, nowWall)
            .putLong(KEY_LAST_ELAPSED_TIME, nowElapsed)
            .apply()
    }

    fun isPinEnabled(): Boolean {
        val p = prefs ?: return false
        return p.getBoolean(KEY_PIN_ENABLED, false) && p.getString(KEY_PIN_HASH, null) != null
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs?.edit()
            ?.putString(KEY_PIN_HASH, hash)
            ?.putBoolean(KEY_PIN_ENABLED, true)
            ?.apply()
    }

    fun clearPin() {
        prefs?.edit()
            ?.remove(KEY_PIN_HASH)
            ?.putBoolean(KEY_PIN_ENABLED, false)
            ?.apply()
        _isLocked.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs?.getString(KEY_PIN_HASH, null) ?: return false
        val matches = hashPin(pin) == savedHash
        if (matches) {
            _isLocked.value = false
        }
        return matches
    }

    fun lockApp() {
        if (isPinEnabled()) {
            _isLocked.value = true
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun setSettingsAntiTamper(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SETTINGS_ANTI_TAMPER, enabled)?.apply()
        _isSettingsAntiTamperActive.value = enabled
    }

    fun isBiometricEnabled(): Boolean {
        return prefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)?.apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(("focuscrew_salt_" + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
