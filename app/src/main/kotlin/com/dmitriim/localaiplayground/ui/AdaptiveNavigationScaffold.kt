package com.dmitriim.localaiplayground.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmitriim.localaiplayground.core.navigation.TopLevelDestination
import com.dmitriim.localaiplayground.core.result.LocalAppDimensions

@Composable
fun AdaptiveNavigationScaffold(
    selectedDestination: TopLevelDestination,
    onSelectDestination: (TopLevelDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateUp: (() -> Unit)?,
    content: @Composable (Modifier) -> Unit,
) {
    val dimensions = LocalAppDimensions.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= dimensions.navigationRailBreakpoint) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    TopLevelItem.entries.forEach { item ->
                        NavigationRailItem(
                            selected = selectedDestination == item.destination,
                            onClick = { onSelectDestination(item.destination) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ) { contentPadding ->
                        content(Modifier.padding(contentPadding))
                    }
                    AppTopBar(
                        onOpenSettings = onOpenSettings,
                        onNavigateUp = onNavigateUp,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalAppDimensions provides dimensions.copy(bottomNavigationOverlayClearance = 112.dp),
                ) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ) { contentPadding ->
                        content(Modifier.padding(contentPadding))
                    }
                }
                LiquidGlassNavigationBar(
                    selectedDestination = selectedDestination,
                    onSelectDestination = onSelectDestination,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                AppTopBar(
                    onOpenSettings = onOpenSettings,
                    onNavigateUp = onNavigateUp,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassNavigationBar(
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(72.dp)
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
            .padding(6.dp),
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
        targetValue = if (selected) {
            colors.onSecondaryContainer
        } else {
            colors.onSurfaceVariant
        },
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
                                    colors.secondaryContainer.copy(alpha = 0.82f),
                                    colors.secondaryContainer.copy(alpha = 0.58f),
                                    colors.secondaryContainer.copy(alpha = 0.36f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.outlineVariant.copy(alpha = 0.72f),
                                    colors.outline.copy(alpha = 0.18f),
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(
    onOpenSettings: () -> Unit,
    onNavigateUp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            onNavigateUp?.let { navigateUp ->
                LiquidGlassToolbarButton(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Navigate back",
                    onClick = navigateUp,
                )
            }
        },
        actions = {
            LiquidGlassToolbarButton(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Open settings and privacy",
                onClick = onOpenSettings,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun LiquidGlassToolbarButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
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
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.11f),
                        Color.White.copy(alpha = 0.07f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.48f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ),
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}
