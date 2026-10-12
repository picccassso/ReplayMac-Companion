package com.replaymac.companion.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.PreviouslyConnectedItem
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.ui.components.AccentButton
import com.replaymac.companion.ui.components.GlassBackground
import com.replaymac.companion.ui.components.GlassButton
import com.replaymac.companion.ui.components.GlassButtonGroup
import com.replaymac.companion.ui.components.GlassCard
import com.replaymac.companion.ui.components.GlassDialog
import com.replaymac.companion.ui.components.GlassTextField
import com.replaymac.companion.ui.components.GroupIconButton
import com.replaymac.companion.ui.components.RowSeparator
import com.replaymac.companion.ui.components.SectionHeader
import com.replaymac.companion.ui.components.StatusDot
import com.replaymac.companion.ui.components.glass
import com.replaymac.companion.ui.components.glassPressable
import com.replaymac.companion.ui.components.safeContentPadding
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.AppRadii

private const val ADB_REVERSE_COMMAND = "adb reverse tcp:7375 tcp:7375"

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
    previouslyConnectedItems: List<PreviouslyConnectedItem> = emptyList(),
    onConnectRemembered: (PreviouslyConnectedItem) -> Unit = {},
    onForgetMac: (RememberedMac) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var macToForget by remember { mutableStateOf<RememberedMac?>(null) }

    GlassBackground(modifier = modifier) {
        when (connectionState) {
            is ConnectionState.PendingApproval -> CenteredStatus {
                PendingApprovalView(
                    serverName = connectionState.serverName,
                    pairingCode = connectionState.pairingCode,
                    onCancel = onCancelConnection
                )
            }
            is ConnectionState.Connecting -> CenteredStatus {
                ConnectingProgressView(host = connectionState.host, port = connectionState.port, onCancel = onCancelConnection)
            }
            is ConnectionState.Reconnecting -> CenteredStatus {
                ReconnectingView(serverName = connectionState.serverName, onCancel = onCancelConnection)
            }
            else -> {
                val failure = connectionState as? ConnectionState.Failed
                val sections = ConnectionSections(
                    discoveredMacs = discoveredMacs,
                    previouslyConnectedItems = previouslyConnectedItems,
                    isDiscovering = isDiscovering,
                    manualHost = manualHost,
                    manualPort = manualPort,
                    onManualHostChange = onManualHostChange,
                    onManualPortChange = onManualPortChange,
                    onConnect = onConnect,
                    onConnectRemembered = onConnectRemembered,
                    onPromptForget = { macToForget = it },
                    onConnectAdbUsb = onConnectAdbUsb
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize().imePadding(),
                    contentPadding = safeContentPadding(
                        horizontal = if (isLandscape) 24.dp else 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ConnectionHeader(
                            isDiscovering = isDiscovering,
                            onRefresh = onRefreshDiscovery,
                            onOpenTutorial = onOpenTutorial
                        )
                    }
                    if (failure != null) {
                        item {
                            ErrorBanner(
                                title = if (failure.wasConnected) "Connection lost" else "Couldn't connect",
                                error = failure.error,
                                onDismiss = onDismissError
                            )
                        }
                    }
                    if (isLandscape) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    sections.PreviouslyConnected()
                                    sections.Network()
                                    sections.Usb()
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    sections.Manual()
                                }
                            }
                        }
                    } else {
                        item { sections.PreviouslyConnected() }
                        item { sections.Network() }
                        item { sections.Usb() }
                        item { sections.Manual() }
                    }
                }

                macToForget?.let { targetMac ->
                    GlassDialog(onDismissRequest = { macToForget = null }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(if (isLandscape) 0.55f else 0.88f)
                                .widthIn(max = 420.dp)
                                .glass(RoundedCornerShape(AppRadii.Large.dp), AppColors.SurfaceElevated)
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Forget “${targetMac.name}”?",
                                style = MaterialTheme.typography.titleLarge,
                                color = AppColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "You will need to allow this device again if you reconnect.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(22.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassButton(
                                    text = "Cancel",
                                    onClick = { macToForget = null },
                                    modifier = Modifier.weight(1f)
                                )
                                AccentButton(
                                    text = "Forget",
                                    onClick = {
                                        val mac = targetMac
                                        macToForget = null
                                        onForgetMac(mac)
                                    },
                                    color = AppColors.Danger,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The ways to reach a Mac, each a titled glass group like a desktop Settings section. */
private class ConnectionSections(
    val discoveredMacs: List<DiscoveredMac>,
    val previouslyConnectedItems: List<PreviouslyConnectedItem>,
    val isDiscovering: Boolean,
    val manualHost: String,
    val manualPort: String,
    val onManualHostChange: (String) -> Unit,
    val onManualPortChange: (String) -> Unit,
    val onConnect: (String, String) -> Unit,
    val onConnectRemembered: (PreviouslyConnectedItem) -> Unit,
    val onPromptForget: (RememberedMac) -> Unit,
    val onConnectAdbUsb: () -> Unit
) {
    @Composable
    fun PreviouslyConnected() {
        if (previouslyConnectedItems.isEmpty()) return
        Section(title = "Previously Connected") {
            previouslyConnectedItems.forEachIndexed { index, item ->
                PreviouslyConnectedRow(
                    item = item,
                    onConnect = { onConnectRemembered(item) },
                    onForget = { onPromptForget(item.mac) }
                )
                if (index < previouslyConnectedItems.lastIndex) {
                    RowSeparator(startIndent = 56.dp)
                }
            }
        }
    }

    @Composable
    fun Network() {
        Section(title = "On This Network", trailing = {
            if (isDiscovering && discoveredMacs.isNotEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AppColors.TextSecondary, strokeWidth = 1.5.dp)
            }
        }) {
            if (discoveredMacs.isEmpty()) {
                SearchingRow(isSearching = isDiscovering)
            } else {
                discoveredMacs.forEachIndexed { index, mac ->
                    DiscoveredMacRow(mac = mac, onConnect = { onConnect(mac.host, mac.port.toString()) })
                    if (index < discoveredMacs.lastIndex) RowSeparator(startIndent = 56.dp)
                }
            }
        }
    }

    @Composable
    fun Usb() {
        Section(title = "USB") {
            UsbConnectionRows(onConnect = onConnectAdbUsb)
        }
    }

    @Composable
    fun Manual() {
        Section(title = "Enter Manually") {
            ManualConnectionForm(
                manualHost = manualHost,
                manualPort = manualPort,
                onManualHostChange = onManualHostChange,
                onManualPortChange = onManualPortChange,
                onConnect = onConnect
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        SectionHeader(title = title, trailing = trailing)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().animateContentSize()) { content() }
        }
    }
}

// ==================== Header & banner ====================

@Composable
private fun ConnectionHeader(
    isDiscovering: Boolean,
    onRefresh: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Connect to your Mac", style = MaterialTheme.typography.headlineLarge, color = AppColors.TextPrimary)
            Text(
                text = "Turn on Companion Server in ReplayMac’s settings, then choose your Mac.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        Spacer(Modifier.size(12.dp))
        GlassButtonGroup {
            GroupIconButton(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = "Quick guide",
                onClick = onOpenTutorial,
                tint = AppColors.TextSecondary
            )
            GroupIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Search again",
                onClick = onRefresh,
                tint = if (isDiscovering) AppColors.Accent else AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorBanner(title: String, error: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(AppRadii.Medium.dp + 4.dp), AppColors.Danger.copy(alpha = 0.14f))
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = AppColors.TextPrimary)
            Text(error, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
        Box(
            modifier = Modifier.size(40.dp).glassPressable(onClick = onDismiss, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss error", tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

// ==================== Rows ====================

@Composable
fun PreviouslyConnectedRow(
    item: PreviouslyConnectedItem,
    onConnect: () -> Unit,
    onForget: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(
                onClick = onConnect,
                enabled = item.isOnline,
                shape = RoundedCornerShape(0.dp),
                haptic = false
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            Icons.Rounded.LaptopMac,
            contentDescription = null,
            tint = if (item.isOnline) AppColors.TextPrimary else AppColors.TextSecondary,
            modifier = Modifier.size(26.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.mac.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (item.isOnline) AppColors.TextPrimary else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusDot(
                    isLive = item.isOnline,
                    activeColor = AppColors.Success,
                    idleColor = AppColors.IndicatorIdle,
                    size = 7.dp
                )
                Text(
                    text = if (item.isOnline) {
                        item.onlineEndpoint?.let { "${it.host}:${it.port}" } ?: "Available on network"
                    } else {
                        "Offline"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .glassPressable(onClick = onForget, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "Forget ${item.mac.name}",
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        AccentButton(
            text = "Connect",
            onClick = onConnect,
            enabled = item.isOnline,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun DiscoveredMacRow(mac: DiscoveredMac, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(onClick = onConnect, shape = RoundedCornerShape(0.dp), haptic = false)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Rounded.LaptopMac, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f)) {
            Text(mac.name, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${mac.host}:${mac.port}", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
        }
        AccentButton(
            text = "Connect",
            onClick = onConnect,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun SearchingRow(isSearching: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isSearching) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = AppColors.TextSecondary, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (isSearching) "Searching…" else "No Macs found",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary
            )
            Text(
                text = "ReplayMac needs to be open, and on the same Wi-Fi as this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun UsbConnectionRows(onConnect: () -> Unit) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPressable(onClick = onConnect, shape = RoundedCornerShape(0.dp), haptic = false)
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Rounded.Usb, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f)) {
            Text("USB Cable", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            Text("Connects to 127.0.0.1:7375", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
        }
        Box(
            modifier = Modifier.size(40.dp).glassPressable(onClick = { showHelp = !showHelp }, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = if (showHelp) "Hide USB setup" else "Show USB setup",
                tint = if (showHelp) AppColors.Accent else AppColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AppColors.TextTertiary)
    }

    AnimatedVisibility(visible = showHelp, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Column {
            RowSeparator(startIndent = 16.dp)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "With the phone plugged in, run this on your Mac first:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.BackgroundPrimary.copy(alpha = 0.5f), RoundedCornerShape(AppRadii.Small.dp))
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ADB_REVERSE_COMMAND,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = AppColors.AccentVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .glassPressable(onClick = { clipboard.setText(AnnotatedString(ADB_REVERSE_COMMAND)) }, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy command", tint = AppColors.TextSecondary, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualConnectionForm(
    manualHost: String,
    manualPort: String,
    onManualHostChange: (String) -> Unit,
    onManualPortChange: (String) -> Unit,
    onConnect: (String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val connect = {
        focusManager.clearFocus()
        onConnect(manualHost, manualPort)
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassTextField(
                value = manualHost,
                onValueChange = onManualHostChange,
                label = "IP address or host",
                placeholder = "192.168.1.100",
                modifier = Modifier.weight(2.2f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
            )
            GlassTextField(
                value = manualPort,
                onValueChange = onManualPortChange,
                label = "Port",
                placeholder = "7375",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (manualHost.isNotBlank()) connect() })
            )
        }
        AccentButton(
            text = "Connect",
            onClick = connect,
            enabled = manualHost.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== In-progress states ====================

@Composable
private fun CenteredStatus(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(safeContentPadding(horizontal = 24.dp, top = 0.dp, bottom = 0.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.widthIn(max = 440.dp)) { content() }
    }
}

@Composable
private fun StatusPanel(
    title: String,
    message: String,
    onCancel: () -> Unit,
    detail: (@Composable () -> Unit)? = null,
    badge: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            badge()
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = AppColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
            if (detail != null) {
                Spacer(Modifier.height(18.dp))
                detail()
            }
            Spacer(Modifier.height(24.dp))
            GlassButton(text = "Cancel", onClick = onCancel, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PendingApprovalView(
    serverName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    pairingCode: String? = null
) {
    Box(modifier) {
        StatusPanel(
            title = "Waiting for approval",
            message = if (pairingCode != null) {
                "Check that ReplayMac on “$serverName” shows this code, then click Allow."
            } else {
                "Click Allow in ReplayMac on “$serverName” to pair this phone."
            },
            onCancel = onCancel,
            detail = pairingCode?.let { code -> { PairingCodeText(code) } }
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(32.dp))
        }
    }
}

/** The code is read off the Mac and compared digit by digit, so it's large and evenly spaced. */
@Composable
private fun PairingCodeText(code: String) {
    Text(
        text = code,
        style = MaterialTheme.typography.displaySmall.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 8.sp
        ),
        color = AppColors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { contentDescription = "Pairing code ${code.toList().joinToString(" ")}" }
    )
}

@Composable
fun ConnectingProgressView(host: String, port: Int, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        StatusPanel(title = "Connecting", message = "$host:$port", onCancel = onCancel) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = AppColors.TextSecondary, strokeWidth = 2.5.dp)
        }
    }
}

@Composable
fun ReconnectingView(serverName: String, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        StatusPanel(
            title = "Reconnecting",
            message = "Looking for “$serverName” on this network and over USB.",
            onCancel = onCancel
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = AppColors.TextSecondary, strokeWidth = 2.5.dp)
        }
    }
}
