package com.dmitriim.localaiplayground.feature.models.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ModelDownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .border(
                width = 1.dp,
                brush = DownloadButtonBorder,
                shape = shape,
            )
            .background(
                brush = DownloadButtonBackground,
                shape = shape,
            ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFF3F0FF),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Download", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val DownloadButtonBackground = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF071448),
        Color(0xFF331A7C),
        Color(0xFF4C3B88),
    ),
)

private val DownloadButtonBorder = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF3A426E),
        Color(0xFF4F315E),
        Color(0xFF14102A),
    ),
)
