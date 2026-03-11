package com.nexusflow.data.repository

import com.nexusflow.core.model.*
import com.nexusflow.core.triggers.TriggerFactory
import com.nexusflow.core.actions.ActionFactory
import com.nexusflow.core.conditions.ConditionFactory
import com.nexusflow.data.db.entities.ActionDto
import com.nexusflow.data.db.entities.ConditionDto
import com.nexusflow.data.db.entities.RuleEntity
import com.nexusflow.data.db.entities.TriggerDto
import com.nexusflow.data.db.entities.RuleDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RuleRepository(
    private val dao: RuleDao,
    private val gson: Gson = Gson()
) {

    fun observeAllRules(): Flow<List<Rule>> =
        dao.observeAllRules().map { entities -> entities.map { it.toDomain() } }

    suspend fun getEnabledRules(): List<Rule> =
        dao.getEnabledRules().map { it.toDomain() }

    suspend fun getRuleById(id: String): Rule? =
        dao.getById(id)?.toDomain()

    suspend fun save(rule: Rule) = dao.insert(rule.toEntity())

    suspend fun delete(ruleId: String) = dao.deleteById(ruleId)

    suspend fun setEnabled(ruleId: String, enabled: Boolean) =
        dao.setEnabled(ruleId, enabled)

    suspend fun setAllEnabled(enabled: Boolean) = dao.setAllEnabled(enabled)

    suspend fun recordTrigger(ruleId: String) =
        dao.recordTrigger(ruleId, System.currentTimeMillis())

    private fun Rule.toEntity() = RuleEntity(
        id = id,
        name = name,
        description = description,
        triggersJson = gson.toJson(triggers.map { TriggerDto(it.type.name, it.parameters) }),
        conditionsJson = gson.toJson(conditions.map { ConditionDto(it.type.name, it.parameters) }),
        actionsJson = gson.toJson(actions.map { ActionDto(it.type.name, it.parameters) }),
        isEnabled = isEnabled,
        priority = priority,
        triggerMode = triggerMode.name,
        conditionMode = conditionMode.name,
        createdAt = createdAt,
        lastTriggeredAt = lastTriggeredAt,
        triggerCount = triggerCount
    )

    private fun RuleEntity.toDomain(): Rule {
        val triggerDtos: List<TriggerDto> = try {
            gson.fromJson(triggersJson, object : TypeToken<List<TriggerDto>>() {}.type)
        } catch (e: Exception) { emptyList() } ?: emptyList()

        val conditionDtos: List<ConditionDto> = try {
            gson.fromJson(conditionsJson, object : TypeToken<List<ConditionDto>>() {}.type)
        } catch (e: Exception) { emptyList() } ?: emptyList()

        val actionDtos: List<ActionDto> = try {
            gson.fromJson(actionsJson, object : TypeToken<List<ActionDto>>() {}.type)
        } catch (e: Exception) { emptyList() } ?: emptyList()

        return Rule(
            id = id,
            name = name,
            description = description,
            triggers = triggerDtos.map {
                TriggerFactory.create(TriggerType.valueOf(it.type), it.parameters)
            },
            conditions = conditionDtos.map {
                ConditionFactory.create(ConditionType.valueOf(it.type), it.parameters)
            },
            actions = actionDtos.map {
                ActionFactory.create(ActionType.valueOf(it.type), it.parameters)
            },
            isEnabled = isEnabled,
            priority = priority,
            triggerMode = TriggerMode.valueOf(triggerMode),
            conditionMode = ConditionMode.valueOf(conditionMode),
            createdAt = createdAt,
            lastTriggeredAt = lastTriggeredAt,
            triggerCount = triggerCount
        )
    }
}