package com.dmitriim.localaiplayground.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelImportRequest
import com.dmitriim.localaiplayground.core.model.ModelLibrary
import com.dmitriim.localaiplayground.core.model.ModelTransfers
import com.dmitriim.localaiplayground.core.model.ModelValidationState
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
    private val modelLibrary: ModelLibrary,
    private val modelTransfers: ModelTransfers,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(modelLibrary.installedModels, modelTransfers.catalog, modelTransfers.transfers) { installed, catalog, transfers ->
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
        modelLibrary.import(
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
        modelTransfers.download(modelId).getOrThrow()
        "Download scheduled."
    }

    fun validate(modelId: ModelId) {
        if (modelId in mutableUiState.value.validatingModelIds) return
        mutableUiState.update {
            it.copy(
                validatingModelIds = it.validatingModelIds + modelId,
                validationFeedback = it.validationFeedback - modelId,
            )
        }
        viewModelScope.launch {
            val feedback = modelLibrary.validate(modelId).fold(
                onSuccess = { model ->
                    val ready = model.validationState == ModelValidationState.READY
                    ModelValidationFeedback(
                        message = if (ready) {
                            "Validation passed."
                        } else {
                            model.validationMessage ?: "Validation found a problem."
                        },
                        isError = !ready,
                    )
                },
                onFailure = { error ->
                    ModelValidationFeedback(
                        message = error.message ?: "Validation failed.",
                        isError = true,
                    )
                },
            )
            mutableUiState.update {
                it.copy(
                    validatingModelIds = it.validatingModelIds - modelId,
                    validationFeedback = it.validationFeedback + (modelId to feedback),
                )
            }
        }
    }

    fun requestDelete(modelId: ModelId) {
        mutableUiState.update { state -> state.copy(pendingDelete = state.installed.firstOrNull { it.manifest.modelId == modelId }) }
    }

    fun cancelDelete() = mutableUiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val model = mutableUiState.value.pendingDelete ?: return
        mutableUiState.update {
            it.copy(
                pendingDelete = null,
                validationFeedback = it.validationFeedback - model.manifest.modelId,
            )
        }
        launchOperation {
            modelLibrary.delete(model.manifest.modelId).getOrThrow()
            "${model.manifest.displayName} was deleted."
        }
    }

    fun cancelTransfer(modelId: ModelId) {
        viewModelScope.launch { modelTransfers.cancelTransfer(modelId) }
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
