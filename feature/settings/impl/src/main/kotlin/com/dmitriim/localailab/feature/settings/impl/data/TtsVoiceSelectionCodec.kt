package com.dmitriim.localailab.feature.settings.impl.data

import java.nio.charset.StandardCharsets
import java.util.Base64

/** Encodes model and voice IDs safely for storage in a DataStore string set. */
internal object TtsVoiceSelectionCodec {
    fun encode(modelId: String, voiceId: String): String = "${encodePart(modelId)}:${encodePart(voiceId)}"

    fun decode(value: String): Pair<String, String>? = runCatching {
        val separator = value.indexOf(':')
        require(separator > 0 && separator < value.lastIndex)
        val modelPart = value.substring(0, separator)
        val voicePart = value.substring(separator + 1)
        val modelId = decodePart(modelPart)
        val voiceId = decodePart(voicePart)
        require(encodePart(modelId) == modelPart && encodePart(voiceId) == voicePart)
        modelId to voiceId
    }.getOrNull()

    private fun encodePart(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodePart(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )
}
