package com.nexusflow.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val triggersJson: String,
    val conditionsJson: String,
    val actionsJson: String,
    val isEnabled: Boolean,
    val priority: Int,
    val triggerMode: String,
    val conditionMode: String,
    val createdAt: Long,
    val lastTriggeredAt: Long?,
    val triggerCount: Int
)

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: String,
    val ruleName: String,
    val triggerDescription: String,
    val actionSummary: String,
    val success: Boolean,
    val timestamp: Long
)

data class TriggerDto(
    val type: String,
    val parameters: Map<String, String>
)

data class ConditionDto(
    val type: String,
    val parameters: Map<String, String>
)

data class ActionDto(
    val type: String,
    val parameters: Map<String, String>
)