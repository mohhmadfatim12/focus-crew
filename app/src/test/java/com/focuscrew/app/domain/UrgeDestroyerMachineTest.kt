package com.focuscrew.app.domain

import com.focuscrew.app.domain.statemachine.UrgeDestroyerMachine
import com.focuscrew.app.domain.statemachine.UrgeDestroyerState
import com.focuscrew.app.domain.statemachine.UrgeRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UrgeDestroyerMachineTest {

    private lateinit var machine: UrgeDestroyerMachine

    @Before
    fun setUp() {
        machine = UrgeDestroyerMachine(initialLockdownSeconds = 5)
    }

    @Test
    fun `starts in round 1 biological reset`() {
        assertEquals(UrgeRound.ROUND_1_BIOLOGICAL_RESET, machine.state.currentRound)
        assertEquals(0, machine.state.round1CompletedBreathCycles)
        assertTrue(machine.state.round1GroundingDone.isEmpty())
    }

    @Test
    fun `records breath cycles and toggles grounding in round 1`() {
        machine.recordBreathCycleCompleted()
        machine.recordBreathCycleCompleted()
        assertEquals(2, machine.state.round1CompletedBreathCycles)

        machine.toggleGroundingItem(0)
        machine.toggleGroundingItem(2)
        assertEquals(setOf(0, 2), machine.state.round1GroundingDone)

        machine.toggleGroundingItem(0)
        assertEquals(setOf(2), machine.state.round1GroundingDone)
    }

    @Test
    fun `can claim victory directly from round 1`() {
        val won = machine.claimVictory(complimentSeed = 1)
        assertTrue(won)
        assertEquals(UrgeRound.VICTORY_REWARD, machine.state.currentRound)
        assertEquals(1, machine.state.totalUrgesDefeated)
    }

    @Test
    fun `advances to round 2 when urge remains strong`() {
        val advanced = machine.advanceToRound2()
        assertTrue(advanced)
        assertEquals(UrgeRound.ROUND_2_MENTAL_MIRAGE, machine.state.currentRound)

        // Cannot advance to round 2 again from round 2
        assertFalse(machine.advanceToRound2())
    }

    @Test
    fun `round 2 handles HALT trigger selection and oath typing validation`() {
        machine.advanceToRound2()
        machine.selectHaltTrigger("Tired")
        assertEquals("Tired", machine.state.round2SelectedHalt)

        // Typing incorrect oath
        machine.updateRound2Oath("Just a peek")
        assertFalse(machine.state.round2OathMatched)

        // Typing exact oath (case-insensitive)
        machine.updateRound2Oath(UrgeDestroyerState.REQUIRED_OATH)
        assertTrue(machine.state.round2OathMatched)
    }

    @Test
    fun `advances to round 3 strict lockdown and enforces unskippable timer`() {
        machine.advanceToRound2()
        val advanced = machine.advanceToRound3()
        assertTrue(advanced)
        assertEquals(UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN, machine.state.currentRound)
        assertEquals(5, machine.state.round3LockdownSecondsLeft)
        assertFalse(machine.state.round3LockdownComplete)

        // Cannot claim victory while lockdown timer is ticking
        val prematureVictory = machine.claimVictory()
        assertFalse(prematureVictory)
        assertEquals(UrgeRound.ROUND_3_NUCLEAR_LOCKDOWN, machine.state.currentRound)

        // Tick timer to 0
        repeat(5) {
            machine.tickRound3Timer()
        }

        assertEquals(0, machine.state.round3LockdownSecondsLeft)
        assertTrue(machine.state.round3LockdownComplete)

        // Now victory can be claimed
        val finalVictory = machine.claimVictory(complimentSeed = 3)
        assertTrue(finalVictory)
        assertEquals(UrgeRound.VICTORY_REWARD, machine.state.currentRound)
    }

    @Test
    fun `reset returns state back to initial state`() {
        machine.advanceToRound2()
        machine.advanceToRound3()
        machine.reset()
        assertEquals(UrgeRound.ROUND_1_BIOLOGICAL_RESET, machine.state.currentRound)
        assertEquals(5, machine.state.round3LockdownSecondsLeft)
    }
}
