package com.focuscrew.app.data.model

/**
 * State model for Freedom Vault (Addiction Recovery & Calm Shield).
 * 100% Offline, private, persisted locally in SharedPreferences.
 */
data class FreedomVaultState(
    val cleanDaysStreak: Int = 0,
    val bestStreakDays: Int = 0,
    val totalCleanDays: Int = 0,
    val lastRelapseTimestamp: Long = 0L,
    val urgesOvercomeCount: Int = 0,
    val whyIStartedNote: String = "I am reclaiming my focus, energy, and self-respect. My mind is my sanctuary.",
    
    // Danger Zone Curfew (Bedtime Protection)
    val dangerZoneEnabled: Boolean = true,
    val dangerZoneStartHour: Int = 23,   // 11:00 PM
    val dangerZoneStartMinute: Int = 0,
    val dangerZoneEndHour: Int = 6,      // 06:00 AM
    val dangerZoneEndMinute: Int = 0,
    val dangerZoneTriggerPackages: Set<String> = defaultTriggerPackages,

    // Adult Content & Incognito Protection
    val adultDnsProtectionEnabled: Boolean = false,
    val blockIncognitoEnabled: Boolean = true,

    // 15-Minute Anti-Impulse Delay
    val antiImpulseCooldownActive: Boolean = false,
    val antiImpulseCooldownEndsAt: Long = 0L
) {
    companion object {
        val defaultTriggerPackages: Set<String> = setOf(
            "com.android.chrome",
            "com.google.android.youtube",
            "com.instagram.android",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.brave.browser",
            "com.sec.android.app.sbrowser", // Samsung Internet
            "com.microsoft.emmx",           // Microsoft Edge
            "com.reddit.frontpage",
            "com.twitter.android"
        )
    }

    /**
     * Neuro-recovery milestone stage based on clean days.
     */
    fun getRecoveryMilestone(): Pair<String, String> {
        return when {
            cleanDaysStreak < 3 -> "Phase 1: Breaking the Fog" to "Your brain is adapting to lower dopamine spikes. Physical restless energy is completely normal."
            cleanDaysStreak < 7 -> "Phase 2: Receptor Reset" to "Dopamine D2 receptor upregulation begins. Sleep architecture and morning mental clarity improve."
            cleanDaysStreak < 14 -> "Phase 3: Cognitive Surge" to "Prefrontal cortex gray matter density begins recovering. Concentration and memory sharpen."
            cleanDaysStreak < 30 -> "Phase 4: Emotional Balance" to "Natural baseline dopamine restores. Joy from simple daily activities returns."
            cleanDaysStreak < 90 -> "Phase 5: Neural Rewiring" to "Old compulsive distraction pathways are undergoing long-term synaptic depression (fading away)."
            else -> "Phase 6: Mastery & Freedom" to "You have built new synaptic pathways. You are in complete control of your mind."
        }
    }
}
