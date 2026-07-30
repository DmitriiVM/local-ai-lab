package com.dmitriim.localaiplayground.ai.system

import android.app.Application
import android.os.Build
import android.speech.SpeechRecognizer
import com.dmitriim.localaiplayground.ai.api.SystemSpeechToTextSupport
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AndroidSpeechRecognizerSupport(
    private val application: Application,
) : SystemSpeechToTextSupport {
    override val isOnDeviceRecognizerAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(application)
}
