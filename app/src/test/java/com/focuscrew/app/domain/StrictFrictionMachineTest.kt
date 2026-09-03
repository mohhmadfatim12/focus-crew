package com.focuscrew.app.domain

import com.focuscrew.app.domain.statemachine.FrictionState
import com.focuscrew.app.domain.statemachine.StrictFrictionMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StrictFrictionMachineTest {

    private lateinit var frictionMachine: StrictFrictionMachine

    @Before
    fun setUp() {
        frictionMachine = StrictFrictionMachine(cooldownDurationSeconds = 30)
    }

    @Test
    fun `non-strict mode starts ready to confirm immediately`() {
        val state = frictionMachine.startExitFlow(isStrict = false)
        assertTrue(state is FrictionState.ReadyToConfirm)
        assertTrue(frictionMachine.confirmStop())
    }

    @Test
    fun `strict mode initiates 30-second cooldown`() {
        val state = frictionMachine.startExitFlow(isStrict = true)
        assertTrue(state is FrictionState.Cooldown)
        assertEquals(30, (state as FrictionState.Cooldown).secondsRemaining)
    }

    @Test
    fun `ticking cooldown reaches zero and requests phrase`() {
        frictionMachine.startExitFlow(isStrict = true)

        repeat(29) {
            val state = frictionMachine.tickCooldown()
            assertTrue(state is FrictionState.Cooldown)
        }

        // 30th tick reaches completion
        val finalState = frictionMachine.tickCooldown()
        assertTrue(finalState is FrictionState.ReadyToConfirm)
        assertEquals(StrictFrictionMachine.REQUIRED_PHRASE, (finalState as FrictionState.ReadyToConfirm).requiredPhrase)
        assertFalse(finalState.isPhraseMatched)
    }

    @Test
    fun `confirmation requires exact phrase match`() {
        frictionMachine.startExitFlow(isStrict = true)
        repeat(30) { frictionMachine.tickCooldown() }

        // Mismatched phrase fails
        frictionMachine.updateTypedPhrase("quit now")
        assertFalse(frictionMachine.confirmStop())

        // Exact phrase succeeds
        frictionMachine.updateTypedPhrase(StrictFrictionMachine.REQUIRED_PHRASE)
        assertTrue(frictionMachine.confirmStop())
    }

    @Test
    fun `emergency exit is always available during cooldown`() {
        frictionMachine.startExitFlow(isStrict = true)
        // 5 seconds into cooldown, emergency exit is tapped
        repeat(5) { frictionMachine.tickCooldown() }

        val exitState = frictionMachine.triggerEmergencyExit()
        assertTrue(exitState is FrictionState.CancelledEarly)
        assertTrue((exitState as FrictionState.CancelledEarly).isEmergencyExit)
    }

    @Test
    fun `request emergency exit triggers warning and can be aborted or confirmed`() {
        frictionMachine.startExitFlow(isStrict = true)
        repeat(10) { frictionMachine.tickCooldown() }

        // Tapping emergency exit triggers psychological warning
        val warningState = frictionMachine.requestEmergencyExit()
        assertTrue(warningState is FrictionState.EmergencyWarning)

        // User rethinks and stays focused (aborts emergency exit)
        val revertedState = frictionMachine.abortEmergencyExit()
        assertTrue(revertedState is FrictionState.Cooldown)
        assertEquals(20, (revertedState as FrictionState.Cooldown).secondsRemaining)

        // User taps emergency exit again and confirms
        frictionMachine.requestEmergencyExit()
        val finalExit = frictionMachine.confirmEmergencyExit()
        assertTrue(finalExit is FrictionState.CancelledEarly)
        assertTrue((finalExit as FrictionState.CancelledEarly).isEmergencyExit)
    }
}
