package com.dmitriim.localailab.feature.assistant.impl.domain

import android.util.Log
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.chat.ChatEngine
import com.dmitriim.localailab.ai.api.chat.LlmGenerationOption
import com.dmitriim.localailab.ai.api.chat.LlmGenerationOptions
import com.dmitriim.localailab.ai.api.chat.LlmGenerationRequest
import com.dmitriim.localailab.ai.api.chat.LlmLoadOption
import com.dmitriim.localailab.ai.api.chat.LlmLoadOptions
import com.dmitriim.localailab.ai.api.chat.LlmLoadRequest
import com.dmitriim.localailab.ai.api.profiling.InferencePhase
import com.dmitriim.localailab.ai.api.profiling.InferenceProfiler
import com.dmitriim.localailab.ai.performance.profiling.LightweightInferenceProfiler
import com.dmitriim.localailab.feature.models.api.domain.runtime.LocalModelResolver
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow

private const val CHAT_TAG = "AiP123Chat"

/** Runs one local chat turn and emits progress without imposing presentation state. */
@Inject
class GenerateAssistantResponse(
    private val modelResolver: LocalModelResolver,
    private val chatEngine: ChatEngine,
    private val profiler: InferenceProfiler = LightweightInferenceProfiler,
) {
    private val promptPreparer = ChatPromptPreparer(chatEngine)

    internal fun execute(request: ChatGenerationRequest): Flow<ChatGenerationEvent> = channelFlow {
        Log.i(
            CHAT_TAG,
            "Chat generation requested: modelId=${request.modelId.value}, turns=${request.turns.size}, " +
                "contextSize=${request.config.contextSize}, maxOutputTokens=${request.config.maxOutputTokens}, " +
                "temperature=${request.config.temperature}, topK=${request.config.topK}, " +
                "topP=${request.config.topP}, " +
                "seed=${request.config.seed}, requestedThreads=${request.config.threadCount}",
        )
        val profile = profiler.start(request.runId, AiCapability.CHAT)
        try {
            val model = profile.trace(InferencePhase.MODEL_RESOLUTION) {
                modelResolver.resolveChatModel(request.modelId).getOrThrow()
            }
            val capabilities = requireNotNull(chatEngine.capabilitiesFor(model.engineId)) {
                "No packaged LLM runtime supports ${model.engineId.value}."
            }
            Log.i(CHAT_TAG, "Chat model resolved: name=${model.displayName}, engine=${model.engineId.value}")
            val load = profile.trace(InferencePhase.MODEL_LOAD) {
                chatEngine.load(
                    LlmLoadRequest(
                        model = model,
                        options = LlmLoadOptions(
                            contextSize = request.config.contextSize.takeIf {
                                LlmLoadOption.CONTEXT_SIZE in capabilities.loadOptions
                            },
                            threadCount = request.config.threadCount.takeIf {
                                LlmLoadOption.THREAD_COUNT in capabilities.loadOptions
                            },
                            computePreference = request.config.computePreference,
                        ),
                    ),
                )
            }
            Log.i(
                CHAT_TAG,
                "Chat model loaded: coldStart=${load.coldStart}, loadMs=${load.loadDurationMs}, " +
                    "compute=${load.effectiveComputePreference}, " +
                    "effectiveThreads=${load.diagnostics.effectiveThreadCount}",
            )
            val prepared = profile.trace(InferencePhase.PROMPT_PREPARATION) {
                promptPreparer.prepare(request.turns, request.config, capabilities)
            }
            Log.i(
                CHAT_TAG,
                "Chat prompt prepared: promptChars=${prepared.prompt.length}, " +
                    "promptTokens=${prepared.usage.promptTokens}, " +
                    "contextSize=${prepared.usage.contextSize}, " +
                    "reservedOutputTokens=${prepared.usage.reservedOutputTokens}, " +
                    "omittedMessages=${prepared.usage.omittedTurnCount}",
            )
            trySend(ChatGenerationEvent.Prepared(prepared.usage))
            var streamedTokenCallbacks = 0
            val generation = profile.trace(InferencePhase.DECODE) {
                chatEngine.generate(
                    request = generationRequest(prepared.prompt, request, capabilities),
                    onToken = { token ->
                        streamedTokenCallbacks += 1
                        if (streamedTokenCallbacks == 1) {
                            Log.i(
                                CHAT_TAG,
                                "Chat first streamed token callback received: " +
                                    "tokenChars=${token.length}",
                            )
                        }
                        trySend(ChatGenerationEvent.Token(token))
                    },
                )
            }
            Log.i(
                CHAT_TAG,
                "Chat generation completed: callbacks=$streamedTokenCallbacks, " +
                    "outputChars=${generation.text.length}, " +
                    "promptTokens=${generation.promptTokenCount}, " +
                    "generatedTokens=${generation.generatedTokenCount}, " +
                    "firstTokenMs=${generation.firstTokenLatencyMs}, promptMs=${generation.promptDurationMs}, " +
                    "generationMs=${generation.generationDurationMs}, totalMs=${generation.totalDurationMs}, " +
                    "finishReason=${generation.finishReason}",
            )
            trySend(ChatGenerationEvent.Completed(model.displayName, load, generation, profile.finish()))
        } catch (error: Throwable) {
            Log.e(CHAT_TAG, "Chat generation flow failed: ${error.message}", error)
            throw error
        } finally {
            profile.finish()
        }
    }.buffer(Channel.UNLIMITED)

    private fun generationRequest(
        prompt: String,
        request: ChatGenerationRequest,
        capabilities: com.dmitriim.localailab.ai.api.chat.LlmEngineCapabilities,
    ) = LlmGenerationRequest(
        prompt = prompt,
        options = LlmGenerationOptions(
            maxTokens = request.config.maxOutputTokens.takeIf {
                LlmGenerationOption.MAX_OUTPUT_TOKENS in capabilities.generationOptions
            },
            temperature = request.config.temperature.takeIf {
                LlmGenerationOption.TEMPERATURE in capabilities.generationOptions
            },
            topK = request.config.topK.takeIf { LlmGenerationOption.TOP_K in capabilities.generationOptions },
            topP = request.config.topP.takeIf { LlmGenerationOption.TOP_P in capabilities.generationOptions },
            seed = request.config.seed.takeIf { LlmGenerationOption.SEED in capabilities.generationOptions },
        ),
    )
}
