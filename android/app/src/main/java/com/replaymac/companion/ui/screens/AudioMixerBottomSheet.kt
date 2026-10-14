package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.model.CompanionAudioApp
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
fun AudioMixerBottomSheet(
    snapshot: CompanionStateSnapshot,
    onToggleMicrophone: () -> Unit,
    onToggleSystemAudio: () -> Unit,
    onMicrophoneVolumeChange: (Double) -> Unit,
    onSystemAudioVolumeChange: (Double) -> Unit,
    onToggleAppMute: (String) -> Unit,
    onUnmuteAllApps: () -> Unit,
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
                    text = "Audio Mixer",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Adjust source volume and mute individual Mac apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Volume Levels")

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppRadii.Medium.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        VolumeChannelRow(
                            title = "Microphone",
                            icon = if (snapshot.isMicrophoneEnabled && !snapshot.isMicrophoneMuted) {
                                Icons.Rounded.Mic
                            } else {
                                Icons.Rounded.MicOff
                            },
                            isEnabled = snapshot.isMicrophoneEnabled,
                            isMuted = snapshot.isMicrophoneMuted,
                            volume = snapshot.microphoneVolume,
                            onToggleMute = onToggleMicrophone,
                            onVolumeChange = onMicrophoneVolumeChange
                        )

                        RowSeparator(startIndent = 16.dp)

                        VolumeChannelRow(
                            title = "System Audio",
                            icon = if (snapshot.isSystemAudioEnabled && !snapshot.isSystemAudioMuted) {
                                Icons.AutoMirrored.Rounded.VolumeUp
                            } else {
                                Icons.AutoMirrored.Rounded.VolumeOff
                            },
                            isEnabled = snapshot.isSystemAudioEnabled,
                            isMuted = snapshot.isSystemAudioMuted,
                            volume = snapshot.systemAudioVolume,
                            onToggleMute = onToggleSystemAudio,
                            onVolumeChange = onSystemAudioVolumeChange
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val hasMutedApps = snapshot.audioApps.any { it.isMuted }
                SectionHeader(title = "Application Audio") {
                    if (hasMutedApps && snapshot.isSystemAudioEnabled && !snapshot.perAppAudioEnabled) {
                        Box(
                            modifier = Modifier
                                .glassPressable(onClick = onUnmuteAllApps, shape = CircleShape)
                                .background(AppColors.Accent.copy(alpha = 0.16f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Unmute All",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.Accent
                            )
                        }
                    }
                }

                when {
                    !snapshot.isSystemAudioEnabled -> {
                        EmptyAudioInfoBox(
                            message = "System audio capture is turned off. Turn on System Audio to manage individual app audio."
                        )
                    }
                    snapshot.perAppAudioEnabled -> {
                        EmptyAudioInfoBox(
                            message = "Selected app only mode is active on your Mac. Per-app muting is available when capturing full system audio."
                        )
                    }
                    snapshot.audioApps.isEmpty() -> {
                        EmptyAudioInfoBox(
                            message = "No audio applications reported by ReplayMac yet."
                        )
                    }
                    else -> {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppRadii.Medium.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                snapshot.audioApps.forEachIndexed { index, app ->
                                    AudioAppRow(
                                        app = app,
                                        onToggleMute = { onToggleAppMute(app.bundleId) }
                                    )
                                    if (index < snapshot.audioApps.lastIndex) {
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
}

@Composable
private fun VolumeChannelRow(
    title: String,
    icon: ImageVector,
    isEnabled: Boolean,
    isMuted: Boolean,
    volume: Double,
    onToggleMute: () -> Unit,
    onVolumeChange: (Double) -> Unit
) {
    var sliderPosition by remember(volume) { mutableFloatStateOf(volume.coerceIn(0.0, 1.0).toFloat()) }
    val isActive = isEnabled && !isMuted
    val statusBadgeText = when {
        !isEnabled -> "Off"
        isMuted -> "Muted"
        else -> HomeControlLabels.volumePercentageLabel(sliderPosition.toDouble())
    }
    val iconTint = when {
        !isEnabled -> AppColors.TextSecondary
        isMuted -> AppColors.Warning
        else -> AppColors.Accent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary
                )
                Text(
                    text = statusBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMuted) AppColors.Warning else AppColors.TextSecondary
                )
            }
            MuteTogglePill(
                label = when {
                    !isEnabled -> "Enable"
                    isMuted -> "Unmute"
                    else -> "Mute"
                },
                isHighlighted = isMuted,
                onClick = onToggleMute
            )
        }

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                onVolumeChange(newValue.toDouble())
            },
            valueRange = 0f..1f,
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = if (isActive) Color.White else AppColors.TextSecondary,
                activeTrackColor = if (isActive) AppColors.Accent else AppColors.TextTertiary,
                inactiveTrackColor = AppColors.SurfaceGlass
            )
        )
    }
}

@Composable
private fun AudioAppRow(
    app: CompanionAudioApp,
    onToggleMute: () -> Unit
) {
    val iconBitmap: ImageBitmap? = remember(app.bundleId, app.iconBase64) {
        if (!app.iconBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(app.iconBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(onClick = onToggleMute, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (iconBitmap != null) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .alpha(if (app.isMuted) 0.65f else 1f)
                )
                if (app.isMuted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(15.dp)
                            .background(AppColors.BackgroundSecondary, CircleShape)
                            .padding(1.5.dp)
                            .background(AppColors.Warning, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (app.isMuted) AppColors.Warning.copy(alpha = 0.16f) else AppColors.SurfaceGlass,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (app.isMuted) {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                        Icons.Rounded.Apps
                    },
                    contentDescription = null,
                    tint = if (app.isMuted) AppColors.Warning else AppColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (app.isRunning) AppColors.TextPrimary else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!app.isRunning) {
                Text(
                    text = "Not running",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary
                )
            }
        }

        MuteTogglePill(
            label = if (app.isMuted) "Muted" else "Audible",
            isHighlighted = app.isMuted,
            onClick = onToggleMute
        )
    }
}

@Composable
private fun MuteTogglePill(
    label: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isHighlighted) AppColors.Warning.copy(alpha = 0.18f) else AppColors.SurfaceGlass
    val fg = if (isHighlighted) AppColors.Warning else AppColors.TextPrimary
    Box(
        modifier = Modifier
            .glassPressable(onClick = onClick, shape = CircleShape)
            .glass(CircleShape, bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
private fun EmptyAudioInfoBox(message: String) {
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
