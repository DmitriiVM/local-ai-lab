package com.dmitriim.localailab.feature.playground.impl.presentation

import com.dmitriim.localailab.ai.api.capability.CapabilityReadiness
import com.dmitriim.localailab.feature.playground.impl.presentation.state.OperationState

data class PlaygroundUiState(
    val operation: OperationState<Unit> = OperationState.Preparing("Checking bundled engines…"),
    val capabilities: List<CapabilityReadiness> = emptyList(),
)
