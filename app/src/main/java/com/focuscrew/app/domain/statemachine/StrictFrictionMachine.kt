package com.focuscrew.app.domain.statemachine

const val DEFAULT_EXIT_PHRASE = "I want to stop my session"

sealed interface FrictionState {
    object Idle : FrictionState

    data class Cooldown(
        val secondsRemaining: Int,
        val totalSeconds: Int = 30,
        val requiredPhrase: String = DEFAULT_EXIT_PHRASE
    ) : FrictionState

    data class ReadyToConfirm(
        val requiredPhrase: String = DEFAULT_EXIT_PHRASE,
        val typedInput: String = "",
        val isPhraseMatched: Boolean = false
    ) : FrictionState

    data class EmergencyWarning(
        val previousState: FrictionState
    ) : FrictionState

    data class CancelledEarly(
        val isEmergencyExit: Boolean
    ) : FrictionState
}

/**
 * Pure Kotlin state machine governing strict mode friction.
 * Requires a 30-second cooldown and a typed phrase before quitting,
 * while always providing an immediate emergency exit.
 */
class StrictFrictionMachine(
    val cooldownDurationSeconds: Int = 30
) {
    var state: FrictionState = FrictionState.Idle
        private set

    fun startExitFlow(isStrict: Boolean): FrictionState {
        state = if (isStrict) {
            FrictionState.Cooldown(
                secondsRemaining = cooldownDurationSeconds,
                totalSeconds = cooldownDurationSeconds
            )
        } else {
            // Non-strict mode skips cooldown directly to confirmation
            FrictionState.ReadyToConfirm(
                requiredPhrase = "",
                typedInput = "",
                isPhraseMatched = true
            )
        }
        return state
    }

    fun tickCooldown(): FrictionState {
        val current = state as? FrictionState.Cooldown ?: return state
        val nextRemaining = current.secondsRemaining - 1

        state = if (nextRemaining <= 0) {
            FrictionState.ReadyToConfirm(
                requiredPhrase = current.requiredPhrase,
                typedInput = "",
                isPhraseMatched = false
            )
        } else {
            current.copy(secondsRemaining = nextRemaining)
        }
        return state
    }

    fun updateTypedPhrase(input: String): FrictionState {
        val current = state as? FrictionState.ReadyToConfirm ?: return state
        val matches = if (current.requiredPhrase.isEmpty()) {
            true
        } else {
            input.trim().equals(current.requiredPhrase.trim(), ignoreCase = true)
        }

        state = current.copy(
            typedInput = input,
            isPhraseMatched = matches
        )
        return state
    }

    fun confirmStop(): Boolean {
        val current = state as? FrictionState.ReadyToConfirm ?: return false
        if (current.isPhraseMatched) {
            state = FrictionState.CancelledEarly(isEmergencyExit = false)
            return true
        }
        return false
    }

    fun requestEmergencyExit(): FrictionState {
        state = FrictionState.EmergencyWarning(previousState = state)
        return state
    }

    fun abortEmergencyExit(): FrictionState {
        val current = state as? FrictionState.EmergencyWarning
        state = current?.previousState ?: FrictionState.Idle
        return state
    }

    fun confirmEmergencyExit(): FrictionState {
        state = FrictionState.CancelledEarly(isEmergencyExit = true)
        return state
    }

    fun triggerEmergencyExit(): FrictionState {
        return confirmEmergencyExit()
    }

    fun dismissFriction(): FrictionState {
        state = FrictionState.Idle
        return state
    }

    companion object {
        const val REQUIRED_PHRASE = DEFAULT_EXIT_PHRASE
    }
}
