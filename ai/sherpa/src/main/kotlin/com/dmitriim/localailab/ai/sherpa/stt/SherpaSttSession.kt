package com.dmitriim.localailab.ai.sherpa.stt

import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizer

/**
 * Owns a loaded Sherpa-ONNX speech recognizer and exposes one complete-utterance transcription.
 *
 * A session creates a native stream for each [transcribe] call and releases that stream before the
 * call returns. Call [release] when the loaded model is no longer needed to free the recognizer's
 * native resources.
 *
 * [NonStreaming] decodes an entire utterance at once. [Streaming] uses Sherpa's incremental
 * decoder; it still accepts a complete audio buffer here, so "streaming" does not imply network
 * access.
 */
interface SherpaSttSession {
    fun transcribe(samples: FloatArray, sampleRateHz: Int, cancelled: () -> Boolean): String
    fun release()

    class NonStreaming(private val recognizer: OfflineRecognizer) : SherpaSttSession {
        override fun transcribe(samples: FloatArray, sampleRateHz: Int, cancelled: () -> Boolean): String {
            val stream = recognizer.createStream()
            return try {
                check(!cancelled()) { "Transcription was cancelled." }
                stream.acceptWaveform(samples, sampleRateHz)
                recognizer.decode(stream)
                check(!cancelled()) { "Transcription was cancelled." }
                recognizer.getResult(stream).text
            } finally {
                stream.release()
            }
        }

        override fun release() = recognizer.release()
    }

    class Streaming(private val recognizer: OnlineRecognizer) : SherpaSttSession {
        override fun transcribe(samples: FloatArray, sampleRateHz: Int, cancelled: () -> Boolean): String {
            val stream = recognizer.createStream()
            return try {
                stream.acceptWaveform(samples, sampleRateHz)
                stream.inputFinished()
                while (recognizer.isReady(stream)) {
                    check(!cancelled()) { "Transcription was cancelled." }
                    recognizer.decode(stream)
                }
                recognizer.getResult(stream).text
            } finally {
                stream.release()
            }
        }

        override fun release() = recognizer.release()
    }
}
