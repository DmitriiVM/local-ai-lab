package com.dmitriim.localaiplayground.core.result

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val screenPadding: Dp = 20.dp,
    val sectionSpacing: Dp = 16.dp,
    val itemSpacing: Dp = 12.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val navigationRailBreakpoint: Dp = 720.dp,
    val topBarOverlayClearance: Dp = 88.dp,
)

val LocalAppDimensions = staticCompositionLocalOf { AppDimensions() }
