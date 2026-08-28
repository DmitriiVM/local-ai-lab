package com.dmitriim.localailab.ai.api.system

/** Reports whether an operating-system speech recognizer can be offered on the current device. */
interface SystemSpeechToTextSupport {
    /** True only when an on-device recognizer is available without a network requirement. */
    val isOnDeviceRecognizerAvailable: Boolean
}
