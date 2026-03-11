package com.nexusflow.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusflow.core.actions.ActionFactory
import com.nexusflow.core.conditions.ConditionFactory
import com.nexusflow.core.model.*
import com.nexusflow.core.triggers.TriggerFactory
import com.nexusflow.data.repository.RuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateRuleUiState(
    val id: String? = null,
    val name: String = "",
    val selectedTriggers: List<Pair<TriggerType, Map<String, String>>> = emptyList(),
    val selectedConditions: List<Pair<ConditionType, Map<String, String>>> = emptyList(),
    val selectedActions: List<Pair<ActionType, Map<String, String>>> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class CreateRuleViewModel(
    private val repository: RuleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRuleUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRule(ruleId: String) {
        viewModelScope.launch {
            val rule = repository.getRuleById(ruleId)
            if (rule != null) {
                _uiState.update { it.copy(
                    id = rule.id,
                    name = rule.name,
                    selectedTriggers = rule.triggers.map { t -> t.type to t.parameters },
                    selectedConditions = rule.conditions.map { c -> c.type to c.parameters },
                    selectedActions = rule.actions.map { a -> a.type to a.parameters }
                ) }
            }
        }
    }

    fun setName(name: String) = _uiState.update { it.copy(name = name) }

    fun addTrigger(type: TriggerType, params: Map<String, String>) = _uiState.update {
        // One trigger per flow as requested
        it.copy(selectedTriggers = listOf(type to params))
    }

    fun addCondition(type: ConditionType, params: Map<String, String>) = _uiState.update {
        it.copy(selectedConditions = it.selectedConditions + (type to params))
    }

    fun removeCondition(index: Int) = _uiState.update {
        val newList = it.selectedConditions.toMutableList()
        if (index in newList.indices) newList.removeAt(index)
        it.copy(selectedConditions = newList)
    }

    fun addAction(type: ActionType, params: Map<String, String>) = _uiState.update {
        it.copy(selectedActions = it.selectedActions + (type to params))
    }

    fun removeAction(index: Int) = _uiState.update {
        val newList = it.selectedActions.toMutableList()
        if (index in newList.indices) newList.removeAt(index)
        it.copy(selectedActions = newList)
    }

    fun saveRule() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Flow name is required") }
            return
        }
        if (state.selectedTriggers.isEmpty()) {
            _uiState.update { it.copy(error = "Please select a starting condition (IF)") }
            return
        }
        if (state.selectedActions.isEmpty()) {
            _uiState.update { it.copy(error = "Please add at least one task (THEN)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val rule = Rule(
                    id = state.id ?: java.util.UUID.randomUUID().toString(),
                    name = state.name,
                    triggers = state.selectedTriggers.map { TriggerFactory.create(it.first, it.second) },
                    conditions = state.selectedConditions.map { ConditionFactory.create(it.first, it.second) },
                    actions = state.selectedActions.map { ActionFactory.create(it.first, it.second) }
                )
                repository.save(rule)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save flow") }
            }
        }
    }
}