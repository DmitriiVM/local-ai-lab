package com.dmitriim.localaiplayground.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.core.audio.input.storage.AudioInputStore
import com.dmitriim.localaiplayground.core.audio.output.storage.GeneratedAudioStore
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.RunRepository
import com.dmitriim.localaiplayground.core.model.StorageUsage
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
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.settings.collectLatest { settings -> mutableState.update { it.copy(settings = settings) } } }
        refreshStorage()
    }

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        settingsRepository.update(transform(mutableState.value.settings))
    }

    fun refreshStorage() = viewModelScope.launch(Dispatchers.IO) {
        mutableState.update { it.copy(storage = runRepository.storageUsage()) }
    }

    fun requestClearHistory() = mutableState.update { it.copy(pendingHistoryClear = true) }

    fun dismissClearHistory() = mutableState.update { it.copy(pendingHistoryClear = false) }

    fun clearHistory() = viewModelScope.launch(Dispatchers.IO) {
        runRepository.clearHistory()
        mutableState.update { it.copy(pendingHistoryClear = false, storage = runRepository.storageUsage()) }
    }

    fun clearTemporaryMedia() = viewModelScope.launch(Dispatchers.IO) {
        audioInputStore.clearAll()
        generatedAudioStore.clearLatest()
        mutableState.update { it.copy(storage = runRepository.storageUsage()) }
    }
}

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val storage: StorageUsage = StorageUsage(),
    val pendingHistoryClear: Boolean = false,
)
