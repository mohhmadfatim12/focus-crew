package com.focuscrew.app.service

import android.content.Context
import android.content.SharedPreferences
import com.focuscrew.app.domain.statemachine.UlyssesLockMachine
import com.focuscrew.app.domain.statemachine.UlyssesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UlyssesLockManager — Manages persistent state for the 24-Hour Cool-Down Lock.
 */
object UlyssesLockManager {

    private const val PREFS_NAME = "focuscrew_ulysses_lock"
    private const val KEY_IS_ENABLED = "ulysses_is_enabled"
    private const val KEY_REQUESTED_AT = "ulysses_requested_at"

    private var prefs: SharedPreferences? = null
    private val machine = UlyssesLockMachine()

    private val _state = MutableStateFlow(UlyssesState())
    val state: StateFlow<UlyssesState> = _state.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs?.getBoolean(KEY_IS_ENABLED, false) ?: false
            val requestedAt = prefs?.getLong(KEY_REQUESTED_AT, -1L)?.takeIf { it > 0 }

            val initial = UlyssesState(
                isEnabled = isEnabled,
                deactivationRequestedAtMs = requestedAt
            )
            machine.requestDeactivation(0) // sync structure
            if (isEnabled) machine.enable()
            if (requestedAt != null) machine.requestDeactivation(requestedAt)

            _state.value = machine.state
        }
    }

    fun enable() {
        machine.enable()
        persist()
    }

    fun requestDeactivation(nowMs: Long = System.currentTimeMillis()) {
        machine.requestDeactivation(nowMs)
        persist()
    }

    fun cancelDeactivation() {
        machine.cancelDeactivation()
        persist()
    }

    fun completeDeactivation(nowMs: Long = System.currentTimeMillis()): Boolean {
        val success = machine.completeDeactivation(nowMs)
        if (success) persist()
        return success
    }

    fun getRemainingCoolDownMs(nowMs: Long = System.currentTimeMillis()): Long {
        return machine.getRemainingCoolDownMs(nowMs)
    }

    fun isPendingDeactivation(): Boolean {
        return machine.isPendingDeactivation()
    }

    fun isEligibleToDeactivate(nowMs: Long = System.currentTimeMillis()): Boolean {
        return machine.isEligibleToDeactivate(nowMs)
    }

    private fun persist() {
        _state.value = machine.state
        prefs?.edit()
            ?.putBoolean(KEY_IS_ENABLED, machine.state.isEnabled)
            ?.putLong(KEY_REQUESTED_AT, machine.state.deactivationRequestedAtMs ?: -1L)
            ?.apply()
    }
}
