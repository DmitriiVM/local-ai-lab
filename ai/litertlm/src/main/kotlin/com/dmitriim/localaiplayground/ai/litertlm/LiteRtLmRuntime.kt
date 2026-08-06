package com.dmitriim.localaiplayground.ai.litertlm

import android.content.Context
import android.util.Log
import androidx.tracing.Trace
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatFormatter
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatMessage
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatRole
import com.dmitriim.localaiplayground.ai.api.llm.LlmChatTemplateHandling
import com.dmitriim.localaiplayground.ai.api.llm.LlmContextManagement
import com.dmitriim.localaiplayground.ai.api.llm.LlmEngineCapabilities
import com.dmitriim.localaiplayground.ai.api.llm.LlmFinishReason
import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationOption
import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.llm.LlmGenerationResult
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadOption
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadRequest
import com.dmitriim.localaiplayground.ai.api.llm.LlmLoadResult
import com.dmitriim.localaiplayground.ai.api.llm.LlmRuntime
import com.dmitriim.localaiplayground.ai.api.llm.LlmRuntimeDiagnostics
import com.dmitriim.localaiplayground.core.model.engine.ComputePreference
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileRoles
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import com.dmitriim.localaiplayground.core.model.runtime.ChatModelReference
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/** LiteRT-LM Kotlin-SDK runtime. It owns one engine and creates a conversation per app chat turn. */
class LiteRtLmRuntime(context: Context) :
    LlmRuntime,
    LlmChatFormatter {
    private val applicationContext = context.applicationContext
    private val lock = ReentrantLock()
    private var engine: Engine? = null
    private var activeRequest: LlmLoadRequest? = null
    private var effectiveComputePreference = ComputePreference.CPU
    private var effectiveThreadCount: Int? = null

    @Volatile
    private var activeConversation: Conversation? = null

    override var isLoaded: Boolean = false
        private set

    override val engineId = EngineId("litert-lm")

    override val capabilities = LlmEngineCapabilities(
        computePreferences = setOf(ComputePreference.CPU, ComputePreference.GPU),
        streaming = true,
        cancellation = true,
        tokenCounting = false,
        chatTemplateHandling = LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES,
        systemInstructions = true,
        contextManagement = LlmContextManagement.ESTIMATED_CALLER_BUDGET,
        loadOptions = setOf(LlmLoadOption.CONTEXT_SIZE, LlmLoadOption.THREAD_COUNT),
        generationOptions = setOf(
            LlmGenerationOption.MAX_OUTPUT_TOKENS,
            LlmGenerationOption.TEMPERATURE,
            LlmGenerationOption.TOP_K,
            LlmGenerationOption.TOP_P,
            LlmGenerationOption.SEED,
        ),
    )

    override fun load(request: LlmLoadRequest): LlmLoadResult = lock.withLock {
        val reference = request.model
        require(reference.engineId == engineId) {
            "Unsupported LLM engine: ${reference.engineId.value}"
        }
        require(reference.profileType == ModelProfileIds.LLM) {
            "Unsupported chat profile: ${reference.profileType.value}"
        }
        require(reference is ChatModelReference.ArtifactBacked) {
            "The LiteRT-LM runtime requires an artifact-backed model."
        }
        val artifact = requireNotNull(
            reference.artifacts.firstOrNull {
                it.role == ModelFileRoles.PRIMARY_MODEL &&
                    !it.directory
            },
        ) { "The LiteRT-LM model does not declare a primary model file." }
        val model = File(artifact.path)
        require(model.isFile && model.canRead()) { "Model file is not readable: ${model.name}" }
        require(model.extension.equals("litertlm", ignoreCase = true)) {
            "LiteRT-LM requires a .litertlm model bundle."
        }
        val contextSize = request.options.contextSize
        require(contextSize == null || contextSize > 0) { "Context size must be positive." }
        val requestedThreads = request.options.threadCount
        require(requestedThreads == null || requestedThreads >= 0) {
            "Thread count cannot be negative."
        }
        val backend = backendFor(request.options.computePreference, requestedThreads)
        if (isLoaded && activeRequest == request) {
            return LlmLoadResult(
                effectiveComputePreference = effectiveComputePreference,
                loadDurationMs = 0,
                coldStart = false,
                diagnostics = runtimeDiagnostics(),
            )
        }

        val coldStart = !isLoaded
        closeEngine()
        val newEngine = Engine(
            EngineConfig(
                modelPath = model.absolutePath,
                backend = backend,
                maxNumTokens = contextSize,
                cacheDir = File(
                    applicationContext.cacheDir,
                    CACHE_DIRECTORY,
                ).also(File::mkdirs).absolutePath,
            ),
        )
        val durationMs = try {
            measureTimeMillis { newEngine.initialize() }
        } catch (error: Throwable) {
            if (newEngine.isInitialized()) runCatching { newEngine.close() }
            Log.e(TAG, "LiteRT-LM model load failed: ${error.message}", error)
            throw error
        }
        engine = newEngine
        activeRequest = request
        isLoaded = true
        Log.i(
            TAG,
            "LiteRT-LM model loaded: model=${model.name}, loadMs=$durationMs, compute=$effectiveComputePreference, " +
                "effectiveThreads=$effectiveThreadCount",
        )
        LlmLoadResult(
            effectiveComputePreference = effectiveComputePreference,
            loadDurationMs = durationMs,
            coldStart = coldStart,
            diagnostics = runtimeDiagnostics(),
        )
    }

    override fun format(messages: List<LlmChatMessage>): String = lock.withLock {
        check(isLoaded) { "Load a model before formatting a chat prompt." }
        LiteRtLmPromptCodec.encode(messages)
    }

    override fun generate(
        request: LlmGenerationRequest,
        onToken: (String) -> Unit,
    ): LlmGenerationResult = lock.withLock {
        val loadedEngine = checkNotNull(engine) { "Load a model before generating text." }
        val messages = LiteRtLmPromptCodec.decode(request.prompt)
        val latestUserIndex = messages.indexOfLast { it.role == LlmChatRole.USER }
        require(latestUserIndex >= 0) { "A LiteRT-LM chat prompt needs a user message." }
        require(messages.drop(latestUserIndex + 1).isEmpty()) {
            "The latest LiteRT-LM chat message must be from the user."
        }
        val systemInstruction = messages.firstOrNull { it.role == LlmChatRole.SYSTEM }?.content
        val history = messages
            .filterIndexed { index, message ->
                index < latestUserIndex &&
                    message.role != LlmChatRole.SYSTEM
            }
            .map { message ->
                when (message.role) {
                    LlmChatRole.USER -> Message.user(message.content)
                    LlmChatRole.ASSISTANT -> Message.model(message.content)
                    LlmChatRole.SYSTEM -> error(
                        "System instructions must precede conversation history.",
                    )
                }
            }
        val options = request.options
        val maxTokens = options.maxTokens
        require(maxTokens == null || maxTokens > 0) { "Maximum output tokens must be positive." }
        val samplerConfig = samplerConfig(request)
        val conversation = loadedEngine.createConversation(
            ConversationConfig(
                systemInstruction = systemInstruction?.takeIf(
                    String::isNotBlank,
                )?.let(Contents::of),
                initialMessages = history,
                samplerConfig = samplerConfig,
                maxOutputToken = maxTokens,
            ),
        )
        activeConversation = conversation
        try {
            generateInConversation(
                conversation = conversation,
                userMessage = messages[latestUserIndex].content,
                maxTokens = maxTokens,
                onToken = onToken,
            )
        } finally {
            activeConversation = null
            conversation.close()
        }
    }

    override fun cancel() {
        Log.i(TAG, "LiteRT-LM cancellation requested.")
        activeConversation?.let { conversation -> runCatching { conversation.cancelProcess() } }
    }

    override fun unload() = lock.withLock { closeEngine() }

    private fun generateInConversation(
        conversation: Conversation,
        userMessage: String,
        maxTokens: Int?,
        onToken: (String) -> Unit,
    ): LlmGenerationResult {
        require(userMessage.isNotBlank()) { "Prompt must not be empty." }
        val output = StringBuilder()
        val outputLock = Any()
        val done = CountDownLatch(1)
        val error = AtomicReference<Throwable?>(null)
        val cancelled = AtomicBoolean(false)
        val startedNanos = System.nanoTime()
        val firstTokenNanos = AtomicLong(0)

        Trace.beginSection("LocalAiPlayground:litert-lm-runtime")
        try {
            conversation.sendMessageAsync(
                text = userMessage,
                callback = object : MessageCallback {
                    override fun onMessage(message: Message) {
                        val chunk = message.toString()
                        if (chunk.isEmpty()) return
                        firstTokenNanos.compareAndSet(0, System.nanoTime())
                        synchronized(outputLock) { output.append(chunk) }
                        onToken(chunk)
                    }

                    override fun onDone() = done.countDown()

                    override fun onError(throwable: Throwable) {
                        if (throwable is java.util.concurrent.CancellationException) {
                            cancelled.set(true)
                        } else {
                            error.set(throwable)
                        }
                        done.countDown()
                    }
                },
                maxOutputToken = maxTokens,
            )
            try {
                done.await()
            } catch (interrupted: InterruptedException) {
                runCatching { conversation.cancelProcess() }
                Thread.currentThread().interrupt()
                throw interrupted
            }
        } finally {
            Trace.endSection()
        }
        error.get()?.let { throw it }
        val finishedNanos = System.nanoTime()
        val totalDurationMs = (finishedNanos - startedNanos) / NANOS_PER_MILLISECOND
        val firstTokenLatencyMs = firstTokenNanos.get()
            .takeIf { it > 0 }
            ?.let { (it - startedNanos) / NANOS_PER_MILLISECOND }
        val text = synchronized(outputLock) { output.toString() }
        return LlmGenerationResult(
            text = text,
            promptTokenCount = null,
            generatedTokenCount = null,
            firstTokenLatencyMs = firstTokenLatencyMs,
            promptDurationMs = firstTokenLatencyMs ?: totalDurationMs,
            generationDurationMs = (totalDurationMs - (firstTokenLatencyMs ?: 0)).coerceAtLeast(0),
            totalDurationMs = totalDurationMs,
            finishReason = if (cancelled.get()) LlmFinishReason.CANCELLED else LlmFinishReason.STOP_TOKEN,
        )
    }

    private fun samplerConfig(request: LlmGenerationRequest): SamplerConfig? {
        val options = request.options
        if (options.temperature == null &&
            options.topK == null &&
            options.topP == null &&
            options.seed == null
        ) {
            return null
        }
        return SamplerConfig(
            topK = options.topK ?: DEFAULT_TOP_K,
            topP = (options.topP ?: DEFAULT_TOP_P).toDouble(),
            temperature = (options.temperature ?: DEFAULT_TEMPERATURE).toDouble(),
            seed = options.seed ?: DEFAULT_SEED,
        )
    }

    private fun backendFor(preference: ComputePreference, requestedThreads: Int?): Backend = when (preference) {
        ComputePreference.AUTO,
        ComputePreference.CPU,
        -> Backend.CPU(threadCount = requestedThreads?.takeIf { it > 0 }).also {
            effectiveComputePreference = ComputePreference.CPU
            effectiveThreadCount = requestedThreads?.takeIf { it > 0 }
        }
        ComputePreference.GPU -> Backend.GPU().also {
            effectiveComputePreference = ComputePreference.GPU
            effectiveThreadCount = null
        }
        ComputePreference.NPU -> error(
            "This build does not bundle the vendor NPU libraries required by LiteRT-LM.",
        )
        ComputePreference.SYSTEM_SERVICE -> error(
            "LiteRT-LM does not use an Android system service backend.",
        )
    }

    private fun closeEngine() {
        activeConversation?.let { conversation -> runCatching { conversation.cancelProcess() } }
        activeConversation = null
        engine?.let { loadedEngine ->
            if (loadedEngine.isInitialized()) runCatching { loadedEngine.close() }
        }
        engine = null
        activeRequest = null
        isLoaded = false
    }

    private fun runtimeDiagnostics() = LlmRuntimeDiagnostics(
        computeDetail = "LiteRT-LM ${effectiveComputePreference.name}",
        effectiveThreadCount = effectiveThreadCount,
        fallbackReason = activeRequest?.options?.computePreference
            ?.takeIf { it == ComputePreference.AUTO }
            ?.let { "Automatic compute currently selects the broadly compatible CPU backend." },
    )

    private companion object {
        const val TAG = "AiP123Chat"
        const val CACHE_DIRECTORY = "litert-lm"
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.95f
        const val DEFAULT_TEMPERATURE = 0.8f
        const val DEFAULT_SEED = 0
        const val NANOS_PER_MILLISECOND = 1_000_000
    }
}
