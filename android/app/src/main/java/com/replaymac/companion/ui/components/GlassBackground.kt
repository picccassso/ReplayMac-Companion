package com.replaymac.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.replaymac.companion.ui.theme.AppColors

/**
 * The window background every screen sits on: the neutral dark grey of a
 * macOS window, lifted slightly at the top so glass panels read as layered.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundPrimary)
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.05f), 0.5f to Color.Transparent))
    ) {
        content()
        StatusBarScrim(Modifier.align(Alignment.TopCenter))
    }
}

/** Keeps scrolled content from running under the clock and status icons. */
@Composable
private fun StatusBarScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(AppColors.BackgroundPrimary.copy(alpha = 0.94f))
    )
}
