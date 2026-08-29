package com.dmitriim.localailab.feature.playground.impl.presentation.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.ai.api.capability.AiCapability
import com.dmitriim.localailab.core.ui.R as CoreUiR

@Composable
internal fun CapabilityCard(
    capability: AiCapability,
    onClick: () -> Unit,
) {
    val content = capability.cardContent
    val shape = RoundedCornerShape(24.dp)
    val colors = MaterialTheme.colorScheme
    val accent = content.accent.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.contentColor.copy(alpha = 0.38f),
                        accent.contentColor.copy(alpha = 0.52f),
                        colors.outlineVariant.copy(alpha = 0.26f),
                    ),
                    start = Offset.Zero,
                    end = Offset(0f, 300f),
                ),
                shape = shape,
            ),
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.containerColor.copy(alpha = 0.52f),
                            colors.surfaceContainerHigh.copy(alpha = 0.96f),
                            colors.surfaceContainer.copy(alpha = 0.98f),
                        ),
                        start = Offset.Zero,
                        end = Offset(0f, 300f),
                    ),
                )
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = accent.containerColor.copy(alpha = 0.68f),
                contentColor = accent.contentColor,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = content.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(content.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(content.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accent.contentColor,
            )
        }
    }
}

private val AiCapability.cardContent: CapabilityCardContent
    get() = when (this) {
        AiCapability.CHAT -> CapabilityCardContent(
            titleRes = CoreUiR.string.ui_copy_55,
            descriptionRes = CoreUiR.string.ui_description_27,
            icon = Icons.AutoMirrored.Outlined.Chat,
            accent = CapabilityCardAccent(
                containerColor = Color(0xFF3B2B5A),
                contentColor = Color(0xFFECDDFF),
            ),
        )
        AiCapability.SPEECH_TO_TEXT -> CapabilityCardContent(
            titleRes = CoreUiR.string.ui_copy_56,
            descriptionRes = CoreUiR.string.ui_description_28,
            icon = Icons.Outlined.Mic,
            accent = CapabilityCardAccent(
                containerColor = Color(0xFF3B2B5A),
                contentColor = Color(0xFFECDDFF),
            ),
        )
        AiCapability.TEXT_TO_SPEECH -> CapabilityCardContent(
            titleRes = CoreUiR.string.ui_copy_57,
            descriptionRes = CoreUiR.string.ui_description_29,
            icon = Icons.AutoMirrored.Outlined.VolumeUp,
            accent = CapabilityCardAccent(
                containerColor = Color(0xFF3B2B5A),
                contentColor = Color(0xFFECDDFF),
            ),
        )
        AiCapability.VOICE_ACTIVITY_DETECTION,
        AiCapability.VOICE_ASSISTANT,
        -> error("$this is not displayed in the playground")
    }

private data class CapabilityCardContent(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val accent: CapabilityCardAccent,
)

private data class CapabilityCardAccent(
    val containerColor: Color,
    val contentColor: Color,
) {
    val colors: CapabilityCardAccentColors
        get() = CapabilityCardAccentColors(containerColor, contentColor)
}

private data class CapabilityCardAccentColors(
    val containerColor: Color,
    val contentColor: Color,
)
