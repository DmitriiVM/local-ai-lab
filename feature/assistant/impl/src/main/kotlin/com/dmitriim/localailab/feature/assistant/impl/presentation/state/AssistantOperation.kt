package com.dmitriim.localailab.feature.assistant.impl.presentation.state

sealed interface AssistantOperation {
    data object Idle : AssistantOperation
    data object Recording : AssistantOperation
    data object Transcribing : AssistantOperation
    data object Loading : AssistantOperation
    data object Generating : AssistantOperation
    data object Speaking : AssistantOperation
    data object Cancelling : AssistantOperation
}
