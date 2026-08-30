package com.dmitriim.localailab.feature.assistant.impl.presentation.state

import com.dmitriim.localailab.ai.api.chat.LlmEngineCapabilities
import com.dmitriim.localailab.ai.api.engine.ComputePreference
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelId

data class ChatModelOption(
    val id: ModelId,
    val displayName: String,
    val engineId: EngineId,
    val capabilities: LlmEngineCapabilities?,
    val defaultContextSize: Int,
    val installed: Boolean,
) {
    fun supportedComputePreference(preference: ComputePreference): ComputePreference {
        val supported = capabilities?.computePreferences.orEmpty()
        return preference.takeIf(supported::contains)
            ?: ComputePreference.AUTO.takeIf(supported::contains)
            ?: supported.firstOrNull()
            ?: preference
    }
}
