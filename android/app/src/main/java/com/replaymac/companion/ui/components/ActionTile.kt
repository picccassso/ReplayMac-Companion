package com.replaymac.companion.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replaymac.companion.ui.theme.AppColors

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isProminent: Boolean = false,
    isActiveDanger: Boolean = false,
    isEnabled: Boolean = true,
    isLoading: Boolean = false,
    iconTint: Color? = null
) {
    val view = LocalView.current

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> AppColors.SurfaceGlass.copy(alpha = 0.5f)
            isActiveDanger -> AppColors.Danger
            isProminent -> AppColors.Accent
            else -> AppColors.SurfaceGlass
        },
        label = "tileBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isActiveDanger || isProminent -> Color.White
            else -> AppColors.TextPrimary
        },
        label = "tileContentColor"
    )

    val resolvedIconTint by animateColorAsState(
        targetValue = when {
            iconTint != null -> iconTint
            isActiveDanger || isProminent -> Color.White
            !isEnabled -> AppColors.TextTertiary
            else -> AppColors.Accent
        },
        label = "tileIconTint"
    )

    val subtitleColor by animateColorAsState(
        targetValue = when {
            isActiveDanger || isProminent -> Color.White.copy(alpha = 0.85f)
            else -> AppColors.TextSecondary
        },
        label = "tileSubtitleColor"
    )

    val borderColor = when {
        isActiveDanger -> AppColors.Danger.copy(alpha = 0.5f)
        isProminent -> AppColors.AccentVariant.copy(alpha = 0.6f)
        else -> AppColors.GlassBorder
    }

    Surface(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .alpha(if (isEnabled) 1.0f else 0.45f)
            .then(
                if (isEnabled && !isLoading) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White.copy(alpha = 0.3f))
                    ) {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Exception) {}
                        onClick()
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(24.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    ),
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = subtitleColor
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
