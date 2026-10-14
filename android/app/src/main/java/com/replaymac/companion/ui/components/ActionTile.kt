package com.replaymac.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    minHeight: Dp = 64.dp,
    isCompact: Boolean = false,
    compactTitle: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    val palette = paletteFor(tone)
    val fill by animateColorAsState(palette.fill, label = "tileFill")
    val iconTint by animateColorAsState(palette.icon, label = "tileIcon")
    val titleColor by animateColorAsState(palette.title, label = "tileTitle")
    val subtitleColor by animateColorAsState(palette.subtitle, label = "tileSubtitle")
    val shape = RoundedCornerShape(AppRadii.Medium.dp)

    val displayTitle = (if (isCompact) compactTitle else null) ?: title
    val resolvedMinHeight = if (isCompact && minHeight == 64.dp) 54.dp else minHeight
    val horizontalPadding = if (isCompact) 10.dp else (if (onSecondaryClick != null) 12.dp else 14.dp)
    val verticalPadding = if (isCompact) 8.dp else 10.dp
    val contentSpacing = if (isCompact) {
        if (onSecondaryClick != null) 6.dp else 8.dp
    } else {
        if (onSecondaryClick != null) 10.dp else 12.dp
    }
    val iconSize = if (isCompact) 18.dp else 22.dp
    val progressSize = if (isCompact) 18.dp else 20.dp
    val secondarySize = if (isCompact) 26.dp else 30.dp
    val secondaryIconSize = if (isCompact) 13.dp else 15.dp

    var scaledTitleFontSize by remember(displayTitle, isCompact) {
        mutableStateOf(if (isCompact) 13.5.sp else 14.sp)
    }

    val titleStyle = MaterialTheme.typography.titleSmall.copy(
        fontSize = scaledTitleFontSize,
        lineHeight = (scaledTitleFontSize.value * 1.2f).sp,
        fontWeight = FontWeight.SemiBold
    )

    val subtitleStyle = if (isCompact) {
        MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp)
    } else {
        MaterialTheme.typography.labelMedium
    }

    Row(
        modifier = modifier
            .heightIn(min = resolvedMinHeight)
            .glassPressable(
                onClick = onClick,
                enabled = isEnabled && !isLoading,
                shape = shape,
                onLongClick = onSecondaryClick
            )
            .glass(shape, fill, rimAlpha = if (fill.alpha > 0.5f) 0.5f else 1f)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(contentSpacing)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(progressSize), color = iconTint, strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = displayTitle,
                style = titleStyle,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && scaledTitleFontSize > 10.sp) {
                        scaledTitleFontSize *= 0.92f
                    }
                }
            )
            Text(
                text = subtitle,
                style = subtitleStyle,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
        if (onSecondaryClick != null) {
            Box(
                modifier = Modifier
                    .size(secondarySize)
                    .glassPressable(onClick = onSecondaryClick, shape = CircleShape)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "$title mixer options",
                    tint = subtitleColor,
                    modifier = Modifier.size(secondaryIconSize)
                )
            }
        }
    }
}
