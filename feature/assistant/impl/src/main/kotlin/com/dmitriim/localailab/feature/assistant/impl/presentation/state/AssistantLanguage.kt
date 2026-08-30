package com.dmitriim.localailab.feature.assistant.impl.presentation.state

internal fun normalizeLanguageCode(value: String): String = when (val normalized = value.lowercase()) {
    "english" -> "en"
    "russian" -> "ru"
    "chinese" -> "zh"
    "japanese" -> "ja"
    "korean" -> "ko"
    "cantonese" -> "yue"
    else -> normalized.substringBefore('-').substringBefore('_')
}
