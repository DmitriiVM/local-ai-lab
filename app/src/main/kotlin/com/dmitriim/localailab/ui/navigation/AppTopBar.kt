package com.dmitriim.localailab.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun AppTopBar(
    onNavigateUp: (() -> Unit)?,
    title: String?,
    modifier: Modifier = Modifier,
) {
    onNavigateUp?.let { navigateUp ->
        Row(
            modifier = modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiquidGlassToolbarButton(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Navigate back",
                onClick = navigateUp,
            )
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassToolbarButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.60f),
            )
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        colors.tertiaryContainer.copy(alpha = 0.72f),
                        colors.tertiaryContainer.copy(alpha = 0.48f),
                        colors.surfaceContainer.copy(alpha = 0.62f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        colors.onTertiaryContainer.copy(alpha = 0.42f),
                        colors.outlineVariant.copy(alpha = 0.24f),
                    ),
                ),
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = colors.onTertiaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}
