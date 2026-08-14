package com.dmitriim.localaiplayground.ai.sherpa

import android.util.Log
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechBackend
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechResult
import com.dmitriim.localaiplayground.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.engine.EngineId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileId
import com.dmitriim.localaiplayground.core.model.manifest.ModelProfileIds
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsPocketModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<TextToSpeechBackend>())
class SherpaTextToSpeechEngine : TextToSpeechBackend {
    override val engineId = EngineId("sherpa-onnx")
    private val lock = Any()
    private val cancelled = AtomicBoolean(false)
    private var tts: OfflineTts? = null
    private var loadedDirectory: String? = null
    private var loadedProfile: ModelProfileId? = null
    private var loadedThreadCount: Int = 0
    private var loadedReferenceAudio: Pcm16Wave? = null

    override val isLoaded: Boolean get() = synchronized(lock) { tts != null }

    @Suppress("LongMethod") // Native runtime setup must remain atomic with the loaded-state update.
    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult = synchronized(lock) {
        val requestedDirectory = File(request.modelDirectory).canonicalPath
        val threads = effectiveThreads(request.threadCount)
        Log.i(
            TAG,
            "Sherpa TTS load requested: directory=$requestedDirectory, requestedThreads=${request.threadCount}, effectiveThreads=$threads",
        )
        val active = tts
        if (active != null &&
            loadedDirectory == requestedDirectory &&
            loadedProfile == request.profileType &&
            loadedThreadCount == threads
        ) {
            Log.i(
                TAG,
                "Sherpa TTS model reuse: sampleRateHz=${active.sampleRate()}, speakers=${
                    active.numSpeakers().coerceAtLeast(
                        1,
                    )
                }",
            )
            return TextToSpeechLoadResult(
                effectiveThreadCount = threads,
                loadDurationMs = 0,
                coldStart = false,
                sampleRateHz = active.sampleRate(),
                speakerCount = active.numSpeakers().coerceAtLeast(1),
            )
        }
        unloadLocked()
        validateRequiredFiles(request.profileType, File(requestedDirectory))
        val referenceAudio = if (request.profileType == ModelProfileIds.POCKET_TTS) {
            Pcm16Wave.read(File(requestedDirectory, pocketDefaultReferencePath))
        } else {
            null
        }
        val config = OfflineTtsConfig().apply {
            model = OfflineTtsModelConfig().apply {
                when (request.profileType) {
                    ModelProfileIds.SUPERTONIC_TTS ->
                        supertonic =
                            OfflineTtsSupertonicModelConfig().apply {
                                durationPredictor =
                                    File(requestedDirectory, "duration_predictor.int8.onnx").absolutePath
                                textEncoder =
                                    File(
                                        requestedDirectory,
                                        "text_encoder.int8.onnx",
                                    ).absolutePath
                                vectorEstimator =
                                    File(
                                        requestedDirectory,
                                        "vector_estimator.int8.onnx",
                                    ).absolutePath
                                vocoder =
                                    File(requestedDirectory, "vocoder.int8.onnx").absolutePath
                                ttsJson = File(requestedDirectory, "tts.json").absolutePath
                                unicodeIndexer =
                                    File(requestedDirectory, "unicode_indexer.bin").absolutePath
                                voiceStyle = File(requestedDirectory, "voice.bin").absolutePath
                            }

                    ModelProfileIds.PIPER_VITS_TTS -> vits = OfflineTtsVitsModelConfig().apply {
                        model =
                            File(requestedDirectory, "en_US-lessac-medium.onnx").absolutePath
                        tokens = File(requestedDirectory, "tokens.txt").absolutePath
                        dataDir = File(requestedDirectory, "espeak-ng-data").absolutePath
                    }

                    ModelProfileIds.KOKORO_TTS -> kokoro = OfflineTtsKokoroModelConfig().apply {
                        model = File(requestedDirectory, "model.onnx").absolutePath
                        voices = File(requestedDirectory, "voices.bin").absolutePath
                        tokens = File(requestedDirectory, "tokens.txt").absolutePath
                        dataDir = File(requestedDirectory, "espeak-ng-data").absolutePath
                        lexicon = listOf("lexicon-us-en.txt", "lexicon-zh.txt")
                            .joinToString(",") { File(requestedDirectory, it).absolutePath }
                        dictDir = File(requestedDirectory, "dict").absolutePath
                    }

                    ModelProfileIds.POCKET_TTS -> pocket = OfflineTtsPocketModelConfig().apply {
                        lmFlow = File(requestedDirectory, "lm_flow.int8.onnx").absolutePath
                        lmMain = File(requestedDirectory, "lm_main.int8.onnx").absolutePath
                        encoder = File(requestedDirectory, "encoder.onnx").absolutePath
                        decoder = File(requestedDirectory, "decoder.int8.onnx").absolutePath
                        textConditioner =
                            File(requestedDirectory, "text_conditioner.onnx").absolutePath
                        vocabJson = File(requestedDirectory, "vocab.json").absolutePath
                        tokenScoresJson =
                            File(requestedDirectory, "token_scores.json").absolutePath
                        voiceEmbeddingCacheCapacity = 1
                    }

                    else -> error("Unsupported TTS profile: ${request.profileType.value}")
                }
                numThreads = threads
                provider = "cpu"
                debug = false
            }
        }
        var created: OfflineTts? = null
        val duration = try {
            measureTimeMillis {
                created = OfflineTts(null, config)
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa TTS native model creation failed: ${error.message}", error)
            throw error
        }
        val loaded = checkNotNull(created)
        tts = loaded
        loadedDirectory = requestedDirectory
        loadedProfile = request.profileType
        loadedThreadCount = threads
        loadedReferenceAudio = referenceAudio
        cancelled.set(false)
        Log.i(
            TAG,
            "Sherpa TTS model loaded: loadMs=$duration, sampleRateHz=${loaded.sampleRate()}, " +
                "speakers=${loaded.numSpeakers().coerceAtLeast(1)}, threads=$threads",
        )
        TextToSpeechLoadResult(
            effectiveThreadCount = threads,
            loadDurationMs = duration,
            coldStart = true,
            sampleRateHz = loaded.sampleRate(),
            speakerCount = loaded.numSpeakers().coerceAtLeast(1),
        )
    }

    override fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult {
        val (active, profile, referenceAudio) = synchronized(lock) {
            Triple(
                checkNotNull(tts) { "Load a voice model before synthesis." },
                checkNotNull(loadedProfile),
                loadedReferenceAudio,
            )
        }
        require(request.text.isNotBlank()) { "Enter text to synthesize." }
        require(request.speed in 0.5f..2f) { "Speech rate must be between 0.5 and 2.0." }
        require(request.sentenceSilenceScale in MIN_SENTENCE_SILENCE_SCALE..MAX_SENTENCE_SILENCE_SCALE) {
            "Sentence silence must be between $MIN_SENTENCE_SILENCE_SCALE and $MAX_SENTENCE_SILENCE_SCALE for Sherpa TTS."
        }
        val speaker = request.voice as? TextToSpeechVoiceCondition.FixedSpeaker
            ?: error("Sherpa TTS requires a fixed speaker voice.")
        cancelled.set(false)
        Log.i(
            TAG,
            "Sherpa TTS synthesis started: textLength=${request.text.length}, language=${request.languageCode}, " +
                "speaker=${speaker.speakerId}, speed=${request.speed}, silenceScale=${request.sentenceSilenceScale}",
        )
        val generationConfig = GenerationConfig().apply {
            speed = request.speed
            sid = speaker.speakerId
            silenceScale = request.sentenceSilenceScale
            when (profile) {
                ModelProfileIds.POCKET_TTS -> {
                    val reference = requireNotNull(referenceAudio) {
                        "Pocket TTS requires the bundled default reference voice."
                    }
                    this.referenceAudio = reference.samples
                    referenceSampleRate = reference.sampleRateHz
                    numSteps = 5
                    extra = mapOf(
                        "max_reference_audio_len" to "12",
                        "seed" to "42",
                    )
                }

                else -> extra = mapOf("lang" to request.languageCode)
            }
        }
        val audio = try {
            active.generateWithConfigAndCallback(
                request.text,
                generationConfig,
                AudioChunkCallback(cancelled, onAudioChunk),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Sherpa TTS native synthesis failed: ${error.message}", error)
            throw error
        }
        check(!cancelled.get()) { "Speech synthesis was cancelled." }
        require(audio.samples.isNotEmpty()) { "The voice model returned no audio." }
        Log.i(
            TAG,
            "Sherpa TTS synthesis completed: samples=${audio.samples.size}, sampleRateHz=${audio.sampleRate}",
        )
        return TextToSpeechResult(
            samples = audio.samples,
            sampleRateHz = audio.sampleRate,
        )
    }

    override fun cancel() {
        Log.i(TAG, "Sherpa TTS cancellation flag set.")
        cancelled.set(true)
    }

    override fun unload() = synchronized(lock) {
        Log.i(TAG, "Sherpa TTS unload requested.")
        unloadLocked()
    }

    private fun unloadLocked() {
        if (tts != null) Log.i(TAG, "Sherpa TTS releasing native model.")
        cancelled.set(true)
        tts?.release()
        tts = null
        loadedDirectory = null
        loadedProfile = null
        loadedThreadCount = 0
        loadedReferenceAudio = null
    }

    private fun effectiveThreads(requested: Int): Int = requested.takeIf { it > 0 }
        ?: Runtime.getRuntime().availableProcessors().coerceIn(1, 4)

    private fun validateRequiredFiles(profile: ModelProfileId, directory: File) {
        when (profile) {
            ModelProfileIds.SUPERTONIC_TTS -> {
                val required = SherpaProfiles.missingFiles(
                    directory,
                    SherpaProfiles.supertonic3RequiredFiles,
                )
                require(required.isEmpty()) {
                    "Supertonic model files are missing: ${required.joinToString()}"
                }
            }

            ModelProfileIds.PIPER_VITS_TTS -> {
                val required = listOf("en_US-lessac-medium.onnx", "tokens.txt")
                    .filterNot { File(directory, it).isFile }
                require(required.isEmpty()) {
                    "Piper model files are missing: ${required.joinToString()}"
                }
                require(File(directory, "espeak-ng-data").isDirectory) {
                    "Piper frontend data directory is missing."
                }
            }

            ModelProfileIds.KOKORO_TTS -> {
                val required = listOf(
                    "model.onnx",
                    "voices.bin",
                    "tokens.txt",
                    "lexicon-us-en.txt",
                    "lexicon-zh.txt",
                    "date-zh.fst",
                    "number-zh.fst",
                    "phone-zh.fst",
                ).filterNot { File(directory, it).isFile }
                require(required.isEmpty()) {
                    "Kokoro model files are missing: ${required.joinToString()}"
                }
                require(File(directory, "espeak-ng-data").isDirectory) {
                    "Kokoro frontend data directory is missing."
                }
                require(File(directory, "dict").isDirectory) {
                    "Kokoro dictionary data directory is missing."
                }
            }

            ModelProfileIds.POCKET_TTS -> {
                val required = listOf(
                    "lm_flow.int8.onnx",
                    "lm_main.int8.onnx",
                    "encoder.onnx",
                    "decoder.int8.onnx",
                    "text_conditioner.onnx",
                    "vocab.json",
                    "token_scores.json",
                    pocketDefaultReferencePath,
                ).filterNot { File(directory, it).isFile }
                require(required.isEmpty()) {
                    "Pocket TTS model files are missing: ${required.joinToString()}"
                }
            }

            else -> error("Unsupported TTS profile: ${profile.value}")
        }
    }

    private data class Pcm16Wave(val sampleRateHz: Int, val samples: FloatArray) {
        companion object {
            fun read(file: File): Pcm16Wave = RandomAccessFile(file, "r").use { input ->
                require(readAscii(input, 4) == "RIFF") { "Pocket TTS reference is not a RIFF WAV." }
                input.skipBytes(4)
                require(readAscii(input, 4) == "WAVE") { "Pocket TTS reference is not a WAV file." }
                var sampleRateHz = 0
                var channels = 0
                var bitsPerSample = 0
                var pcmData: ByteArray? = null
                while (input.filePointer + 8 <= input.length()) {
                    val chunk = readAscii(input, 4)
                    val size = Integer.reverseBytes(input.readInt())
                    require(size >= 0 && input.filePointer + size <= input.length()) {
                        "Pocket TTS reference WAV is invalid."
                    }
                    when (chunk) {
                        "fmt " -> {
                            require(size >= 16) { "Pocket TTS reference WAV format is invalid." }
                            val format = java.lang.Short.reverseBytes(input.readShort()).toInt()
                            channels = java.lang.Short.reverseBytes(input.readShort()).toInt()
                            sampleRateHz = Integer.reverseBytes(input.readInt())
                            input.skipBytes(6)
                            bitsPerSample = java.lang.Short.reverseBytes(input.readShort()).toInt()
                            require(format == 1 && channels == 1 && bitsPerSample == 16) {
                                "Pocket TTS reference must be mono PCM16 WAV."
                            }
                            input.seek(input.filePointer + size - 16)
                        }

                        "data" -> pcmData = ByteArray(size).also(input::readFully)
                        else -> input.seek(input.filePointer + size)
                    }
                    if (size % 2 == 1 && input.filePointer < input.length()) input.skipBytes(1)
                }
                val data = requireNotNull(pcmData) { "Pocket TTS reference WAV data is missing." }
                require(sampleRateHz > 0 && channels == 1 && bitsPerSample == 16) {
                    "Pocket TTS reference WAV format is missing."
                }
                Pcm16Wave(
                    sampleRateHz = sampleRateHz,
                    samples = FloatArray(data.size / 2) { index ->
                        val lo = data[index * 2].toInt() and 0xff
                        val hi = data[index * 2 + 1].toInt()
                        ((hi shl 8) or lo).toShort() / 32_768f
                    },
                )
            }

            private fun readAscii(input: RandomAccessFile, size: Int): String = ByteArray(size)
                .also(input::readFully)
                .toString(Charsets.US_ASCII)
        }
    }

    /**
     * The sherpa-onnx JNI binding looks up this exact specialised Kotlin method:
     * `invoke(float[]): Integer`. Do not replace this with an inline lambda: newer
     * Android desugaring emits only the erased `invoke(Object): Object` method.
     */
    private class AudioChunkCallback(
        private val cancelled: AtomicBoolean,
        private val onAudioChunk: (FloatArray) -> Boolean,
    ) : Function1<FloatArray, Int> {
        override fun invoke(samples: FloatArray): Int = when {
            cancelled.get() -> 0
            samples.isEmpty() || onAudioChunk(samples) -> 1
            else -> {
                cancelled.set(true)
                0
            }
        }
    }

    private companion object {
        const val TAG = "AiP123Tts"
        const val pocketDefaultReferencePath = "test_wavs/bria.wav"
        const val MIN_SENTENCE_SILENCE_SCALE = 0.001f
        const val MAX_SENTENCE_SILENCE_SCALE = 1f
    }
}
