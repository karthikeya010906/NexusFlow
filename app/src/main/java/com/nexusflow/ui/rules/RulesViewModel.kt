package com.nexusflow.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusflow.core.engine.AutomationEngine
import com.nexusflow.core.model.Rule
import com.nexusflow.data.repository.RuleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val repository: RuleRepository,
    private val engine: AutomationEngine
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = repository.observeAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isGlobalEnabled: Boolean get() = engine.globalEnabled

    fun toggleGlobalEnabled() {
        engine.globalEnabled = !engine.globalEnabled
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(ruleId, enabled)
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            repository.delete(ruleId)
        }
    }

    fun disableAll() {
        viewModelScope.launch {
            repository.setAllEnabled(false)
        }
    }

    fun enableAll() {
        viewModelScope.launch {
            repository.setAllEnabled(true)
        }
    }
}