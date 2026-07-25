package com.dmitriim.localaiplayground.feature.chat.domain

import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.ai.api.LlmBackend
import com.dmitriim.localaiplayground.ai.api.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.LlmGenerationResult
import com.dmitriim.localaiplayground.ai.api.LlmLoadRequest
import com.dmitriim.localaiplayground.ai.api.LlmLoadResult
import com.dmitriim.localaiplayground.core.model.LocalModelResolver
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
        val model = modelResolver.resolveChatModel(request.modelId).getOrThrow()
        val load = chatEngine.load(
            LlmLoadRequest(
                modelPath = model.modelPath,
                contextSize = request.config.contextSize,
                threadCount = request.config.threadCount,
                requestedBackend = LlmBackend.CPU,
            ),
        )
        val prepared = promptPreparer.prepare(request.turns, request.config)
        trySend(ChatGenerationEvent.Prepared(prepared.usage))
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
            trySend(ChatGenerationEvent.Token(token))
        }
        trySend(ChatGenerationEvent.Completed(model.displayName, load, generation))
    }.buffer(Channel.UNLIMITED)
}

internal sealed interface ChatGenerationEvent {
    data class Prepared(val contextUsage: ChatContextUsage) : ChatGenerationEvent
    data class Token(val text: String) : ChatGenerationEvent
    data class Completed(
        val modelName: String,
        val load: LlmLoadResult,
        val generation: LlmGenerationResult,
    ) : ChatGenerationEvent
}
