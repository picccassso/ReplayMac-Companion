package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.ui.components.GlassCard
import com.replaymac.companion.ui.theme.AppColors

@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    discoveredMacs: List<DiscoveredMac>,
    isDiscovering: Boolean,
    manualHost: String,
    manualPort: String,
    onManualHostChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onConnect: (String, String) -> Unit,
    onConnectAdbUsb: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onCancelConnection: () -> Unit,
    onDismissError: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val focusManager = LocalFocusManager.current
    var showAdbHelp by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundPrimary)
            .padding(
                horizontal = if (isLandscape) 20.dp else 18.dp,
                vertical = if (isLandscape) 14.dp else 20.dp
            )
    ) {
        when (connectionState) {
            is ConnectionState.PendingApproval -> {
                PendingApprovalView(
                    serverName = connectionState.serverName,
                    onCancel = onCancelConnection,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ConnectionState.Connecting -> {
                ConnectingProgressView(
                    host = connectionState.host,
                    port = connectionState.port,
                    onCancel = onCancelConnection,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                if (isLandscape) {
                    // Landscape: Two-Column Split Layout
                    LandscapeConnectionContent(
                        connectionState = connectionState,
                        discoveredMacs = discoveredMacs,
                        isDiscovering = isDiscovering,
                        manualHost = manualHost,
                        manualPort = manualPort,
                        showAdbHelp = showAdbHelp,
                        onToggleAdbHelp = { showAdbHelp = !showAdbHelp },
                        onManualHostChange = onManualHostChange,
                        onManualPortChange = onManualPortChange,
                        onConnect = onConnect,
                        onConnectAdbUsb = onConnectAdbUsb,
                        onRefreshDiscovery = onRefreshDiscovery,
                        onDismissError = onDismissError,
                        onOpenTutorial = onOpenTutorial
                    )
                } else {
                    // Portrait: Single Column
                    PortraitConnectionContent(
                        connectionState = connectionState,
                        discoveredMacs = discoveredMacs,
                        isDiscovering = isDiscovering,
                        manualHost = manualHost,
                        manualPort = manualPort,
                        showAdbHelp = showAdbHelp,
                        onToggleAdbHelp = { showAdbHelp = !showAdbHelp },
                        onManualHostChange = onManualHostChange,
                        onManualPortChange = onManualPortChange,
                        onConnect = onConnect,
                        onConnectAdbUsb = onConnectAdbUsb,
                        onRefreshDiscovery = onRefreshDiscovery,
                        onDismissError = onDismissError,
                        onOpenTutorial = onOpenTutorial
                    )
                }
            }
        }
    }
}

// ==================== Landscape Layout ====================

@Composable
private fun LandscapeConnectionContent(
    connectionState: ConnectionState,
    discoveredMacs: List<DiscoveredMac>,
    isDiscovering: Boolean,
    manualHost: String,
    manualPort: String,
    showAdbHelp: Boolean,
    onToggleAdbHelp: () -> Unit,
    onManualHostChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onConnect: (String, String) -> Unit,
    onConnectAdbUsb: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onDismissError: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Slim Top Header
        HeaderSection(
            isDiscovering = isDiscovering,
            onRefresh = onRefreshDiscovery,
            onOpenTutorial = onOpenTutorial,
            isLandscape = true
        )

        // Error message if present
        if (connectionState is ConnectionState.Failed) {
            ErrorBanner(error = connectionState.error, onDismiss = onDismissError)
        }

        // Two Columns Side-by-Side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Discovered Devices & USB
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AVAILABLE ON LOCAL NETWORK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextTertiary,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (discoveredMacs.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadarPulseIndicator(isSearching = isDiscovering)
                            Column {
                                Text(
                                    text = if (isDiscovering) "Searching Wi-Fi..." else "No Macs found",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = AppColors.TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Text(
                                    text = "Ensure ReplayMac is open on the same Wi-Fi.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AppColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                } else {
                    discoveredMacs.forEach { mac ->
                        DiscoveredMacRow(
                            mac = mac,
                            onConnect = { onConnect(mac.host, mac.port.toString()) }
                        )
                    }
                }

                // USB Card
                UsbTetheringCard(
                    onClick = onConnectAdbUsb,
                    onToggleHelp = onToggleAdbHelp
                )

                if (showAdbHelp) {
                    UsbHelpCard()
                }
            }

            // Right Column: Manual Connection
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MANUAL CONNECTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextTertiary,
                        fontWeight = FontWeight.Bold
                    )
                )

                ManualConnectionCard(
                    manualHost = manualHost,
                    manualPort = manualPort,
                    onManualHostChange = onManualHostChange,
                    onManualPortChange = onManualPortChange,
                    onConnect = onConnect
                )
            }
        }
    }
}

// ==================== Portrait Layout ====================

@Composable
private fun PortraitConnectionContent(
    connectionState: ConnectionState,
    discoveredMacs: List<DiscoveredMac>,
    isDiscovering: Boolean,
    manualHost: String,
    manualPort: String,
    showAdbHelp: Boolean,
    onToggleAdbHelp: () -> Unit,
    onManualHostChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onConnect: (String, String) -> Unit,
    onConnectAdbUsb: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onDismissError: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection(
                isDiscovering = isDiscovering,
                onRefresh = onRefreshDiscovery,
                onOpenTutorial = onOpenTutorial,
                isLandscape = false
            )
        }

        if (connectionState is ConnectionState.Failed) {
            item {
                ErrorBanner(error = connectionState.error, onDismiss = onDismissError)
            }
        }

        // Section header
        item {
            Text(
                text = "AVAILABLE ON LOCAL NETWORK",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (discoveredMacs.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadarPulseIndicator(isSearching = isDiscovering)
                        Text(
                            text = if (isDiscovering) "Looking for ReplayMac on Wi-Fi..." else "No Macs found on network",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = AppColors.TextSecondary
                            )
                        )
                        Text(
                            text = "Ensure ReplayMac is open on your Mac and on the same Wi-Fi.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AppColors.TextTertiary
                            )
                        )
                    }
                }
            }
        } else {
            items(discoveredMacs) { mac ->
                DiscoveredMacRow(
                    mac = mac,
                    onConnect = { onConnect(mac.host, mac.port.toString()) }
                )
            }
        }

        // Quick USB ADB Reverse Button
        item {
            UsbTetheringCard(
                onClick = onConnectAdbUsb,
                onToggleHelp = onToggleAdbHelp
            )
        }

        if (showAdbHelp) {
            item {
                UsbHelpCard()
            }
        }

        // Manual Connection Section
        item {
            Text(
                text = "MANUAL CONNECTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AppColors.TextTertiary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        item {
            ManualConnectionCard(
                manualHost = manualHost,
                manualPort = manualPort,
                onManualHostChange = onManualHostChange,
                onManualPortChange = onManualPortChange,
                onConnect = onConnect
            )
        }
    }
}

// ==================== Shared Sub-Composables ====================

@Composable
private fun HeaderSection(
    isDiscovering: Boolean,
    onRefresh: () -> Unit,
    onOpenTutorial: () -> Unit,
    isLandscape: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "ReplayMac Companion",
                style = if (isLandscape) {
                    MaterialTheme.typography.titleLarge.copy(
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    MaterialTheme.typography.headlineMedium.copy(
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Text(
                text = "Connect to your Mac on the same network or USB",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppColors.TextSecondary
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenTutorial,
                modifier = Modifier
                    .size(32.dp)
                    .background(AppColors.SurfaceGlassSubtle, CircleShape)
                    .border(1.dp, AppColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Tutorial & Guide",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(32.dp)
                    .background(AppColors.SurfaceGlassSubtle, CircleShape)
                    .border(1.dp, AppColors.GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = if (isDiscovering) AppColors.Accent else AppColors.TextPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit
) {
    GlassCard(
        backgroundColor = AppColors.DangerContainer,
        borderColor = AppColors.Danger.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AppColors.Danger,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppColors.Danger
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = AppColors.Danger.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UsbTetheringCard(
    onClick: () -> Unit,
    onToggleHelp: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AppColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cable,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "USB Tethering (adb reverse)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                )
                Text(
                    text = "Connect directly via 127.0.0.1:7375 over USB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextSecondary
                    )
                )
            }

            IconButton(
                onClick = onToggleHelp,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Help",
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun UsbHelpCard() {
    GlassCard(backgroundColor = AppColors.BackgroundSecondary) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "USB Setup Command:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Accent
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "adb reverse tcp:7375 tcp:7375",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AppColors.TextPrimary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Run this in your Mac terminal when your phone is plugged in via USB.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AppColors.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun ManualConnectionCard(
    manualHost: String,
    manualPort: String,
    onManualHostChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onConnect: (String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = manualHost,
                    onValueChange = onManualHostChange,
                    label = { Text("IP Address / Host") },
                    placeholder = { Text("e.g. 192.168.1.100") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.GlassBorder,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedLabelColor = AppColors.Accent,
                        unfocusedLabelColor = AppColors.TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = manualPort,
                    onValueChange = onManualPortChange,
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Accent,
                        unfocusedBorderColor = AppColors.GlassBorder,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedLabelColor = AppColors.Accent,
                        unfocusedLabelColor = AppColors.TextSecondary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (manualHost.isNotBlank()) {
                                onConnect(manualHost, manualPort)
                            }
                        }
                    )
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onConnect(manualHost, manualPort)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = manualHost.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Connect to Mac",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun DiscoveredMacRow(
    mac: DiscoveredMac,
    onConnect: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onConnect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AppColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mac.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                )
                Text(
                    text = "${mac.host}:${mac.port}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.TextSecondary
                    )
                )
            }

            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
fun RadarPulseIndicator(isSearching: Boolean) {
    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSearching) {
            val transition = rememberInfiniteTransition(label = "radar")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 2.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radarScale"
            )
            val alpha by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "radarAlpha"
            )

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .scale(scale)
                    .background(AppColors.Accent.copy(alpha = alpha), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .background(AppColors.Accent.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PendingApprovalView(
    serverName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AppColors.Warning.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = AppColors.Warning,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Approval Required",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            )

            Text(
                text = "Please click \"Approve\" on \"$serverName\" to pair this companion device.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppColors.TextSecondary
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = AppColors.Accent,
                strokeWidth = 3.dp
            )

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ConnectingProgressView(
    host: String,
    port: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(38.dp),
                color = AppColors.Accent
            )

            Text(
                text = "Connecting to ReplayMac...",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            )

            Text(
                text = "ws://$host:$port",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppColors.TextSecondary
                )
            )

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
