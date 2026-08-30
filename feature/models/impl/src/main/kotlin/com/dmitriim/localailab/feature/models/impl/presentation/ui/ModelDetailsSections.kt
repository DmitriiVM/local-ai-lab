package com.dmitriim.localailab.feature.models.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.model.library.CatalogModel
import com.dmitriim.localailab.ai.api.model.manifest.ModelFileSpec
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.ai.api.model.manifest.SttRecognitionMode
import com.dmitriim.localailab.ai.api.model.manifest.TtsControl
import com.dmitriim.localailab.ai.api.model.manifest.TtsVoiceMode
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.StatusMessage
import com.dmitriim.localailab.feature.models.api.domain.diagnostics.ModelCompatibilityState
import com.dmitriim.localailab.feature.models.api.domain.library.ModelValidationState
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferState
import com.dmitriim.localailab.feature.models.impl.domain.transfer.downloadStorageEstimate
import com.dmitriim.localailab.feature.models.impl.presentation.ModelValidationFeedback
import com.dmitriim.localailab.feature.models.impl.presentation.ModelsUiState
import java.text.DateFormat
import java.util.Date

@Composable
internal fun CapabilityDetails(manifest: ModelManifest) {
    DetailsSection("Capabilities") {
        when {
            AiCapability.CHAT in manifest.capabilities -> {
                DetailValue("Capability", "Chat")
                manifest.contextSize?.let { DetailValue("Default context", "$it tokens") }
            }
            AiCapability.TEXT_TO_SPEECH in manifest.capabilities -> {
                DetailValue("Capability", "Text to speech")
                manifest.sampleRateHz?.let { DetailValue("Sample rate", it.toSampleRate()) }
                manifest.speakerCount?.let { DetailValue("Speakers", it.toString()) }
                DetailValue("Voice mode", manifest.ttsVoiceMode.displayLabel())
                if (manifest.ttsControls.isNotEmpty()) {
                    DetailValue("Controls", manifest.ttsControls.joinToString { it.displayLabel() })
                }
                if (manifest.voices.isNotEmpty()) {
                    val shownVoices = manifest.voices.take(8).joinToString { it.displayName }
                    val remaining = manifest.voices.size - 8
                    DetailValue("Voices", if (remaining > 0) "$shownVoices +$remaining more" else shownVoices)
                }
            }
            AiCapability.SPEECH_TO_TEXT in manifest.capabilities -> {
                DetailValue("Capability", "Speech to text")
                manifest.sampleRateHz?.let { DetailValue("Sample rate", it.toSampleRate()) }
                DetailValue("Recognition", manifest.sttRecognitionMode.displayLabel())
            }
            else -> DetailValue("Capabilities", manifest.capabilities.joinToString { it.name.displayLabel() })
        }
    }
}

@Composable
internal fun CompatibilityDetails(uiState: ModelsUiState, modelId: ModelId) {
    DetailsSection("Device compatibility") {
        when {
            uiState.isCheckingCompatibility && uiState.compatibilityModelId == modelId -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(CoreUiR.string.models_model_details_screen_48))
            }
            uiState.compatibilityError != null && uiState.compatibilityModelId == modelId -> {
                StatusMessage(stringResource(CoreUiR.string.models_compatibility_unavailable), uiState.compatibilityError)
            }
            uiState.compatibility != null && uiState.compatibilityModelId == modelId -> {
                val compatibility = uiState.compatibility
                Text(
                    compatibility.state.displayLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    color = when (compatibility.state) {
                        ModelCompatibilityState.COMPATIBLE -> MaterialTheme.colorScheme.primary
                        ModelCompatibilityState.ADVISORY_WARNING -> MaterialTheme.colorScheme.tertiary
                        ModelCompatibilityState.INCOMPATIBLE -> MaterialTheme.colorScheme.error
                    },
                )
                compatibility.reasons.forEach { reason ->
                    Text(stringResource(CoreUiR.string.models_model_details_screen_format_5, reason), style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> Text(stringResource(CoreUiR.string.models_model_details_screen_49))
        }
    }
}

@Composable
internal fun DownloadStorageDetails(model: CatalogModel) {
    val estimate = model.downloadStorageEstimate()
    DetailsSection("Download and storage") {
        DetailValue("Download", estimate.downloadBytes.toDetailsReadableBytes())
        DetailValue("Peak app storage", estimate.peakRequiredBytes.toDetailsReadableBytes())
        if (estimate.temporaryExtractionBytes > 0L) {
            DetailValue("Temporary extraction", estimate.temporaryExtractionBytes.toDetailsReadableBytes())
        }
        Text(
            "Peak storage includes the download, temporary extraction when needed, and a safety reserve.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun InstallationDetails(model: com.dmitriim.localailab.feature.models.api.domain.library.InstalledModel?, validationFeedback: ModelValidationFeedback?) {
    DetailsSection("Installation") {
        if (model == null) {
            DetailValue("Status", "Not installed")
        } else {
            DetailValue("Status", model.validationState.detailsStatusLabel())
            model.manifest.installedAtEpochMs.takeIf { it > 0 }?.let { installedAt ->
                DetailValue("Installed", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(installedAt)))
            }
            model.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            validationFeedback?.let {
                StatusMessage(
                    title = stringResource(if (it.isError) CoreUiR.string.models_validation_issue else CoreUiR.string.models_validation),
                    explanation = it.message,
                )
            }
        }
    }
}

@Composable
internal fun TechnicalDetails(manifest: ModelManifest, expanded: Boolean, onToggle: () -> Unit) {
    DetailsSection("Technical details") {
        TextButton(onClick = onToggle) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (expanded) CoreUiR.string.models_hide_technical_details else CoreUiR.string.models_show_technical_details))
        }
        if (expanded) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailValue("Model ID", manifest.modelId.value)
                    DetailValue("Profile", manifest.profileType.value)
                    manifest.revision?.let { DetailValue("Revision", it) }
                    manifest.catalogVersion?.let { DetailValue("Catalog version", it.toString()) }
                    Text(stringResource(CoreUiR.string.models_model_details_screen_47), style = MaterialTheme.typography.titleSmall)
                    manifest.files.forEachIndexed { index, file ->
                        FileDetails(file)
                        if (index != manifest.files.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
internal fun DetailsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
internal fun DetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FileDetails(file: ModelFileSpec) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(file.relativePath, style = MaterialTheme.typography.bodyMedium)
        Text(
            buildString {
                append(file.role.value.displayLabel())
                file.expectedBytes?.let { append(" • ${it.toDetailsReadableBytes()}") }
                if (file.directory) append(" • Directory")
                if (!file.required) append(" • Optional")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        file.sha256?.let {
            Text(stringResource(CoreUiR.string.models_model_details_screen_format_6, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ModelManifest.detailsTypeLabel(): String = stringResource(
    when {
        AiCapability.CHAT in capabilities -> CoreUiR.string.models_type_llm
        AiCapability.TEXT_TO_SPEECH in capabilities -> CoreUiR.string.models_type_tts
        AiCapability.SPEECH_TO_TEXT in capabilities -> CoreUiR.string.models_type_stt
        AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> CoreUiR.string.models_type_vad
        else -> CoreUiR.string.models_type_model
    },
)

internal fun ModelManifest.detailsLanguageSummary(): String {
    if (AiCapability.VOICE_ACTIVITY_DETECTION in capabilities) return "Language-independent"
    if (languages.isEmpty()) return "Not specified"
    val listed = languages.joinToString()
    return supportedLanguageCount?.takeIf { it > languages.size }?.let { "$listed • $it languages total" } ?: listed
}

@Composable
internal fun ModelValidationState.detailsStatusLabel(): String = stringResource(
    when (this) {
        ModelValidationState.READY -> CoreUiR.string.models_status_ready
        ModelValidationState.INVALID -> CoreUiR.string.models_status_invalid
        ModelValidationState.MISSING_FILES -> CoreUiR.string.models_status_missing_files
        ModelValidationState.INCOMPATIBLE -> CoreUiR.string.models_status_incompatible
    },
)

@Composable
internal fun ModelTransferState?.detailsStatusLabel(): String = stringResource(
    when (this) {
        is ModelTransferState.Queued -> CoreUiR.string.models_status_queued
        is ModelTransferState.Running -> if (completedBytes >= totalBytes) CoreUiR.string.models_status_verifying else CoreUiR.string.models_status_downloading
        is ModelTransferState.Paused -> CoreUiR.string.models_status_paused
        is ModelTransferState.Failed -> CoreUiR.string.models_status_download_failed
        ModelTransferState.Installing, ModelTransferState.Completed -> CoreUiR.string.models_status_installing
        ModelTransferState.Idle, null -> CoreUiR.string.models_status_not_installed
    },
)

@Composable
private fun ModelCompatibilityState.displayLabel(): String = stringResource(
    when (this) {
        ModelCompatibilityState.COMPATIBLE -> CoreUiR.string.models_compatibility_compatible
        ModelCompatibilityState.ADVISORY_WARNING -> CoreUiR.string.models_compatibility_warnings
        ModelCompatibilityState.INCOMPATIBLE -> CoreUiR.string.models_status_incompatible
    },
)

@Composable
private fun TtsVoiceMode.displayLabel(): String = stringResource(
    when (this) {
        TtsVoiceMode.SPEAKER_ID -> CoreUiR.string.models_voice_mode_bundled
        TtsVoiceMode.REFERENCE_AUDIO -> CoreUiR.string.models_voice_mode_reference
        TtsVoiceMode.PLATFORM -> CoreUiR.string.models_voice_mode_platform
    },
)

@Composable
private fun SttRecognitionMode.displayLabel(): String = stringResource(
    when (this) {
        SttRecognitionMode.OFFLINE -> CoreUiR.string.models_recognition_offline
        SttRecognitionMode.STREAMING -> CoreUiR.string.models_recognition_streaming
    },
)

private fun TtsControl.displayLabel(): String = name.displayLabel()

private fun Enum<*>.displayLabel(): String = name.displayLabel()

private fun String.displayLabel(): String = lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)

private fun Int.toSampleRate(): String = if (this % 1_000 == 0) "${this / 1_000} kHz" else "%.2f kHz".format(this / 1_000.0)

internal fun Long.toDetailsReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    this >= 1_024 -> "%.1f KiB".format(toDouble() / 1_024)
    else -> "$this B"
}
