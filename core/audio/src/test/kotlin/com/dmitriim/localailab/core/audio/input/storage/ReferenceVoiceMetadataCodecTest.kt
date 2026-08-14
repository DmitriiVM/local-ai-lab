package com.dmitriim.localailab.core.audio.input.storage

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReferenceVoiceMetadataCodecTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `metadata round trip requires its PCM file`() {
        val directory = temporaryFolder.newFolder("voices")
        val voice = voice(directory)
        File(voice.pcmFilePath).writeBytes(ByteArray(8))
        val metadata = File(directory, "${voice.id}.properties")

        ReferenceVoiceMetadataCodec.write(metadata, voice)

        assertEquals(voice, ReferenceVoiceMetadataCodec.read(directory, metadata))
        File(voice.pcmFilePath).delete()
        assertNull(ReferenceVoiceMetadataCodec.read(directory, metadata))
    }

    @Test
    fun `corrupt or unsafe metadata is ignored`() {
        val directory = temporaryFolder.newFolder("voices")
        val metadata = File(directory, "unsafe.properties")
        metadata.writeText("id=../outside\ndisplayName=Voice\ndurationMs=1\ncreatedAtEpochMs=1\nsourceDescription=Test")

        assertNull(ReferenceVoiceMetadataCodec.read(directory, metadata))
    }

    @Test
    fun `retained PCM length is bounded to ten seconds`() {
        assertEquals(480_000L, ReferenceVoiceMetadataCodec.retainedPcmBytes(10_000L))
        assertEquals(480_000L, ReferenceVoiceMetadataCodec.retainedPcmBytes(60_000L))
        assertEquals(0L, ReferenceVoiceMetadataCodec.retainedPcmBytes(-1L))
    }

    private fun voice(directory: File) = ReferenceVoice(
        id = "voice-1",
        displayName = "Voice",
        durationMs = 1,
        createdAtEpochMs = 2,
        sourceDescription = "Test",
        pcmFilePath = File(directory, "voice-1.pcm").absolutePath,
    )
}
