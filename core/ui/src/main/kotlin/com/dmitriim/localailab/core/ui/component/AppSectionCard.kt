package com.dmitriim.localailab.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    tone: AppSurfaceTone = AppSurfaceTone.GLASS,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurfaceCard(
        modifier = modifier,
        tone = tone,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
