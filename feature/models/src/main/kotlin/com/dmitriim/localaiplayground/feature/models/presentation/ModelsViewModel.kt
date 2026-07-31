package com.dmitriim.localaiplayground.feature.models.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.EngineId
import com.dmitriim.localaiplayground.core.model.ModelDiagnostics
import com.dmitriim.localaiplayground.core.model.ModelId
import com.dmitriim.localaiplayground.core.model.ModelImportRequest
import com.dmitriim.localaiplayground.core.model.ModelLibrary
import com.dmitriim.localaiplayground.core.model.ModelManifest
import com.dmitriim.localaiplayground.core.model.ModelProfileId
import com.dmitriim.localaiplayground.core.model.ModelProfileIds
import com.dmitriim.localaiplayground.core.model.ModelTransfers
import com.dmitriim.localaiplayground.core.model.ModelValidationState
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
    private val modelDiagnostics: ModelDiagnostics,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(modelLibrary.installedModels, modelTransfers.catalog, modelTransfers.transfers) { installed, catalog, transfers ->
                Triple(installed, catalog, transfers)
            }.collect { (installed, catalog, transfers) ->
                mutableUiState.update { current ->
                    current.copy(
                        isModelDataLoaded = true,
                        installed = installed,
                        catalog = catalog,
                        transfers = transfers,
                    )
                }
            }
        }
    }

    fun import(profile: ModelProfileId, uris: List<String>) = launchOperation("import ${profile.value}") {
        Log.i(TAG, "Models UI import requested: profile=${profile.value}, documentCount=${uris.size}")
        val engine = if (profile == ModelProfileIds.LLM) EngineId("llama.cpp") else EngineId("sherpa-onnx")
        val modelId = modelLibrary.import(
            ModelImportRequest(
                displayName = "Imported ${profile.displayName}",
                engineId = engine,
                profileType = profile,
                documentUris = uris,
            ),
        ).getOrThrow()
        Log.i(TAG, "Models UI import completed: profile=$profile, modelId=${modelId.value}")
        "Model imported and validated."
    }

    fun selectModel(modelId: ModelId) {
        mutableUiState.update { state ->
            if (state.selectedModelId == modelId) {
                state
            } else {
                state.copy(
                    selectedModelId = modelId,
                    compatibilityModelId = null,
                    compatibility = null,
                    isCheckingCompatibility = false,
                    compatibilityError = null,
                )
            }
        }
    }

    fun checkCompatibility(manifest: ModelManifest) {
        val state = mutableUiState.value
        if (state.selectedModelId != manifest.modelId ||
            state.isCheckingCompatibility ||
            state.compatibilityModelId == manifest.modelId
        ) {
            return
        }
        mutableUiState.update {
            it.copy(
                compatibilityModelId = manifest.modelId,
                compatibility = null,
                isCheckingCompatibility = true,
                compatibilityError = null,
            )
        }
        viewModelScope.launch {
            val result = runCatching { modelDiagnostics.compatibility(manifest) }
            mutableUiState.update { current ->
                if (current.selectedModelId != manifest.modelId) {
                    current
                } else {
                    result.fold(
                        onSuccess = { compatibility ->
                            current.copy(
                                compatibility = compatibility,
                                isCheckingCompatibility = false,
                            )
                        },
                        onFailure = { error ->
                            Log.e(
                                TAG,
                                "Model compatibility check failed: modelId=${manifest.modelId.value}, message=${error.message}",
                                error,
                            )
                            current.copy(
                                isCheckingCompatibility = false,
                                compatibilityError = error.message ?: "Compatibility could not be checked.",
                            )
                        },
                    )
                }
            }
        }
    }

    fun download(modelId: ModelId) = launchOperation("download") {
        Log.i(TAG, "Models UI download requested: modelId=${modelId.value}")
        modelTransfers.download(modelId).getOrThrow()
        Log.i(TAG, "Models UI download scheduled: modelId=${modelId.value}")
        "Download scheduled."
    }

    fun validate(modelId: ModelId) {
        if (modelId in mutableUiState.value.validatingModelIds) {
            Log.w(TAG, "Ignoring duplicate model validation request: modelId=${modelId.value}")
            return
        }
        Log.i(TAG, "Models UI validation requested: modelId=${modelId.value}")
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
                    Log.i(TAG, "Models UI validation completed: modelId=${modelId.value}, state=${model.validationState}, bytes=${model.totalBytes}")
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
                    Log.e(TAG, "Models UI validation failed: modelId=${modelId.value}, message=${error.message}", error)
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
        Log.i(TAG, "Models UI delete confirmation requested: modelId=${modelId.value}")
        mutableUiState.update { state -> state.copy(pendingDelete = state.installed.firstOrNull { it.manifest.modelId == modelId }) }
    }

    fun cancelDelete() {
        Log.i(TAG, "Models UI delete confirmation cancelled.")
        mutableUiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val model = mutableUiState.value.pendingDelete ?: return
        mutableUiState.update {
            it.copy(
                pendingDelete = null,
                validationFeedback = it.validationFeedback - model.manifest.modelId,
            )
        }
        launchOperation("delete") {
            Log.i(TAG, "Models UI delete confirmed: modelId=${model.manifest.modelId.value}, displayName=${model.manifest.displayName}, bytes=${model.totalBytes}")
            modelLibrary.delete(model.manifest.modelId).getOrThrow()
            Log.i(TAG, "Models UI delete completed: modelId=${model.manifest.modelId.value}")
            "${model.manifest.displayName} was deleted."
        }
    }

    fun cancelTransfer(modelId: ModelId) {
        viewModelScope.launch {
            Log.i(TAG, "Models UI transfer cancellation requested: modelId=${modelId.value}")
            modelTransfers.cancelTransfer(modelId)
        }
    }

    private fun launchOperation(operation: String, action: suspend () -> String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(message = null) }
            runCatching { action() }
                .onSuccess { message ->
                    Log.i(TAG, "Models UI operation succeeded: operation=$operation")
                    mutableUiState.update { it.copy(message = message) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Models UI operation failed: operation=$operation, message=${error.message}", error)
                    mutableUiState.update { it.copy(message = error.message ?: "The operation failed.") }
                }
        }
    }

    private companion object {
        const val TAG = "AiP123Models"
    }
}

private val ModelProfileId.displayName: String
    get() = when (this) {
        ModelProfileIds.LLM -> "GGUF chat model"
        ModelProfileIds.WHISPER_STT -> "Whisper STT bundle"
        ModelProfileIds.PARAKEET_CTC_STT -> "Parakeet CTC STT bundle"
        ModelProfileIds.GIGAAM_CTC_STT -> "GigaAM CTC STT bundle"
        ModelProfileIds.ZIPFORMER_STT -> "Zipformer STT bundle"
        ModelProfileIds.SENSE_VOICE_STT -> "SenseVoice STT bundle"
        ModelProfileIds.PARAFORMER_STT -> "Paraformer STT bundle"
        ModelProfileIds.MOONSHINE_STT -> "Moonshine STT bundle"
        ModelProfileIds.VOSK_STT -> "Vosk STT bundle"
        ModelProfileIds.ANDROID_SPEECH_RECOGNIZER_STT -> "Android on-device speech recognizer"
        ModelProfileIds.SUPERTONIC_TTS -> "Supertonic TTS bundle"
        ModelProfileIds.PIPER_VITS_TTS -> "Piper/VITS TTS bundle"
        ModelProfileIds.KOKORO_TTS -> "Kokoro TTS bundle"
        ModelProfileIds.POCKET_TTS -> "Pocket TTS bundle"
        else -> "Model bundle"
    }
