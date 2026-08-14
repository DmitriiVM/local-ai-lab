package com.dmitriim.localailab.feature.benchmark.domain

import com.dmitriim.localailab.ai.api.llm.ChatEngine
import com.dmitriim.localailab.ai.api.llm.LlmChatMessage
import com.dmitriim.localailab.ai.api.llm.LlmChatRole
import com.dmitriim.localailab.ai.api.llm.LlmChatTemplateHandling
import com.dmitriim.localailab.ai.api.llm.LlmGenerationOptions
import com.dmitriim.localailab.ai.api.llm.LlmGenerationRequest
import com.dmitriim.localailab.ai.api.llm.LlmLoadOptions
import com.dmitriim.localailab.ai.api.llm.LlmLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechEngine
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.capability.AiCapability
import com.dmitriim.localailab.core.model.runs.RunModelSnapshot
import com.dmitriim.localailab.core.model.service.LocalModelResolver
import com.dmitriim.localailab.core.performance.BenchmarkIterationResult
import com.dmitriim.localailab.core.performance.BenchmarkStartupMode
import com.dmitriim.localailab.core.performance.BenchmarkWorkload
import com.dmitriim.localailab.core.performance.InferencePhase
import com.dmitriim.localailab.core.performance.InferenceProfiler
import com.dmitriim.localailab.core.performance.putInferenceTelemetry
import com.dmitriim.localailab.core.voice.stt.SpeechTranscriptionEvent
import com.dmitriim.localailab.core.voice.stt.SpeechTranscriptionRequest
import com.dmitriim.localailab.core.voice.stt.SttTranscriptionSettings
import com.dmitriim.localailab.core.voice.stt.TranscribeAudio
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Inject
@SingleIn(AppScope::class)
class LocalBenchmarkWorkloadRunner(
    private val modelResolver: LocalModelResolver,
    private val chatEngine: ChatEngine,
    private val transcribeAudio: TranscribeAudio,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val profiler: InferenceProfiler,
) : BenchmarkWorkloadRunner {
    override suspend fun run(
        workload: BenchmarkWorkload,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult = when (workload) {
        is BenchmarkWorkload.Chat -> runChat(workload, runId, iteration, startupMode)
        is BenchmarkWorkload.SpeechToText -> runStt(workload, runId, iteration, startupMode)
        is BenchmarkWorkload.TextToSpeech -> runTts(workload, runId, iteration, startupMode)
    }

    override fun unload(workload: BenchmarkWorkload) = when (workload) {
        is BenchmarkWorkload.Chat -> chatEngine.unload()
        is BenchmarkWorkload.SpeechToText -> transcribeAudio.unload()
        is BenchmarkWorkload.TextToSpeech -> textToSpeechEngine.unload()
    }

    private suspend fun runChat(
        workload: BenchmarkWorkload.Chat,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult {
        if (startupMode == BenchmarkStartupMode.COLD) chatEngine.unload()
        val profile = profiler.start(runId, AiCapability.CHAT, extendedTelemetry = true)
        try {
            val model = profile.trace(InferencePhase.MODEL_RESOLUTION) {
                modelResolver.resolveChatModel(workload.modelId).getOrThrow()
            }
            val capabilities = requireNotNull(chatEngine.capabilitiesFor(model.engineId)) {
                "No packaged LLM runtime supports ${model.engineId.value}."
            }
            val load = profile.trace(InferencePhase.MODEL_LOAD) {
                chatEngine.load(
                    LlmLoadRequest(
                        model,
                        LlmLoadOptions(workload.contextSize, workload.threadCount, workload.computePreference),
                    ),
                )
            }
            val prompt = profile.trace(InferencePhase.PROMPT_PREPARATION) {
                val messages = if (capabilities.systemInstructions) {
                    workload.messages
                } else {
                    workload.messages.filterNot { it.role == LlmChatRole.SYSTEM }
                }
                when (capabilities.chatTemplateHandling) {
                    LlmChatTemplateHandling.ENGINE_FORMATS_MESSAGES -> requireNotNull(chatEngine.activeChatFormatter()) {
                        "The active LLM runtime does not provide its declared chat formatter."
                    }.format(messages)
                    LlmChatTemplateHandling.CALLER_PROVIDES_PROMPT -> formatRoleLabeledPrompt(messages)
                }
            }
            val generation = profile.trace(InferencePhase.DECODE) {
                chatEngine.generate(
                    LlmGenerationRequest(
                        prompt,
                        LlmGenerationOptions(
                            maxTokens = workload.maxTokens,
                            temperature = workload.temperature,
                            topK = workload.topK,
                            topP = workload.topP,
                            seed = workload.seed,
                        ),
                    ),
                ) { }
            }
            return chatResult(workload, runId, iteration, model, load, generation, profile.finish())
        } finally {
            profile.finish()
        }
    }

    private fun chatResult(
        workload: BenchmarkWorkload.Chat,
        runId: String,
        iteration: Int,
        model: com.dmitriim.localailab.core.model.runtime.ChatModelReference,
        load: com.dmitriim.localailab.ai.api.llm.LlmLoadResult,
        generation: com.dmitriim.localailab.ai.api.llm.LlmGenerationResult,
        telemetry: com.dmitriim.localailab.core.performance.InferenceTelemetry,
    ): BenchmarkWorkloadResult {
        val rate = generation.generatedTokenCount?.takeIf { generation.generationDurationMs > 0 }
            ?.toDouble()?.times(1_000.0 / generation.generationDurationMs)
        return BenchmarkWorkloadResult(
            iteration = BenchmarkIterationResult(runId, iteration, telemetry.wallDurationMs, rate, generation.generatedTokenCount, telemetry),
            model = RunModelSnapshot(model.modelId.value, model.displayName, model.engineId.value, null),
            input = workload.messages.joinToString("\n") { message -> "${message.role.wireName}: ${message.content}" },
            output = generation.text,
            parametersJson = Json.encodeToString(
                buildJsonObject {
                    put("computePreference", workload.computePreference.name)
                    put("contextSize", workload.contextSize)
                    put("threadCount", workload.threadCount)
                    put("maxTokens", workload.maxTokens)
                },
            ),
            metricsJson = Json.encodeToString(
                buildJsonObject {
                    put("coldStart", load.coldStart)
                    put("loadDurationMs", load.loadDurationMs)
                    put("promptTokens", generation.promptTokenCount)
                    put("promptDurationMs", generation.promptDurationMs)
                    put("generatedTokens", generation.generatedTokenCount)
                    put("generationDurationMs", generation.generationDurationMs)
                    put("generatedTokensPerSecond", rate)
                    put("timeToFirstTokenMs", generation.firstTokenLatencyMs)
                    put("totalDurationMs", generation.totalDurationMs)
                    put("effectiveComputePreference", load.effectiveComputePreference.name)
                    put("computeDetail", load.diagnostics.computeDetail)
                    put("fallbackReason", load.diagnostics.fallbackReason)
                    putInferenceTelemetry(telemetry)
                },
            ),
        )
    }

    private fun formatRoleLabeledPrompt(messages: List<LlmChatMessage>): String = buildString {
        messages.forEachIndexed { index, message ->
            if (index > 0) append("\n\n")
            append(message.role.wireName)
            append(": ")
            append(message.content)
        }
        append("\n\nassistant:")
    }

    private suspend fun runStt(
        workload: BenchmarkWorkload.SpeechToText,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult {
        if (startupMode == BenchmarkStartupMode.COLD) transcribeAudio.unload()
        var completed: SpeechTranscriptionEvent.Completed? = null
        transcribeAudio.execute(
            SpeechTranscriptionRequest(
                modelId = workload.modelId,
                input = workload.input,
                settings = SttTranscriptionSettings(workload.languageCode, workload.threadCount),
                runId = runId,
                keepLoaded = startupMode == BenchmarkStartupMode.WARM,
                extendedProfiling = true,
            ),
        ).collect { event -> if (event is SpeechTranscriptionEvent.Completed) completed = event }
        val event = requireNotNull(completed) { "STT did not return a completed benchmark result." }
        val model = modelResolver.resolveSpeechToTextModel(workload.modelId).getOrThrow()
        val metrics = event.metrics
        val telemetry = requireNotNull(metrics.telemetry)
        return BenchmarkWorkloadResult(
            iteration = BenchmarkIterationResult(runId, iteration, telemetry.wallDurationMs, metrics.realTimeFactor?.let { 1.0 / it }, metrics.segmentCount, telemetry),
            model = RunModelSnapshot(model.modelId.value, model.displayName, model.engineId.value, null),
            input = "${workload.input.displayName} (${workload.input.durationMs} ms)",
            output = event.transcript,
            parametersJson = Json.encodeToString(
                buildJsonObject {
                    put("language", workload.languageCode)
                    put("threadCount", workload.threadCount)
                },
            ),
            metricsJson = Json.encodeToString(
                buildJsonObject {
                    put("audioDurationMs", metrics.audioDurationMs)
                    put("processingDurationMs", metrics.processingDurationMs)
                    put("timeToFinalMs", metrics.timeToFinalMs)
                    put("realTimeFactor", metrics.realTimeFactor)
                    put("segmentCount", metrics.segmentCount)
                    put("loadDurationMs", metrics.loadDurationMs)
                    put("effectiveThreadCount", metrics.effectiveThreadCount)
                    putInferenceTelemetry(metrics.telemetry)
                },
            ),
        )
    }

    private suspend fun runTts(
        workload: BenchmarkWorkload.TextToSpeech,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkWorkloadResult {
        if (startupMode == BenchmarkStartupMode.COLD) textToSpeechEngine.unload()
        val profile = profiler.start(runId, AiCapability.TEXT_TO_SPEECH, extendedTelemetry = true)
        try {
            val model = profile.trace(InferencePhase.MODEL_RESOLUTION) {
                modelResolver.resolveTextToSpeechModel(workload.modelId).getOrThrow()
            }
            val load = profile.trace(InferencePhase.MODEL_LOAD) {
                textToSpeechEngine.load(TextToSpeechLoadRequest(model.engineId, model.profileType, model.modelDirectory, workload.threadCount))
            }
            val startedAt = android.os.SystemClock.elapsedRealtime()
            val result = profile.trace(InferencePhase.SYNTHESIS) {
                textToSpeechEngine.synthesize(
                    TextToSpeechRequest(workload.text, workload.languageCode, workload.voice, workload.speed, workload.sentenceSilenceScale),
                ) { true }
            }
            val durationMs = android.os.SystemClock.elapsedRealtime() - startedAt
            val audioDurationMs = result.samples.size * 1_000L / result.sampleRateHz
            val telemetry = profile.finish()
            val rtf = audioDurationMs.takeIf { it > 0 }?.let { durationMs.toDouble() / it }
            return BenchmarkWorkloadResult(
                iteration = BenchmarkIterationResult(runId, iteration, telemetry.wallDurationMs, rtf?.let { 1.0 / it }, result.samples.size, telemetry),
                model = RunModelSnapshot(model.modelId.value, model.displayName, model.engineId.value, null),
                input = workload.text,
                output = "Generated $audioDurationMs ms PCM at ${result.sampleRateHz} Hz.",
                parametersJson = Json.encodeToString(
                    buildJsonObject {
                        put("language", workload.languageCode)
                        put("speed", workload.speed)
                        put("sentenceSilenceScale", workload.sentenceSilenceScale)
                        put("threadCount", workload.threadCount)
                    },
                ),
                metricsJson = Json.encodeToString(
                    buildJsonObject {
                        put("loadDurationMs", load.loadDurationMs)
                        put("effectiveThreadCount", load.effectiveThreadCount)
                        put("synthesisDurationMs", durationMs)
                        put("generatedAudioDurationMs", audioDurationMs)
                        put("realTimeFactor", rtf)
                        put("sampleRateHz", result.sampleRateHz)
                        put("conditioningDurationMs", result.stageMetrics.conditioningDurationMs)
                        put("tokenGenerationDurationMs", result.stageMetrics.tokenGenerationDurationMs)
                        put("decoderDurationMs", result.stageMetrics.decoderDurationMs)
                        put("generatedTokenCount", result.stageMetrics.generatedTokenCount)
                        putInferenceTelemetry(telemetry)
                    },
                ),
            )
        } finally {
            profile.finish()
        }
    }
}
