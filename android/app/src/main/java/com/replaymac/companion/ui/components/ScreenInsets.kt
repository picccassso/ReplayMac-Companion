package com.replaymac.companion.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Content padding that keeps a screen clear of the status bar, navigation bar,
 * camera cutout and keyboard, plus the screen's own margins. The app draws
 * edge to edge, so every scrolling screen goes through this. [includeTopInset]
 * is false when something pinned above the list already clears the status bar.
 */
@Composable
fun safeContentPadding(horizontal: Dp, top: Dp, bottom: Dp, includeTopInset: Boolean = true): PaddingValues {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = insets.calculateStartPadding(direction) + horizontal,
        end = insets.calculateEndPadding(direction) + horizontal,
        top = (if (includeTopInset) insets.calculateTopPadding() else 0.dp) + top,
        bottom = insets.calculateBottomPadding() + bottom
    )
}
