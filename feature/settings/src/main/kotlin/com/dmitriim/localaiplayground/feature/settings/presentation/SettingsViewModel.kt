package com.dmitriim.localaiplayground.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.device.StorageUsage
import com.dmitriim.localaiplayground.core.model.service.HuggingFaceCredentialStatus
import com.dmitriim.localaiplayground.core.model.service.ModelDownloadCredentials
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import com.dmitriim.localaiplayground.source.settings.AppSettings
import com.dmitriim.localaiplayground.source.settings.AppSettingsRepository
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
class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository,
    private val runRepository: RunRepository,
    private val audioInputStore: AudioInputStore,
    private val generatedAudioStore: GeneratedAudioStore,
    private val downloadCredentials: ModelDownloadCredentials,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.settings.collectLatest { settings -> mutableState.update { it.copy(settings = settings) } } }
        viewModelScope.launch {
            downloadCredentials.huggingFaceCredentialStatus.collectLatest { status ->
                mutableState.update { it.copy(huggingFaceCredentialStatus = status) }
            }
        }
        refreshStorage()
    }

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        settingsRepository.update(transform(mutableState.value.settings))
    }

    fun refreshStorage() = viewModelScope.launch(Dispatchers.IO) {
        mutableState.update { it.copy(storage = runRepository.storageUsage()) }
    }

    fun requestClearRunHistory() = mutableState.update { it.copy(pendingRunHistoryClear = true) }

    fun dismissClearRunHistory() = mutableState.update { it.copy(pendingRunHistoryClear = false) }

    fun clearRunHistory() = viewModelScope.launch(Dispatchers.IO) {
        runRepository.clearRuns()
        mutableState.update { it.copy(pendingRunHistoryClear = false, storage = runRepository.storageUsage()) }
    }

    fun clearTemporaryMedia() = viewModelScope.launch(Dispatchers.IO) {
        audioInputStore.clearAll()
        generatedAudioStore.clearLatest()
        mutableState.update { it.copy(storage = runRepository.storageUsage()) }
    }

    fun requestHuggingFaceToken() = mutableState.update {
        it.copy(showHuggingFaceTokenDialog = true, huggingFaceTokenError = null)
    }

    fun dismissHuggingFaceToken() = mutableState.update {
        it.copy(showHuggingFaceTokenDialog = false, isSavingHuggingFaceToken = false, huggingFaceTokenError = null)
    }

    fun saveHuggingFaceToken(token: String) = viewModelScope.launch {
        mutableState.update { it.copy(isSavingHuggingFaceToken = true, huggingFaceTokenError = null) }
        downloadCredentials.saveHuggingFaceToken(token).fold(
            onSuccess = {
                mutableState.update {
                    it.copy(showHuggingFaceTokenDialog = false, isSavingHuggingFaceToken = false)
                }
            },
            onFailure = { error ->
                mutableState.update {
                    it.copy(
                        isSavingHuggingFaceToken = false,
                        huggingFaceTokenError = error.message ?: "The token could not be saved.",
                    )
                }
            },
        )
    }

    fun clearHuggingFaceToken() = viewModelScope.launch {
        downloadCredentials.clearHuggingFaceToken()
    }
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val storage: StorageUsage = StorageUsage(),
    val pendingRunHistoryClear: Boolean = false,
    val huggingFaceCredentialStatus: HuggingFaceCredentialStatus = HuggingFaceCredentialStatus.MISSING,
    val showHuggingFaceTokenDialog: Boolean = false,
    val isSavingHuggingFaceToken: Boolean = false,
    val huggingFaceTokenError: String? = null,
)
