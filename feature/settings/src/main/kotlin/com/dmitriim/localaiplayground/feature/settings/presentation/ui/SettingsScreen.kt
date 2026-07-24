package com.dmitriim.localaiplayground.feature.settings.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions

@Composable
fun SettingsScreen() {
    val dimensions = LocalAppDimensions.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = dimensions.topBarOverlayClearance + 20.dp,
                end = 20.dp,
                bottom = 20.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Privacy & app behavior", style = MaterialTheme.typography.headlineMedium)
        PrivacyCard(
            title = "Local by design",
            body = "Prompts, recorded audio, transcripts, and generated audio remain on this " +
                "device. The app has no cloud inference fallback.",
        )
        PrivacyCard(
            title = "Downloads are explicit",
            body = "Models can use substantial storage and memory. The app opens without a " +
                "model and starts no model download until you confirm it.",
        )
        PrivacyCard(
            title = "Foreground-only playgrounds",
            body = "Microphone capture, inference, speech synthesis, and playground playback " +
                "stop or request cancellation when the app leaves the foreground.",
        )
        PrivacyCard(
            title = "No analytics",
            body = "The MVP includes no analytics or telemetry SDK and does not log prompt, " +
                "transcript, or audio content during normal operation.",
        )
    }
}

@Composable
private fun PrivacyCard(title: String, body: String) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
