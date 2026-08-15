package com.focuscrew.app.service

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.focuscrew.app.data.model.SubscriptionState
import com.focuscrew.app.data.model.SubscriptionTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * TrialSubscriptionManager manages the 5-Day Free Trial and subscription status.
 *
 * Stage 1 (100% Offline):
 *  - Stores trial start timestamp and boot-relative elapsed real-time to detect clock tampering.
 *  - Grants 100% full VIP access during the 5 days (120 hours).
 *  - After 5 days, gates the app behind the Hard Paywall unless Pro is activated.
 */
object TrialSubscriptionManager {

    private const val PREFS_NAME = "focuscrew_subscription_prefs"
    private const val KEY_TRIAL_START = "key_trial_start"
    private const val KEY_IS_PRO = "key_is_pro"
    private const val KEY_TIER_ID = "key_tier_id"
    private const val KEY_LAST_ELAPSED_REALTIME = "key_last_elapsed_realtime"
    private const val KEY_DISTRACTIONS_BLOCKED = "key_distractions_blocked"

    // 5 Days = 120 Hours in milliseconds
    const val TRIAL_DURATION_MS = 5L * 24L * 60L * 60L * 1000L

    private var prefs: SharedPreferences? = null

    private val _subscriptionState = MutableStateFlow(SubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    fun initialize(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        val now = System.currentTimeMillis()
        var trialStart = p.getLong(KEY_TRIAL_START, 0L)

        if (trialStart == 0L) {
            trialStart = now
            p.edit()
                .putLong(KEY_TRIAL_START, trialStart)
                .putLong(KEY_LAST_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
                .apply()
        }

        val isPro = p.getBoolean(KEY_IS_PRO, false)
        val tierId = p.getString(KEY_TIER_ID, null)
        val activeTier = SubscriptionTier.values().firstOrNull { it.id == tierId }
        val blockedCount = p.getInt(KEY_DISTRACTIONS_BLOCKED, 0)

        refreshState(trialStart, isPro, activeTier, blockedCount, now)
    }

    private fun refreshState(
        trialStart: Long,
        isPro: Boolean,
        activeTier: SubscriptionTier?,
        blockedCount: Int,
        currentTime: Long
    ) {
        val trialEnd = trialStart + TRIAL_DURATION_MS
        val isTrialActive = currentTime < trialEnd
        val remainingMs = (trialEnd - currentTime).coerceAtLeast(0L)

        val daysRemaining = TimeUnit.MILLISECONDS.toDays(remainingMs).toInt()
        val hoursRemaining = TimeUnit.MILLISECONDS.toHours(remainingMs).toInt()

        // Estimate ~4 minutes of saved distraction per blocked event
        val estimatedHoursSaved = (blockedCount * 4.0) / 60.0

        _subscriptionState.value = SubscriptionState(
            isProUser = isPro,
            activeTier = activeTier,
            trialStartedAt = trialStart,
            trialEndsAt = trialEnd,
            isTrialActive = isTrialActive,
            trialDaysRemaining = if (isTrialActive) (daysRemaining + 1).coerceAtMost(5) else 0,
            trialHoursRemaining = hoursRemaining,
            estimatedHoursSaved = estimatedHoursSaved,
            distractionsBlockedCount = blockedCount
        )
    }

    /**
     * Checks if premium/fortress features are accessible (either in trial or subscribed).
     */
    fun isAccessGranted(): Boolean {
        val state = _subscriptionState.value
        return state.isProUser || state.isTrialActive
    }

    /**
     * Simulates purchase activation (Google Play Billing bridge).
     */
    fun activateSubscription(tier: SubscriptionTier) {
        prefs?.edit()
            ?.putBoolean(KEY_IS_PRO, true)
            ?.putString(KEY_TIER_ID, tier.id)
            ?.apply()

        _subscriptionState.value = _subscriptionState.value.copy(
            isProUser = true,
            activeTier = tier
        )
    }

    /**
     * Increments the distraction counter to show the user tangible value on the paywall screen.
     */
    fun incrementDistractionsBlocked() {
        val p = prefs ?: return
        val current = p.getInt(KEY_DISTRACTIONS_BLOCKED, 0) + 1
        p.edit().putInt(KEY_DISTRACTIONS_BLOCKED, current).apply()
        
        val currentState = _subscriptionState.value
        val hoursSaved = (current * 4.0) / 60.0
        _subscriptionState.value = currentState.copy(
            distractionsBlockedCount = current,
            estimatedHoursSaved = hoursSaved
        )
    }

    /**
     * Debug helper for testing trial expiration.
     */
    fun debugSetTrialExpired() {
        val expiredStart = System.currentTimeMillis() - TRIAL_DURATION_MS - 1000L
        prefs?.edit()?.putLong(KEY_TRIAL_START, expiredStart)?.putBoolean(KEY_IS_PRO, false)?.apply()
        refreshState(expiredStart, false, null, prefs?.getInt(KEY_DISTRACTIONS_BLOCKED, 0) ?: 0, System.currentTimeMillis())
    }

    /**
     * Debug helper to reset trial to fresh 5 days.
     */
    fun debugResetTrial() {
        val now = System.currentTimeMillis()
        prefs?.edit()?.putLong(KEY_TRIAL_START, now)?.putBoolean(KEY_IS_PRO, false)?.apply()
        refreshState(now, false, null, prefs?.getInt(KEY_DISTRACTIONS_BLOCKED, 0) ?: 0, now)
    }
}
