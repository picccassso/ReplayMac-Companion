package com.replaymac.companion.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
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
            !isEnabled -> AppColors.SurfaceGlass.copy(alpha = 0.45f)
            isActiveDanger -> AppColors.Danger
            isProminent -> AppColors.Accent
            else -> AppColors.SurfaceGlass
        },
        label = "tileBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isActiveDanger || isProminent -> Color.White
            !isEnabled -> AppColors.TextTertiary
            else -> AppColors.TextPrimary
        },
        label = "tileContentColor"
    )

    val resolvedIconTint by animateColorAsState(
        targetValue = when {
            isActiveDanger || isProminent -> Color.White
            !isEnabled -> AppColors.TextTertiary
            iconTint != null -> iconTint
            else -> AppColors.Accent
        },
        label = "tileIconTint"
    )

    val subtitleColor by animateColorAsState(
        targetValue = when {
            isActiveDanger || isProminent -> Color.White.copy(alpha = 0.85f)
            !isEnabled -> AppColors.TextTertiary.copy(alpha = 0.7f)
            else -> AppColors.TextSecondary
        },
        label = "tileSubtitleColor"
    )

    val borderColor = when {
        isActiveDanger -> AppColors.Danger.copy(alpha = 0.6f)
        isProminent -> AppColors.AccentVariant.copy(alpha = 0.7f)
        else -> AppColors.GlassBorder
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
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
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Title + Status Subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = subtitleColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Spinner if loading
            if (isLoading) {
                Spacer(modifier = Modifier.width(6.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
