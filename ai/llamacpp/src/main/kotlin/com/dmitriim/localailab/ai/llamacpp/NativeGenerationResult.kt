package com.dmitriim.localailab.ai.llamacpp

/** Typed result returned by the llama.cpp JNI generation boundary. */
internal data class NativeGenerationResult(
    val errorMessage: String?,
    val text: String,
    val promptTokenCount: Int,
    val generatedTokenCount: Int,
    val firstTokenLatencyMs: Long,
    val promptDurationMs: Long,
    val generationDurationMs: Long,
    val totalDurationMs: Long,
    val finishReason: String?,
)
