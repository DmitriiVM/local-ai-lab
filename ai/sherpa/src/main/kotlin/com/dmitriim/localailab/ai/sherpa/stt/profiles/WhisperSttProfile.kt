package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.sherpa.stt.offlineSherpaSession
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import dev.zacsweers.metro.Inject

private val whisperSttProfileId = ModelProfileId("WHISPER_STT")

@Inject
class WhisperSttProfile :
    BaseSherpaSttProfile(
        profileId = whisperSttProfileId,
    ) {
    override fun open(
        request: SpeechToTextLoadRequest,
        artifacts: ModelArtifacts,
        threadCount: Int,
    ) = offlineSherpaSession(artifacts, threadCount) {
        whisper = OfflineWhisperModelConfig().apply {
            encoder = artifacts.require(ModelFileRoles.ENCODER).path
            decoder = artifacts.require(ModelFileRoles.DECODER).path
            language = request.languageCode
            task = "transcribe"
            enableSegmentTimestamps = true
        }
    }
}
