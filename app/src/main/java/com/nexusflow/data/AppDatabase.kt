package com.nexusflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexusflow.data.db.entities.LogDao
import com.nexusflow.data.db.entities.LogEntry
import com.nexusflow.data.db.entities.RuleDao
import com.nexusflow.data.db.entities.RuleEntity

@Database(
    entities = [RuleEntity::class, LogEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao

    companion object {
        const val DB_NAME = "nexusflow.db"
    }
}
