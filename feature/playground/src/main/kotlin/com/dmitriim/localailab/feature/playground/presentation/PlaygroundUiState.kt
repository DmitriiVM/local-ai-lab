package com.dmitriim.localailab.feature.playground.presentation

import com.dmitriim.localailab.core.model.capability.CapabilityReadiness
import com.dmitriim.localailab.core.result.OperationState

data class PlaygroundUiState(
    val operation: OperationState<Unit> = OperationState.Preparing("Checking bundled engines…"),
    val capabilities: List<CapabilityReadiness> = emptyList(),
)
