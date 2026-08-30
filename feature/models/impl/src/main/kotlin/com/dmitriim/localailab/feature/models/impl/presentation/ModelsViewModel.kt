package com.dmitriim.localailab.feature.models.impl.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localailab.ai.api.model.library.CatalogDownloadAuthentication
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.models.api.data.HuggingFaceCredentialStatus
import com.dmitriim.localailab.feature.models.api.data.ModelDownloadCredentials
import com.dmitriim.localailab.feature.models.api.data.ModelLibrary
import com.dmitriim.localailab.feature.models.api.data.ModelTransfers
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelDiagnostics
import com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
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
    private val downloadCredentials: ModelDownloadCredentials,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modelLibrary.installedModels,
                modelTransfers.catalog,
                modelTransfers.transfers,
                downloadCredentials.huggingFaceCredentialStatus,
            ) { installed, catalog, transfers, credentialStatus ->
                ModelData(installed, catalog, transfers, credentialStatus)
            }.collect { data ->
                mutableUiState.update { current ->
                    current.copy(
                        isModelDataLoaded = true,
                        installed = data.installed,
                        catalog = data.catalog,
                        transfers = data.transfers,
                        huggingFaceCredentialStatus = data.credentialStatus,
                    )
                }
            }
        }
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
                                "Model compatibility check failed: modelId=${manifest.modelId.value}, " +
                                    "message=${error.message}",
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

    fun download(modelId: ModelId) {
        val entry = mutableUiState.value.catalog.firstOrNull { it.manifest.modelId == modelId }
        if (entry?.download?.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN &&
            mutableUiState.value.huggingFaceCredentialStatus == HuggingFaceCredentialStatus.MISSING
        ) {
            requestHuggingFaceToken(modelId)
            return
        }
        launchOperation("download") {
            Log.i(TAG, "Models UI download requested: modelId=${modelId.value}")
            modelTransfers.download(modelId, ModelTransferNetworkPolicy.WIFI_ONLY).getOrThrow()
            Log.i(TAG, "Models UI download scheduled: modelId=${modelId.value}")
            "Download scheduled."
        }
    }

    fun pauseTransfer(modelId: ModelId) {
        viewModelScope.launch {
            modelTransfers.pauseTransfer(modelId)
            mutableUiState.update { it.copy(message = "Download paused.") }
        }
    }

    fun resumeOnWifi(modelId: ModelId) = resumeTransfer(modelId, ModelTransferNetworkPolicy.WIFI_ONLY)

    fun resumeOnAnyNetwork(modelId: ModelId) = resumeTransfer(modelId, ModelTransferNetworkPolicy.ANY_NETWORK)

    private fun resumeTransfer(
        modelId: ModelId,
        networkPolicy: ModelTransferNetworkPolicy,
    ) = launchOperation("resume download") {
        modelTransfers.resumeTransfer(modelId, networkPolicy).getOrThrow()
        if (networkPolicy == ModelTransferNetworkPolicy.ANY_NETWORK) {
            "Download scheduled using any network."
        } else {
            "Download scheduled on Wi-Fi."
        }
    }

    fun requestHuggingFaceToken(modelId: ModelId) {
        mutableUiState.update {
            it.copy(
                pendingHuggingFaceTokenModelId = modelId,
                huggingFaceTokenError = null,
            )
        }
    }

    fun dismissHuggingFaceToken() {
        mutableUiState.update {
            it.copy(
                pendingHuggingFaceTokenModelId = null,
                isSavingHuggingFaceToken = false,
                huggingFaceTokenError = null,
            )
        }
    }

    fun saveHuggingFaceTokenAndDownload(token: String) {
        val modelId = mutableUiState.value.pendingHuggingFaceTokenModelId ?: return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSavingHuggingFaceToken = true, huggingFaceTokenError = null) }
            val result = downloadCredentials.saveHuggingFaceToken(token)
                .mapCatching {
                    modelTransfers.download(modelId, ModelTransferNetworkPolicy.WIFI_ONLY).getOrThrow()
                }
            result.fold(
                onSuccess = {
                    mutableUiState.update {
                        it.copy(
                            pendingHuggingFaceTokenModelId = null,
                            isSavingHuggingFaceToken = false,
                            message = "Download scheduled.",
                        )
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isSavingHuggingFaceToken = false,
                            huggingFaceTokenError = error.message ?: "The token could not be saved.",
                        )
                    }
                },
            )
        }
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
                    Log.i(
                        TAG,
                        "Models UI validation completed: modelId=${modelId.value}, " +
                            "state=${model.validationState}, bytes=${model.totalBytes}",
                    )
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
        mutableUiState.update { state ->
            state.copy(
                pendingDelete = state.installed.firstOrNull { it.manifest.modelId == modelId },
            )
        }
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
            Log.i(
                TAG,
                "Models UI delete confirmed: modelId=${model.manifest.modelId.value}, " +
                    "displayName=${model.manifest.displayName}, bytes=${model.totalBytes}",
            )
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

private data class ModelData(
    val installed: List<InstalledModel>,
    val catalog: List<CatalogModel>,
    val transfers: Map<ModelId, ModelTransferState>,
    val credentialStatus: HuggingFaceCredentialStatus,
)
