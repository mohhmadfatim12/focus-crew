package com.focuscrew.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focuscrew.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT SUM(actualDurationMs) FROM sessions WHERE startedAt >= :sinceTimestamp")
    fun getTotalFocusTimeSince(sinceTimestamp: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM sessions WHERE startedAt >= :sinceTimestamp AND endReason = 'COMPLETED'")
    fun getCompletedSessionCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()
}
