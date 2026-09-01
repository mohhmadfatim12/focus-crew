package com.focuscrew.app.domain

import com.focuscrew.app.core.time.TimeProvider
import com.focuscrew.app.domain.model.CommitmentMode
import com.focuscrew.app.domain.model.SessionMode
import com.focuscrew.app.domain.model.SessionTask
import com.focuscrew.app.domain.model.TimerStatus
import com.focuscrew.app.domain.statemachine.SessionStateMachine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CommitmentModeTest {

    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var stateMachine: SessionStateMachine

    private class FakeTimeProvider : TimeProvider {
        var elapsed: Long = 100_000L
        var wallClock: Long = 1_700_000_000_000L

        override fun elapsedRealtime(): Long = elapsed
        override fun currentTimeMillis(): Long = wallClock

        fun advance(ms: Long) {
            elapsed += ms
            wallClock += ms
        }
    }

    @Before
    fun setup() {
        fakeTimeProvider = FakeTimeProvider()
        stateMachine = SessionStateMachine(fakeTimeProvider)
    }

    @Test
    fun testDefaultCommitmentModeIsStrictTimer() {
        val snapshot = stateMachine.getSnapshot()
        assertEquals(CommitmentMode.STRICT_TIMER, snapshot.commitmentMode)
        assertTrue(snapshot.sessionTasks.isEmpty())
        assertFalse(snapshot.areTasksCompleted)
    }

    @Test
    fun testSwitchToTaskBasedCommitmentMode() {
        stateMachine.setCommitmentMode(CommitmentMode.TASK_BASED)
        val snapshot = stateMachine.getSnapshot()
        assertEquals(CommitmentMode.TASK_BASED, snapshot.commitmentMode)
    }

    @Test
    fun testTaskLifecycleAndCompletion() {
        stateMachine.setCommitmentMode(CommitmentMode.TASK_BASED)

        val task1 = stateMachine.addTask("Read chapter 4")
        val task2 = stateMachine.addTask("Solve 10 problems")

        assertEquals(2, stateMachine.sessionTasks.size)
        assertFalse(stateMachine.areAllTasksCompleted())

        // Toggle task 1
        stateMachine.toggleTask(task1.id)
        assertTrue(stateMachine.sessionTasks.first { it.id == task1.id }.isCompleted)
        assertFalse(stateMachine.areAllTasksCompleted())

        // Toggle task 2
        stateMachine.toggleTask(task2.id)
        assertTrue(stateMachine.areAllTasksCompleted())

        val snapshot = stateMachine.getSnapshot()
        assertTrue(snapshot.areTasksCompleted)
    }

    @Test
    fun testUnlockViaTasksCompleted() {
        stateMachine.setCommitmentMode(CommitmentMode.TASK_BASED)
        val task = stateMachine.addTask("Finish code review")

        stateMachine.startSession(SessionMode.FOCUS_50)
        assertTrue(stateMachine.currentStatus is TimerStatus.Running)

        // Complete the task
        stateMachine.toggleTask(task.id)
        assertTrue(stateMachine.areAllTasksCompleted())

        // Unlock session early via tasks
        val snapshot = stateMachine.unlockViaTasksCompleted()
        assertTrue(snapshot.status is TimerStatus.Completed)
        assertTrue((snapshot.status as TimerStatus.Completed).wasTaskUnlocked)
    }

    @Test
    fun testRemoveTask() {
        stateMachine.setCommitmentMode(CommitmentMode.TASK_BASED)
        val task1 = stateMachine.addTask("Task 1")
        val task2 = stateMachine.addTask("Task 2")

        assertEquals(2, stateMachine.sessionTasks.size)
        stateMachine.removeTask(task1.id)

        assertEquals(1, stateMachine.sessionTasks.size)
        assertEquals("Task 2", stateMachine.sessionTasks.first().title)
    }

    @Test
    fun testStrictLockdownEnablesSnapshotProperty() {
        assertFalse(stateMachine.isStrictLockdown)
        assertFalse(stateMachine.getSnapshot().isStrictLockdown)

        stateMachine.setStrictLockdown(true)
        assertTrue(stateMachine.isStrictLockdown)
        assertTrue(stateMachine.getSnapshot().isStrictLockdown)
    }

    @Test
    fun testStrictLockdownRetainsLockdownOnSessionStart() {
        stateMachine.setStrictLockdown(true)
        val snapshot = stateMachine.startSession(SessionMode.POMODORO)

        assertTrue(snapshot.isStrictLockdown)
        assertTrue(snapshot.status is TimerStatus.Running)
    }
}
