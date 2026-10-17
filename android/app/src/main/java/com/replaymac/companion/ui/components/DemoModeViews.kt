package com.replaymac.companion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

/** The Connect screen's way into demo mode, for trying the app without a Mac. */
@Composable
fun TryDemoEntry(onTryDemo: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlassButton(
            text = "Try without a Mac",
            icon = Icons.Rounded.PlayCircleOutline,
            onClick = onTryDemo,
            contentColor = AppColors.Accent
        )
        Text(
            text = "Explore every control with sample data. Nothing is sent anywhere.",
            fontSize = 12.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Sits above Home for as long as the demo runs, tinted so sample data can't be
 * mistaken for a real Mac. It can't be dismissed, only left with "Exit Demo".
 */
@Composable
fun DemoModeBanner(onExitDemo: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadii.Medium.dp),
        tint = AppColors.Warning.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = AppColors.Warning,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Demo mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Sample data — no Mac is connected.",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
            GlassButton(
                text = "Exit Demo",
                onClick = onExitDemo,
                contentColor = AppColors.Warning,
                tint = AppColors.Warning.copy(alpha = 0.2f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
