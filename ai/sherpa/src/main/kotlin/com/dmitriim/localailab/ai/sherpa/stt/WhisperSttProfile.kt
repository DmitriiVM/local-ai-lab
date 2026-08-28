package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import dev.zacsweers.metro.Inject

@Inject
class WhisperSttProfile :
    BaseSherpaSttProfile(
        profileId = ModelProfileIds.WHISPER_STT,
        displayName = "Whisper STT bundle",
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = offlineSherpaSession(artifacts, threadCount) {
        whisper = OfflineWhisperModelConfig().apply {
            encoder = artifacts.require(ModelFileRoles.ENCODER).path
            decoder = artifacts.require(ModelFileRoles.DECODER).path
            language = request.languageCode
            task = "transcribe"
            enableSegmentTimestamps = true
        }
    }
}
