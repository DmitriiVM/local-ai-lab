package com.dmitriim.localailab.ai.system

import android.app.Application
import android.media.AudioFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileKey
import com.dmitriim.localailab.ai.api.system.SystemTextToSpeechSupport
import com.dmitriim.localailab.ai.api.system.SystemTextToSpeechVoice
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechLoadResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRequest
import com.dmitriim.localailab.ai.api.tts.TextToSpeechResult
import com.dmitriim.localailab.ai.api.tts.TextToSpeechRuntime
import com.dmitriim.localailab.ai.api.tts.TextToSpeechVoiceCondition
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<TextToSpeechRuntime>())
@ContributesBinding(AppScope::class, binding = binding<SystemTextToSpeechSupport>())
class AndroidTextToSpeechRuntime(
    private val application: Application,
    private val profiles: ModelRuntimeProfileRegistry,
) : TextToSpeechRuntime,
    SystemTextToSpeechSupport {
    override val engineId = EngineId("android-text-to-speech")

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableVoices = MutableStateFlow<List<SystemTextToSpeechVoice>>(emptyList())
    override val voices: StateFlow<List<SystemTextToSpeechVoice>> = mutableVoices.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var initialization: Initialization? = null

    @Volatile private var activeSession: SynthesisSession? = null

    @Volatile private var cancelled = false

    override val isLoaded: Boolean
        get() = synchronized(lock) { textToSpeech != null }

    override fun refresh() {
        val engine = ensureInitialized()
        mutableVoices.value = engine.voices
            .orEmpty()
            .asSequence()
            .filter(::isUsableOnDeviceVoice)
            .map(::toSystemVoice)
            .distinctBy(SystemTextToSpeechVoice::id)
            .sortedWith(
                compareBy<SystemTextToSpeechVoice> { it.languageTag }
                    .thenBy { it.displayName.lowercase() },
            )
            .toList()
    }

    override fun load(request: TextToSpeechLoadRequest): TextToSpeechLoadResult {
        require(request.engineId == engineId) {
            "Unsupported Android text-to-speech engine: ${request.engineId.value}"
        }
        profiles.requireTyped<AndroidTextToSpeechRuntimeProfile>(
            ModelProfileKey(request.engineId, request.profileType),
        )
        val coldStart = !isLoaded
        val startedAt = System.nanoTime()
        ensureInitialized()
        if (mutableVoices.value.isEmpty()) refresh()
        require(mutableVoices.value.isNotEmpty()) {
            "Android does not have an installed on-device voice for English, Russian, or Chinese."
        }
        return TextToSpeechLoadResult(
            effectiveThreadCount = 1,
            loadDurationMs = (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND,
            coldStart = coldStart,
            // Android reports the selected voice's output format from onBeginSynthesis.
            sampleRateHz = 0,
            speakerCount = null,
        )
    }

    override fun synthesize(
        request: TextToSpeechRequest,
        onAudioChunk: (FloatArray) -> Boolean,
    ): TextToSpeechResult {
        require(request.text.length <= TextToSpeech.getMaxSpeechInputLength()) {
            "Android TextToSpeech accepts at most ${TextToSpeech.getMaxSpeechInputLength()} characters."
        }
        val selected = request.voice as? TextToSpeechVoiceCondition.PlatformVoice
            ?: error("Select an Android system voice before synthesis.")
        val engine = ensureInitialized()
        val voice = engine.voices.orEmpty().firstOrNull { it.name == selected.voiceId }
            ?.takeIf(::isUsableOnDeviceVoice)
            ?: error("The selected Android voice is no longer installed.")
        require(engine.setVoice(voice) == TextToSpeech.SUCCESS) {
            "Android could not select ${voice.name}."
        }
        require(engine.setSpeechRate(request.speed) == TextToSpeech.SUCCESS) {
            "Android could not apply the requested speech rate."
        }

        cancelled = false
        val utteranceId = "local-ai-${UUID.randomUUID()}"
        val session = SynthesisSession(utteranceId)
        activeSession = session
        engine.setOnUtteranceProgressListener(session.listener)
        val output = File.createTempFile("android-tts-", ".audio", application.cacheDir)
        return try {
            val queued = engine.synthesizeToFile(request.text, Bundle(), output, utteranceId)
            require(queued == TextToSpeech.SUCCESS) {
                "Android rejected the text-to-speech request."
            }
            require(session.await(SYNTHESIS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                engine.stop()
                "Android TextToSpeech did not finish in time."
            }
            session.failure?.let { throw it }
            check(!cancelled) { "Speech synthesis was cancelled." }
            val samples = session.samples()
            require(samples.isNotEmpty()) {
                "Android TextToSpeech completed without returning audio."
            }
            TextToSpeechResult(
                samples = samples,
                sampleRateHz = session.sampleRateHz,
            )
        } finally {
            activeSession = null
            output.delete()
        }
    }

    override fun cancel() {
        cancelled = true
        activeSession?.fail(IllegalStateException("Speech synthesis was cancelled."))
        synchronized(lock) { textToSpeech }?.stop()
    }

    override fun unload() {
        cancel()
        val engine = synchronized(lock) {
            textToSpeech.also { textToSpeech = null }
        }
        engine?.shutdown()
        cancelled = false
    }

    private fun ensureInitialized(): TextToSpeech {
        synchronized(lock) { textToSpeech }?.let { return it }
        val pending = synchronized(lock) {
            textToSpeech?.let { return it }
            initialization ?: Initialization().also {
                initialization = it
                startInitialization(it)
            }
        }
        require(pending.latch.await(INITIALIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "Android TextToSpeech did not initialize in time."
        }
        pending.failure?.let { throw it }
        return synchronized(lock) {
            checkNotNull(textToSpeech) { "Android TextToSpeech is unavailable on this device." }
        }
    }

    private fun startInitialization(initialization: Initialization) {
        val initialize = {
            lateinit var candidate: TextToSpeech
            candidate = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    synchronized(lock) {
                        textToSpeech = candidate
                        if (this.initialization === initialization) this.initialization = null
                    }
                } else {
                    initialization.failure =
                        IllegalStateException("Android TextToSpeech failed to initialize.")
                    candidate.shutdown()
                    synchronized(lock) {
                        if (this.initialization === initialization) this.initialization = null
                    }
                }
                initialization.latch.countDown()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initialize()
        } else {
            check(mainHandler.post(initialize)) {
                "Could not initialize Android TextToSpeech on the main thread."
            }
        }
    }

    private fun isUsableOnDeviceVoice(voice: Voice): Boolean = !voice.isNetworkConnectionRequired &&
        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty() &&
        voice.locale.language.lowercase() in SUPPORTED_LANGUAGES

    private fun toSystemVoice(voice: Voice): SystemTextToSpeechVoice {
        val localeName = voice.locale.getDisplayName(Locale.getDefault())
        return SystemTextToSpeechVoice(
            id = voice.name,
            displayName = "$localeName · ${voice.name}",
            languageTag = voice.locale.toLanguageTag(),
            description = "On-device · ${voice.quality.qualityLabel()}",
        )
    }

    private class Initialization {
        val latch = CountDownLatch(1)

        @Volatile var failure: Throwable? = null
    }

    private class SynthesisSession(private val utteranceId: String) {
        private val audio = ByteArrayOutputStream()
        private val completion = CountDownLatch(1)

        @Volatile var sampleRateHz: Int = 0
            private set

        @Volatile private var audioFormat: Int = AudioFormat.ENCODING_INVALID

        @Volatile private var channelCount: Int = 0

        @Volatile var failure: Throwable? = null
            private set

        val listener = object : UtteranceProgressListener() {
            override fun onStart(id: String) = Unit

            override fun onBeginSynthesis(
                id: String,
                sampleRateInHz: Int,
                format: Int,
                channels: Int,
            ) {
                if (id != utteranceId) return
                sampleRateHz = sampleRateInHz
                audioFormat = format
                channelCount = channels
            }

            override fun onAudioAvailable(id: String, chunk: ByteArray) {
                if (id != utteranceId) return
                synchronized(audio) { audio.write(chunk) }
            }

            override fun onDone(id: String) {
                if (id == utteranceId) completion.countDown()
            }

            @Deprecated("Deprecated by Android")
            override fun onError(id: String) {
                if (id ==
                    utteranceId
                ) {
                    fail(IllegalStateException("Android speech synthesis failed."))
                }
            }

            override fun onError(id: String, errorCode: Int) {
                if (id == utteranceId) {
                    fail(
                        IllegalStateException(
                            "Android speech synthesis failed with error $errorCode.",
                        ),
                    )
                }
            }

            override fun onStop(id: String, interrupted: Boolean) {
                if (id == utteranceId) {
                    fail(IllegalStateException("Android speech synthesis was stopped."))
                }
            }
        }

        fun await(timeout: Long, unit: TimeUnit): Boolean = completion.await(timeout, unit)

        fun fail(error: Throwable) {
            failure = error
            completion.countDown()
        }

        fun samples(): FloatArray {
            require(sampleRateHz > 0 && channelCount > 0) {
                "Android TextToSpeech returned an invalid audio format."
            }
            val bytes = synchronized(audio) { audio.toByteArray() }
            val bytesPerSample = when (audioFormat) {
                AudioFormat.ENCODING_PCM_8BIT -> 1
                AudioFormat.ENCODING_PCM_16BIT -> 2
                AudioFormat.ENCODING_PCM_FLOAT -> 4
                else -> error(
                    "Android TextToSpeech returned unsupported audio encoding $audioFormat.",
                )
            }
            val frameSize = bytesPerSample * channelCount
            require(bytes.size % frameSize == 0) {
                "Android TextToSpeech returned a truncated audio frame."
            }
            val frames = bytes.size / frameSize
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(frames) {
                var mixed = 0f
                repeat(channelCount) {
                    mixed += when (audioFormat) {
                        AudioFormat.ENCODING_PCM_8BIT ->
                            ((buffer.get().toInt() and 0xff) - 128) / 128f
                        AudioFormat.ENCODING_PCM_16BIT ->
                            buffer.short / Short.MAX_VALUE.toFloat()
                        AudioFormat.ENCODING_PCM_FLOAT ->
                            buffer.float.coerceIn(-1f, 1f)
                        else -> error("Unsupported Android speech audio encoding.")
                    }
                }
                (mixed / channelCount).coerceIn(-1f, 1f)
            }
        }
    }

    private fun Int.qualityLabel(): String = when (this) {
        Voice.QUALITY_VERY_HIGH -> "very high quality"
        Voice.QUALITY_HIGH -> "high quality"
        Voice.QUALITY_LOW -> "low quality"
        Voice.QUALITY_VERY_LOW -> "very low quality"
        else -> "normal quality"
    }

    private companion object {
        const val INITIALIZATION_TIMEOUT_SECONDS = 15L
        const val SYNTHESIS_TIMEOUT_SECONDS = 120L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val SUPPORTED_LANGUAGES = setOf("en", "ru", "zh")
    }
}
