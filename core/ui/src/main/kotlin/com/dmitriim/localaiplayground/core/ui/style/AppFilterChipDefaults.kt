package com.dmitriim.localaiplayground.core.ui.style

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppFilterChipDefaults {
    @Composable
    fun colors() = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        containerColor = Color.Transparent,
    )
}
