package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DesktopMac
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.model.CompanionDisplayInfo
import com.replaymac.companion.data.model.CompanionProfileSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.ui.components.GlassCard
import com.replaymac.companion.ui.components.RowSeparator
import com.replaymac.companion.ui.components.SectionHeader
import com.replaymac.companion.ui.components.glass
import com.replaymac.companion.ui.components.glassPressable
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSetupBottomSheet(
    snapshot: CompanionStateSnapshot,
    onSelectDisplay: (String) -> Unit,
    onApplyProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = if (isLandscape) 760.dp else 640.dp,
        containerColor = AppColors.BackgroundSecondary,
        contentColor = AppColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = if (isLandscape) 8.dp else 10.dp, bottom = if (isLandscape) 8.dp else 14.dp)
                    .size(width = 40.dp, height = if (isLandscape) 4.dp else 5.dp)
                    .background(AppColors.TextTertiary, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Display & Profiles",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Switch the active Mac display or apply a saved capture profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Capture Display")

                if (snapshot.displays.isEmpty()) {
                    EmptySetupInfoBox(
                        message = snapshot.capturedDisplayName?.let {
                            "Capturing $it."
                        } ?: "No external displays reported by ReplayMac."
                    )
                } else {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadii.Medium.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            snapshot.displays.forEachIndexed { index, display ->
                                DisplayOptionRow(
                                    display = display,
                                    isFallback = display.isActive && snapshot.isUsingFallbackDisplay,
                                    onSelect = { onSelectDisplay(display.id) }
                                )
                                if (index < snapshot.displays.lastIndex) {
                                    RowSeparator(startIndent = 56.dp)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Capture Profiles")

                if (snapshot.profiles.isEmpty()) {
                    EmptySetupInfoBox(
                        message = "No capture profiles configured on your Mac. Create profiles in ReplayMac Settings to switch presets remotely."
                    )
                } else {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppRadii.Medium.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            snapshot.profiles.forEachIndexed { index, profile ->
                                ProfileOptionRow(
                                    profile = profile,
                                    onSelect = { onApplyProfile(profile.id) }
                                )
                                if (index < snapshot.profiles.lastIndex) {
                                    RowSeparator(startIndent = 56.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayOptionRow(
    display: CompanionDisplayInfo,
    isFallback: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(
                onClick = onSelect,
                enabled = display.isConnected,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (display.isActive) AppColors.Accent.copy(alpha = 0.18f) else AppColors.SurfaceGlass,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.DesktopMac,
                contentDescription = null,
                tint = if (display.isActive) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = display.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val statusSubtitle = when {
                !display.isConnected -> "Disconnected"
                display.isActive && isFallback -> "Capturing (Fallback)"
                display.isActive -> "Active display"
                else -> "Tap to switch"
            }
            Text(
                text = statusSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (display.isActive) AppColors.Accent else AppColors.TextSecondary
            )
        }

        if (display.isActive) {
            ActiveBadge()
        }
    }
}

@Composable
private fun ProfileOptionRow(
    profile: CompanionProfileSummary,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(onClick = onSelect, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (profile.isActive) AppColors.Accent.copy(alpha = 0.18f) else AppColors.SurfaceGlass,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = if (profile.isActive) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (profile.summary.isNotBlank()) {
                Text(
                    text = profile.summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (profile.isActive) {
            ActiveBadge()
        }
    }
}

@Composable
private fun ActiveBadge() {
    Row(
        modifier = Modifier
            .background(AppColors.Accent.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Active",
            tint = AppColors.Accent,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "Active",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Accent
        )
    }
}

@Composable
private fun EmptySetupInfoBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(AppRadii.Medium.dp), AppColors.SurfaceGlassSubtle, rimAlpha = 0.6f)
            .padding(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}
