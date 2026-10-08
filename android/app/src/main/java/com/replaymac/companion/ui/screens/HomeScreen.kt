package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onOpenTutorial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLive = snapshot.isRecording || snapshot.isSessionRecording
    val serverName = (connectionState as? ConnectionState.Connected)?.serverName ?: "ReplayMac"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundPrimary)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (isLandscape) 20.dp else 16.dp,
                end = if (isLandscape) 20.dp else 16.dp,
                top = if (isLandscape) 12.dp else 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)
        ) {
            if (isLandscape) {
                // Landscape Top Bar & Status (Slim Desktop-matching bar)
                item {
                    LandscapeTopStatusBar(
                        serverName = serverName,
                        snapshot = snapshot,
                        isLive = isLive,
                        keepScreenAwake = keepScreenAwake,
                        onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                        onToggleBuffer = onToggleBuffer,
                        onOpenTutorial = onOpenTutorial,
                        onDisconnect = onDisconnect
                    )
                }

                // Landscape Dual Progress Meters (when buffer active)
                if (snapshot.isRecording) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
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

                // Landscape 3-Column x 2-Row Tile Grid (Desktop Parity!)
                item {
                    LandscapeThreeColumnGrid(
                        snapshot = snapshot,
                        onSaveReplay = onSaveReplay,
                        onSaveExtendedReplay = onSaveExtendedReplay,
                        onToggleBuffer = onToggleBuffer,
                        onToggleSession = onToggleSession,
                        onToggleMicrophone = onToggleMicrophone,
                        onToggleSystemAudio = onToggleSystemAudio
                    )
                }
            } else {
                // Portrait Top App Bar
                item {
                    PortraitTopAppBar(
                        serverName = serverName,
                        keepScreenAwake = keepScreenAwake,
                        onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                        onOpenTutorial = onOpenTutorial,
                        onDisconnect = onDisconnect
                    )
                }

                // Portrait Status Card
                item {
                    PortraitStatusCard(
                        snapshot = snapshot,
                        isLive = isLive,
                        onToggleBuffer = onToggleBuffer
                    )
                }

                // Portrait 2-Column x 3-Row Grid (Compact Horizontal Pills)
                item {
                    PortraitSixButtonGrid(
                        snapshot = snapshot,
                        onSaveReplay = onSaveReplay,
                        onSaveExtendedReplay = onSaveExtendedReplay,
                        onToggleBuffer = onToggleBuffer,
                        onToggleSession = onToggleSession,
                        onToggleMicrophone = onToggleMicrophone,
                        onToggleSystemAudio = onToggleSystemAudio
                    )
                }
            }

            // Recent Clips ("This Session") Section (works seamlessly in both portrait & landscape)
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

// ==================== Portrait Components ====================

@Composable
private fun PortraitTopAppBar(
    serverName: String,
    keepScreenAwake: Boolean,
    onToggleKeepScreenAwake: () -> Unit,
    onOpenTutorial: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connected Server Badge
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
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Actions: Deck Mode, Help, Disconnect
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckModeButton(
                keepScreenAwake = keepScreenAwake,
                onToggle = onToggleKeepScreenAwake
            )

            IconButton(
                onClick = onOpenTutorial,
                modifier = Modifier
                    .size(32.dp)
                    .background(AppColors.SurfaceGlassSubtle, CircleShape)
                    .border(1.dp, AppColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help & Tutorial",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

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
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun PortraitStatusCard(
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
        "Start replay buffer to keep last ${snapshot.bufferDurationSeconds}s ready to save."
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusDot(isLive = isLive)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                        )
                        Text(
                            text = subheadline,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AppColors.TextSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                BufferPillButton(
                    isRunning = snapshot.isRecording,
                    onClick = onToggleBuffer
                )
            }

            AnimatedVisibility(
                visible = snapshot.isRecording,
                enter = fadeIn(),
                exit = fadeOut()
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
private fun PortraitSixButtonGrid(
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Save Quick Replay & Save Extended Replay
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isSaving = snapshot.isSaveInProgress
            ActionTile(
                title = "Save Last ${snapshot.bufferDurationSeconds}s",
                subtitle = when {
                    isSaving -> "Saving clip..."
                    snapshot.isSaveSuccess -> "Saved!"
                    snapshot.isSaveFailed -> "Failed"
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
                    snapshot.isSaveInProgress -> "Saving..."
                    !snapshot.isRecording -> "Buffer idle"
                    !snapshot.longBufferEnabled -> "Disabled"
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = if (snapshot.isRecording) "Stop Buffer" else "Start Buffer",
                subtitle = if (snapshot.isRecording) "Running" else "Stopped",
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val micMuted = snapshot.isMicrophoneMuted
            val micEnabled = snapshot.isMicrophoneEnabled
            ActionTile(
                title = "Microphone",
                subtitle = when {
                    !micEnabled -> "Disabled"
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

// ==================== Landscape Components (Desktop Parity) ====================

@Composable
private fun LandscapeTopStatusBar(
    serverName: String,
    snapshot: CompanionStateSnapshot,
    isLive: Boolean,
    keepScreenAwake: Boolean,
    onToggleKeepScreenAwake: () -> Unit,
    onToggleBuffer: () -> Unit,
    onOpenTutorial: () -> Unit,
    onDisconnect: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Status indicator + Server Name + Live Duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusDot(isLive = isLive)

                Text(
                    text = serverName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppColors.TextTertiary
                    )
                )

                val statusText = when {
                    snapshot.isSessionRecording -> "Session: ${formatDurationSeconds(snapshot.sessionElapsedSeconds)}"
                    snapshot.isRecording -> "Buffer: ${formatDurationSeconds(snapshot.recordingElapsedSeconds)}"
                    else -> "Not recording"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isLive) AppColors.AccentVariant else AppColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )

                snapshot.capturedDisplayName?.let { display ->
                    Text(
                        text = "($display)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppColors.TextTertiary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Buffer quick toggle + Deck button + Help + Disconnect
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BufferPillButton(
                    isRunning = snapshot.isRecording,
                    onClick = onToggleBuffer
                )

                DeckModeButton(
                    keepScreenAwake = keepScreenAwake,
                    onToggle = onToggleKeepScreenAwake
                )

                IconButton(
                    onClick = onOpenTutorial,
                    modifier = Modifier
                        .size(30.dp)
                        .background(AppColors.SurfaceGlassSubtle, CircleShape)
                        .border(1.dp, AppColors.GlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Help & Tutorial",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .size(30.dp)
                        .background(AppColors.SurfaceGlassSubtle, CircleShape)
                        .border(1.dp, AppColors.GlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Disconnect",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeThreeColumnGrid(
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: [Save Last 30s] [Start Buffer] [Record Session] (Matches Desktop Layout!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isSaving = snapshot.isSaveInProgress
            ActionTile(
                title = "Save Last ${snapshot.bufferDurationSeconds}s",
                subtitle = when {
                    isSaving -> "Saving clip..."
                    snapshot.isSaveSuccess -> "Saved!"
                    snapshot.isSaveFailed -> "Failed"
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

        // Row 2: [Save Last 5m] [Microphone] [System Audio]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val extSeconds = snapshot.longBufferDurationSeconds
            val extTitle = if (extSeconds % 60 == 0) "Save Last ${extSeconds / 60}m" else "Save Last ${extSeconds}s"
            ActionTile(
                title = extTitle,
                subtitle = when {
                    snapshot.isSaveInProgress -> "Saving..."
                    !snapshot.isRecording -> "Buffer idle"
                    !snapshot.longBufferEnabled -> "Disabled"
                    else -> "Extended replay"
                },
                icon = Icons.Default.History,
                onClick = onSaveExtendedReplay,
                isEnabled = snapshot.canSaveExtendedReplay,
                modifier = Modifier.weight(1f)
            )

            val micMuted = snapshot.isMicrophoneMuted
            val micEnabled = snapshot.isMicrophoneEnabled
            ActionTile(
                title = "Microphone",
                subtitle = when {
                    !micEnabled -> "Disabled"
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

// ==================== Shared UI Components ====================

@Composable
private fun DeckModeButton(
    keepScreenAwake: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (keepScreenAwake) AppColors.Accent.copy(alpha = 0.2f) else AppColors.SurfaceGlass,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (keepScreenAwake) AppColors.Accent else AppColors.GlassBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Deck Mode",
                tint = if (keepScreenAwake) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier.size(15.dp)
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
}

@Composable
fun BufferPillButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isRunning) AppColors.DangerContainer else AppColors.Accent.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRunning) AppColors.Danger.copy(alpha = 0.5f) else AppColors.Accent.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isRunning) AppColors.Danger else AppColors.Accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRunning) "Stop Buffer" else "Start",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) AppColors.Danger else AppColors.Accent
                )
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    modifier = Modifier.padding(14.dp)
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
