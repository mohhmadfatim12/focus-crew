package com.focuscrew.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focuscrew.app.data.local.entity.DistractionEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistractionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistraction(entry: DistractionEntryEntity): Long

    @Query("SELECT * FROM distraction_entries WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getDistractionsForSession(sessionId: Long): Flow<List<DistractionEntryEntity>>

    @Query("SELECT * FROM distraction_entries ORDER BY timestamp DESC")
    fun getAllDistractions(): Flow<List<DistractionEntryEntity>>

    @Query("SELECT COUNT(*) FROM distraction_entries WHERE sessionId = :sessionId")
    suspend fun getCountForSession(sessionId: Long): Int

    @Query("DELETE FROM distraction_entries WHERE id = :id")
    suspend fun deleteDistraction(id: Long)

    @Query("DELETE FROM distraction_entries")
    suspend fun clearAllDistractions()
}
