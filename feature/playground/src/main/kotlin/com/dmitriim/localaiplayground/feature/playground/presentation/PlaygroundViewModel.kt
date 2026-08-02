package com.dmitriim.localaiplayground.feature.playground.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmitriim.localaiplayground.ai.api.availability.EngineAvailabilitySource
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.service.ModelLibrary
import com.dmitriim.localaiplayground.core.result.DomainError
import com.dmitriim.localaiplayground.core.result.DomainErrorCategory
import com.dmitriim.localaiplayground.core.result.ForegroundOperationCoordinator
import com.dmitriim.localaiplayground.core.result.OperationState
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class PlaygroundViewModel(
    private val availabilitySource: EngineAvailabilitySource,
    private val modelLibrary: ModelLibrary,
    private val operationCoordinator: ForegroundOperationCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlaygroundUiState())
    val state: StateFlow<PlaygroundUiState> = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            combine(availabilitySource.availability, modelLibrary.installedModels) { availability, installed ->
                availability to installed
            }.collectLatest { (availability, installed) ->
                if (availability.isNotEmpty()) {
                    mutableState.update {
                        it.copy(
                            operation = OperationState.Completed(Unit),
                            capabilities = buildCapabilityReadiness(availability, installed),
                        )
                    }
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return

        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutableState.update {
                it.copy(operation = OperationState.Preparing("Checking bundled engines…"))
            }
            try {
                availabilitySource.refresh()
            } catch (_: CancellationException) {
                mutableState.update {
                    it.copy(
                        operation = OperationState.Error(
                            DomainError(
                                category = DomainErrorCategory.CANCELLED,
                                title = "Availability check interrupted",
                                explanation = "The check stopped when the app left the foreground.",
                                suggestedAction = "Return to the app and retry.",
                                retryable = true,
                            ),
                        ),
                    )
                }
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        operation = OperationState.Error(
                            DomainError(
                                category = DomainErrorCategory.UNEXPECTED,
                                title = "Availability check failed",
                                explanation = "The bundled engine status could not be read.",
                                technicalDetails = error.message,
                                retryable = true,
                            ),
                        ),
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
