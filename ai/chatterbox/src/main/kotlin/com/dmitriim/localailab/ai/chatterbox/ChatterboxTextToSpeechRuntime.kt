package com.dmitriim.localailab.ai.chatterbox

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.app.ActivityManager
import android.app.Application
import android.os.Debug
import android.util.Log
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRuntime
import com.dmitriim.localailab.ai.api.tts.TextToSpeechStageMetrics
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<TextToSpeechRuntime>())
class ChatterboxTextToSpeechRuntime(
    private val application: Application,
    private val profiles: ModelRuntimeProfileRegistry,
) : TextToSpeechRuntime {
    override val engineId = EngineId("chatterbox-onnx")
    private val lock = Any()
    private val cancelled = AtomicBoolean(false)
    private val environment by lazy(OrtEnvironment::getEnvironment)
    private var runtime: Runtime? = null
    private var loadedDirectory: String? = null
    private var loadedThreads = 0

    override val isLoaded: Boolean get() = synchronized(lock) { runtime != null }

    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult = synchronized(lock) {
        require(request.engineId == engineId)
        profiles.requireTyped<ChatterboxProfile>(ModelProfileKey(request.engineId, request.profileType))
        val directory = File(request.modelDirectory).canonicalFile
        val threads = request.threadCount.takeIf { it > 0 }
            ?: java.lang.Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        runtime?.takeIf { loadedDirectory == directory.path && loadedThreads == threads }?.let {
            cancelled.set(false)
            return TextToSpeechLoadResult(threads, 0, false, SAMPLE_RATE_HZ, null)
        }
        unloadLocked()
        val missing = ChatterboxRuntimeProfile.requiredFiles.keys
            .filterNot { File(directory, it).isFile }
        require(missing.isEmpty()) {
            "Chatterbox model files are missing: ${missing.joinToString()}"
        }
        var created: Runtime? = null
        val loadMs = measureTimeMillis {
            val options = OrtSession.SessionOptions()
            try {
                options.setIntraOpNumThreads(threads)
                options.setInterOpNumThreads(1)
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                created = Runtime(
                    speechEncoder = environment.createSession(
                        File(directory, SPEECH_ENCODER).path,
                        options,
                    ),
                    embedTokens = environment.createSession(
                        File(directory, EMBED_TOKENS).path,
                        options,
                    ),
                    languageModel = environment.createSession(
                        File(directory, LANGUAGE_MODEL).path,
                        options,
                    ),
                    conditionalDecoder = environment.createSession(
                        File(directory, CONDITIONAL_DECODER).path,
                        options,
                    ),
                    tokenizer = ChatterboxTokenizer(File(directory, "tokenizer.json")),
                )
            } finally {
                options.close()
            }
        }
        runtime = checkNotNull(created)
        loadedDirectory = directory.path
        loadedThreads = threads
        cancelled.set(false)
        Log.i(
            TAG,
            "Chatterbox sessions loaded: directory=${directory.name}, loadMs=$loadMs, threads=$threads",
        )
        TextToSpeechLoadResult(threads, loadMs, true, SAMPLE_RATE_HZ, null)
    }

    @Suppress("LongMethod") // Coordinates the ONNX inference lifecycle and resource ownership.
    override fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult = synchronized(lock) {
        val active = checkNotNull(runtime) { "Load Chatterbox before synthesis." }
        require(request.languageCode == "en") { "Chatterbox Turbo Q4 supports English only." }
        val reference = request.voice as? TextToSpeechVoiceCondition.ReferenceAudio
            ?: error("Select a saved reference voice for Chatterbox.")
        cancelled.set(false)
        val inputIds = active.tokenizer.encode(request.text)
        val memory = ChatterboxMemorySampler(application)
        memory.sample()

        val cacheHit = active.conditioning?.referenceId == reference.referenceId
        var conditioningMs = 0L
        if (!cacheHit) {
            val samples = readReferencePcm(reference)
            var encoded: OrtSession.Result? = null
            conditioningMs = measureTimeMillis {
                OnnxTensor.createTensor(
                    environment,
                    FloatBuffer.wrap(samples),
                    longArrayOf(1, samples.size.toLong()),
                ).use { audio ->
                    encoded = active.speechEncoder.run(mapOf("audio_values" to audio))
                }
            }
            active.conditioning?.close()
            active.conditioning = Conditioning(reference.referenceId, checkNotNull(encoded))
        }
        memory.sample()
        checkNotCancelled()
        val conditioning = checkNotNull(active.conditioning)

        val generated = ArrayList<Long>(MAX_NEW_TOKENS + 1).apply { add(START_SPEECH_TOKEN) }
        var languageResult: OrtSession.Result? = null
        var tokenGenerationMs = 0L
        try {
            tokenGenerationMs = measureTimeMillis {
                for (iteration in 0 until MAX_NEW_TOKENS) {
                    checkNotCancelled()
                    val tokenInput = if (iteration == 0) inputIds else longArrayOf(generated.last())
                    OnnxTensor.createTensor(
                        environment,
                        LongBuffer.wrap(tokenInput),
                        longArrayOf(1, tokenInput.size.toLong()),
                    ).use { idsTensor ->
                        active.embedTokens.run(mapOf("input_ids" to idsTensor)).use { embedResult ->
                            val rawEmbeddings = embedResult.tensor(0)
                            val embeddings = if (iteration == 0) {
                                concatenateEmbeddings(conditioning.result.tensor(0), rawEmbeddings)
                            } else {
                                null
                            }
                            val effectiveEmbeddings = embeddings ?: rawEmbeddings
                            val embeddingShape = effectiveEmbeddings.info.shape
                            val batchSize = embeddingShape[0].toInt()
                            val sequenceLength = embeddingShape[1].toInt()
                            val attention = if (iteration == 0) {
                                LongArray(sequenceLength) { 1L }
                            } else {
                                LongArray(
                                    inputIds.size +
                                        conditioning.result.tensor(0).info.shape[1].toInt() +
                                        iteration,
                                ) { 1L }
                            }
                            val positions = if (iteration == 0) {
                                LongArray(sequenceLength) { it.toLong() }
                            } else {
                                longArrayOf((attention.size - 1).toLong())
                            }
                            val initialCache = if (iteration == 0) {
                                createEmptyCache(active.languageModel.inputInfo, batchSize)
                            } else {
                                emptyMap()
                            }
                            OnnxTensor.createTensor(
                                environment,
                                LongBuffer.wrap(attention),
                                longArrayOf(batchSize.toLong(), attention.size.toLong()),
                            ).use { attentionTensor ->
                                OnnxTensor.createTensor(
                                    environment,
                                    LongBuffer.wrap(positions),
                                    longArrayOf(batchSize.toLong(), positions.size.toLong()),
                                ).use { positionTensor ->
                                    val inputs = linkedMapOf<String, OnnxTensor>(
                                        "inputs_embeds" to effectiveEmbeddings,
                                        "attention_mask" to attentionTensor,
                                        "position_ids" to positionTensor,
                                    )
                                    if (iteration == 0) {
                                        inputs.putAll(initialCache)
                                    } else {
                                        val previous = checkNotNull(languageResult)
                                        active.pastInputNames.forEachIndexed { index, name ->
                                            inputs[name] = previous.tensor(index + 1)
                                        }
                                    }
                                    val previous = languageResult
                                    val nextResult = try {
                                        active.languageModel.run(inputs)
                                    } finally {
                                        initialCache.values.forEach(OnnxTensor::close)
                                        embeddings?.close()
                                    }
                                    languageResult = nextResult
                                    previous?.close()
                                }
                            }
                        }
                    }
                    val logits = checkNotNull(languageResult).tensor(0).floatValues()
                    val vocabSize = checkNotNull(languageResult).tensor(0).info.shape.last().toInt()
                    val offset = logits.size - vocabSize
                    generated.distinct().forEach { token ->
                        val index = offset + token.toInt()
                        if (index in logits.indices) {
                            logits[index] = if (logits[index] < 0f) {
                                logits[index] * REPETITION_PENALTY
                            } else {
                                logits[index] / REPETITION_PENALTY
                            }
                        }
                    }
                    var next = 0
                    var best = -Float.MAX_VALUE
                    for (index in 0 until vocabSize) {
                        val value = logits[offset + index]
                        if (value > best) {
                            best = value
                            next = index
                        }
                    }
                    generated += next.toLong()
                    if (next.toLong() == STOP_SPEECH_TOKEN) break
                }
            }
            checkNotCancelled()

            val prompt = conditioning.result.tensor(1).longValues()
            val bodyEnd = max(1, generated.size - 1)
            val speechTokens = LongArray(prompt.size + (bodyEnd - 1) + SILENCE_TOKEN_COUNT)
            prompt.copyInto(speechTokens)
            for (index in 1 until bodyEnd) speechTokens[prompt.size + index - 1] = generated[index]
            repeat(SILENCE_TOKEN_COUNT) { index ->
                speechTokens[speechTokens.size - SILENCE_TOKEN_COUNT + index] = SILENCE_TOKEN
            }

            var decoderMs = 0L
            var samples = FloatArray(0)
            decoderMs = measureTimeMillis {
                OnnxTensor.createTensor(
                    environment,
                    LongBuffer.wrap(speechTokens),
                    longArrayOf(1, speechTokens.size.toLong()),
                ).use { speech ->
                    active.conditionalDecoder.run(
                        mapOf(
                            "speech_tokens" to speech,
                            "speaker_embeddings" to conditioning.result.tensor(2),
                            "speaker_features" to conditioning.result.tensor(3),
                        ),
                    ).use { decoded ->
                        samples = decoded.tensor(0).floatValues()
                        memory.sample()
                    }
                }
            }
            memory.sample()
            checkNotCancelled()
            require(samples.isNotEmpty()) { "Chatterbox returned no audio." }
            if (!onAudioChunk(samples)) {
                cancelled.set(true)
                error("Speech synthesis was cancelled.")
            }
            TextToSpeechResult(
                samples = samples,
                sampleRateHz = SAMPLE_RATE_HZ,
                stageMetrics = TextToSpeechStageMetrics(
                    conditioningDurationMs = conditioningMs,
                    tokenGenerationDurationMs = tokenGenerationMs,
                    decoderDurationMs = decoderMs,
                    generatedTokenCount = generated.size - 1,
                    conditioningCacheHit = cacheHit,
                    peakProcessPssBytes = memory.peakPssBytes,
                    availableDeviceMemoryBytes = memory.availableDeviceMemoryBytes,
                ),
            )
        } finally {
            languageResult?.close()
        }
    }

    override fun cancel() {
        cancelled.set(true)
    }

    override fun unload() = synchronized(lock) {
        unloadLocked()
    }

    private fun unloadLocked() {
        cancelled.set(true)
        runtime?.close()
        runtime = null
        loadedDirectory = null
        loadedThreads = 0
        Log.i(TAG, "Chatterbox sessions and conditioning cache released.")
    }

    private fun concatenateEmbeddings(condition: OnnxTensor, text: OnnxTensor): OnnxTensor {
        val conditionShape = condition.info.shape
        val textShape = text.info.shape
        require(conditionShape.size == 3 && textShape.size == 3)
        require(conditionShape[0] == textShape[0] && conditionShape[2] == textShape[2])
        val combined =
            FloatArray(condition.info.numElements.toInt() + text.info.numElements.toInt())
        condition.floatValues().copyInto(combined)
        text.floatValues().copyInto(combined, condition.info.numElements.toInt())
        return OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(combined),
            longArrayOf(conditionShape[0], conditionShape[1] + textShape[1], conditionShape[2]),
        )
    }

    private fun createEmptyCache(
        inputInfo: Map<String, NodeInfo>,
        batchSize: Int,
    ): Map<String, OnnxTensor> = buildMap {
        inputInfo.forEach { (name, node) ->
            if (!name.contains("past_key_values")) return@forEach
            val type = (node.info as TensorInfo).type
            val shape = longArrayOf(batchSize.toLong(), NUM_KV_HEADS.toLong(), 0, HEAD_DIM.toLong())
            put(
                name,
                when (type) {
                    OnnxJavaType.FLOAT -> OnnxTensor.createTensor(
                        environment,
                        FloatBuffer.allocate(0),
                        shape,
                    )
                    OnnxJavaType.FLOAT16 -> OnnxTensor.createTensor(
                        environment,
                        ShortBuffer.allocate(0),
                        shape,
                        OnnxJavaType.FLOAT16,
                    )
                    else -> error("Unsupported Chatterbox KV-cache type: $type")
                },
            )
        }
    }

    private fun readReferencePcm(reference: TextToSpeechVoiceCondition.ReferenceAudio): FloatArray {
        require(reference.sampleRateHz == SAMPLE_RATE_HZ) { "Reference voice must be 24 kHz." }
        val file = File(reference.pcmFilePath)
        require(file.isFile) { "The selected reference voice was deleted." }
        val sampleCount = (file.length() / 2).coerceAtMost(MAX_REFERENCE_SAMPLES.toLong()).toInt()
        require(sampleCount >= MIN_REFERENCE_SAMPLES) {
            "Reference voice must be at least 5 seconds."
        }
        val bytes = ByteArray(sampleCount * 2)
        FileInputStream(file).use { input ->
            var offset = 0
            while (offset < bytes.size) {
                val read = input.read(bytes, offset, bytes.size - offset)
                if (read < 0) break
                offset += read
            }
            require(offset == bytes.size) { "Reference voice PCM is incomplete." }
        }
        return FloatArray(sampleCount) { index ->
            val lo = bytes[index * 2].toInt() and 0xff
            val hi = bytes[index * 2 + 1].toInt()
            ((hi shl 8) or lo).toShort() / 32_768f
        }
    }

    private fun checkNotCancelled() {
        check(!cancelled.get()) { "Speech synthesis was cancelled." }
    }

    private class Runtime(
        val speechEncoder: OrtSession,
        val embedTokens: OrtSession,
        val languageModel: OrtSession,
        val conditionalDecoder: OrtSession,
        val tokenizer: ChatterboxTokenizer,
    ) : AutoCloseable {
        val pastInputNames = languageModel.inputNames.filter { it.contains("past_key_values") }
        var conditioning: Conditioning? = null

        override fun close() {
            conditioning?.close()
            conditioning = null
            listOf(conditionalDecoder, languageModel, embedTokens, speechEncoder)
                .forEach(OrtSession::close)
        }
    }

    private data class Conditioning(val referenceId: String, val result: OrtSession.Result) : AutoCloseable {
        override fun close() = result.close()
    }

    private fun OrtSession.Result.tensor(index: Int): OnnxTensor = get(index) as? OnnxTensor ?: error("Chatterbox output $index is not a tensor.")

    private fun OnnxTensor.floatValues(): FloatArray {
        val buffer = requireNotNull(floatBuffer) { "Expected a floating-point Chatterbox tensor." }
        return FloatArray(buffer.remaining()).also(buffer::get)
    }

    private fun OnnxTensor.longValues(): LongArray {
        val buffer = requireNotNull(longBuffer) { "Expected an INT64 Chatterbox tensor." }
        return LongArray(buffer.remaining()).also(buffer::get)
    }

    private companion object {
        const val TAG = "AiP123Chatterbox"
        const val SAMPLE_RATE_HZ = 24_000
        const val MIN_REFERENCE_SAMPLES = SAMPLE_RATE_HZ * 5
        const val MAX_REFERENCE_SAMPLES = SAMPLE_RATE_HZ * 10
        const val START_SPEECH_TOKEN = 6_561L
        const val STOP_SPEECH_TOKEN = 6_562L
        const val SILENCE_TOKEN = 4_299L
        const val SILENCE_TOKEN_COUNT = 3
        const val NUM_KV_HEADS = 16
        const val HEAD_DIM = 64
        const val MAX_NEW_TOKENS = 1_024
        const val REPETITION_PENALTY = 1.2f
        const val CONDITIONAL_DECODER = "conditional_decoder_q4.onnx"
        const val EMBED_TOKENS = "embed_tokens_q4.onnx"
        const val LANGUAGE_MODEL = "language_model_q4.onnx"
        const val SPEECH_ENCODER = "speech_encoder_q4.onnx"
    }
}

private class ChatterboxMemorySampler(
    private val application: Application,
) {
    var peakPssBytes: Long = 0
        private set
    var availableDeviceMemoryBytes: Long = 0
        private set

    fun sample() {
        peakPssBytes = max(peakPssBytes, Debug.getPss().toLong() * 1_024)
        val info = ActivityManager.MemoryInfo()
        application.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        availableDeviceMemoryBytes = info.availMem
    }
}
