package com.dmitriim.localailab.ai.system

import android.annotation.TargetApi
import android.app.Application
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRequest
import com.dmitriim.localailab.ai.api.stt.SpeechToTextResult
import com.dmitriim.localailab.ai.api.stt.SpeechToTextRuntime
import com.dmitriim.localailab.ai.runtime.model.ModelRuntimeProfileRegistry
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.engine.EngineId
import com.dmitriim.localailab.core.model.manifest.ModelProfileKey
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

@Inject
@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<SpeechToTextRuntime>())
class AndroidSpeechRecognizerRuntime(
    private val application: Application,
    private val profiles: ModelRuntimeProfileRegistry,
) : SpeechToTextRuntime {
    override val engineId = EngineId("android-speech-recognizer")

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var languageCode: String? = null

    @Volatile private var activeSession: RecognitionSession? = null

    @Volatile private var activeAudioSink: ParcelFileDescriptor? = null

    @Volatile private var cancelled = false

    override val isLoaded: Boolean
        get() = synchronized(lock) { recognizer != null }

    override fun load(request: SpeechToTextLoadRequest): SpeechToTextLoadResult = synchronized(lock) {
        require(request.engineId == engineId) {
            "Unsupported STT engine: ${request.engineId.value}"
        }
        profiles.requireTyped<AndroidSpeechRecognizerRuntimeProfile>(
            ModelProfileKey(request.engineId, request.profileType),
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            error("Android SpeechRecognizer audio input requires Android 13 or newer.")
        }
        require(SpeechRecognizer.isOnDeviceRecognitionAvailable(application)) {
            "The device does not provide an on-device Android speech recognition service."
        }
        if (recognizer != null && languageCode == request.languageCode) {
            return SpeechToTextLoadResult(1, loadDurationMs = 0, coldStart = false)
        }
        unloadLocked()
        val duration = measureTimeMillis {
            recognizer = onMain {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(application)
            }
        }
        languageCode =
            resolveLanguageTag(
                checkNotNull(recognizer),
                request.languageCode,
            ).also { resolvedLanguage ->
                Log.i(
                    TAG,
                    "Android SpeechRecognizer language resolved: requested=${request.languageCode}, resolved=$resolvedLanguage",
                )
            }
        cancelled = false
        SpeechToTextLoadResult(1, duration, coldStart = true)
    }

    override fun transcribe(request: SpeechToTextRequest): SpeechToTextResult = synchronized(lock) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            error("Android SpeechRecognizer audio input requires Android 13 or newer.")
        }
        require(request.samples.isNotEmpty()) { "The audio input is empty." }
        check(!cancelled) { "Transcription was cancelled." }
        val activeRecognizer = checkNotNull(recognizer) {
            "Load Android SpeechRecognizer before transcription."
        }
        val locale = checkNotNull(languageCode)
        val session = RecognitionSession()
        val pipe = ParcelFileDescriptor.createPipe()
        val source = pipe[0]
        val sink = pipe[1]
        activeSession = session
        activeAudioSink = sink

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, source)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, request.sampleRateHz)
        }

        return try {
            var text = ""
            val duration = measureTimeMillis {
                onMain {
                    activeRecognizer.setRecognitionListener(session.listener)
                    activeRecognizer.startListening(intent)
                }
                // The recognizer dispatches this request asynchronously. Keep our read descriptor
                // open until the service reports that it is ready and has opened the audio source.
                session.awaitReady(STARTUP_TIMEOUT_MS)
                Log.i(TAG, "Android SpeechRecognizer ready for injected audio.")
                runCatching { source.close() }
                val writeFailure = runCatching {
                    writePcm16(sink, request.samples, request.sampleRateHz)
                }.exceptionOrNull()
                if (writeFailure != null) session.resolveWriteFailure(writeFailure)
                activeAudioSink = null
                Log.i(
                    TAG,
                    "Android SpeechRecognizer audio replay completed: samples=${request.samples.size}",
                )
                if (!session.isCompleted) {
                    onMain { activeRecognizer.stopListening() }
                    Log.i(TAG, "Android SpeechRecognizer finalization requested.")
                }
                if (!session.await(RESULT_TIMEOUT_MS)) {
                    Log.e(TAG, "Android SpeechRecognizer result timed out.")
                    onMain { activeRecognizer.cancel() }
                    error("Android SpeechRecognizer did not finish processing the supplied audio.")
                }
                text = session.result.getOrThrow()
            }
            SpeechToTextResult(text.trim(), duration)
        } catch (error: AndroidSpeechRecognitionException) {
            if (error.errorCode == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                val requested = requestLanguageModelDownload(activeRecognizer, locale)
                throw languageModelUnavailable(locale, requested, error)
            }
            throw error
        } finally {
            activeAudioSink = null
            activeSession = null
            runCatching { source.close() }
            runCatching { sink.close() }
        }
    }

    override fun cancel() {
        cancelled = true
        activeSession?.fail(IllegalStateException("Transcription was cancelled."))
        runCatching { activeAudioSink?.close() }
        val activeRecognizer = synchronized(lock) { recognizer }
        if (activeRecognizer != null) {
            runCatching { onMain { activeRecognizer.cancel() } }
        }
    }

    override fun unload() = synchronized(lock) {
        unloadLocked()
    }

    private fun unloadLocked() {
        runCatching { activeAudioSink?.close() }
        activeAudioSink = null
        activeSession?.fail(IllegalStateException("Transcription was cancelled."))
        activeSession = null
        recognizer?.let { active ->
            runCatching {
                onMain {
                    active.cancel()
                    active.destroy()
                }
            }
        }
        recognizer = null
        languageCode = null
        cancelled = false
    }

    private fun writePcm16(sink: ParcelFileDescriptor, samples: FloatArray, sampleRateHz: Int) {
        ParcelFileDescriptor.AutoCloseOutputStream(sink).use { output ->
            val buffer = ByteArray(PCM_CHUNK_SAMPLES * 2)
            val leadingSamples = sampleRateHz * LEADING_SILENCE_MS / MILLIS_PER_SECOND
            val trailingSamples = sampleRateHz * TRAILING_SILENCE_MS / MILLIS_PER_SECOND
            val replaySamples = leadingSamples + samples.size + trailingSamples
            val startedAtNs = System.nanoTime()
            var offset = 0
            while (offset < replaySamples) {
                check(!cancelled) { "Transcription was cancelled." }
                val count = minOf(PCM_CHUNK_SAMPLES, replaySamples - offset)
                repeat(count) { index ->
                    val sourceIndex = offset + index - leadingSamples
                    val sample = if (sourceIndex in samples.indices) samples[sourceIndex] else 0f
                    val pcm = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE)
                        .roundToInt()
                        .toShort()
                        .toInt()
                    buffer[index * 2] = (pcm and 0xff).toByte()
                    buffer[index * 2 + 1] = ((pcm ushr 8) and 0xff).toByte()
                }
                output.write(buffer, 0, count * 2)
                offset += count
                paceAudioReplay(startedAtNs, offset, sampleRateHz)
            }
        }
    }

    /** Android's on-device recognizers apply live voice-activity detection to injected PCM. */
    private fun paceAudioReplay(startedAtNs: Long, writtenSamples: Int, sampleRateHz: Int) {
        val targetElapsedNs =
            writtenSamples.toLong() * NANOS_PER_SECOND / sampleRateHz.coerceAtLeast(1)
        val remainingNs = targetElapsedNs - (System.nanoTime() - startedAtNs)
        if (remainingNs > 0) {
            Thread.sleep(
                remainingNs / NANOS_PER_MILLISECOND,
                (remainingNs % NANOS_PER_MILLISECOND).toInt(),
            )
        }
    }

    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val task = FutureTask(block)
        check(mainHandler.post(task)) { "Could not dispatch Android SpeechRecognizer work." }
        return task.get()
    }

    /**
     * The rest of the app uses compact language codes (for example, "en"), while platform
     * recognizers often accept only an installed regional BCP-47 tag (for example, "en-US").
     */
    private fun resolveLanguageTag(
        recognizer: SpeechRecognizer,
        requestedLanguage: String,
    ): String {
        val fallback = platformLanguageFallback(requestedLanguage)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return fallback
        if (Looper.myLooper() == Looper.getMainLooper()) return fallback
        val support = RecognitionSupportSession()
        onMain {
            recognizer.checkRecognitionSupport(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, fallback)
                },
                Executor { command -> mainHandler.post(command) },
                support,
            )
        }
        val recognitionSupport = support.await(SUPPORT_CHECK_TIMEOUT_MS) ?: return fallback.also {
            Log.w(TAG, "Android SpeechRecognizer support check timed out; using fallback=$fallback")
        }
        matchingLanguage(recognitionSupport.installedOnDeviceLanguages, requestedLanguage)?.let {
            Log.i(TAG, "Android SpeechRecognizer selected installed language: language=$it")
            return it
        }
        matchingLanguage(recognitionSupport.pendingOnDeviceLanguages, requestedLanguage)?.let {
            error(
                "The $it on-device speech model is still downloading. " +
                    "Wait for Android to finish installing it, then retry.",
            )
        }
        matchingLanguage(recognitionSupport.supportedOnDeviceLanguages, requestedLanguage)?.let {
            val requested = requestLanguageModelDownload(recognizer, it)
            throw languageModelUnavailable(it, requested)
        }
        val reportedLanguageCount = recognitionSupport.installedOnDeviceLanguages.size +
            recognitionSupport.pendingOnDeviceLanguages.size +
            recognitionSupport.supportedOnDeviceLanguages.size
        if (reportedLanguageCount == 0) {
            Log.w(
                TAG,
                "Android SpeechRecognizer did not report language availability; using fallback=$fallback",
            )
            return fallback
        }
        error("The selected language is not supported by the on-device recognizer.")
    }

    private fun matchingLanguage(languages: List<String>, requestedLanguage: String): String? {
        val requestedLocale = Locale.forLanguageTag(requestedLanguage)
        return languages.firstOrNull { it.equals(requestedLanguage, ignoreCase = true) }
            ?: languages.firstOrNull {
                Locale.forLanguageTag(
                    it,
                ).language.equals(requestedLocale.language, ignoreCase = true)
            }
    }

    private fun requestLanguageModelDownload(
        recognizer: SpeechRecognizer,
        languageTag: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val request = runCatching {
            onMain {
                recognizer.triggerModelDownload(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    },
                )
            }
        }
        request.onSuccess {
            Log.i(
                TAG,
                "Android SpeechRecognizer language model download requested: language=$languageTag",
            )
        }.onFailure { error ->
            Log.e(
                TAG,
                "Android SpeechRecognizer could not request language model download: language=$languageTag",
                error,
            )
        }
        return request.isSuccess
    }

    private fun languageModelUnavailable(
        languageTag: String,
        downloadRequested: Boolean,
        cause: Throwable? = null,
    ): IllegalStateException {
        val action = if (downloadRequested) {
            "Android was asked to download it. Complete any system prompt, wait for the download, then retry."
        } else {
            "Install it from the device's on-device speech recognition settings, then retry."
        }
        return IllegalStateException(
            "The $languageTag on-device speech model is not installed. $action",
            cause,
        )
    }

    private fun platformLanguageFallback(languageCode: String): String = when (languageCode) {
        "en" -> "en-US"
        "ru" -> "ru-RU"
        "zh" -> "zh-CN"
        "ja" -> "ja-JP"
        "ko" -> "ko-KR"
        "yue" -> "yue-Hant-HK"
        else -> languageCode
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private class RecognitionSupportSession : RecognitionSupportCallback {
        private val latch = CountDownLatch(1)

        @Volatile private var recognitionSupport: RecognitionSupport? = null

        override fun onSupportResult(recognitionSupport: RecognitionSupport) {
            this.recognitionSupport = recognitionSupport
            latch.countDown()
        }

        override fun onError(error: Int) {
            latch.countDown()
        }

        fun await(timeoutMs: Long): RecognitionSupport? {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            return recognitionSupport
        }
    }

    private class RecognitionSession {
        private val completed = AtomicBoolean(false)
        private val ready = AtomicBoolean(false)
        private val startupLatch = CountDownLatch(1)
        private val latch = CountDownLatch(1)
        private val segmentTranscript = StringBuilder()
        private var partialTranscript = ""

        @Volatile var result: Result<String> = Result.failure(
            IllegalStateException("Android SpeechRecognizer did not return a result."),
        )
            private set

        val listener = object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val finalText = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                val text = finalText
                    .ifBlank { segmentTranscript.toString() }
                    .ifBlank { partialTranscript }
                Log.i(
                    TAG,
                    "Android SpeechRecognizer final result received: transcriptLength=${text.length}",
                )
                succeed(text)
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Android SpeechRecognizer callback error: code=$error")
                fail(AndroidSpeechRecognitionException(error, errorMessage(error)))
            }

            override fun onReadyForSpeech(params: Bundle) {
                if (!isCompleted) {
                    ready.set(true)
                    startupLatch.countDown()
                }
            }
            override fun onBeginningOfSpeech() {
                Log.i(TAG, "Android SpeechRecognizer detected beginning of speech.")
            }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray) = Unit
            override fun onEndOfSpeech() {
                Log.i(TAG, "Android SpeechRecognizer detected end of speech.")
            }
            override fun onPartialResults(partialResults: Bundle) {
                val text = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) {
                    partialTranscript = text
                    Log.i(
                        TAG,
                        "Android SpeechRecognizer partial result received: transcriptLength=${text.length}",
                    )
                }
            }
            override fun onSegmentResults(segmentResults: Bundle) {
                val text = segmentResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) {
                    if (segmentTranscript.isNotEmpty()) segmentTranscript.append(' ')
                    segmentTranscript.append(text)
                }
            }
            override fun onEndOfSegmentedSession() {
                succeed(segmentTranscript.toString().ifBlank { partialTranscript })
            }
            override fun onEvent(eventType: Int, params: Bundle) = Unit
        }

        fun await(timeoutMs: Long): Boolean = latch.await(timeoutMs, TimeUnit.MILLISECONDS)

        fun awaitReady(timeoutMs: Long) {
            check(startupLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                "Android SpeechRecognizer did not become ready to receive audio."
            }
            if (isCompleted) result.getOrThrow()
            check(ready.get()) { "Android SpeechRecognizer ended before accepting audio." }
        }

        /**
         * The service may close the audio pipe as it reports its startup error on the main thread.
         * Give that callback a short chance to arrive so callers see the platform error, rather
         * than the implementation-detail EPIPE thrown by the pipe writer.
         */
        fun resolveWriteFailure(writeFailure: Throwable) {
            if (isCompleted || await(WRITE_FAILURE_RESULT_GRACE_MS)) {
                result.exceptionOrNull()?.let { throw it }
                return
            }
            throw IllegalStateException(
                "The Android on-device recognizer stopped accepting the supplied audio. " +
                    "This device's recognition service does not support transcription of saved audio.",
                writeFailure,
            )
        }

        val isCompleted: Boolean get() = completed.get()

        fun fail(error: Throwable) = complete(Result.failure(error))

        private fun succeed(text: String) = complete(Result.success(text))

        private fun complete(value: Result<String>) {
            if (completed.compareAndSet(false, true)) {
                result = value
                startupLatch.countDown()
                latch.countDown()
            }
        }

        private fun errorMessage(error: Int) = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Android SpeechRecognizer could not read the supplied audio."
            SpeechRecognizer.ERROR_CLIENT -> "Android SpeechRecognizer rejected the recognition request."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Android SpeechRecognizer lacks audio permission."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "The selected language is not supported by the on-device recognizer."
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "The selected on-device language model is not installed."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            -> "The system recognizer unexpectedly requested a network connection."
            SpeechRecognizer.ERROR_NO_MATCH -> "Android SpeechRecognizer could not find speech in this audio."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Android SpeechRecognizer is busy."
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            -> "The Android speech recognition service failed."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Android SpeechRecognizer did not detect speech."
            else -> "Android SpeechRecognizer failed with error $error."
        }
    }

    private class AndroidSpeechRecognitionException(val errorCode: Int, message: String) : IllegalStateException(message)

    private companion object {
        const val TAG = "AiP123Stt"
        const val PCM_CHUNK_SAMPLES = 320
        const val LEADING_SILENCE_MS = 500
        const val TRAILING_SILENCE_MS = 750
        const val MILLIS_PER_SECOND = 1_000
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val STARTUP_TIMEOUT_MS = 10_000L
        const val WRITE_FAILURE_RESULT_GRACE_MS = 1_000L
        const val SUPPORT_CHECK_TIMEOUT_MS = 2_000L
        const val RESULT_TIMEOUT_MS = 15_000L
    }
}
