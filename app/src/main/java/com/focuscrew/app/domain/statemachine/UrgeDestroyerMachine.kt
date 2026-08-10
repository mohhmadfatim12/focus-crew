package com.focuscrew.app.domain.statemachine

enum class UrgeRound {
    ROUND_1_BIOLOGICAL_RESET,
    ROUND_2_MENTAL_MIRAGE,
    ROUND_3_NUCLEAR_LOCKDOWN,
    VICTORY_REWARD
}

data class UrgeDestroyerState(
    val currentRound: UrgeRound = UrgeRound.ROUND_1_BIOLOGICAL_RESET,
    val round1CompletedBreathCycles: Int = 0,
    val round1GroundingDone: Set<Int> = emptySet(),
    val round2SelectedHalt: String? = null,
    val round2TypedOath: String = "",
    val round2OathMatched: Boolean = false,
    val round3LockdownSecondsLeft: Int = 45,
    val round3LockdownComplete: Boolean = false,
    val victoryComplimentIndex: Int = 0,
    val totalUrgesDefeated: Int = 0
) {
    companion object {
        const val REQUIRED_OATH = "I will not trade my real life for pixels on glass"
    }
}

/**
 * Pure Kotlin state machine governing the 3-Round Escalation Urge Destroyer:
 * - Round 1: Biological Reset (4-4-4-4 Box Breathing, Somatic Grounding, Dopamine Wave Clock)
 * - Round 2: Mental Mirage Shatterer (H.A.L.T. Trigger Analysis, 10s vs 24h Reality Split, Cognitive Typing Oath)
 * - Round 3: Nuclear Truth & Lockdown (Post-Nut Clarity Simulation, Raw De-glamorization, Personal Anchor, Unskippable 45s Stare-Down)
 * - Victory: Psychological Praise, Healthy Dopamine Recovery Compliments, and Armor Badges.
 */
class UrgeDestroyerMachine(
    val initialLockdownSeconds: Int = 45
) {
    var state: UrgeDestroyerState = UrgeDestroyerState(
        round3LockdownSecondsLeft = initialLockdownSeconds,
        round3LockdownComplete = (initialLockdownSeconds <= 0)
    )
        private set

    fun recordBreathCycleCompleted() {
        state = state.copy(round1CompletedBreathCycles = state.round1CompletedBreathCycles + 1)
    }

    fun toggleGroundingItem(index: Int) {
        val current = state.round1GroundingDone
        val updated = if (current.contains(index)) current - index else current + index
        state = state.copy(round1GroundingDone = updated)
    }

    fun advanceToRound2(): Boolean {
        if (state.currentRound == UrgeRound.ROUND_1_BIOLOGICAL_RESET) {
            state = state.copy(currentRound = UrgeRound.ROUND_2_MENTAL_MIRAGE)
            return true
        }
        return false
    }

    fun selectHaltTrigger(trigger: String) {
        state = state.copy(round2SelectedHalt = trigger)
    }

    fun updateRound2Oath(input: String) {
        val matches = input.trim().equals(UrgeDestroyerState.REQUIRED_OATH, ignoreCase = true)
        state = state.copy(
            round2TypedOath = input,
            round2OathMatched = matches
        )
    }

    fun advanceToRound3(): Boolean {
        if (state.currentRound == UrgeRound.ROUND_2_MENTAL_MIRAGE) {
            state = state.copy(
                currentRound = UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN,
                round3LockdownSecondsLeft = initialLockdownSeconds,
                round3LockdownComplete = (initialLockdownSeconds <= 0)
            )
            return true
        }
        return false
    }

    fun tickRound3Timer(): Int {
        if (state.currentRound != UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN) {
            return state.round3LockdownSecondsLeft
        }
        val next = (state.round3LockdownSecondsLeft - 1).coerceAtLeast(0)
        state = state.copy(
            round3LockdownSecondsLeft = next,
            round3LockdownComplete = (next == 0)
        )
        return next
    }

    fun claimVictory(complimentSeed: Int = 0): Boolean {
        // In Round 3, cannot claim victory until lockdown timer completes
        if (state.currentRound == UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN && !state.round3LockdownComplete) {
            return false
        }
        state = state.copy(
            currentRound = UrgeRound.VICTORY_REWARD,
            victoryComplimentIndex = complimentSeed,
            totalUrgesDefeated = state.totalUrgesDefeated + 1
        )
        return true
    }

    fun reset() {
        state = UrgeDestroyerState(
            round3LockdownSecondsLeft = initialLockdownSeconds,
            round3LockdownComplete = (initialLockdownSeconds <= 0)
        )
    }
}
