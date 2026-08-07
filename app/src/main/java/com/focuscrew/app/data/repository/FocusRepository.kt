package com.focuscrew.app.data.repository

import com.focuscrew.app.data.local.FocusDatabase
import com.focuscrew.app.data.local.entity.BlockedAppEntity
import com.focuscrew.app.data.local.entity.DistractionEntryEntity
import com.focuscrew.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

class FocusRepository(private val database: FocusDatabase) {

    private val sessionDao = database.sessionDao()
    private val distractionDao = database.distractionDao()
    private val blockedAppDao = database.blockedAppDao()

    // --- SESSIONS ---

    suspend fun startSession(
        mode: String,
        plannedDurationMs: Long,
        isStrict: Boolean = true,
        subject: String = "Deep Focus"
    ): Long {
        val entity = SessionEntity(
            subject = subject,
            mode = mode,
            plannedDurationMs = plannedDurationMs,
            actualDurationMs = 0,
            startedAt = System.currentTimeMillis(),
            isStrict = isStrict,
            distractionCount = 0
        )
        return sessionDao.insertSession(entity)
    }

    suspend fun finishSession(
        sessionId: Long,
        actualDurationMs: Long,
        endReason: String
    ) {
        val existing = sessionDao.getSessionById(sessionId) ?: return
        val distractionCount = distractionDao.getCountForSession(sessionId)
        sessionDao.updateSession(
            existing.copy(
                endedAt = System.currentTimeMillis(),
                actualDurationMs = actualDurationMs,
                endReason = endReason,
                distractionCount = distractionCount
            )
        )
    }

    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    fun getTotalFocusTimeSince(sinceTimestamp: Long): Flow<Long?> =
        sessionDao.getTotalFocusTimeSince(sinceTimestamp)

    fun getCompletedSessionCountSince(sinceTimestamp: Long): Flow<Int> =
        sessionDao.getCompletedSessionCountSince(sinceTimestamp)

    suspend fun deleteSession(id: Long) = sessionDao.deleteSession(id)

    // --- DISTRACTION JOURNAL ---

    suspend fun addDistraction(sessionId: Long, thoughtText: String): Long {
        val entity = DistractionEntryEntity(
            sessionId = sessionId,
            thoughtText = thoughtText.trim(),
            timestamp = System.currentTimeMillis()
        )
        return distractionDao.insertDistraction(entity)
    }

    fun getDistractionsForSession(sessionId: Long): Flow<List<DistractionEntryEntity>> =
        distractionDao.getDistractionsForSession(sessionId)

    fun getAllDistractions(): Flow<List<DistractionEntryEntity>> =
        distractionDao.getAllDistractions()

    suspend fun deleteDistraction(id: Long) = distractionDao.deleteDistraction(id)

    // --- BLOCKED APPS ---

    fun getAllBlockedApps(): Flow<List<BlockedAppEntity>> = blockedAppDao.getAllBlockedApps()

    fun getBlockedPackageNames(): Flow<List<String>> = blockedAppDao.getBlockedPackageNames()

    suspend fun setAppBlocked(packageName: String, appName: String, isBlocked: Boolean) {
        if (isBlocked) {
            blockedAppDao.insertBlockedApp(
                BlockedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isBlocked = true
                )
            )
        } else {
            blockedAppDao.deleteBlockedApp(packageName)
        }
    }

    suspend fun clearAllBlockedApps() = blockedAppDao.clearAll()

    // --- DATA WIPING & PRIVACY DELETION ---

    suspend fun clearAllHistory() {
        distractionDao.clearAllDistractions()
        sessionDao.clearAllSessions()
    }

    suspend fun factoryResetAllData() {
        distractionDao.clearAllDistractions()
        sessionDao.clearAllSessions()
        blockedAppDao.clearAll()
    }
}
