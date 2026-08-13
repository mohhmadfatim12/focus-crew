package com.focuscrew.app.service

import android.content.Context
import android.content.SharedPreferences
import com.focuscrew.app.data.model.FreedomVaultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Manager for Freedom Vault (Addiction Recovery Hub).
 * 100% Offline, private, persists in SharedPreferences.
 */
object FreedomVaultManager {

    private const val PREFS_NAME = "focuscrew_freedom_vault"
    private const val KEY_CLEAN_STREAK = "clean_days_streak"
    private const val KEY_BEST_STREAK = "best_streak_days"
    private const val KEY_TOTAL_CLEAN = "total_clean_days"
    private const val KEY_LAST_RELAPSE = "last_relapse_timestamp"
    private const val KEY_URGES_COUNT = "urges_overcome_count"
    private const val KEY_WHY_NOTE = "why_i_started_note"
    private const val KEY_DANGER_ENABLED = "danger_zone_enabled"
    private const val KEY_DANGER_START_H = "danger_zone_start_h"
    private const val KEY_DANGER_START_M = "danger_zone_start_m"
    private const val KEY_DANGER_END_H = "danger_zone_end_h"
    private const val KEY_DANGER_END_M = "danger_zone_end_m"
    private const val KEY_TRIGGER_PKGS = "danger_trigger_packages"
    private const val KEY_ADULT_DNS = "adult_dns_protection"
    private const val KEY_BLOCK_INCOGNITO = "block_incognito"
    private const val KEY_COOLDOWN_ENDS_AT = "anti_impulse_cooldown_ends_at"

    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(FreedomVaultState())
    val state: StateFlow<FreedomVaultState> = _state.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPreferences()
            recalculateStreakIfNeeded()
        }
    }

    private fun loadFromPreferences() {
        val p = prefs ?: return
        val cleanStreak = p.getInt(KEY_CLEAN_STREAK, 0)
        val bestStreak = p.getInt(KEY_BEST_STREAK, 0)
        val totalClean = p.getInt(KEY_TOTAL_CLEAN, 0)
        val lastRelapse = p.getLong(KEY_LAST_RELAPSE, System.currentTimeMillis())
        val urgesCount = p.getInt(KEY_URGES_COUNT, 0)
        val whyNote = p.getString(
            KEY_WHY_NOTE,
            "I am reclaiming my focus, energy, and self-respect. My mind is my sanctuary."
        ) ?: ""
        val dangerEnabled = p.getBoolean(KEY_DANGER_ENABLED, true)
        val startH = p.getInt(KEY_DANGER_START_H, 23)
        val startM = p.getInt(KEY_DANGER_START_M, 0)
        val endH = p.getInt(KEY_DANGER_END_H, 6)
        val endM = p.getInt(KEY_DANGER_END_M, 0)
        val triggerPkgs = p.getStringSet(KEY_TRIGGER_PKGS, FreedomVaultState.defaultTriggerPackages)
            ?: FreedomVaultState.defaultTriggerPackages
        val adultDns = p.getBoolean(KEY_ADULT_DNS, false)
        val blockIncognito = p.getBoolean(KEY_BLOCK_INCOGNITO, true)
        val cooldownEndsAt = p.getLong(KEY_COOLDOWN_ENDS_AT, 0L)
        val isCooldownActive = cooldownEndsAt > System.currentTimeMillis()

        _state.value = FreedomVaultState(
            cleanDaysStreak = cleanStreak,
            bestStreakDays = bestStreak,
            totalCleanDays = totalClean,
            lastRelapseTimestamp = lastRelapse,
            urgesOvercomeCount = urgesCount,
            whyIStartedNote = whyNote,
            dangerZoneEnabled = dangerEnabled,
            dangerZoneStartHour = startH,
            dangerZoneStartMinute = startM,
            dangerZoneEndHour = endH,
            dangerZoneEndMinute = endM,
            dangerZoneTriggerPackages = triggerPkgs,
            adultDnsProtectionEnabled = adultDns,
            blockIncognitoEnabled = blockIncognito,
            antiImpulseCooldownActive = isCooldownActive,
            antiImpulseCooldownEndsAt = cooldownEndsAt
        )
    }

    private fun recalculateStreakIfNeeded() {
        val lastRelapse = _state.value.lastRelapseTimestamp
        if (lastRelapse > 0L) {
            val now = System.currentTimeMillis()
            val diffMs = now - lastRelapse
            val days = (diffMs / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            if (days != _state.value.cleanDaysStreak) {
                val newBest = maxOf(days, _state.value.bestStreakDays)
                _state.value = _state.value.copy(
                    cleanDaysStreak = days,
                    bestStreakDays = newBest
                )
                prefs?.edit()?.putInt(KEY_CLEAN_STREAK, days)?.putInt(KEY_BEST_STREAK, newBest)?.apply()
            }
        }
    }

    fun recordUrgeOvercome() {
        val current = _state.value
        val newUrges = current.urgesOvercomeCount + 1
        _state.value = current.copy(urgesOvercomeCount = newUrges)
        prefs?.edit()?.putInt(KEY_URGES_COUNT, newUrges)?.apply()
    }

    fun saveWhyIStarted(note: String) {
        val trimmed = note.trim()
        _state.value = _state.value.copy(whyIStartedNote = trimmed)
        prefs?.edit()?.putString(KEY_WHY_NOTE, trimmed)?.apply()
    }

    fun setDangerZone(
        enabled: Boolean,
        startHour: Int = _state.value.dangerZoneStartHour,
        startMinute: Int = _state.value.dangerZoneStartMinute,
        endHour: Int = _state.value.dangerZoneEndHour,
        endMinute: Int = _state.value.dangerZoneEndMinute
    ) {
        _state.value = _state.value.copy(
            dangerZoneEnabled = enabled,
            dangerZoneStartHour = startHour,
            dangerZoneStartMinute = startMinute,
            dangerZoneEndHour = endHour,
            dangerZoneEndMinute = endMinute
        )
        prefs?.edit()
            ?.putBoolean(KEY_DANGER_ENABLED, enabled)
            ?.putInt(KEY_DANGER_START_H, startHour)
            ?.putInt(KEY_DANGER_START_M, startMinute)
            ?.putInt(KEY_DANGER_END_H, endHour)
            ?.putInt(KEY_DANGER_END_M, endMinute)
            ?.apply()
    }

    fun setTriggerPackages(packages: Set<String>) {
        _state.value = _state.value.copy(dangerZoneTriggerPackages = packages)
        prefs?.edit()?.putStringSet(KEY_TRIGGER_PKGS, packages)?.apply()
    }

    fun setAdultDnsProtection(enabled: Boolean) {
        _state.value = _state.value.copy(adultDnsProtectionEnabled = enabled)
        prefs?.edit()?.putBoolean(KEY_ADULT_DNS, enabled)?.apply()
    }

    fun setBlockIncognito(enabled: Boolean) {
        _state.value = _state.value.copy(blockIncognitoEnabled = enabled)
        prefs?.edit()?.putBoolean(KEY_BLOCK_INCOGNITO, enabled)?.apply()
    }

    fun startAntiImpulseCooldown() {
        val cooldownEnd = System.currentTimeMillis() + (15 * 60 * 1000L) // 15 mins
        _state.value = _state.value.copy(
            antiImpulseCooldownActive = true,
            antiImpulseCooldownEndsAt = cooldownEnd
        )
        prefs?.edit()?.putLong(KEY_COOLDOWN_ENDS_AT, cooldownEnd)?.apply()
    }

    fun isAntiImpulseActive(): Boolean {
        val endsAt = _state.value.antiImpulseCooldownEndsAt
        val isActive = endsAt > System.currentTimeMillis()
        if (!isActive && _state.value.antiImpulseCooldownActive) {
            _state.value = _state.value.copy(antiImpulseCooldownActive = false)
        }
        return isActive
    }

    /**
     * Compassionate Relapse Reset.
     * Never shames the user. Records lessons and resets current counter while preserving total clean days.
     */
    fun resetStreak() {
        val current = _state.value
        val now = System.currentTimeMillis()
        val totalClean = current.totalCleanDays + current.cleanDaysStreak
        _state.value = current.copy(
            cleanDaysStreak = 0,
            lastRelapseTimestamp = now,
            totalCleanDays = totalClean
        )
        prefs?.edit()
            ?.putInt(KEY_CLEAN_STREAK, 0)
            ?.putLong(KEY_LAST_RELAPSE, now)
            ?.putInt(KEY_TOTAL_CLEAN, totalClean)
            ?.apply()
    }

    /**
     * Checks if current clock time falls inside the Danger Zone curfew.
     * Correctly handles midnight wrap-around (e.g. 23:00 to 06:00).
     */
    fun isCurrentTimeInDangerZone(): Boolean {
        if (!_state.value.dangerZoneEnabled) return false

        val cal = Calendar.getInstance()
        val currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val startMinuteOfDay = _state.value.dangerZoneStartHour * 60 + _state.value.dangerZoneStartMinute
        val endMinuteOfDay = _state.value.dangerZoneEndHour * 60 + _state.value.dangerZoneEndMinute

        return if (startMinuteOfDay <= endMinuteOfDay) {
            // Same day range (e.g. 13:00 to 18:00)
            currentMinuteOfDay in startMinuteOfDay until endMinuteOfDay
        } else {
            // Overnight wrap-around (e.g. 23:00 to 06:00)
            currentMinuteOfDay >= startMinuteOfDay || currentMinuteOfDay < endMinuteOfDay
        }
    }

    /**
     * Checks if a package should be blocked by Freedom Vault right now.
     */
    fun shouldInterceptPackage(packageName: String): Boolean {
        if (isCurrentTimeInDangerZone()) {
            if (_state.value.dangerZoneTriggerPackages.contains(packageName)) {
                return true
            }
        }
        return false
    }
}
