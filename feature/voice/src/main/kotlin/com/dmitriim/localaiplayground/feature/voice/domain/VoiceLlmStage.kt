package com.dmitriim.localaiplayground.feature.voice.domain

import android.util.Log
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
    Log.i(TAG, "Voice LLM stage loading: model=${model.displayName}, contextSize=${request.contextSize}, requestedThreads=${request.llmThreadCount}")
    val load = engine.load(
        LlmLoadRequest(
            modelPath = model.modelPath,
            contextSize = request.contextSize,
            threadCount = request.llmThreadCount,
            requestedBackend = LlmBackend.CPU,
        ),
    )
    Log.i(TAG, "Voice LLM stage loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, effectiveThreads=${load.effectiveThreadCount}")
    return try {
        val prompt = VoicePromptPreparer(engine).prepare(request, userText)
        Log.i(TAG, "Voice LLM prompt prepared: promptChars=${prompt.value.length}, promptTokens=${prompt.usage.promptTokens}, omittedTurns=${prompt.usage.omittedTurnCount}")
        onPrepared(prompt.usage)
        val streamed = StringBuilder()
        var tokenCallbacks = 0
        val result = engine.generate(
            LlmGenerationRequest(
                prompt = prompt.value,
                maxTokens = request.maxOutputTokens,
                temperature = request.temperature,
            ),
        ) { token ->
            tokenCallbacks++
            if (tokenCallbacks == 1) Log.i(TAG, "Voice LLM first streamed token callback received: tokenChars=${token.length}")
            streamed.append(token)
            onToken(token)
        }
        ensureNotCancelled()
        GeneratedVoiceResponse(
            text = result.text.ifBlank { streamed.toString() }.trim(),
            llmTimeToFirstTokenMs = result.firstTokenLatencyMs,
            llmCompletionDurationMs = result.totalDurationMs,
        ).also { response ->
            Log.i(TAG, "Voice LLM stage completed: tokenCallbacks=$tokenCallbacks, responseLength=${response.text.length}, firstTokenMs=${response.llmTimeToFirstTokenMs}, completionMs=${response.llmCompletionDurationMs}, finishReason=${result.finishReason}")
        }
    } catch (error: Throwable) {
        Log.e(TAG, "Voice LLM stage failed: ${error.message}", error)
        throw error
    } finally {
        engine.unload()
        Log.i(TAG, "Voice LLM stage unloaded.")
    }
}

private class VoicePromptPreparer(private val engine: ChatEngine) {
    fun prepare(request: VoiceTurnRequest, userText: String): PreparedVoicePrompt {
        val history = request.history.toMutableList()
        var omitted = 0
        Log.i(TAG, "Voice LLM prompt preparation started: historyTurns=${history.size}, systemPromptChars=${request.systemPrompt.length}, transcriptLength=${userText.length}")
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
                Log.i(TAG, "Voice LLM prompt fits context: promptTokens=$tokens, reservedOutputTokens=${request.maxOutputTokens}, omittedTurns=$omitted")
                return PreparedVoicePrompt(
                    value = value,
                    usage = VoiceContextUsage(tokens, request.contextSize, request.maxOutputTokens, omitted),
                )
            }
            history.removeAt(0)
            omitted++
            Log.i(TAG, "Voice LLM prompt truncated oldest turn: totalOmitted=$omitted, promptTokens=$tokens")
        }
    }
}

private data class PreparedVoicePrompt(val value: String, val usage: VoiceContextUsage)

private const val TAG = "AiP123Voice"
