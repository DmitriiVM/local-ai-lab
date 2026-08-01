package com.dmitriim.localaiplayground.feature.playground.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.capability.CapabilityReadiness
import com.dmitriim.localaiplayground.core.model.capability.CapabilityReadinessState

@Composable
internal fun CapabilityCard(
    readiness: CapabilityReadiness,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    readiness.capability.displayName,
                    style = MaterialTheme.typography.titleLarge,
                )
                AssistChip(onClick = onClick, label = { Text(readiness.state.displayName) })
            }
            Text(
                readiness.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (readiness.engines.isNotEmpty()) {
                Text(
                    readiness.engines.joinToString { it.descriptor.displayName },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private val AiCapability.displayName: String
    get() = when (this) {
        AiCapability.CHAT -> "Chat"
        AiCapability.SPEECH_TO_TEXT -> "Speech to Text"
        AiCapability.TEXT_TO_SPEECH -> "Text to Speech"
        AiCapability.VOICE_ACTIVITY_DETECTION -> "Voice Activity Detection"
        AiCapability.VOICE_ASSISTANT -> "Voice Assistant"
    }

private val CapabilityReadinessState.displayName: String
    get() = when (this) {
        CapabilityReadinessState.READY -> "Ready"
        CapabilityReadinessState.MODEL_REQUIRED -> "Model required"
        CapabilityReadinessState.INSTALLING -> "Installing"
        CapabilityReadinessState.UNSUPPORTED -> "Unsupported"
        CapabilityReadinessState.TEMPORARILY_UNAVAILABLE -> "Unavailable"
    }
