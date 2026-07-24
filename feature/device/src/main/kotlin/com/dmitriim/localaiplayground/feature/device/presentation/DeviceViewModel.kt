package com.dmitriim.localaiplayground.feature.device.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.EngineAvailabilitySource
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.result.DomainError
import com.dmitriim.localaiplayground.core.result.DomainErrorCategory
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.core.result.OperationState
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val operationCoordinator: ForegroundOperationCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = mutableState.asStateFlow()
    private var refreshJob: Job? = null
    private var foundationJob: Job? = null

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
                availabilitySource.refresh()
                mutableState.update { it.copy(snapshot = snapshot, refreshing = false) }
            } catch (cancelled: CancellationException) {
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

    fun runFoundationLifecycleCheck() {
        if (foundationJob?.isActive == true) return
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutableState.update {
                it.copy(
                    foundationOperation =
                        OperationState.Preparing("Preparing a placeholder foreground operation…"),
                )
            }
            try {
                mutableState.update {
                    it.copy(
                        foundationOperation = OperationState.Running(
                            message = "Foundation lifecycle check is active.",
                        ),
                    )
                }
                delay(5_000)
                mutableState.update { it.copy(foundationOperation = OperationState.Completed(Unit)) }
            } catch (cancelled: CancellationException) {
                mutableState.update {
                    it.copy(
                        foundationOperation = OperationState.Error(
                            DomainError(
                                category = DomainErrorCategory.CANCELLED,
                                title = "Foreground operation interrupted",
                                explanation = if (cancelled.message == "App moved to the background") {
                                    "The placeholder operation stopped because the app left the foreground. " +
                                        "It is safe to retry."
                                } else {
                                    "The placeholder operation was cancelled. It is safe to retry."
                                },
                                retryable = true,
                            ),
                        ),
                    )
                }
            }
        }
        foundationJob = job
        val registration = operationCoordinator.register {
            job.cancel(CancellationException("App moved to the background"))
        }
        job.invokeOnCompletion { registration.close() }
        job.start()
    }

    fun cancelFoundationLifecycleCheck() {
        if (foundationJob?.isActive != true) return
        mutableState.update {
            it.copy(
                foundationOperation =
                    OperationState.Cancelling("Cancelling the foreground operation…"),
            )
        }
        foundationJob?.cancel(CancellationException("Cancelled by user"))
    }
}
