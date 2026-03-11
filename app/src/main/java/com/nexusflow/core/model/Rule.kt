package com.nexusflow.core.model

import java.util.UUID

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val triggers: List<Trigger>,
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action>,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val triggerMode: TriggerMode = TriggerMode.ANY,
    val conditionMode: ConditionMode = ConditionMode.ALL,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null,
    val triggerCount: Int = 0
)

enum class TriggerMode { ANY, ALL }

enum class ConditionMode { ALL, ANY }
