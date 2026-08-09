package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.library.CatalogModel
import com.dmitriim.localaiplayground.core.model.library.CatalogDownloadAuthentication
import com.dmitriim.localaiplayground.core.model.library.InstalledModel
import com.dmitriim.localaiplayground.core.model.library.ModelCompatibilityState
import com.dmitriim.localaiplayground.core.model.library.ModelTransferState
import com.dmitriim.localaiplayground.core.model.library.ModelValidationState
import com.dmitriim.localaiplayground.core.model.manifest.ModelFileSpec
import com.dmitriim.localaiplayground.core.model.manifest.ModelId
import com.dmitriim.localaiplayground.core.model.manifest.ModelManifest
import com.dmitriim.localaiplayground.core.model.manifest.SttRecognitionMode
import com.dmitriim.localaiplayground.core.model.manifest.TtsControl
import com.dmitriim.localaiplayground.core.model.manifest.TtsVoiceMode
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions
import com.dmitriim.localaiplayground.core.result.StatusMessage
import com.dmitriim.localaiplayground.feature.models.presentation.ModelsUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun ModelDetailsScreen(
    modelId: ModelId,
    uiState: ModelsUiState,
    onNavigateBack: () -> Unit,
    onDownload: (ModelId) -> Unit,
    onPauseTransfer: (ModelId) -> Unit,
    onResumeOnWifi: (ModelId) -> Unit,
    onResumeOnAnyNetwork: (ModelId) -> Unit,
    onCancelTransfer: (ModelId) -> Unit,
    onValidate: (ModelId) -> Unit,
    onDelete: (ModelId) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onRequestHuggingFaceToken: (ModelId) -> Unit,
    onSaveHuggingFaceToken: (String) -> Unit,
    onDismissHuggingFaceToken: () -> Unit,
) {
    val catalogModel = uiState.catalog.firstOrNull { it.manifest.modelId == modelId }
    val installedModel = uiState.installed.firstOrNull { it.manifest.modelId == modelId }
    val manifest = catalogModel?.manifest ?: installedModel?.manifest

    when {
        manifest != null -> ModelDetailsContent(
            manifest = manifest,
            catalogModel = catalogModel,
            installedModel = installedModel,
            transfer = uiState.transfers[modelId],
            uiState = uiState,
            onDownload = { onDownload(modelId) },
            onPauseTransfer = { onPauseTransfer(modelId) },
            onResumeOnWifi = { onResumeOnWifi(modelId) },
            onResumeOnAnyNetwork = { onResumeOnAnyNetwork(modelId) },
            onCancelTransfer = { onCancelTransfer(modelId) },
            onValidate = { onValidate(modelId) },
            onDelete = { onDelete(modelId) },
            onRequestHuggingFaceToken = { onRequestHuggingFaceToken(modelId) },
        )
        !uiState.isModelDataLoaded -> ModelDetailsLoading()
        else -> ModelUnavailable(uiState.message, onNavigateBack)
    }

    uiState.pendingDelete?.takeIf { it.manifest.modelId == modelId }?.let { model ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete ${model.manifest.displayName}?") },
            text = {
                Text(
                    "This deletes the model files and reclaims about " +
                        "${model.totalBytes.toDetailsReadableBytes()}. Historical run metadata is preserved.",
                )
            },
            confirmButton = { Button(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { OutlinedButton(onClick = onCancelDelete) { Text("Cancel") } },
        )
    }

    if (uiState.pendingHuggingFaceTokenModelId == modelId) {
        HuggingFaceTokenDialog(
            saving = uiState.isSavingHuggingFaceToken,
            error = uiState.huggingFaceTokenError,
            onSave = onSaveHuggingFaceToken,
            onDismiss = onDismissHuggingFaceToken,
        )
    }
}

@Composable
private fun ModelDetailsContent(
    manifest: ModelManifest,
    catalogModel: CatalogModel?,
    installedModel: InstalledModel?,
    transfer: ModelTransferState?,
    uiState: ModelsUiState,
    onDownload: () -> Unit,
    onPauseTransfer: () -> Unit,
    onResumeOnWifi: () -> Unit,
    onResumeOnAnyNetwork: () -> Unit,
    onCancelTransfer: () -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
    onRequestHuggingFaceToken: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val uriHandler = LocalUriHandler.current
    var technicalExpanded by rememberSaveable(manifest.modelId.value) { mutableStateOf(false) }
    val size = installedModel?.totalBytes ?: catalogModel?.download?.expectedBytes
    val status = installedModel?.validationState?.detailsStatusLabel() ?: transfer.detailsStatusLabel()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            ModelActionBar(
                installedModel = installedModel,
                transfer = transfer,
                validating = manifest.modelId in uiState.validatingModelIds,
                onDownload = onDownload,
                onPauseTransfer = onPauseTransfer,
                onResumeOnWifi = onResumeOnWifi,
                onResumeOnAnyNetwork = onResumeOnAnyNetwork,
                onCancelTransfer = onCancelTransfer,
                onValidate = onValidate,
                onDelete = onDelete,
            )
        },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = dimensions.topBarOverlayClearance + 40.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(manifest.displayName, style = MaterialTheme.typography.headlineMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailsBadge(manifest.detailsTypeLabel)
                        DetailsBadge(status)
                    }
                    Text(
                        text = manifest.description
                            ?: if (manifest.family == "Imported") {
                                "User-imported model."
                            } else {
                                "No description is available for this model."
                            },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            uiState.message?.let { message ->
                item { StatusMessage("Model lifecycle", message) }
            }

            item {
                DetailsSection("At a glance") {
                    size?.let { DetailValue("Size", it.toDetailsReadableBytes()) }
                    DetailValue("Languages", manifest.detailsLanguageSummary())
                    manifest.approximateRamBytes?.let {
                        DetailValue("Approximate RAM", it.toDetailsReadableBytes())
                    }
                }
            }

            item {
                DetailsSection("Model") {
                    DetailValue("Family", manifest.family)
                    DetailValue("Engine", manifest.engineId.value)
                    DetailValue("Format", manifest.format.displayLabel())
                    manifest.architecture?.let { DetailValue("Architecture", it) }
                    manifest.quantization?.let { DetailValue("Quantization", it) }
                }
            }

            item {
                CapabilityDetails(manifest)
            }

            item {
                CompatibilityDetails(uiState, manifest.modelId)
            }

            item {
                InstallationDetails(
                    model = installedModel,
                    validationFeedback = uiState.validationFeedback[manifest.modelId],
                )
            }

            item {
                DetailsSection("Source and license") {
                    DetailValue("License", manifest.source.licenseName)
                    Text(
                        manifest.source.attribution,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    manifest.source.url?.let { url ->
                        OutlinedButton(onClick = { uriHandler.openUri(url) }) {
                            Text("Open source")
                        }
                    }
                }
            }

            if (catalogModel?.download?.authentication == CatalogDownloadAuthentication.HUGGING_FACE_USER_TOKEN) {
                item {
                    HuggingFaceAccessSection(
                        accessUrl = manifest.source.url,
                        credentialStatus = uiState.huggingFaceCredentialStatus,
                        onConfigure = onRequestHuggingFaceToken,
                    )
                }
            }

            item {
                DetailsSection("Technical details") {
                    TextButton(onClick = { technicalExpanded = !technicalExpanded }) {
                        androidx.compose.material3.Icon(
                            imageVector = if (technicalExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (technicalExpanded) "Hide technical details" else "Show technical details")
                    }
                    if (technicalExpanded) {
                        SelectionContainer {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                DetailValue("Model ID", manifest.modelId.value)
                                DetailValue("Profile", manifest.profileType.value)
                                manifest.revision?.let { DetailValue("Revision", it) }
                                manifest.catalogVersion?.let { DetailValue("Catalog version", it.toString()) }
                                Text("Files", style = MaterialTheme.typography.titleSmall)
                                manifest.files.forEachIndexed { index, file ->
                                    FileDetails(file)
                                    if (index != manifest.files.lastIndex) HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityDetails(manifest: ModelManifest) {
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
                    DetailValue(
                        "Controls",
                        manifest.ttsControls.joinToString { it.displayLabel() },
                    )
                }
                if (manifest.voices.isNotEmpty()) {
                    val shownVoices = manifest.voices.take(8).joinToString { it.displayName }
                    val remaining = manifest.voices.size - 8
                    DetailValue(
                        "Voices",
                        if (remaining > 0) "$shownVoices +$remaining more" else shownVoices,
                    )
                }
            }
            AiCapability.SPEECH_TO_TEXT in manifest.capabilities -> {
                DetailValue("Capability", "Speech to text")
                manifest.sampleRateHz?.let { DetailValue("Sample rate", it.toSampleRate()) }
                DetailValue("Recognition", manifest.sttRecognitionMode.displayLabel())
            }
            else -> {
                DetailValue(
                    "Capabilities",
                    manifest.capabilities.joinToString { it.name.displayLabel() },
                )
            }
        }
    }
}

@Composable
private fun CompatibilityDetails(uiState: ModelsUiState, modelId: ModelId) {
    DetailsSection("Device compatibility") {
        when {
            uiState.isCheckingCompatibility && uiState.compatibilityModelId == modelId -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Checking this device…")
                }
            }
            uiState.compatibilityError != null && uiState.compatibilityModelId == modelId -> {
                StatusMessage("Compatibility unavailable", uiState.compatibilityError)
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
                    Text("• $reason", style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> Text("Compatibility has not been checked yet.")
        }
    }
}

@Composable
private fun InstallationDetails(
    model: InstalledModel?,
    validationFeedback: com.dmitriim.localaiplayground.feature.models.presentation.ModelValidationFeedback?,
) {
    DetailsSection("Installation") {
        if (model == null) {
            DetailValue("Status", "Not installed")
        } else {
            DetailValue("Status", model.validationState.detailsStatusLabel())
            model.manifest.installedAtEpochMs.takeIf { it > 0 }?.let { installedAt ->
                DetailValue(
                    "Installed",
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(installedAt)),
                )
            }
            model.validationMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            validationFeedback?.let {
                StatusMessage(
                    title = if (it.isError) "Validation issue" else "Validation",
                    explanation = it.message,
                )
            }
        }
    }
}

@Composable
internal fun DetailsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
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
private fun DetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            Text(
                "SHA-256: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelActionBar(
    installedModel: InstalledModel?,
    transfer: ModelTransferState?,
    validating: Boolean,
    onDownload: () -> Unit,
    onPauseTransfer: () -> Unit,
    onResumeOnWifi: () -> Unit,
    onResumeOnAnyNetwork: () -> Unit,
    onCancelTransfer: () -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    var confirmAnyNetwork by rememberSaveable { mutableStateOf(false) }
    Surface(shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (installedModel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onValidate,
                        enabled = !validating,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (validating) "Validating…" else "Validate")
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Delete")
                    }
                }
            } else {
                when (transfer) {
                    is ModelTransferState.Queued -> {
                        Text("Waiting to download · ${transfer.networkPolicy.detailsNetworkLabel()}")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onPauseTransfer, modifier = Modifier.weight(1f)) { Text("Pause") }
                            OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        }
                        if (transfer.networkPolicy == com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.WIFI_ONLY) {
                            OutlinedButton(onClick = { confirmAnyNetwork = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Use mobile data")
                            }
                        }
                    }
                    is ModelTransferState.Running -> {
                        val total = transfer.totalBytes.toDetailsReadableBytes()
                        Text("${transfer.completedBytes.toDetailsReadableBytes()} / $total")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onPauseTransfer, modifier = Modifier.weight(1f)) { Text("Pause") }
                            OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        }
                    }
                    is ModelTransferState.Paused -> {
                        Text("${transfer.completedBytes.toDetailsReadableBytes()} / ${transfer.totalBytes.toDetailsReadableBytes()}")
                        transfer.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = onResumeOnWifi, modifier = Modifier.fillMaxWidth()) { Text("Resume on Wi-Fi") }
                        OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                    }
                    ModelTransferState.Installing,
                    ModelTransferState.Completed,
                    -> {
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Installing…")
                        }
                    }
                    is ModelTransferState.Failed -> {
                        Text(transfer.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                            Text("Retry download")
                        }
                    }
                    ModelTransferState.Idle,
                    null,
                    -> {
                        ModelDownloadButton(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
    if (confirmAnyNetwork) {
        AlertDialog(
            onDismissRequest = { confirmAnyNetwork = false },
            title = { Text("Use mobile data?") },
            text = { Text("This transfer may use a large amount of mobile data.") },
            confirmButton = { Button(onClick = { confirmAnyNetwork = false; onResumeOnAnyNetwork() }) { Text("Use mobile data") } },
            dismissButton = { OutlinedButton(onClick = { confirmAnyNetwork = false }) { Text("Keep Wi-Fi only") } },
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel download?") },
            text = { Text("The partial model download will be permanently deleted.") },
            confirmButton = { Button(onClick = { confirmCancel = false; onCancelTransfer() }) { Text("Cancel download") } },
            dismissButton = { OutlinedButton(onClick = { confirmCancel = false }) { Text("Keep download") } },
        )
    }
}

@Composable
private fun DetailsBadge(label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ModelDetailsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ModelUnavailable(message: String?, onNavigateBack: () -> Unit) {
    val dimensions = LocalAppDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = dimensions.topBarOverlayClearance + 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Model unavailable", style = MaterialTheme.typography.headlineMedium)
        Text(message ?: "This model is no longer present in the catalog or installed library.")
        Button(onClick = onNavigateBack) { Text("Back to models") }
    }
}

private val ModelManifest.detailsTypeLabel: String
    get() = when {
        AiCapability.CHAT in capabilities -> "LLM"
        AiCapability.TEXT_TO_SPEECH in capabilities -> "TTS"
        AiCapability.SPEECH_TO_TEXT in capabilities -> "STT"
        AiCapability.VOICE_ACTIVITY_DETECTION in capabilities -> "VAD"
        else -> "Model"
    }

private fun ModelManifest.detailsLanguageSummary(): String {
    if (AiCapability.VOICE_ACTIVITY_DETECTION in capabilities) return "Language-independent"
    if (languages.isEmpty()) return "Not specified"
    val listed = languages.joinToString()
    return supportedLanguageCount
        ?.takeIf { it > languages.size }
        ?.let { "$listed • $it languages total" }
        ?: listed
}

private fun ModelValidationState.detailsStatusLabel(): String = when (this) {
    ModelValidationState.READY -> "Ready"
    ModelValidationState.INVALID -> "Invalid"
    ModelValidationState.MISSING_FILES -> "Missing files"
    ModelValidationState.INCOMPATIBLE -> "Incompatible"
}

private fun ModelTransferState?.detailsStatusLabel(): String = when (this) {
    is ModelTransferState.Queued -> "Queued"
    is ModelTransferState.Running -> if (completedBytes >= totalBytes) "Verifying" else "Downloading"
    is ModelTransferState.Paused -> "Paused"
    ModelTransferState.Installing -> "Installing"
    is ModelTransferState.Failed -> "Download failed"
    ModelTransferState.Completed -> "Installing"
    ModelTransferState.Idle,
    null,
    -> "Not installed"
}

private fun com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.detailsNetworkLabel(): String = when (this) {
    com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.WIFI_ONLY -> "Wi-Fi only"
    com.dmitriim.localaiplayground.core.model.library.ModelTransferNetworkPolicy.ANY_NETWORK -> "Any network"
}

private fun ModelCompatibilityState.displayLabel(): String = when (this) {
    ModelCompatibilityState.COMPATIBLE -> "Compatible"
    ModelCompatibilityState.ADVISORY_WARNING -> "Compatible with warnings"
    ModelCompatibilityState.INCOMPATIBLE -> "Incompatible"
}

private fun TtsVoiceMode.displayLabel(): String = when (this) {
    TtsVoiceMode.SPEAKER_ID -> "Bundled speakers"
    TtsVoiceMode.REFERENCE_AUDIO -> "Reference audio"
    TtsVoiceMode.PLATFORM -> "Platform voices"
}

private fun SttRecognitionMode.displayLabel(): String = when (this) {
    SttRecognitionMode.OFFLINE -> "Offline"
    SttRecognitionMode.STREAMING -> "Streaming"
}

private fun TtsControl.displayLabel(): String = name.displayLabel()

private fun Enum<*>.displayLabel(): String = name.displayLabel()

private fun String.displayLabel(): String = lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)

private fun Int.toSampleRate(): String = if (this % 1_000 == 0) "${this / 1_000} kHz" else "%.2f kHz".format(this / 1_000.0)

private fun Long.toDetailsReadableBytes(): String = when {
    this >= 1_073_741_824 -> "%.2f GiB".format(toDouble() / 1_073_741_824)
    this >= 1_048_576 -> "%.1f MiB".format(toDouble() / 1_048_576)
    this >= 1_024 -> "%.1f KiB".format(toDouble() / 1_024)
    else -> "$this B"
}
