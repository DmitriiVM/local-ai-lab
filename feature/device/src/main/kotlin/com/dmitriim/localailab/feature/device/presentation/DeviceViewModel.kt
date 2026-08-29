package com.dmitriim.localailab.feature.device.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localailab.ai.api.availability.EngineAvailabilitySource
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelDiagnostics
import com.dmitriim.localailab.core.operation.ForegroundOperationCoordinator
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class DeviceViewModel(
    private val application: Application,
    private val availabilitySource: EngineAvailabilitySource,
    private val modelDiagnostics: ModelDiagnostics,
    private val operationCoordinator: ForegroundOperationCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            availabilitySource.availability.collectLatest { engines ->
                if (engines.isNotEmpty()) {
                    mutableState.update { it.copy(engines = engines) }
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutableState.update { it.copy(refreshing = true, interruptionMessage = null) }
            try {
                val snapshot = withContext(Dispatchers.Default) { readDeviceSnapshot(application) }
                val diagnostics = withContext(Dispatchers.IO) { modelDiagnostics.runDiagnostics() }
                availabilitySource.refresh()
                mutableState.update { it.copy(snapshot = snapshot, diagnostics = diagnostics, refreshing = false) }
            } catch (_: CancellationException) {
                mutableState.update {
                    it.copy(
                        refreshing = false,
                        interruptionMessage = "Refresh stopped when the app left the foreground.",
                    )
                }
            }
        }
        refreshJob = job
        val registration = operationCoordinator.register {
            job.cancel(CancellationException("App moved to the background"))
        }
        job.invokeOnCompletion { registration.close() }
        job.start()
    }
}
