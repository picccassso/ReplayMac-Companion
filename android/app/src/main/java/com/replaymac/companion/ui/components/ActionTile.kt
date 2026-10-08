package com.replaymac.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

/** How a tile is coloured, independent of how it is drawn. */
enum class TileTone {
    Normal,
    /** Accent-filled primary action, like the desktop's prominent Save tile. */
    Prominent,
    /** Red while something is actively capturing. */
    Live,
    /** Orange for a muted audio source. */
    Muted,
    /** Grey for a source that is switched off on the Mac. */
    Off
}

private data class TilePalette(val fill: Color, val icon: Color, val title: Color, val subtitle: Color)

private fun paletteFor(tone: TileTone): TilePalette = when (tone) {
    TileTone.Prominent -> TilePalette(AppColors.Accent, Color.White, Color.White, Color.White.copy(alpha = 0.75f))
    TileTone.Live -> TilePalette(AppColors.Danger, Color.White, Color.White, Color.White.copy(alpha = 0.8f))
    TileTone.Muted -> TilePalette(AppColors.SurfaceGlass, AppColors.Warning, AppColors.TextPrimary, AppColors.TextSecondary)
    TileTone.Off -> TilePalette(AppColors.SurfaceGlass, AppColors.TextSecondary, AppColors.TextPrimary, AppColors.TextSecondary)
    TileTone.Normal -> TilePalette(AppColors.SurfaceGlass, AppColors.Accent, AppColors.TextPrimary, AppColors.TextSecondary)
}

/**
 * The desktop Home page's action tile: symbol beside the title, an accent
 * fill for the primary action and a quiet fill otherwise. A status line
 * stands in for the desktop's keyboard shortcut hint.
 */
@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: TileTone = TileTone.Normal,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Dp = 64.dp
) {
    val palette = paletteFor(tone)
    val fill by animateColorAsState(palette.fill, label = "tileFill")
    val iconTint by animateColorAsState(palette.icon, label = "tileIcon")
    val titleColor by animateColorAsState(palette.title, label = "tileTitle")
    val subtitleColor by animateColorAsState(palette.subtitle, label = "tileSubtitle")
    val shape = RoundedCornerShape(AppRadii.Medium.dp)

    Row(
        modifier = modifier
            .heightIn(min = minHeight)
            .glassPressable(onClick = onClick, enabled = isEnabled && !isLoading, shape = shape)
            .glass(shape, fill, rimAlpha = if (fill.alpha > 0.5f) 0.5f else 1f)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = iconTint, strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
    }
}
