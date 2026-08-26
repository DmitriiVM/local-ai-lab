package com.dmitriim.localailab.ai.api.system

/** Reports whether an operating-system STT runtime can be offered on the current device. */
interface SystemSpeechToTextSupport {
    val isOnDeviceRecognizerAvailable: Boolean
}
