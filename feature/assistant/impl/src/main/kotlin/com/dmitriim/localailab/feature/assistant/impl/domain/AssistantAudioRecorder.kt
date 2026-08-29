package com.dmitriim.localailab.feature.assistant.impl.domain

import com.dmitriim.localailab.core.audio.input.model.AudioLevel
import com.dmitriim.localailab.core.audio.input.model.PcmAudioInput
import com.dmitriim.localailab.core.audio.input.storage.AudioInputStore
import dev.zacsweers.metro.Inject

@Inject
class AssistantAudioRecorder(
    private val audioInputStore: AudioInputStore,
) {
    suspend fun record(sampleRateHz: Int, onLevel: (AudioLevel) -> Unit): PcmAudioInput = audioInputStore.capture(sampleRateHz, onLevel)

    fun stop() = audioInputStore.stopCapture()

    fun clear(input: PcmAudioInput?) = audioInputStore.clear(input)
}
