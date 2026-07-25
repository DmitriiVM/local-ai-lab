package com.dmitriim.localaiplayground.ai.llamacpp

import android.content.Context
import android.util.Log
import com.dmitriim.localaiplayground.ai.api.ChatEngine
import com.dmitriim.localaiplayground.ai.api.LlmBackend
import com.dmitriim.localaiplayground.ai.api.LlmChatMessage
import com.dmitriim.localaiplayground.ai.api.LlmFinishReason
import com.dmitriim.localaiplayground.ai.api.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.LlmGenerationResult
import com.dmitriim.localaiplayground.ai.api.LlmLoadRequest
import com.dmitriim.localaiplayground.ai.api.LlmLoadResult
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/** JNI-backed llama.cpp engine. It owns one model/context and serializes native access. */
class NativeLlama(context: Context) : ChatEngine {
    private val native = NativeBridge(context.applicationInfo.nativeLibraryDir)
    private val lock = ReentrantLock()
    private var activeRequest: LlmLoadRequest? = null

    override var isLoaded: Boolean = false
        private set

    override fun load(request: LlmLoadRequest): LlmLoadResult = lock.withLock {
        Log.i(TAG, "llama.cpp load requested: model=${File(request.modelPath).name}, contextSize=${request.contextSize}, requestedThreads=${request.threadCount}, backend=${request.requestedBackend}")
        require(request.requestedBackend == LlmBackend.CPU) {
            "Only the CPU backend is enabled; ${request.requestedBackend} is experimental."
        }
        require(request.contextSize >= 128) { "Context size must be at least 128 tokens." }
        require(request.threadCount >= 0) { "Thread count cannot be negative." }
        val model = File(request.modelPath)
        require(model.isFile && model.canRead()) { "Model file is not readable: ${model.name}" }
        if (isLoaded && activeRequest == request) {
            Log.i(TAG, "llama.cpp model reuse: effectiveThreads=${native.nativeEffectiveThreads()}")
            return LlmLoadResult(
                effectiveBackend = LlmBackend.CPU,
                effectiveThreadCount = native.nativeEffectiveThreads(),
                loadDurationMs = 0,
                systemInfo = native.nativeSystemInfo(),
                coldStart = false,
            )
        }
        val coldStart = !isLoaded
        val durationMs = try {
            measureTimeMillis {
                native.requireSuccess(native.nativeLoad(model.absolutePath, request.contextSize, request.threadCount))
            }
        } catch (error: Throwable) {
            Log.e(TAG, "llama.cpp model load failed: ${error.message}", error)
            throw error
        }
        isLoaded = true
        activeRequest = request
        Log.i(TAG, "llama.cpp model loaded: coldStart=$coldStart, loadMs=$durationMs, effectiveThreads=${native.nativeEffectiveThreads()}")
        LlmLoadResult(
            effectiveBackend = LlmBackend.CPU,
            effectiveThreadCount = native.nativeEffectiveThreads(),
            loadDurationMs = durationMs,
            systemInfo = native.nativeSystemInfo(),
            coldStart = coldStart,
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
                Log.i(TAG, "llama.cpp chat prompt formatted: messages=${messages.size}, promptChars=${formatted.length}")
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
                Log.i(TAG, "llama.cpp prompt tokenized: promptChars=${prompt.length}, tokens=$count")
            }
        } catch (error: Throwable) {
            Log.e(TAG, "llama.cpp prompt tokenization failed: ${error.message}", error)
            throw error
        }
    }

    override fun generate(request: LlmGenerationRequest, onToken: (String) -> Unit): LlmGenerationResult = lock.withLock {
        check(isLoaded) { "Load a model before generating text." }
        require(request.prompt.isNotBlank()) { "Prompt must not be empty." }
        require(request.maxTokens > 0) { "Maximum output tokens must be positive." }
        require(request.temperature in 0f..2f) { "Temperature must be between 0 and 2." }
        require(request.topK in 1..200) { "Top-K must be between 1 and 200." }
        require(request.topP in 0.05f..1f) { "Top-P must be between 0.05 and 1." }
        Log.i(TAG, "llama.cpp generation started: promptChars=${request.prompt.length}, maxTokens=${request.maxTokens}, temperature=${request.temperature}, topK=${request.topK}, topP=${request.topP}, seed=${request.seed}")
        val result = try {
            native.nativeGenerate(
                prompt = request.prompt,
                maxTokens = request.maxTokens,
                temperature = request.temperature,
                topK = request.topK,
                topP = request.topP,
                seed = request.seed,
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
            Log.i(TAG, "llama.cpp generation completed: outputChars=${generation.text.length}, promptTokens=${generation.promptTokenCount}, generatedTokens=${generation.generatedTokenCount}, firstTokenMs=${generation.firstTokenLatencyMs}, totalMs=${generation.totalDurationMs}, finishReason=${generation.finishReason}")
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
    }
}

private class NativeTokenCallback(
    private val callback: (String) -> Unit,
) {
    @Suppress("unused") // Called from JNI.
    fun onToken(token: String) = callback(token)
}
