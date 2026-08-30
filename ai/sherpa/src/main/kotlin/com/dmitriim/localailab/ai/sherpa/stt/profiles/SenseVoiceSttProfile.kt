package com.dmitriim.localailab.ai.sherpa.stt.profiles

import com.dmitriim.localailab.ai.api.model.manifest.ModelFileRoles
import com.dmitriim.localailab.ai.api.model.manifest.ModelProfileId
import com.dmitriim.localailab.ai.api.model.runtime.ModelArtifacts
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.ai.sherpa.stt.offlineSherpaSession
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import dev.zacsweers.metro.Inject

private val senseVoiceSttProfileId = ModelProfileId("SENSE_VOICE_STT")

@Inject
class SenseVoiceSttProfile :
    BaseSherpaSttProfile(
        senseVoiceSttProfileId,
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) =
        offlineSherpaSession(artifacts, threadCount) {
            senseVoice = OfflineSenseVoiceModelConfig().apply {
                model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
                language = request.languageCode
                useInverseTextNormalization = true
            }
        }
}
