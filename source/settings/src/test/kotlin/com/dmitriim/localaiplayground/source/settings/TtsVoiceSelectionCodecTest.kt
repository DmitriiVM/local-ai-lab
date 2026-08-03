package com.dmitriim.localaiplayground.source.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsVoiceSelectionCodecTest {
    @Test
    fun roundTripsUnicodeIdsAndDelimiters() {
        val encoded = TtsVoiceSelectionCodec.encode("model:東京", "voice:đặc biệt")

        assertEquals("model:東京" to "voice:đặc biệt", TtsVoiceSelectionCodec.decode(encoded))
    }

    @Test
    fun ignoresMalformedStoredSelections() {
        assertNull(TtsVoiceSelectionCodec.decode("missing-separator"))
        assertNull(TtsVoiceSelectionCodec.decode("not-base64:not-base64"))
    }
}
