package com.dmitriim.localailab.feature.models.impl.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.ui.R as CoreUiR

@Composable
internal fun ModelOverviewSection(
    manifest: ModelManifest,
    size: Long?,
) {
    DetailsSection(title = "At a glance") {
        size?.let {
            DetailValue(
                label = "Size",
                value = it.toDetailsReadableBytes(),
            )
        }
        DetailValue(
            label = "Languages",
            value = manifest.detailsLanguageSummary(),
        )
        manifest.approximateRamBytes?.let {
            DetailValue(
                label = "Approximate RAM",
                value = it.toDetailsReadableBytes(),
            )
        }
    }
}

@Composable
internal fun ModelMetadataSection(manifest: ModelManifest) {
    DetailsSection(title = "Model") {
        DetailValue(label = "Family", value = manifest.family)
        DetailValue(label = "Engine", value = manifest.engineId.value)
        DetailValue(label = "Format", value = manifest.format.displayLabel())
        manifest.architecture?.let { DetailValue(label = "Architecture", value = it) }
        manifest.quantization?.let { DetailValue(label = "Quantization", value = it) }
    }
}

@Composable
internal fun ModelDetailsHeader(manifest: ModelManifest, status: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(manifest.displayName, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailsBadge(manifest.detailsTypeLabel())
            DetailsBadge(status)
        }
        Text(
            manifest.description ?: if (manifest.family == "Imported") {
                "User-imported model."
            } else {
                "No description is available for this model."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ModelSourceDetails(manifest: ModelManifest, onOpenUrl: (String) -> Unit) {
    DetailsSection("Source and license") {
        DetailValue(label = "License", value = manifest.source.licenseName)
        Text(
            text = manifest.source.attribution,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        manifest.source.url?.let { url ->
            OutlinedButton(onClick = { onOpenUrl(url) }) {
                Text(stringResource(CoreUiR.string.models_model_details_screen_46))
            }
        }
    }
}

@Composable
internal fun DetailsBadge(label: String) = androidx.compose.material3.Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Text(
        text = label,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
    )
}

private fun Enum<*>.displayLabel(): String = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
