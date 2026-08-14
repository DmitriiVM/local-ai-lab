package com.dmitriim.localailab.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dmitriim.localailab.core.ui.style.AppSurfaceStyle

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    tone: AppSurfaceTone = AppSurfaceTone.GLASS,
    contentPadding: Dp = 18.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val backgroundModifier = if (tone == AppSurfaceTone.TONAL) {
        Modifier.background(AppSurfaceStyle.tonalCardColor(colors))
    } else {
        Modifier.background(AppSurfaceStyle.glassCardBackgroundBrush(colors))
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = AppSurfaceStyle.cardBorderBrush(colors, tonal = tone == AppSurfaceTone.TONAL),
                shape = AppSurfaceStyle.CardShape,
            ),
        shape = AppSurfaceStyle.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(backgroundModifier)
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
