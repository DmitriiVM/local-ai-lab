package com.dmitriim.localaiplayground.core.audio.output.storage

import android.app.Application
import android.net.Uri
import android.util.Log
import com.dmitriim.localaiplayground.core.audio.output.model.GeneratedAudioFile
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.FileInputStream

/**
 * Retains exactly one successful generated WAV. A temporary file is never exposed
 * and is replaced atomically only after a complete, valid WAV has been written.
 */
@Inject
@SingleIn(AppScope::class)
class GeneratedAudioStore(private val application: Application) {
    private val directory = File(application.filesDir, DIRECTORY_NAME)
    private val latestFile get() = File(directory, LATEST_NAME)
    private val partialFile get() = File(directory, PARTIAL_NAME)
    private val backupFile get() = File(directory, BACKUP_NAME)

    @Synchronized
    fun saveLatest(samples: FloatArray, sampleRateHz: Int): GeneratedAudioFile {
        require(samples.isNotEmpty()) { "Generated audio is empty." }
        require(sampleRateHz > 0) { "Generated audio has an invalid sample rate." }
        require(directory.exists() || directory.mkdirs()) {
            "Could not create app-private generated-audio storage."
        }
        Log.i(TAG, "Saving generated WAV: samples=${samples.size}, sampleRateHz=$sampleRateHz")
        partialFile.delete()
        WaveFileCodec.write(partialFile, samples, sampleRateHz)
        require(WaveFileCodec.readMetadata(partialFile) != null) { "The generated WAV could not be validated." }
        backupFile.delete()
        if (latestFile.exists()) {
            require(latestFile.renameTo(backupFile)) {
                "Could not preserve the previous generated audio during replacement."
            }
        }
        try {
            if (!partialFile.renameTo(latestFile)) {
                partialFile.copyTo(latestFile, overwrite = true)
                require(partialFile.delete()) { "Could not finish generated-audio cleanup." }
            }
            require(WaveFileCodec.readMetadata(latestFile) != null) { "The retained WAV could not be validated." }
            backupFile.delete()
        } catch (error: Throwable) {
            latestFile.delete()
            if (backupFile.exists()) backupFile.renameTo(latestFile)
            throw error
        }
        return requireNotNull(WaveFileCodec.readMetadata(latestFile)).also {
            Log.i(
                TAG,
                "Generated WAV saved: bytes=${latestFile.length()}, durationMs=${it.durationMs}",
            )
        }
    }

    @Synchronized
    fun latest(): GeneratedAudioFile? {
        partialFile.delete()
        if (!latestFile.exists() && backupFile.exists()) backupFile.renameTo(latestFile)
        if (latestFile.exists()) backupFile.delete()
        return WaveFileCodec.readMetadata(latestFile).also { audio ->
            Log.i(TAG, "Latest generated WAV lookup: available=${audio != null}")
        }
    }

    fun discardPartial() {
        synchronized(this) {
            val discarded = partialFile.delete()
            if (discarded) Log.i(TAG, "Discarded partial generated WAV.")
        }
    }

    /** Clears only app-private retained output; previously exported documents are unaffected. */
    fun clearLatest() {
        synchronized(this) {
            partialFile.delete()
            backupFile.delete()
            latestFile.delete()
        }
    }

    fun export(audio: GeneratedAudioFile, destination: Uri) {
        val source = checkedFile(audio)
        Log.i(
            TAG,
            "Exporting generated WAV: bytes=${source.length()}, destinationScheme=${destination.scheme}",
        )
        application.contentResolver.openOutputStream(destination, "w")?.use { output ->
            FileInputStream(source).use { input -> input.copyTo(output) }
        } ?: error("Android could not open the selected export destination.")
        Log.i(TAG, "Generated WAV export completed.")
    }

    fun streamPcm16(audio: GeneratedAudioFile, onChunk: (ByteArray) -> Boolean) {
        val source = checkedFile(audio)
        Log.i(
            TAG,
            "Streaming retained WAV for replay: bytes=${source.length()}, samples=${audio.sampleCount}",
        )
        WaveFileCodec.streamPcm16(audio, onChunk)
    }

    private fun checkedFile(audio: GeneratedAudioFile): File {
        val file = File(audio.filePath)
        require(file.canonicalFile == latestFile.canonicalFile && file.isFile) {
            "The latest generated audio is no longer available."
        }
        return file
    }

    companion object {
        private const val TAG = "AiP123Tts"
        const val DIRECTORY_NAME = "generated-audio"
        private const val LATEST_NAME = "latest.wav"
        private const val PARTIAL_NAME = "latest.partial"
        private const val BACKUP_NAME = "latest.backup"
    }
}
