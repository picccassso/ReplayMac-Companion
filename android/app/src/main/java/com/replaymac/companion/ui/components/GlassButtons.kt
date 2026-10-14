package com.replaymac.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

/**
 * Click handling shared by every glass control: a light haptic tick, a
 * spring press-in (like a macOS glass button), and dimming when disabled.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.glassPressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: Shape,
    haptic: Boolean = true,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "pressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .alpha(if (enabled) 1f else 0.4f)
        .clip(shape)
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = ripple(color = Color.White),
                    enabled = enabled,
                    role = role,
                    onClick = {
                        if (haptic) CompanionHaptics.performTap(view)
                        onClick()
                    },
                    onLongClick = {
                        if (haptic) CompanionHaptics.performTap(view)
                        onLongClick()
                    }
                )
            } else {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = ripple(color = Color.White),
                    enabled = enabled,
                    role = role
                ) {
                    if (haptic) CompanionHaptics.performTap(view)
                    onClick()
                }
            }
        )
}

private val ButtonShape = RoundedCornerShape(AppRadii.Small.dp)

/** Filled accent button, the equivalent of the desktop's `.glassProminent` style. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = AppColors.Accent,
    shape: Shape = ButtonShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
) {
    ButtonContent(
        text = text,
        icon = icon,
        contentColor = Color.White,
        contentPadding = contentPadding,
        modifier = modifier
            .glassPressable(onClick = onClick, enabled = enabled, shape = shape)
            .background(color, shape)
    )
}

/** Translucent secondary button, the equivalent of the desktop's `.glass` style. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = AppColors.TextPrimary,
    tint: Color = AppColors.SurfaceGlass,
    shape: Shape = ButtonShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
) {
    ButtonContent(
        text = text,
        icon = icon,
        contentColor = contentColor,
        contentPadding = contentPadding,
        modifier = modifier
            .glassPressable(onClick = onClick, enabled = enabled, shape = shape)
            .glass(shape, tint)
    )
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    contentColor: Color,
    contentPadding: PaddingValues,
    modifier: Modifier
) {
    Row(
        modifier = modifier.padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(17.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

/** A round glass icon button with a full 48dp touch target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextPrimary,
    selected: Boolean = false,
    size: Dp = 40.dp
) {
    val fill by animateColorAsState(
        if (selected) AppColors.Accent else AppColors.SurfaceGlass,
        label = "iconButtonFill"
    )
    Box(
        modifier = modifier
            .defaultMinSize(48.dp, 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .glassPressable(onClick = onClick, shape = CircleShape)
                .glass(CircleShape, fill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (selected) Color.White else tint,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

/**
 * Several icon actions sharing one glass capsule, like a macOS 26 toolbar
 * group. Children are usually [GroupIconButton]s.
 */
@Composable
fun GlassButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .glass(CircleShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun GroupIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextPrimary,
    selected: Boolean = false
) {
    val fill by animateColorAsState(
        if (selected) AppColors.Accent else Color.Transparent,
        label = "groupButtonFill"
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .glassPressable(onClick = onClick, shape = CircleShape)
            .background(fill, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Color.White else tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
