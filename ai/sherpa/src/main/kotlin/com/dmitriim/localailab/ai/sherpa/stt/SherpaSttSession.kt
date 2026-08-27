package com.dmitriim.localailab.ai.sherpa.stt

import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizer

interface SherpaSttSession {
    fun transcribe(samples: FloatArray, sampleRateHz: Int, cancelled: () -> Boolean): String
    fun release()

    class Offline(private val recognizer: OfflineRecognizer) : SherpaSttSession {
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

    class Online(private val recognizer: OnlineRecognizer) : SherpaSttSession {
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
