package com.dmitriim.localaiplayground.feature.playground.presentation

import com.dmitriim.localaiplayground.core.model.capability.CapabilityReadiness
import com.dmitriim.localaiplayground.core.result.OperationState

data class PlaygroundUiState(
    val operation: OperationState<Unit> = OperationState.Preparing("Checking bundled engines…"),
    val capabilities: List<CapabilityReadiness> = emptyList(),
)
