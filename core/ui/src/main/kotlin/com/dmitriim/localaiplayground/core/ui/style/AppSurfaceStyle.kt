package com.dmitriim.localaiplayground.core.ui.style

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AppSurfaceStyle {
    val CardShape = RoundedCornerShape(24.dp)

    fun tonalCardColor(colors: ColorScheme): Color = colors.tertiaryContainer.copy(alpha = 0.38f)

    fun pageBackgroundBrush(colors: ColorScheme) = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to colors.tertiaryContainer.copy(alpha = 0.52f),
            0.45f to colors.tertiaryContainer.copy(alpha = 0.20f),
            0.82f to colors.tertiaryContainer.copy(alpha = 0f),
        ),
    )

    fun glassCardBackgroundBrush(colors: ColorScheme): Brush = Brush.linearGradient(
        colors = listOf(
            colors.tertiaryContainer.copy(alpha = 0.32f),
            colors.tertiaryContainer.copy(alpha = 0.52f),
            colors.surfaceContainerHigh.copy(alpha = 0.96f),
            colors.surfaceContainer.copy(alpha = 0.98f),
        ),
        start = Offset.Zero,
        end = Offset(0f, 500f),
    )

    fun cardBorderBrush(colors: ColorScheme, tonal: Boolean): Brush = Brush.linearGradient(
        colors = listOf(
            colors.onTertiaryContainer.copy(alpha = if (tonal) 0.22f else 0.38f),
            colors.onTertiaryContainer.copy(alpha = if (tonal) 0.34f else 0.52f),
            colors.outlineVariant.copy(alpha = 0.26f),
        ),
        start = Offset.Zero,
        end = Offset(0f, 500f),
    )
}
