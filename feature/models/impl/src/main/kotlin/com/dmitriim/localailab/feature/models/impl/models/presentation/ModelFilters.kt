package com.dmitriim.localailab.feature.models.impl.models.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.ai.api.engine.EngineId
import com.dmitriim.localailab.ai.api.model.manifest.ModelManifest
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.component.AppSurfaceCard
import com.dmitriim.localailab.core.ui.style.AppFilterChipDefaults

@Composable
internal fun ModelFilters(
    typeFilter: ModelTypeFilter,
    runtimeIds: List<EngineId>,
    runtimeFilter: String?,
    installationFilter: ModelInstallationFilter,
    onTypeFilterChange: (ModelTypeFilter) -> Unit,
    onRuntimeFilterChange: (String?) -> Unit,
    onInstallationFilterChange: (ModelInstallationFilter) -> Unit,
) {
    AppSurfaceCard(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ModelTypeFilter.entries.size) { index ->
                val filter = ModelTypeFilter.entries[index]
                FilterChip(
                    selected = typeFilter == filter,
                    onClick = { onTypeFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = runtimeFilter == null,
                    onClick = { onRuntimeFilterChange(null) },
                    label = { Text(stringResource(CoreUiR.string.models_models_screen_73)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
            items(runtimeIds.size) { index ->
                val runtime = runtimeIds[index]
                FilterChip(
                    selected = runtimeFilter == runtime.value,
                    onClick = { onRuntimeFilterChange(runtime.value) },
                    label = { Text(runtime.displayLabel()) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ModelInstallationFilter.entries.size) { index ->
                val filter = ModelInstallationFilter.entries[index]
                FilterChip(
                    selected = installationFilter == filter,
                    onClick = { onInstallationFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes)) },
                    colors = AppFilterChipDefaults.colors(),
                )
            }
        }
    }
}

@Composable
private fun EngineId.displayLabel(): String = when (value) {
    "litert-lm" -> stringResource(CoreUiR.string.models_engine_litert_lm)
    else -> value
}

internal enum class ModelTypeFilter(
    val labelRes: Int,
    private val capability: AiCapability? = null,
) {
    ALL(labelRes = CoreUiR.string.models_filter_all),
    LLM(labelRes = CoreUiR.string.models_type_llm, capability = AiCapability.CHAT),
    TTS(labelRes = CoreUiR.string.models_type_tts, capability = AiCapability.TEXT_TO_SPEECH),
    STT(labelRes = CoreUiR.string.models_type_stt, capability = AiCapability.SPEECH_TO_TEXT),
    ;

    fun matches(manifest: ModelManifest): Boolean = capability == null || capability in manifest.capabilities
}

internal enum class ModelInstallationFilter(val labelRes: Int) {
    ALL(labelRes = CoreUiR.string.models_filter_all),
    INSTALLED(labelRes = CoreUiR.string.models_status_installed),
    NOT_INSTALLED(labelRes = CoreUiR.string.models_status_not_installed),
    ;

    fun matches(item: ModelListItem): Boolean = when (this) {
        ALL -> true
        INSTALLED -> item is ModelListItem.Installed
        NOT_INSTALLED -> item is ModelListItem.Catalog
    }
}
