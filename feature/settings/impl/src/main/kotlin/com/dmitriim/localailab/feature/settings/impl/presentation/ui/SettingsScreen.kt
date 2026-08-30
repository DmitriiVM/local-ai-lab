package com.dmitriim.localailab.feature.settings.impl.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.R as CoreUiR
import com.dmitriim.localailab.core.ui.layout.LocalAppDimensions
import com.dmitriim.localailab.feature.settings.api.domain.AppSettings
import com.dmitriim.localailab.feature.settings.impl.presentation.SettingsUiState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenDeviceAndRuntimes: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onClearTemporaryMedia: () -> Unit,
    onRequestClearRunHistory: () -> Unit,
    onDismissClearRunHistory: () -> Unit,
    onClearRunHistory: () -> Unit,
    onRequestHuggingFaceToken: () -> Unit,
    onDismissHuggingFaceToken: () -> Unit,
    onSaveHuggingFaceToken: (String) -> Unit,
    onClearHuggingFaceToken: () -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    val settings = state.settings
    SettingsDialogs(
        state = state,
        onDismissHistory = onDismissClearRunHistory,
        onClearHistory = onClearRunHistory,
        onSaveToken = onSaveHuggingFaceToken,
        onDismissToken = onDismissHuggingFaceToken,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = dimensions.screenPadding,
                top = dimensions.topBarOverlayClearance + 20.dp,
                end = dimensions.screenPadding,
                bottom = 44.dp + dimensions.bottomNavigationOverlayClearance,
            ),
        verticalArrangement = Arrangement.spacedBy(dimensions.sectionSpacing),
    ) {
        Text(
            text = stringResource(CoreUiR.string.settings_settings_screen_111),
            style = MaterialTheme.typography.headlineMedium,
        )
        DeviceAndRuntimeCard(onClick = onOpenDeviceAndRuntimes)
        if (settings.showAdvancedControls) {
            PerformanceSettingsCard(settings, onUpdate)
        }
        AppearanceSettingsCard(settings, onUpdate)
        RetentionSettingsCard()
        ModelDownloadsSettingsCard(
            credentialStatus = state.huggingFaceCredentialStatus,
            onRequestToken = onRequestHuggingFaceToken,
            onClearToken = onClearHuggingFaceToken,
        )
        StoragePrivacySettingsCard(
            state = state,
            settings = settings,
            onClearTemporaryMedia = onClearTemporaryMedia,
            onRequestClearRunHistory = onRequestClearRunHistory,
            onClearRunHistory = onClearRunHistory,
        )
    }
}
