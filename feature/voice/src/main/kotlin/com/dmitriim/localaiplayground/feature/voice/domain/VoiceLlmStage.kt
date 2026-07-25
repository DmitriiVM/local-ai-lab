package com.dmitriim.localaiplayground.feature.voice.domain

import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.ai.api.LlmBackend
import com.dmitriim.localaiplayground.ai.api.LlmChatMessage
import com.dmitriim.localaiplayground.ai.api.LlmChatRole
import com.dmitriim.localaiplayground.ai.api.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.LlmLoadRequest
import com.dmitriim.localaiplayground.core.model.ChatModelReference

internal fun generateVoiceResponse(
    engine: ChatEngine,
    request: VoiceTurnRequest,
    model: ChatModelReference,
    userText: String,
    onPrepared: (VoiceContextUsage) -> Unit,
    onToken: (String) -> Unit,
    ensureNotCancelled: () -> Unit,
): GeneratedVoiceResponse {
    engine.load(
        LlmLoadRequest(
            modelPath = model.modelPath,
            contextSize = request.contextSize,
            threadCount = request.llmThreadCount,
            requestedBackend = LlmBackend.CPU,
        ),
    )
    return try {
        val prompt = VoicePromptPreparer(engine).prepare(request, userText)
        onPrepared(prompt.usage)
        val streamed = StringBuilder()
        val result = engine.generate(
            LlmGenerationRequest(
                prompt = prompt.value,
                maxTokens = request.maxOutputTokens,
                temperature = request.temperature,
            ),
        ) { token ->
            streamed.append(token)
            onToken(token)
        }
        ensureNotCancelled()
        GeneratedVoiceResponse(
            text = result.text.ifBlank { streamed.toString() }.trim(),
            llmTimeToFirstTokenMs = result.firstTokenLatencyMs,
            llmCompletionDurationMs = result.totalDurationMs,
        )
    } finally {
        engine.unload()
    }
}

private class VoicePromptPreparer(private val engine: ChatEngine) {
    fun prepare(request: VoiceTurnRequest, userText: String): PreparedVoicePrompt {
        val history = request.history.toMutableList()
        var omitted = 0
        while (true) {
            val messages = buildList {
                if (request.systemPrompt.isNotBlank()) add(LlmChatMessage(LlmChatRole.SYSTEM, request.systemPrompt))
                history.forEach { turn ->
                    add(LlmChatMessage(LlmChatRole.USER, turn.userText))
                    add(LlmChatMessage(LlmChatRole.ASSISTANT, turn.assistantText))
                }
                add(LlmChatMessage(LlmChatRole.USER, userText))
            }
            val value = engine.format(messages)
            val tokens = engine.countTokens(value)
            if (tokens + request.maxOutputTokens <= request.contextSize || history.isEmpty()) {
                require(tokens + request.maxOutputTokens <= request.contextSize) {
                    "The system prompt and current transcript exceed the selected context size. Increase context or shorten the prompt."
                }
                return PreparedVoicePrompt(
                    value = value,
                    usage = VoiceContextUsage(tokens, request.contextSize, request.maxOutputTokens, omitted),
                )
            }
            history.removeAt(0)
            omitted++
        }
    }
}

private data class PreparedVoicePrompt(val value: String, val usage: VoiceContextUsage)
