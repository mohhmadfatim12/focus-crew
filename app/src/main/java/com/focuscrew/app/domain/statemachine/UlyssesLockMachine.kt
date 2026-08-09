package com.focuscrew.app.domain.statemachine

/**
 * UlyssesLockMachine — 24-Hour Pre-Commitment Cool-Down State Machine.
 *
 * Implements a behavioral Ulysses contract: in cold states, the user locks the fortress.
 * In hot states (e.g. 2:00 AM urges), deactivation requires a 24-hour waiting period,
 * guaranteeing the temporary dopamine surge has dissipated before settings can change.
 */
data class UlyssesState(
    val isEnabled: Boolean = false,
    val deactivationRequestedAtMs: Long? = null,
    val coolDownDurationMs: Long = DEFAULT_COOLDOWN_MS
) {
    val isPendingDeactivation: Boolean
        get() = isEnabled && deactivationRequestedAtMs != null

    companion object {
        const val DEFAULT_COOLDOWN_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}

class UlyssesLockMachine(initialState: UlyssesState = UlyssesState()) {

    var state: UlyssesState = initialState
        private set

    /**
     * Toggles Ulysses Lock on. Once on, it cannot be toggled off without the 24-hour cooldown.
     */
    fun enable() {
        state = state.copy(isEnabled = true, deactivationRequestedAtMs = null)
    }

    /**
     * Requests deactivation. Starts the 24-hour countdown if not already started.
     */
    fun requestDeactivation(nowMs: Long): UlyssesState {
        if (!state.isEnabled) return state
        if (state.deactivationRequestedAtMs == null) {
            state = state.copy(deactivationRequestedAtMs = nowMs)
        }
        return state
    }

    /**
     * Cancels the deactivation request, keeping the lock active and unbroken.
     */
    fun cancelDeactivation(): UlyssesState {
        if (!state.isEnabled) return state
        state = state.copy(deactivationRequestedAtMs = null)
        return state
    }

    /**
     * Completes deactivation ONLY if the cooldown period has elapsed.
     */
    fun completeDeactivation(nowMs: Long): Boolean {
        if (!state.isEnabled) return true
        val requestedAt = state.deactivationRequestedAtMs ?: return false
        val elapsed = nowMs - requestedAt
        if (elapsed >= state.coolDownDurationMs) {
            state = UlyssesState(isEnabled = false, deactivationRequestedAtMs = null)
            return true
        }
        return false
    }

    /**
     * Remaining milliseconds before deactivation can be completed.
     */
    fun getRemainingCoolDownMs(nowMs: Long): Long {
        if (!state.isEnabled) return 0L
        val requestedAt = state.deactivationRequestedAtMs ?: return 0L
        val remaining = (requestedAt + state.coolDownDurationMs) - nowMs
        return remaining.coerceAtLeast(0L)
    }

    fun isPendingDeactivation(): Boolean {
        return state.isEnabled && state.deactivationRequestedAtMs != null
    }

    fun isEligibleToDeactivate(nowMs: Long): Boolean {
        return isPendingDeactivation() && getRemainingCoolDownMs(nowMs) == 0L
    }
}
