package com.focuscrew.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focuscrew.app.data.local.entity.TaskAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskAlarmDao {

    @Query("SELECT * FROM task_alarms ORDER BY isCompleted ASC, scheduledTimeMillis ASC")
    fun getAllTasksFlow(): Flow<List<TaskAlarmEntity>>

    @Query("SELECT * FROM task_alarms WHERE isEnabled = 1 AND isCompleted = 0 ORDER BY scheduledTimeMillis ASC")
    suspend fun getActivePendingTasks(): List<TaskAlarmEntity>

    @Query("SELECT * FROM task_alarms WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskAlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskAlarmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskAlarmEntity>)

    @Update
    suspend fun updateTask(task: TaskAlarmEntity)

    @Delete
    suspend fun deleteTask(task: TaskAlarmEntity)

    @Query("DELETE FROM task_alarms WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("UPDATE task_alarms SET isCompleted = :completed, completedAtMillis = :completedAt, isEnabled = CASE WHEN :completed = 1 THEN 0 ELSE 1 END, updatedAtMillis = :now WHERE id = :id")
    suspend fun setTaskCompleted(id: String, completed: Boolean, completedAt: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE task_alarms SET isEnabled = :enabled, updatedAtMillis = :now WHERE id = :id")
    suspend fun setTaskEnabled(id: String, enabled: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE task_alarms SET scheduledTimeMillis = :newScheduledTime, snoozeCount = :snoozeCount, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateSchedule(id: String, newScheduledTime: Long, snoozeCount: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE task_alarms SET scheduledTimeMillis = :newScheduledTime, snoozeCount = snoozeCount + :snoozeIncrement, updatedAtMillis = :now WHERE id = :id")
    suspend fun snoozeTask(id: String, newScheduledTime: Long, snoozeIncrement: Int = 1, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM task_alarms")
    suspend fun deleteAllTasks()
}
