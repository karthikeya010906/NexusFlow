package com.nexusflow.data.db.entities

import androidx.room.*
import com.nexusflow.data.db.entities.LogEntry
import com.nexusflow.data.db.entities.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY priority DESC, createdAt DESC")
    fun observeAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledRules(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: String): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity)

    @Update
    suspend fun update(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE rules SET lastTriggeredAt = :timestamp, triggerCount = triggerCount + 1 WHERE id = :id")
    suspend fun recordTrigger(id: String, timestamp: Long)

    @Query("UPDATE rules SET isEnabled = :enabled")
    suspend fun setAllEnabled(enabled: Boolean)
}

@Dao
interface LogDao {

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 200")
    fun observeRecentLogs(): Flow<List<LogEntry>>

    @Insert
    suspend fun insert(log: LogEntry)

    @Query("DELETE FROM logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM logs")
    suspend fun clearAll()
}
