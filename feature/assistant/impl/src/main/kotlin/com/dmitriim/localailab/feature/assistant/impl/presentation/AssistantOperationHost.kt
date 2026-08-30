package com.dmitriim.localailab.feature.assistant.impl.presentation

import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal interface AssistantOperationHost {
    val scope: CoroutineScope
    val state: MutableStateFlow<AssistantUiState>
    val activeLinkedRunIds: MutableList<String>

    fun launchForeground(block: suspend () -> Unit)

    fun handleOperationFailure(error: Throwable)
}
