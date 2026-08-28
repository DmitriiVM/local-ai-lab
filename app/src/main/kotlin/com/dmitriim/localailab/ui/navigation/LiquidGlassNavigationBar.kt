package com.dmitriim.localailab.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmitriim.localailab.core.navigation.TopLevelDestination

@Composable
internal fun LiquidGlassNavigationBar(
    selectedDestination: TopLevelDestination,
    onSelectDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(32.dp)
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(64.dp)
            .shadow(
                elevation = 24.dp,
                shape = barShape,
                ambientColor = colors.scrim.copy(alpha = 0.22f),
                spotColor = colors.scrim.copy(alpha = 0.34f),
            )
            .clip(barShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.surfaceContainerHigh.copy(alpha = 0.94f),
                        colors.surfaceContainer.copy(alpha = 0.96f),
                        colors.surface.copy(alpha = 0.98f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.outlineVariant.copy(alpha = 0.68f),
                        colors.outline.copy(alpha = 0.20f),
                        colors.scrim.copy(alpha = 0.18f),
                    ),
                ),
                shape = barShape,
            )
            .padding(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            TopLevelItem.entries.forEach { item ->
                LiquidGlassNavigationItem(
                    item = item,
                    selected = selectedDestination == item.destination,
                    onClick = { onSelectDestination(item.destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNavigationItem(
    item: TopLevelItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onTertiaryContainer else colors.onSurfaceVariant,
        label = "navigation item color",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "navigation item scale",
    )
    val itemShape = RoundedCornerShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(itemShape)
            .then(
                if (selected) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    colors.tertiaryContainer.copy(alpha = 0.72f),
                                    colors.tertiaryContainer.copy(alpha = 0.50f),
                                    colors.tertiaryContainer.copy(alpha = 0.28f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.tertiary.copy(alpha = 0.38f),
                                    colors.tertiary.copy(alpha = 0.24f),
                                ),
                            ),
                            shape = itemShape,
                        )
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(25.dp),
            )
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}
