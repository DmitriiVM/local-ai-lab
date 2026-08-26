package com.dmitriim.localailab.ai.sherpa

import android.util.Log
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRuntime
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextResult
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelFileRole
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<SpeechToTextRuntime>())
class SherpaSpeechToTextRuntime : SpeechToTextRuntime {
    override val engineId = EngineId("sherpa-onnx")

    private val lock = Any()
    private var offlineRecognizer: OfflineRecognizer? = null
    private var onlineRecognizer: OnlineRecognizer? = null

    @Volatile private var cancelled = false
    private var loadedModelKey: String? = null
    private var loadedThreads = 0

    override val isLoaded: Boolean
        get() = synchronized(lock) { offlineRecognizer != null || onlineRecognizer != null }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult = synchronized(lock) {
        require(request.engineId == engineId) {
            "Unsupported STT engine: ${request.engineId.value}"
        }
        require(request.profileType in SUPPORTED_PROFILES) {
            "Unsupported sherpa-onnx STT profile: ${request.profileType.value}"
        }
        val files = request.files.mapValues { (_, path) -> File(path).canonicalFile }
        val requiredRoles = requiredRoles(request.profileType)
        val missingRoles = requiredRoles.filterNot(files::containsKey)
        require(missingRoles.isEmpty()) {
            "The speech model is missing roles: ${missingRoles.joinToString { it.value }}"
        }
        val missingFiles = requiredRoles.map(files::getValue).filterNot(File::isFile)
        require(missingFiles.isEmpty()) {
            "Speech model files are missing: ${missingFiles.joinToString { it.name }}"
        }
        val threads = effectiveThreads(request.threadCount)
        val requestedModelKey = buildString {
            append(request.profileType.value)
            append('|')
            append(request.languageCode)
            files.toSortedMap(compareBy(ModelFileRole::value)).forEach { (role, file) ->
                append('|').append(role.value).append('=').append(file.path)
            }
        }
        if (isLoaded && loadedModelKey == requestedModelKey && loadedThreads == threads) {
            return SpeechToTextLoadResult(threads, loadDurationMs = 0, coldStart = false)
        }

        unloadLocked()
        Log.i(
            TAG,
            "Sherpa STT load requested: profile=${request.profileType.value}, language=${request.languageCode}, threads=$threads",
        )
        val duration = try {
            measureTimeMillis {
                if (request.profileType == ModelProfileIds.ZIPFORMER_STT) {
                    onlineRecognizer = createOnlineZipformer(files, threads)
                } else {
                    offlineRecognizer = createOfflineRecognizer(request, files, threads)
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa STT native model creation failed: ${error.message}", error)
            unloadLocked()
            throw error
        }
        loadedModelKey = requestedModelKey
        loadedThreads = threads
        cancelled = false
        Log.i(TAG, "Sherpa STT model loaded: loadMs=$duration, effectiveThreads=$threads")
        SpeechToTextLoadResult(threads, duration, coldStart = true)
    }

    override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = synchronized(lock) {
        check(!cancelled) { "Transcription was cancelled." }
        require(request.samples.isNotEmpty()) { "The audio input is empty." }
        val offline = offlineRecognizer
        val online = onlineRecognizer
        check(offline != null || online != null) { "Load a speech model before transcription." }
        var text = ""
        val duration = measureTimeMillis {
            text = when {
                offline != null -> transcribeOffline(offline, request)
                else -> transcribeOnline(checkNotNull(online), request)
            }
        }
        Log.i(
            TAG,
            "Sherpa STT inference completed: processingMs=$duration, transcriptLength=${text.length}",
        )
        SpeechToTextResult(text.trim(), duration)
    }

    private fun createOfflineRecognizer(
        request: SpeechToTextLoadRequest,
        files: Map<ModelFileRole, File>,
        threads: Int,
    ) = OfflineRecognizer(
        null,
        OfflineRecognizerConfig().apply {
            modelConfig = OfflineModelConfig().apply {
                tokens = files.require(ModelFileRoles.TOKENS).path
                numThreads = threads
                provider = "cpu"
                debug = false
                when (request.profileType) {
                    ModelProfileIds.WHISPER_STT -> whisper = OfflineWhisperModelConfig().apply {
                        encoder = files.require(ModelFileRoles.ENCODER).path
                        decoder = files.require(ModelFileRoles.DECODER).path
                        language = request.languageCode
                        task = "transcribe"
                        enableSegmentTimestamps = true
                    }
                    ModelProfileIds.PARAKEET_CTC_STT,
                    ModelProfileIds.GIGAAM_CTC_STT,
                    -> nemo = OfflineNemoEncDecCtcModelConfig().apply {
                        model = files.require(ModelFileRoles.PRIMARY_MODEL).path
                    }
                    ModelProfileIds.SENSE_VOICE_STT ->
                        senseVoice =
                            OfflineSenseVoiceModelConfig().apply {
                                model = files.require(ModelFileRoles.PRIMARY_MODEL).path
                                language = request.languageCode
                                useInverseTextNormalization = true
                            }
                    ModelProfileIds.PARAFORMER_STT ->
                        paraformer =
                            OfflineParaformerModelConfig().apply {
                                model = files.require(ModelFileRoles.PRIMARY_MODEL).path
                            }
                    ModelProfileIds.MOONSHINE_STT ->
                        moonshine =
                            OfflineMoonshineModelConfig().apply {
                                encoder = files.require(ModelFileRoles.ENCODER).path
                                mergedDecoder = files.require(ModelFileRoles.MERGED_DECODER).path
                            }
                    ModelProfileIds.MOONSHINE_V1_STT ->
                        moonshine =
                            OfflineMoonshineModelConfig().apply {
                                preprocessor = files.require(ModelFileRoles.PREPROCESSOR).path
                                encoder = files.require(ModelFileRoles.ENCODER).path
                                uncachedDecoder = files.require(ModelFileRoles.UNCACHED_DECODER).path
                                cachedDecoder = files.require(ModelFileRoles.CACHED_DECODER).path
                            }
                    else -> error("Unsupported offline STT profile: ${request.profileType.value}")
                }
            }
        },
    )

    private fun createOnlineZipformer(files: Map<ModelFileRole, File>, threads: Int) = OnlineRecognizer(
        null,
        OnlineRecognizerConfig().apply {
            modelConfig = OnlineModelConfig().apply {
                transducer = OnlineTransducerModelConfig().apply {
                    encoder = files.require(ModelFileRoles.ENCODER).path
                    decoder = files.require(ModelFileRoles.DECODER).path
                    joiner = files.require(ModelFileRoles.JOINER).path
                }
                tokens = files.require(ModelFileRoles.TOKENS).path
                numThreads = threads
                provider = "cpu"
                debug = false
            }
            enableEndpoint = false
            decodingMethod = "greedy_search"
        },
    )

    private fun transcribeOffline(
        recognizer: OfflineRecognizer,
        request: SpeechToTextRequest,
    ): String {
        val stream = recognizer.createStream()
        return try {
            check(!cancelled) { "Transcription was cancelled." }
            stream.acceptWaveform(request.samples, request.sampleRateHz)
            recognizer.decode(stream)
            check(!cancelled) { "Transcription was cancelled." }
            recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    private fun transcribeOnline(
        recognizer: OnlineRecognizer,
        request: SpeechToTextRequest,
    ): String {
        val stream = recognizer.createStream()
        return try {
            stream.acceptWaveform(request.samples, request.sampleRateHz)
            stream.inputFinished()
            while (recognizer.isReady(stream)) {
                check(!cancelled) { "Transcription was cancelled." }
                recognizer.decode(stream)
            }
            recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun cancel() {
        cancelled = true
    }

    override fun unload() = synchronized(lock) {
        unloadLocked()
    }

    private fun unloadLocked() {
        offlineRecognizer?.release()
        onlineRecognizer?.release()
        offlineRecognizer = null
        onlineRecognizer = null
        loadedModelKey = null
        loadedThreads = 0
        cancelled = false
    }

    private fun requiredRoles(
        profileType: com.dmitriim.localailab.core.model.manifest.ModelProfileId,
    ) = when (profileType) {
        ModelProfileIds.WHISPER_STT -> setOf(
            ModelFileRoles.ENCODER,
            ModelFileRoles.DECODER,
            ModelFileRoles.TOKENS,
        )
        ModelProfileIds.PARAKEET_CTC_STT,
        ModelProfileIds.GIGAAM_CTC_STT,
        ModelProfileIds.SENSE_VOICE_STT,
        ModelProfileIds.PARAFORMER_STT,
        -> setOf(ModelFileRoles.PRIMARY_MODEL, ModelFileRoles.TOKENS)
        ModelProfileIds.ZIPFORMER_STT -> setOf(
            ModelFileRoles.ENCODER,
            ModelFileRoles.DECODER,
            ModelFileRoles.JOINER,
            ModelFileRoles.TOKENS,
        )
        ModelProfileIds.MOONSHINE_STT -> setOf(
            ModelFileRoles.ENCODER,
            ModelFileRoles.MERGED_DECODER,
            ModelFileRoles.TOKENS,
        )
        ModelProfileIds.MOONSHINE_V1_STT -> setOf(
            ModelFileRoles.PREPROCESSOR,
            ModelFileRoles.ENCODER,
            ModelFileRoles.UNCACHED_DECODER,
            ModelFileRoles.CACHED_DECODER,
            ModelFileRoles.TOKENS,
        )
        else -> emptySet()
    }

    private fun Map<ModelFileRole, File>.require(role: ModelFileRole): File = requireNotNull(this[role]) { "Missing ${role.value} model file." }

    private fun effectiveThreads(requested: Int): Int = requested.takeIf { it > 0 }
        ?: Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

    private companion object {
        const val TAG = "AiP123Stt"
        val SUPPORTED_PROFILES = setOf(
            ModelProfileIds.WHISPER_STT,
            ModelProfileIds.PARAKEET_CTC_STT,
            ModelProfileIds.GIGAAM_CTC_STT,
            ModelProfileIds.ZIPFORMER_STT,
            ModelProfileIds.SENSE_VOICE_STT,
            ModelProfileIds.PARAFORMER_STT,
            ModelProfileIds.MOONSHINE_STT,
            ModelProfileIds.MOONSHINE_V1_STT,
        )
    }
}
