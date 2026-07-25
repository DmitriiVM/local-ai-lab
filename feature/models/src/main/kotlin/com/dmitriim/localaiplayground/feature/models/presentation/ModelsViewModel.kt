package com.dmitriim.localaiplayground.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelImportRequest
import com.dmitriim.localaiplayground.core.model.ModelRepository
import com.dmitriim.localaiplayground.core.model.RuntimeProfileType
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ModelsViewModel(
    private val repository: ModelRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.installedModels, repository.catalog, repository.transfers) { installed, catalog, transfers ->
                Triple(installed, catalog, transfers)
            }.collect { (installed, catalog, transfers) ->
                mutableUiState.update { current ->
                    current.copy(installed = installed, catalog = catalog, transfers = transfers)
                }
            }
        }
    }

    fun import(profile: RuntimeProfileType, uris: List<String>) = launchOperation {
        val engine = if (profile == RuntimeProfileType.LLM) EngineId("llama.cpp") else EngineId("sherpa-onnx")
        repository.import(
            ModelImportRequest(
                displayName = "Imported ${profile.displayName}",
                engineId = engine,
                profileType = profile,
                documentUris = uris,
            ),
        ).getOrThrow()
        "Model imported and validated."
    }

    fun download(modelId: ModelId) = launchOperation {
        repository.download(modelId).getOrThrow()
        "Download scheduled."
    }

    fun load(modelId: ModelId) = launchOperation {
        repository.load(modelId).getOrThrow()
        "Model loaded."
    }

    fun unload(modelId: ModelId) = launchOperation {
        repository.unload(modelId).getOrThrow()
        "Model unloaded."
    }

    fun validate(modelId: ModelId) = launchOperation {
        repository.validate(modelId).getOrThrow()
        "Validation completed."
    }

    fun requestDelete(modelId: ModelId) {
        mutableUiState.update { state -> state.copy(pendingDelete = state.installed.firstOrNull { it.manifest.modelId == modelId }) }
    }

    fun cancelDelete() = mutableUiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val model = mutableUiState.value.pendingDelete ?: return
        mutableUiState.update { it.copy(pendingDelete = null) }
        launchOperation {
            repository.delete(model.manifest.modelId).getOrThrow()
            "${model.manifest.displayName} was deleted."
        }
    }

    fun cancelTransfer(modelId: ModelId) {
        viewModelScope.launch { repository.cancelTransfer(modelId) }
    }

    private fun launchOperation(action: suspend () -> String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(message = null) }
            runCatching { action() }
                .onSuccess { message -> mutableUiState.update { it.copy(message = message) } }
                .onFailure { error -> mutableUiState.update { it.copy(message = error.message ?: "The operation failed.") } }
        }
    }
}

private val RuntimeProfileType.displayName: String
    get() = when (this) {
        RuntimeProfileType.LLM -> "GGUF chat model"
        RuntimeProfileType.WHISPER_STT -> "Whisper STT bundle"
        RuntimeProfileType.SILERO_VAD -> "Silero VAD model"
        RuntimeProfileType.SUPERTONIC_TTS -> "Supertonic TTS bundle"
    }
