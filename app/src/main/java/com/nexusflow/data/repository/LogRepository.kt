package com.nexusflow.data.repository

import com.nexusflow.data.db.entities.LogDao
import com.nexusflow.data.db.entities.LogEntry
import kotlinx.coroutines.flow.Flow

class LogRepository(private val dao: LogDao) {

    fun observeRecentLogs(): Flow<List<LogEntry>> = dao.observeRecentLogs()

    suspend fun insert(log: LogEntry) = dao.insert(log)

    suspend fun clearAll() = dao.clearAll()

    suspend fun pruneOldLogs() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        dao.deleteOlderThan(thirtyDaysAgo)
    }
}