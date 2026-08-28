package com.dmitriim.localailab.ai.sherpa.stt

import com.dmitriim.localailab.ai.api.model.ModelRuntimeProfile
import com.dmitriim.localailab.ai.api.stt.SpeechToTextLoadRequest
import com.dmitriim.localailab.core.model.manifest.ModelFileRoles
import com.dmitriim.localailab.core.model.manifest.ModelProfileIds
import com.dmitriim.localailab.core.model.runtime.ModelArtifacts
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import dev.zacsweers.metro.Inject

@Inject
class SenseVoiceSttProfile :
    BaseSherpaSttProfile(
        ModelProfileIds.SENSE_VOICE_STT,
    ) {
    override fun open(request: SpeechToTextLoadRequest, artifacts: ModelArtifacts, threadCount: Int) = offlineSherpaSession(artifacts, threadCount) {
        senseVoice = OfflineSenseVoiceModelConfig().apply {
            model = artifacts.require(ModelFileRoles.PRIMARY_MODEL).path
            language = request.languageCode
            useInverseTextNormalization = true
        }
    }
}
