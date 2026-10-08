package com.replaymac.companion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.ui.components.ActionTile
import com.replaymac.companion.ui.components.DualProgressMeters
import com.replaymac.companion.ui.components.GlassCard
import com.replaymac.companion.ui.components.RecentClipItem
import com.replaymac.companion.ui.components.StatusDot
import com.replaymac.companion.ui.components.formatDurationSeconds
import com.replaymac.companion.ui.theme.AppColors

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    snapshot: CompanionStateSnapshot,
    thumbnails: Map<String, String>,
    keepScreenAwake: Boolean,
    onToggleKeepScreenAwake: () -> Unit,
    onDisconnect: () -> Unit,
    onSaveReplay: () -> Unit,
    onSaveExtendedReplay: () -> Unit,
    onToggleBuffer: () -> Unit,
    onToggleSession: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleSystemAudio: () -> Unit,
    onClipSelected: (CompanionClipSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = snapshot.isRecording || snapshot.isSessionRecording
    val serverName = (connectionState as? ConnectionState.Connected)?.serverName ?: "ReplayMac"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundPrimary)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top App Bar / Connection Bar
            item {
                TopAppBarSection(
                    serverName = serverName,
                    keepScreenAwake = keepScreenAwake,
                    onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                    onDisconnect = onDisconnect
                )
            }

            // Status Card
            item {
                StatusCard(
                    snapshot = snapshot,
                    isLive = isLive,
                    onToggleBuffer = onToggleBuffer
                )
            }

            // 6-Button Grid (2 columns x 3 rows)
            item {
                SixButtonGrid(
                    snapshot = snapshot,
                    onSaveReplay = onSaveReplay,
                    onSaveExtendedReplay = onSaveExtendedReplay,
                    onToggleBuffer = onToggleBuffer,
                    onToggleSession = onToggleSession,
                    onToggleMicrophone = onToggleMicrophone,
                    onToggleSystemAudio = onToggleSystemAudio
                )
            }

            // Recent Clips ("This Session") Section
            item {
                RecentClipsSection(
                    recentClips = snapshot.recentClips,
                    thumbnails = thumbnails,
                    onClipSelected = onClipSelected
                )
            }
        }
    }
}

@Composable
fun TopAppBarSection(
    serverName: String,
    keepScreenAwake: Boolean,
    onToggleKeepScreenAwake: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(AppColors.Success, CircleShape)
            )
            Text(
                text = serverName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Desk Deck Mode Toggle (Screen Awake)
            Surface(
                onClick = onToggleKeepScreenAwake,
                shape = RoundedCornerShape(20.dp),
                color = if (keepScreenAwake) AppColors.Accent.copy(alpha = 0.2f) else AppColors.SurfaceGlass,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (keepScreenAwake) AppColors.Accent else AppColors.GlassBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Deck Mode",
                        tint = if (keepScreenAwake) AppColors.Accent else AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (keepScreenAwake) "Deck On" else "Deck",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (keepScreenAwake) AppColors.Accent else AppColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Disconnect Button
            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(32.dp)
                    .background(AppColors.SurfaceGlassSubtle, CircleShape)
                    .border(1.dp, AppColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Disconnect",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    snapshot: CompanionStateSnapshot,
    isLive: Boolean,
    onToggleBuffer: () -> Unit
) {
    val headline = when {
        snapshot.isSessionRecording -> "Session recording"
        snapshot.isRecording -> "Replay buffer running"
        else -> "Not recording"
    }

    val elapsedSeconds = if (snapshot.isSessionRecording) {
        snapshot.sessionElapsedSeconds
    } else {
        snapshot.recordingElapsedSeconds
    }

    val subheadline = if (isLive) {
        val parts = mutableListOf<String>()
        parts.add(formatDurationSeconds(elapsedSeconds))
        snapshot.capturedDisplayName?.let { parts.add(it) }
        parts.joinToString(" · ")
    } else {
        "Start the replay buffer to keep your last ${snapshot.bufferDurationSeconds} seconds ready to save."
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusDot(isLive = isLive)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                        )
                        Text(
                            text = subheadline,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.TextSecondary
                            )
                        )
                    }
                }

                // Buffer Toggle Button / Pill
                BufferPillButton(
                    isRunning = snapshot.isRecording,
                    onClick = onToggleBuffer
                )
            }

            // Dual Progress Meters (shown when recording buffer is active)
            AnimatedVisibility(
                visible = snapshot.isRecording,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                DualProgressMeters(
                    bufferedSeconds = snapshot.bufferedSeconds,
                    bufferDurationSeconds = snapshot.bufferDurationSeconds,
                    bufferMemoryFormatted = snapshot.bufferMemoryFormatted,
                    longBufferEnabled = snapshot.longBufferEnabled,
                    extendedBufferElapsedSeconds = snapshot.extendedBufferElapsedSeconds,
                    longBufferDurationSeconds = snapshot.longBufferDurationSeconds
                )
            }
        }
    }
}

@Composable
fun BufferPillButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isRunning) AppColors.DangerContainer else AppColors.Accent.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRunning) AppColors.Danger.copy(alpha = 0.5f) else AppColors.Accent.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isRunning) AppColors.Danger else AppColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRunning) "Stop Buffer" else "Start",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) AppColors.Danger else AppColors.Accent
                )
            )
        }
    }
}

@Composable
fun SixButtonGrid(
    snapshot: CompanionStateSnapshot,
    onSaveReplay: () -> Unit,
    onSaveExtendedReplay: () -> Unit,
    onToggleBuffer: () -> Unit,
    onToggleSession: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleSystemAudio: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Save Last 30s & Save Last 5m
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val quickReplayTitle = "Save Last ${snapshot.bufferDurationSeconds}s"
            val isSaving = snapshot.isSaveInProgress
            ActionTile(
                title = quickReplayTitle,
                subtitle = when {
                    isSaving -> "Saving clip..."
                    snapshot.isSaveSuccess -> "Clip saved!"
                    snapshot.isSaveFailed -> "Save failed"
                    !snapshot.isRecording -> "Buffer idle"
                    snapshot.bufferedSeconds < 1.0 -> "Buffering..."
                    else -> "Ready to save"
                },
                icon = Icons.Default.Download,
                onClick = onSaveReplay,
                isProminent = snapshot.canSaveQuickReplay,
                isEnabled = snapshot.canSaveQuickReplay,
                isLoading = isSaving,
                modifier = Modifier.weight(1f)
            )

            val extSeconds = snapshot.longBufferDurationSeconds
            val extTitle = if (extSeconds % 60 == 0) "Save Last ${extSeconds / 60}m" else "Save Last ${extSeconds}s"
            ActionTile(
                title = extTitle,
                subtitle = when {
                    snapshot.isSaveInProgress -> "Saving clip..."
                    !snapshot.isRecording -> "Buffer idle"
                    !snapshot.longBufferEnabled -> "Disabled in Mac"
                    else -> "Extended replay"
                },
                icon = Icons.Default.History,
                onClick = onSaveExtendedReplay,
                isEnabled = snapshot.canSaveExtendedReplay,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Replay Buffer & Session Recording
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionTile(
                title = if (snapshot.isRecording) "Stop Buffer" else "Start Buffer",
                subtitle = if (snapshot.isRecording) "Running" else "Buffer stopped",
                icon = if (snapshot.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                onClick = onToggleBuffer,
                modifier = Modifier.weight(1f)
            )

            ActionTile(
                title = if (snapshot.isSessionRecording) "Stop Session" else "Record Session",
                subtitle = if (snapshot.isSessionRecording) "Recording live" else "Session capture",
                icon = Icons.Default.FiberManualRecord,
                onClick = onToggleSession,
                isActiveDanger = snapshot.isSessionRecording,
                isEnabled = !snapshot.isSaveInProgress,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Microphone & System Audio
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val micMuted = snapshot.isMicrophoneMuted
            val micEnabled = snapshot.isMicrophoneEnabled
            ActionTile(
                title = "Microphone",
                subtitle = when {
                    !micEnabled -> "Disabled in Mac"
                    micMuted -> "Muted"
                    else -> "Active"
                },
                icon = if (micMuted || !micEnabled) Icons.Default.MicOff else Icons.Default.Mic,
                onClick = onToggleMicrophone,
                isEnabled = micEnabled,
                iconTint = when {
                    !micEnabled -> AppColors.TextTertiary
                    micMuted -> AppColors.Warning
                    else -> AppColors.Accent
                },
                modifier = Modifier.weight(1f)
            )

            val sysMuted = snapshot.isSystemAudioMuted
            val sysEnabled = snapshot.isSystemAudioEnabled
            ActionTile(
                title = "System Audio",
                subtitle = when {
                    !sysEnabled -> "Disabled"
                    sysMuted -> "Muted"
                    else -> "Active"
                },
                icon = if (sysMuted || !sysEnabled) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                onClick = onToggleSystemAudio,
                isEnabled = sysEnabled,
                iconTint = when {
                    !sysEnabled -> AppColors.TextTertiary
                    sysMuted -> AppColors.Warning
                    else -> AppColors.Accent
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun RecentClipsSection(
    recentClips: List<CompanionClipSummary>,
    thumbnails: Map<String, String>,
    onClipSelected: (CompanionClipSummary) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This Session",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            )

            if (recentClips.isNotEmpty()) {
                Text(
                    text = "${recentClips.size} saved",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextSecondary
                    )
                )
            }
        }

        if (recentClips.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Clips you save appear here until you quit ReplayMac.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppColors.TextSecondary
                    ),
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    recentClips.forEachIndexed { index, clip ->
                        RecentClipItem(
                            clip = clip,
                            thumbnailBase64 = thumbnails[clip.id],
                            onClick = { onClipSelected(clip) }
                        )

                        if (index < recentClips.size - 1) {
                            HorizontalDivider(
                                color = AppColors.GlassBorder.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 122.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
