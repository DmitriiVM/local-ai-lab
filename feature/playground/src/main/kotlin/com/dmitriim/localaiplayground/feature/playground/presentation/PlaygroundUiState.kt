package com.dmitriim.localaiplayground.feature.playground.presentation

import com.dmitriim.localaiplayground.core.model.CapabilityReadiness
import com.dmitriim.localaiplayground.core.result.OperationState
import com.dmitriim.localaiplayground.core.model.RunRecord

data class PlaygroundUiState(
    val operation: OperationState<Unit> = OperationState.Preparing("Checking bundled engines…"),
    val capabilities: List<CapabilityReadiness> = emptyList(),
    val recentRuns: List<RunRecord> = emptyList(),
)
