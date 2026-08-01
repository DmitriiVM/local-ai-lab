package com.dmitriim.localaiplayground.feature.chat.domain

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.ai.api.llm.LlmBackend
import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadRequest
import com.dmitriim.localaiplayground.core.model.service.LocalModelResolver
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow

/** Runs one local chat turn and emits progress without imposing presentation state. */
@Inject
class GenerateChatResponse(
    private val modelResolver: LocalModelResolver,
    private val chatEngine: ChatEngine,
) {
    private val promptPreparer = ChatPromptPreparer(chatEngine)

    internal fun execute(request: ChatGenerationRequest): Flow<ChatGenerationEvent> = channelFlow {
        Log.i(
            TAG,
            "Chat generation requested: modelId=${request.modelId.value}, turns=${request.turns.size}, " +
                "contextSize=${request.config.contextSize}, maxOutputTokens=${request.config.maxOutputTokens}, " +
                "temperature=${request.config.temperature}, topK=${request.config.topK}, topP=${request.config.topP}, " +
                "seed=${request.config.seed}, requestedThreads=${request.config.threadCount}",
        )
        try {
            val model = modelResolver.resolveChatModel(request.modelId).getOrThrow()
            Log.i(TAG, "Chat model resolved: name=${model.displayName}, file=${model.modelPath.substringAfterLast('/')}")
            val load = chatEngine.load(
                LlmLoadRequest(
                    profileType = model.profileType,
                    modelPath = model.modelPath,
                    contextSize = request.config.contextSize,
                    threadCount = request.config.threadCount,
                    requestedBackend = LlmBackend.CPU,
                ),
            )
            Log.i(
                TAG,
                "Chat model loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, " +
                    "backend=${load.effectiveBackend}, effectiveThreads=${load.effectiveThreadCount}",
            )
            val prepared = promptPreparer.prepare(request.turns, request.config)
            Log.i(
                TAG,
                "Chat prompt prepared: promptChars=${prepared.prompt.length}, promptTokens=${prepared.usage.promptTokens}, " +
                    "contextSize=${prepared.usage.contextSize}, reservedOutputTokens=${prepared.usage.reservedOutputTokens}, " +
                    "omittedMessages=${prepared.usage.omittedTurnCount}",
            )
            trySend(ChatGenerationEvent.Prepared(prepared.usage))
            var streamedTokenCallbacks = 0
            val generation = chatEngine.generate(
                LlmGenerationRequest(
                    prompt = prepared.prompt,
                    maxTokens = request.config.maxOutputTokens,
                    temperature = request.config.temperature,
                    topK = request.config.topK,
                    topP = request.config.topP,
                    seed = request.config.seed,
                ),
            ) { token ->
                streamedTokenCallbacks += 1
                if (streamedTokenCallbacks == 1) Log.i(TAG, "Chat first streamed token callback received: tokenChars=${token.length}")
                trySend(ChatGenerationEvent.Token(token))
            }
            Log.i(
                TAG,
                "Chat generation completed: callbacks=$streamedTokenCallbacks, outputChars=${generation.text.length}, " +
                    "promptTokens=${generation.promptTokenCount}, generatedTokens=${generation.generatedTokenCount}, " +
                    "firstTokenMs=${generation.firstTokenLatencyMs}, promptMs=${generation.promptDurationMs}, " +
                    "generationMs=${generation.generationDurationMs}, totalMs=${generation.totalDurationMs}, " +
                    "finishReason=${generation.finishReason}",
            )
            trySend(ChatGenerationEvent.Completed(model.displayName, load, generation))
        } catch (error: Throwable) {
            Log.e(TAG, "Chat generation flow failed: ${error.message}", error)
            throw error
        }
    }.buffer(Channel.UNLIMITED)

    private companion object {
        const val TAG = "AiP123Chat"
    }
}
