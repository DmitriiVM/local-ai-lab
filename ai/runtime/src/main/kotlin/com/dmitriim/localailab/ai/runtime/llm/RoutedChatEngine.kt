package com.dmitriim.localailab.ai.runtime.llm

import com.dmitriim.localailab.ai.api.chat.ChatEngine
import com.dmitriim.localailab.ai.api.chat.ChatRuntime
import com.dmitriim.localailab.ai.api.chat.LlmChatFormatter
import com.dmitriim.localailab.ai.api.chat.LlmChatTemplateHandling
import com.dmitriim.localailab.ai.api.chat.LlmContextManagement
import com.dmitriim.localailab.ai.api.chat.LlmEngineCapabilities
import com.dmitriim.localailab.ai.api.chat.LlmGenerationOption
import com.dmitriim.localailab.ai.api.chat.LlmGenerationRequest
import com.dmitriim.localailab.ai.api.chat.LlmGenerationResult
import com.dmitriim.localailab.ai.api.chat.LlmLoadOption
import com.dmitriim.localailab.ai.api.chat.LlmLoadRequest
import com.dmitriim.localailab.ai.api.chat.LlmLoadResult
import com.dmitriim.localailab.ai.api.chat.LlmTokenCounter
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.engine.EngineId
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Selects the runtime declared by a model reference and owns the active runtime lifetime. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoutedChatEngine(runtimes: Set<ChatRuntime>) : ChatEngine {
    private val lock = Any()
    private val byEngineId = runtimes.associateBy(ChatRuntime::engineId).also { indexed ->
        require(indexed.size == runtimes.size) {
            "More than one LLM runtime declares the same engine ID."
        }
        runtimes.forEach(::validateOptionalOperations)
    }
    private var active: ChatRuntime? = null

    override val isLoaded: Boolean
        get() = synchronized(lock) { active?.isLoaded == true }

    override fun capabilitiesFor(engineId: EngineId): LlmEngineCapabilities? = byEngineId[engineId]?.capabilities

    override fun activeChatFormatter(): LlmChatFormatter? = activeRuntime() as? LlmChatFormatter

    override fun activeTokenCounter(): LlmTokenCounter? = activeRuntime() as? LlmTokenCounter

    override fun load(request: LlmLoadRequest): LlmLoadResult {
        val runtime = synchronized(lock) {
            val selected = requireNotNull(byEngineId[request.model.engineId]) {
                "No packaged LLM runtime supports ${request.model.engineId.value}."
            }
            if (active !== selected) {
                active?.cancel()
                active?.unload()
                active = selected
            }
            selected
        }
        return runtime.load(request)
    }

    override fun generate(
        request: LlmGenerationRequest,
        onToken: (String) -> Unit,
    ): LlmGenerationResult = activeRuntime().generate(request, onToken)

    override fun cancel() {
        synchronized(lock) { active }?.cancel()
    }

    override fun unload() {
        synchronized(lock) {
            active?.cancel()
            active?.unload()
            active = null
        }
    }

    private fun activeRuntime(): ChatRuntime = synchronized(lock) {
        checkNotNull(active) { "Load a chat model before using the LLM runtime." }
    }

    private fun validateOptionalOperations(runtime: ChatRuntime) {
        if (runtime.capabilities.chatTemplateHandling ==
            LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES
        ) {
            require(runtime is LlmChatFormatter) {
                "${runtime.engineId.value} declares engine chat formatting but does not implement LlmChatFormatter."
            }
        }
        if (runtime.capabilities.tokenCounting) {
            require(runtime is LlmTokenCounter) {
                "${runtime.engineId.value} declares token counting but does not implement LlmTokenCounter."
            }
        }
        when (runtime.capabilities.contextManagement) {
            LlmContextManagement.EXACT_CALLER_BUDGET -> require(
                runtime.capabilities.tokenCounting,
            ) {
                "${runtime.engineId.value} declares exact caller context budgeting without token counting."
            }
            LlmContextManagement.ESTIMATED_CALLER_BUDGET,
            LlmContextManagement.RUNTIME_MANAGED,
            -> Unit
        }
        if (runtime.capabilities.contextManagement != LlmContextManagement.RUNTIME_MANAGED) {
            require(LlmLoadOption.CONTEXT_SIZE in runtime.capabilities.loadOptions) {
                "${runtime.engineId.value} declares caller context budgeting without a context-size control."
            }
            require(
                LlmGenerationOption.MAX_OUTPUT_TOKENS in runtime.capabilities.generationOptions,
            ) {
                "${runtime.engineId.value} declares caller context budgeting without a maximum-output control."
            }
        }
    }
}
