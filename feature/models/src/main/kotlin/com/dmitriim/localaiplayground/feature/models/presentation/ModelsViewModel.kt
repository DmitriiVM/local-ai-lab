package com.dmitriim.localaiplayground.feature.models.presentation

import androidx.lifecycle.ViewModel
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ModelsViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = mutableUiState.asStateFlow()
}
