package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferNetworkPolicy
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState

internal fun ModelManifest.typeLabelRes(): Int = when {
    AiCapability.CHAT in capabilities -> CoreUiR.string.models_type_llm
    AiCapability.TEXT_TO_SPEECH in capabilities -> CoreUiR.string.models_type_tts
    AiCapability.SPEECH_TO_TEXT in capabilities -> CoreUiR.string.models_type_stt
    AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> CoreUiR.string.models_type_vad
    else -> CoreUiR.string.models_type_model
}

@Composable
internal fun ModelManifest.languageSummary(): String {
    val totalLanguageCount = supportedLanguageCount
    return when {
        AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> {
            stringResource(CoreUiR.string.models_language_independent)
        }
        languages.isEmpty() -> stringResource(CoreUiR.string.models_language_not_specified)
        totalLanguageCount != null && totalLanguageCount > languages.size ->
            "${languages.joinToString()} +${totalLanguageCount - languages.size}"
        else -> languages.joinToString()
    }
}

internal fun ModelValidationState.statusLabelRes(): Int = when (this) {
    ModelValidationState.READY -> CoreUiR.string.models_status_ready
    ModelValidationState.INVALID,
    ModelValidationState.MISSING_FILES,
    ModelValidationState.INCOMPATIBLE,
    -> CoreUiR.string.models_status_needs_attention
}

internal fun ModelTransferState?.statusLabelRes(): Int = when (this) {
    is ModelTransferState.Queued -> CoreUiR.string.models_status_queued
    is ModelTransferState.Running -> if (completedBytes >= totalBytes) {
        CoreUiR.string.models_status_verifying
    } else {
        CoreUiR.string.models_status_downloading
    }
    is ModelTransferState.Paused -> CoreUiR.string.models_status_paused
    ModelTransferState.Installing -> CoreUiR.string.models_status_installing
    is ModelTransferState.Failed -> CoreUiR.string.models_status_download_failed
    ModelTransferState.Completed -> CoreUiR.string.models_status_installed
    ModelTransferState.Idle,
    null,
    -> CoreUiR.string.models_status_not_installed
}

internal fun ModelTransferState?.downloadedBytesOrNull(): Long? = when (this) {
    is ModelTransferState.Queued -> completedBytes
    is ModelTransferState.Running -> completedBytes
    is ModelTransferState.Paused -> completedBytes
    ModelTransferState.Completed,
    ModelTransferState.Idle,
    ModelTransferState.Installing,
    is ModelTransferState.Failed,
    null,
    -> null
}

internal fun ModelTransferNetworkPolicy.networkLabelRes(): Int = when (this) {
    ModelTransferNetworkPolicy.WIFI_ONLY -> CoreUiR.string.models_network_wifi_only
    ModelTransferNetworkPolicy.ANY_NETWORK -> CoreUiR.string.models_network_any
}

internal fun Long.toReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    else -> "$this B"
}
