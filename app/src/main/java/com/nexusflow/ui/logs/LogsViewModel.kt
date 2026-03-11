package com.nexusflow.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusflow.data.db.entities.LogEntry
import com.nexusflow.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogsViewModel(
    private val logRepository: LogRepository
) : ViewModel() {

    val logs: StateFlow<List<LogEntry>> = logRepository.observeRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearAll()
        }
    }
}