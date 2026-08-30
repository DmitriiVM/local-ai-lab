package com.dmitriim.localailab.feature.assistant.impl.presentation

import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantInputMode
import com.dmitriim.localailab.feature.assistant.impl.presentation.state.AssistantUiState
import com.dmitriim.localailab.feature.runs.api.domain.history.RunRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object AssistantReplayRestorer {
    fun restore(state: AssistantUiState, run: RunRecord): AssistantUiState {
        val parameters = runCatching { Json.parseToJsonElement(run.parametersJson).jsonObject }.getOrNull()
        val modelId = run.model?.modelId?.let(::ModelId)
        return state.copy(
            selectedChatModelId = modelId?.takeIf { candidate ->
                state.chatModels.any { it.id == candidate && it.installed }
            } ?: state.selectedChatModelId,
            inputMode = if (run.capability == AiCapability.VOICE_ASSISTANT && state.voiceConfigurationError == null) {
                AssistantInputMode.VOICE
            } else {
                AssistantInputMode.DICTATE
            },
            input = run.input.orEmpty(),
            chatSettings = state.chatSettings.copy(
                computePreference = parameters?.get("computePreference")?.jsonPrimitive?.content
                    ?.let { stored -> ComputePreference.entries.firstOrNull { it.name == stored } }
                    ?: state.chatSettings.computePreference,
                systemPrompt = parameters?.get("systemPrompt")?.jsonPrimitive?.content
                    ?: state.chatSettings.systemPrompt,
                temperature = parameters?.get("temperature")?.jsonPrimitive?.content ?: state.chatSettings.temperature,
                topK = parameters?.get("topK")?.jsonPrimitive?.content ?: state.chatSettings.topK,
                topP = parameters?.get("topP")?.jsonPrimitive?.content ?: state.chatSettings.topP,
                maxOutputTokens = parameters?.get("maxOutputTokens")?.jsonPrimitive?.content
                    ?: state.chatSettings.maxOutputTokens,
                seed = parameters?.get("seed")?.jsonPrimitive?.contentOrNull?.takeUnless { it == "-1" } ?: "",
                contextSize = parameters?.get("contextSize")?.jsonPrimitive?.content ?: state.chatSettings.contextSize,
                threadCount = parameters?.get("threadCount")?.jsonPrimitive?.content ?: state.chatSettings.threadCount,
            ),
        )
    }
}
