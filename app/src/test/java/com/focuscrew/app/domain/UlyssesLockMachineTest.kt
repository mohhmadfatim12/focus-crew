package com.focuscrew.app.domain

import com.focuscrew.app.domain.statemachine.UlyssesLockMachine
import com.focuscrew.app.domain.statemachine.UlyssesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UlyssesLockMachineTest {

    @Test
    fun `initial state is disabled with zero cooldown`() {
        val machine = UlyssesLockMachine()
        assertFalse(machine.state.isEnabled)
        assertFalse(machine.isPendingDeactivation())
        assertEquals(0L, machine.getRemainingCoolDownMs(1000L))
    }

    @Test
    fun `enabling lock activates state and clears requests`() {
        val machine = UlyssesLockMachine()
        machine.enable()
        assertTrue(machine.state.isEnabled)
        assertFalse(machine.isPendingDeactivation())
    }

    @Test
    fun `requesting deactivation starts 24 hour cooldown`() {
        val machine = UlyssesLockMachine()
        machine.enable()

        val start = 1_000_000L
        machine.requestDeactivation(start)

        assertTrue(machine.isPendingDeactivation())
        assertEquals(UlyssesState.DEFAULT_COOLDOWN_MS, machine.getRemainingCoolDownMs(start))

        // After 1 hour
        val oneHourLater = start + 3600_000L
        val remaining = machine.getRemainingCoolDownMs(oneHourLater)
        assertEquals(UlyssesState.DEFAULT_COOLDOWN_MS - 3600_000L, remaining)
        assertFalse(machine.isEligibleToDeactivate(oneHourLater))
        assertFalse(machine.completeDeactivation(oneHourLater))
    }

    @Test
    fun `canceling deactivation restores unbroken lock`() {
        val machine = UlyssesLockMachine()
        machine.enable()
        machine.requestDeactivation(1000L)
        assertTrue(machine.isPendingDeactivation())

        machine.cancelDeactivation()
        assertFalse(machine.isPendingDeactivation())
        assertTrue(machine.state.isEnabled)
    }

    @Test
    fun `completes deactivation only after 24 hours pass`() {
        val machine = UlyssesLockMachine()
        machine.enable()
        val start = 1_000_000L
        machine.requestDeactivation(start)

        val full24HoursLater = start + UlyssesState.DEFAULT_COOLDOWN_MS + 100L
        assertTrue(machine.isEligibleToDeactivate(full24HoursLater))

        val success = machine.completeDeactivation(full24HoursLater)
        assertTrue(success)
        assertFalse(machine.state.isEnabled)
    }
}
