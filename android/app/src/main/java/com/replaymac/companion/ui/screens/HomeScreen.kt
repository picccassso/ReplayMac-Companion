package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.ui.components.ActionTile
import com.replaymac.companion.ui.components.DualProgressMeters
import com.replaymac.companion.ui.components.GlassBackground
import com.replaymac.companion.ui.components.GlassButtonGroup
import com.replaymac.companion.ui.components.GlassCard
import com.replaymac.companion.ui.components.GroupIconButton
import com.replaymac.companion.ui.components.RecentClipItem
import com.replaymac.companion.ui.components.RowSeparator
import com.replaymac.companion.ui.components.SectionHeader
import com.replaymac.companion.ui.components.StatusDot
import com.replaymac.companion.ui.components.glass
import com.replaymac.companion.ui.components.glassPressable
import com.replaymac.companion.ui.components.safeContentPadding
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val serverName = (connectionState as? ConnectionState.Connected)?.serverName ?: "ReplayMac"
    val onControl: (HomeControl) -> Unit = { control ->
        when (control) {
            HomeControl.SaveReplay -> onSaveReplay()
            HomeControl.SaveExtendedReplay -> onSaveExtendedReplay()
            HomeControl.ReplayBuffer -> onToggleBuffer()
            HomeControl.Session -> onToggleSession()
            HomeControl.Microphone -> onToggleMicrophone()
            HomeControl.SystemAudio -> onToggleSystemAudio()
        }
    }

    GlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = safeContentPadding(
                horizontal = if (isLandscape) 24.dp else 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HomeTopBar(
                    serverName = serverName,
                    keepScreenAwake = keepScreenAwake,
                    onToggleKeepScreenAwake = onToggleKeepScreenAwake,
                    onOpenTutorial = onOpenTutorial,
                    onDisconnect = onDisconnect
                )
            }

            item {
                if (isLandscape) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatusCard(snapshot, onToggleBuffer, Modifier.weight(1f))
                        ControlGrid(snapshot, columns = 3, onControl = onControl, modifier = Modifier.weight(1.5f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatusCard(snapshot, onToggleBuffer)
                        ControlGrid(snapshot, columns = 2, onControl = onControl)
                    }
                }
            }

            item {
                SessionClipsSection(
                    recentClips = snapshot.recentClips,
                    thumbnails = thumbnails,
                    onClipSelected = onClipSelected
                )
            }
        }
    }
}

// ==================== Top bar ====================

@Composable
private fun HomeTopBar(
    serverName: String,
    keepScreenAwake: Boolean,
    onToggleKeepScreenAwake: () -> Unit,
    onOpenTutorial: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = serverName,
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(AppColors.Success, CircleShape))
                Text("Connected", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            }
        }

        GlassButtonGroup {
            GroupIconButton(
                icon = Icons.Rounded.Lightbulb,
                contentDescription = if (keepScreenAwake) "Deck mode on, screen stays awake" else "Deck mode off",
                onClick = onToggleKeepScreenAwake,
                selected = keepScreenAwake,
                tint = AppColors.TextSecondary
            )
            GroupIconButton(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = "Quick guide",
                onClick = onOpenTutorial,
                tint = AppColors.TextSecondary
            )
            GroupIconButton(
                icon = Icons.Rounded.PowerSettingsNew,
                contentDescription = "Disconnect",
                onClick = onDisconnect,
                tint = AppColors.Danger
            )
        }
    }
}

// ==================== Status ====================

@Composable
private fun StatusCard(
    snapshot: CompanionStateSnapshot,
    onToggleBuffer: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusDot(isLive = snapshot.isRecording || snapshot.isSessionRecording)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = HomeControlLabels.headline(snapshot),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = HomeControlLabels.subheadline(snapshot),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 3
                    )
                }
                BufferToggleButton(isRunning = snapshot.isRecording, onClick = onToggleBuffer)
            }

            AnimatedVisibility(
                visible = snapshot.isRecording,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(18.dp))
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
}

/**
 * The desktop status card's buffer button: a red play circle that grows into
 * a "Stop Buffer" pill while the buffer runs.
 */
@Composable
private fun BufferToggleButton(isRunning: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .glassPressable(onClick = onClick, shape = CircleShape)
            .background(AppColors.Danger.copy(alpha = 0.16f), CircleShape)
            .padding(horizontal = 11.dp)
            .semantics { contentDescription = if (isRunning) "Stop replay buffer" else "Start replay buffer" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = AppColors.Danger,
            modifier = Modifier.size(18.dp)
        )
        AnimatedVisibility(
            visible = isRunning,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Text(
                text = "Stop Buffer",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.Danger,
                modifier = Modifier.padding(start = 6.dp, end = 3.dp)
            )
        }
    }
}

// ==================== Controls ====================

@Composable
private fun ControlGrid(
    snapshot: CompanionStateSnapshot,
    columns: Int,
    onControl: (HomeControl) -> Unit,
    modifier: Modifier = Modifier
) {
    val tiles = HomeControlLabels.tiles(snapshot)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(columns).forEach { row ->
            // Intrinsic height keeps every tile in a row the same size, even
            // when one title wraps onto a second line.
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { spec ->
                    ActionTile(
                        title = spec.title,
                        subtitle = spec.subtitle,
                        icon = iconFor(spec.control, snapshot),
                        onClick = { onControl(spec.control) },
                        tone = spec.tone,
                        isEnabled = spec.isEnabled,
                        isLoading = spec.isLoading,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Material equivalents of the desktop's SF Symbols for each action. */
private fun iconFor(control: HomeControl, snapshot: CompanionStateSnapshot): ImageVector = when (control) {
    HomeControl.SaveReplay -> Icons.Rounded.Download
    HomeControl.SaveExtendedReplay -> Icons.Rounded.History
    HomeControl.ReplayBuffer -> if (snapshot.isRecording) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
    HomeControl.Session -> if (snapshot.isSessionRecording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord
    HomeControl.Microphone ->
        if (snapshot.isMicrophoneEnabled && !snapshot.isMicrophoneMuted) Icons.Rounded.Mic else Icons.Rounded.MicOff
    HomeControl.SystemAudio ->
        if (snapshot.isSystemAudioEnabled && !snapshot.isSystemAudioMuted) {
            Icons.AutoMirrored.Rounded.VolumeUp
        } else {
            Icons.AutoMirrored.Rounded.VolumeOff
        }
}

// ==================== Session history ====================

@Composable
private fun SessionClipsSection(
    recentClips: List<CompanionClipSummary>,
    thumbnails: Map<String, String>,
    onClipSelected: (CompanionClipSummary) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "This Session") {
            if (recentClips.isNotEmpty()) {
                Text(
                    text = "${recentClips.size} saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
        }

        if (recentClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(AppRadii.Medium.dp), AppColors.SurfaceGlassSubtle, rimAlpha = 0.6f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Clips you save appear here until you quit ReplayMac.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadii.Medium.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    recentClips.forEachIndexed { index, clip ->
                        RecentClipItem(
                            clip = clip,
                            thumbnailBase64 = thumbnails[clip.id],
                            onClick = { onClipSelected(clip) }
                        )
                        if (index < recentClips.lastIndex) RowSeparator(startIndent = 122.dp)
                    }
                }
            }
        }
    }
}
