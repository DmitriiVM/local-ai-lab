package com.dmitriim.localaiplayground.feature.runs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import com.dmitriim.localaiplayground.source.runs.RunExporter
import com.dmitriim.localaiplayground.source.runs.RunReplayStore
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class RunsViewModel(
    private val runRepository: RunRepository,
    private val exporter: RunExporter,
    private val replayStore: RunReplayStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RunsUiState())
    val state: StateFlow<RunsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runRepository.runs.collectLatest { runs ->
                mutableState.update { current ->
                    val selected = current.selectedRunId?.takeIf { id -> runs.any { it.id == id } }
                    current.copy(runs = runs, selectedRunId = selected)
                }
            }
        }
    }

    fun setCapabilityFilter(value: AiCapability?) = mutableState.update { it.copy(capability = value) }

    fun setStatusFilter(value: RunStatus?) = mutableState.update { it.copy(status = value) }

    fun selectRun(id: String) = mutableState.update { it.copy(selectedRunId = id) }

    fun closeDetails() = mutableState.update { it.copy(selectedRunId = null) }

    fun requestClearRunHistory() = mutableState.update { it.copy(pendingRunHistoryClear = true) }

    fun dismissClearRunHistory() = mutableState.update { it.copy(pendingRunHistoryClear = false) }

    fun clearRunHistory() = viewModelScope.launch(Dispatchers.IO) {
        runRepository.clearRuns()
        mutableState.update { it.copy(pendingRunHistoryClear = false, selectedRunId = null) }
    }

    fun prepareShare() {
        val run = state.value.selectedRun ?: return
        runCatching { exporter.export(run) }
            .onSuccess { uri -> mutableState.update { it.copy(pendingShareUri = uri, errorMessage = null) } }
            .onFailure { error -> mutableState.update { it.copy(errorMessage = error.message ?: "Could not create the run export.") } }
    }

    fun consumeShare() = mutableState.update { it.copy(pendingShareUri = null) }

    fun repeatSelected(): RunRecord? = state.value.selectedRun?.also(replayStore::select)
}

data class RunsUiState(
    val runs: List<RunRecord> = emptyList(),
    val capability: AiCapability? = null,
    val status: RunStatus? = null,
    val selectedRunId: String? = null,
    val pendingShareUri: String? = null,
    val pendingRunHistoryClear: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredRuns: List<RunRecord>
        get() = runs.filter { (capability == null || it.capability == capability) && (status == null || it.status == status) }
    val selectedRun: RunRecord? get() = runs.firstOrNull { it.id == selectedRunId }
}
