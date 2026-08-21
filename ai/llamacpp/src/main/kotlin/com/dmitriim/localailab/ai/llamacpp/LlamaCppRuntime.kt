package com.dmitriim.localailab.ai.llamacpp

import android.content.Context
import android.util.Log
import com.dmitriim.localailab.ai.api.llm.LlmChatFormatter
import com.dmitriim.localailab.ai.api.llm.LlmChatMessage
import com.dmitriim.localailab.ai.api.llm.LlmChatTemplateHandling
import com.dmitriim.localailab.ai.api.llm.LlmContextManagement
import com.dmitriim.localailab.ai.api.llm.LlmEngineCapabilities
import com.dmitriim.localailab.ai.api.llm.LlmFinishReason
import com.dmitriim.localailab.ai.api.llm.LlmGenerationOption
import com.dmitriim.localailab.ai.api.llm.LlmGenerationRequest
import com.dmitriim.localailab.ai.api.llm.LlmGenerationResult
import com.dmitriim.localailab.ai.api.llm.LlmLoadOption
import com.dmitriim.localailab.ai.api.llm.LlmLoadRequest
import com.dmitriim.localailab.ai.api.llm.LlmLoadResult
import com.dmitriim.localailab.ai.api.llm.LlmRuntime
import com.dmitriim.localailab.ai.api.llm.LlmRuntimeDiagnostics
import com.dmitriim.localailab.ai.api.llm.LlmTokenCounter
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.engine.ComputePreference
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ChatModelReference
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/** JNI-backed llama.cpp engine. It owns one model/context and serializes native access. */
@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<LlmRuntime>())
class LlamaCppRuntime(context: Context) :
    LlmRuntime,
    LlmChatFormatter,
    LlmTokenCounter {
    private val native = NativeBridge(context.applicationInfo.nativeLibraryDir)
    private val lock = ReentrantLock()
    private var activeRequest: LlmLoadRequest? = null

    override var isLoaded: Boolean = false
        private set

    override val engineId = EngineId("llama.cpp")

    override val capabilities = LlmEngineCapabilities(
        computePreferences = setOf(ComputePreference.CPU),
        streaming = true,
        cancellation = true,
        tokenCounting = true,
        chatTemplateHandling = LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES,
        systemInstructions = true,
        contextManagement = LlmContextManagement.EXACT_CALLER_BUDGET,
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
            "The llama.cpp runtime requires an artifact-backed model."
        }
        val artifact = requireNotNull(
            reference.artifacts.firstOrNull {
                it.role == ModelFileRoles.PRIMARY_MODEL &&
                    !it.directory
            },
        ) { "The llama.cpp model does not declare a primary model file." }
        val contextSize = request.options.contextSize ?: DEFAULT_CONTEXT_SIZE
        val threadCount = request.options.threadCount ?: DEFAULT_THREAD_COUNT
        val computePreference = request.options.computePreference
        Log.i(
            TAG,
            "llama.cpp load requested: model=${File(
                artifact.path,
            ).name}, contextSize=$contextSize, requestedThreads=$threadCount, compute=$computePreference",
        )
        require(
            computePreference == ComputePreference.AUTO ||
                computePreference == ComputePreference.CPU,
        ) {
            "The llama.cpp runtime supports CPU compute only; requested $computePreference."
        }
        require(contextSize >= 128) { "Context size must be at least 128 tokens." }
        require(threadCount >= 0) { "Thread count cannot be negative." }
        val model = File(artifact.path)
        require(model.isFile && model.canRead()) { "Model file is not readable: ${model.name}" }
        if (isLoaded && activeRequest == request) {
            Log.i(TAG, "llama.cpp model reuse: effectiveThreads=${native.nativeEffectiveThreads()}")
            return LlmLoadResult(
                effectiveComputePreference = ComputePreference.CPU,
                loadDurationMs = 0,
                coldStart = false,
                diagnostics = runtimeDiagnostics(),
            )
        }
        val coldStart = !isLoaded
        val durationMs = try {
            measureTimeMillis {
                native.requireSuccess(
                    native.nativeLoad(model.absolutePath, contextSize, threadCount),
                )
            }
        } catch (error: Throwable) {
            Log.e(TAG, "llama.cpp model load failed: ${error.message}", error)
            throw error
        }
        isLoaded = true
        activeRequest = request
        Log.i(
            TAG,
            "llama.cpp model loaded: coldStart=$coldStart, loadMs=$durationMs, effectiveThreads=${native.nativeEffectiveThreads()}",
        )
        LlmLoadResult(
            effectiveComputePreference = ComputePreference.CPU,
            loadDurationMs = durationMs,
            coldStart = coldStart,
            diagnostics = runtimeDiagnostics(),
        )
    }

    override fun format(messages: List<LlmChatMessage>): String = lock.withLock {
        check(isLoaded) { "Load a model before formatting a chat prompt." }
        require(messages.isNotEmpty()) { "A chat prompt needs at least one message." }
        try {
            native.nativeFormatChat(
                messages.map { it.role.wireName }.toTypedArray(),
                messages.map { it.content }.toTypedArray(),
            ).also { formatted ->
                check(!formatted.startsWith("ERROR:")) { formatted.removePrefix("ERROR:") }
                Log.i(
                    TAG,
                    "llama.cpp chat prompt formatted: messages=${messages.size}, promptChars=${formatted.length}",
                )
            }
        } catch (error: Throwable) {
            Log.e(TAG, "llama.cpp chat prompt formatting failed: ${error.message}", error)
            throw error
        }
    }

    override fun countTokens(prompt: String): Int = lock.withLock {
        check(isLoaded) { "Load a model before counting tokens." }
        try {
            native.nativeTokenCount(prompt).also { count ->
                check(count >= 0) { "Could not tokenize the formatted chat prompt." }
                Log.i(
                    TAG,
                    "llama.cpp prompt tokenized: promptChars=${prompt.length}, tokens=$count",
                )
            }
        } catch (error: Throwable) {
            Log.e(TAG, "llama.cpp prompt tokenization failed: ${error.message}", error)
            throw error
        }
    }

    override fun generate(
        request: LlmGenerationRequest,
        onToken: (String) -> Unit,
    ): LlmGenerationResult = lock.withLock {
        check(isLoaded) { "Load a model before generating text." }
        val maxTokens = request.options.maxTokens ?: DEFAULT_MAX_TOKENS
        val temperature = request.options.temperature ?: DEFAULT_TEMPERATURE
        val topK = request.options.topK ?: DEFAULT_TOP_K
        val topP = request.options.topP ?: DEFAULT_TOP_P
        val seed = request.options.seed ?: ENGINE_SELECTED_SEED
        require(request.prompt.isNotBlank()) { "Prompt must not be empty." }
        require(maxTokens > 0) { "Maximum output tokens must be positive." }
        require(temperature in 0f..2f) { "Temperature must be between 0 and 2." }
        require(topK in 1..200) { "Top-K must be between 1 and 200." }
        require(topP in 0.05f..1f) { "Top-P must be between 0.05 and 1." }
        Log.i(
            TAG,
            "llama.cpp generation started: promptChars=${request.prompt.length}, maxTokens=$maxTokens, temperature=$temperature, topK=$topK, topP=$topP, seed=$seed",
        )
        val result = try {
            native.nativeGenerate(
                prompt = request.prompt,
                maxTokens = maxTokens,
                temperature = temperature,
                topK = topK,
                topP = topP,
                seed = seed,
                callback = NativeTokenCallback(onToken),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "llama.cpp native generation failed: ${error.message}", error)
            throw error
        }
        check(result.firstOrNull() == "OK") { result.getOrElse(1) { "Native generation failed." } }
        LlmGenerationResult(
            text = result[1],
            promptTokenCount = result[2].toInt(),
            generatedTokenCount = result[3].toInt(),
            firstTokenLatencyMs = result[4].toLong().takeIf { it >= 0 },
            promptDurationMs = result[5].toLong(),
            generationDurationMs = result[6].toLong(),
            totalDurationMs = result[7].toLong(),
            finishReason = LlmFinishReason.valueOf(result[8]),
        ).also { generation ->
            Log.i(
                TAG,
                "llama.cpp generation completed: outputChars=${generation.text.length}, promptTokens=${generation.promptTokenCount}, generatedTokens=${generation.generatedTokenCount}, firstTokenMs=${generation.firstTokenLatencyMs}, totalMs=${generation.totalDurationMs}, finishReason=${generation.finishReason}",
            )
        }
    }

    override fun cancel() {
        Log.i(TAG, "llama.cpp cancellation requested.")
        native.nativeCancel()
    }

    override fun unload() = lock.withLock {
        Log.i(TAG, "llama.cpp unload requested: loaded=$isLoaded")
        native.nativeUnload()
        isLoaded = false
        activeRequest = null
    }

    private fun runtimeDiagnostics() = LlmRuntimeDiagnostics(
        computeDetail = "ggml CPU",
        effectiveThreadCount = native.nativeEffectiveThreads(),
        systemInfo = native.nativeSystemInfo(),
    )

    private class NativeBridge(nativeLibraryDir: String) {
        init {
            System.loadLibrary("local_ai_llamacpp")
            requireSuccess(nativeInitialize(nativeLibraryDir))
            Log.i(TAG, "llama.cpp native bridge initialized.")
        }

        external fun nativeInitialize(nativeLibraryDir: String): String
        external fun nativeLoad(modelPath: String, contextSize: Int, threadCount: Int): String
        external fun nativeFormatChat(roles: Array<String>, contents: Array<String>): String
        external fun nativeTokenCount(prompt: String): Int
        external fun nativeGenerate(
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topK: Int,
            topP: Float,
            seed: Int,
            callback: NativeTokenCallback,
        ): Array<String>
        external fun nativeCancel()
        external fun nativeUnload()
        external fun nativeSystemInfo(): String
        external fun nativeEffectiveThreads(): Int

        fun requireSuccess(message: String) {
            check(message.isEmpty()) { message }
        }
    }

    private companion object {
        const val TAG = "AiP123Chat"
        const val DEFAULT_CONTEXT_SIZE = 512
        const val DEFAULT_THREAD_COUNT = 0
        const val DEFAULT_MAX_TOKENS = 128
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.9f
        const val ENGINE_SELECTED_SEED = -1
    }
}

private class NativeTokenCallback(private val callback: (String) -> Unit) {
    @Suppress("unused") // Called from JNI.
    fun onToken(token: String) = callback(token)
}
