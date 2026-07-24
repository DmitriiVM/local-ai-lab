package com.dmitriim.localaiplayground.ai.llamacpp

import android.content.Context
import com.dmitriim.localaiplayground.ai.api.LlmBackend
import com.dmitriim.localaiplayground.ai.api.LlmEngine
import com.dmitriim.localaiplayground.ai.api.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.LlmGenerationResult
import com.dmitriim.localaiplayground.ai.api.LlmLoadRequest
import com.dmitriim.localaiplayground.ai.api.LlmLoadResult
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis

/** CPU-only llama.cpp implementation used by the Stage 0 feasibility harness. */
class NativeLlama(context: Context) : LlmEngine {
    private val native = NativeBridge(context.applicationInfo.nativeLibraryDir)
    private val lock = ReentrantLock()

    override var isLoaded: Boolean = false
        private set

    override fun load(request: LlmLoadRequest): LlmLoadResult = lock.withLock {
        require(request.requestedBackend == LlmBackend.CPU) {
            "Stage 0 only enables the CPU backend; ${request.requestedBackend} is experimental."
        }
        val model = File(request.modelPath)
        require(model.isFile && model.canRead()) { "Model file is not readable: ${model.name}" }
        val durationMs = measureTimeMillis {
            native.requireSuccess(native.nativeLoad(model.absolutePath, request.contextSize, request.threadCount))
        }
        isLoaded = true
        LlmLoadResult(
            effectiveBackend = LlmBackend.CPU,
            effectiveThreadCount = native.nativeEffectiveThreads(),
            loadDurationMs = durationMs,
            systemInfo = native.nativeSystemInfo(),
        )
    }

    override fun generate(request: LlmGenerationRequest): LlmGenerationResult = lock.withLock {
        check(isLoaded) { "Load a model before generating text." }
        require(request.prompt.isNotBlank()) { "Prompt must not be empty." }
        var result = ""
        val durationMs = measureTimeMillis {
            result = native.nativeGenerate(request.prompt, request.maxTokens)
        }
        when {
            result.startsWith("ERROR:") -> error(result.removePrefix("ERROR:"))
            result.startsWith("CANCELLED:") -> LlmGenerationResult(
                text = result.removePrefix("CANCELLED:"),
                generatedTokenCount = 0,
                firstTokenLatencyMs = null,
                totalDurationMs = durationMs,
                cancelled = true,
            )
            else -> {
                val text = result.removePrefix("OK:")
                LlmGenerationResult(
                    text = text,
                    generatedTokenCount = 0,
                    firstTokenLatencyMs = null,
                    totalDurationMs = durationMs,
                    cancelled = false,
                )
            }
        }
    }

    override fun cancel() {
        native.nativeCancel()
    }

    override fun unload() = lock.withLock {
        native.nativeUnload()
        isLoaded = false
    }

    private class NativeBridge(nativeLibraryDir: String) {
        init {
            System.loadLibrary("local_ai_llamacpp")
            requireSuccess(nativeInitialize(nativeLibraryDir))
        }

        external fun nativeInitialize(nativeLibraryDir: String): String
        external fun nativeLoad(modelPath: String, contextSize: Int, threadCount: Int): String
        external fun nativeGenerate(prompt: String, maxTokens: Int): String
        external fun nativeCancel()
        external fun nativeUnload()
        external fun nativeSystemInfo(): String
        external fun nativeEffectiveThreads(): Int

        fun requireSuccess(message: String) {
            check(message.isEmpty()) { message }
        }
    }
}
