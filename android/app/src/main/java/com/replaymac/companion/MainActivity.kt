package com.replaymac.companion

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.ui.components.OnboardingDialog
import com.replaymac.companion.ui.screens.ClipDetailsBottomSheet
import com.replaymac.companion.ui.screens.ConnectionScreen
import com.replaymac.companion.ui.screens.HomeScreen
import com.replaymac.companion.ui.theme.AppColors
import com.replaymac.companion.ui.theme.ReplayMacCompanionTheme
import com.replaymac.companion.ui.viewmodel.CompanionViewModel
import com.replaymac.companion.ui.viewmodel.CompanionViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: CompanionViewModel by viewModels {
        CompanionViewModelFactory((application as CompanionApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars (Android 15 enforces this anyway); each
        // screen pads itself with the safe-drawing insets.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            ReplayMacCompanionTheme {
                val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()

                LaunchedEffect(keepScreenAwake) {
                    if (keepScreenAwake) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.BackgroundPrimary
                ) {
                    CompanionAppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Catch up on anything that changed on the Mac while away, or find the
        // remembered Mac again if the connection is gone.
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        viewModel.onAppBackgrounded()
        super.onStop()
    }
}

@Composable
fun CompanionAppContent(viewModel: CompanionViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val snapshot by viewModel.snapshot.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()
    val filteredDiscoveredMacs by viewModel.filteredDiscoveredMacs.collectAsState()
    val previouslyConnectedItems by viewModel.previouslyConnectedItems.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
    val selectedClip by viewModel.selectedClip.collectAsState()
    val manualHost by viewModel.manualHost.collectAsState()
    val manualPort by viewModel.manualPort.collectAsState()
    val showTutorialDialog by viewModel.showTutorialDialog.collectAsState()

    BackHandler(enabled = connectionState is ConnectionState.Connected && selectedClip == null) {
        viewModel.disconnect()
    }

    if (connectionState is ConnectionState.Connected) {
        HomeScreen(
            connectionState = connectionState,
            snapshot = snapshot,
            thumbnails = thumbnails,
            keepScreenAwake = keepScreenAwake,
            onToggleKeepScreenAwake = { viewModel.setKeepScreenAwake(!keepScreenAwake) },
            onDisconnect = { viewModel.disconnect() },
            onSaveReplay = { viewModel.saveReplay() },
            onSaveExtendedReplay = { viewModel.saveExtendedReplay() },
            onToggleBuffer = { viewModel.toggleBuffer() },
            onToggleSession = { viewModel.toggleSession() },
            onToggleMicrophone = { viewModel.toggleMicrophone() },
            onToggleSystemAudio = { viewModel.toggleSystemAudio() },
            onClipSelected = { clip -> viewModel.selectClip(clip) },
            onOpenTutorial = { viewModel.openTutorial() }
        )
    } else {
        ConnectionScreen(
            connectionState = connectionState,
            discoveredMacs = filteredDiscoveredMacs,
            previouslyConnectedItems = previouslyConnectedItems,
            isDiscovering = isDiscovering,
            manualHost = manualHost,
            manualPort = manualPort,
            onManualHostChange = { viewModel.setManualHost(it) },
            onManualPortChange = { viewModel.setManualPort(it) },
            onConnect = { host, port -> viewModel.connect(host, port) },
            onConnectRemembered = { item -> viewModel.connectRememberedMac(item) },
            onForgetMac = { mac -> viewModel.forgetMac(mac) },
            onConnectAdbUsb = { viewModel.connectAdbUsb() },
            onRefreshDiscovery = { viewModel.refreshDiscovery() },
            onCancelConnection = { viewModel.disconnect() },
            onDismissError = { viewModel.dismissError() },
            onOpenTutorial = { viewModel.openTutorial() }
        )
    }

    if (showTutorialDialog) {
        OnboardingDialog(
            onDismiss = { viewModel.dismissTutorial() }
        )
    }

    selectedClip?.let { clip ->
        ClipDetailsBottomSheet(
            clip = clip,
            thumbnailBase64 = thumbnails[clip.id],
            onRevealInFinder = { clipId -> viewModel.revealClip(clipId) },
            onDeleteClip = { clipId -> viewModel.deleteClip(clipId) },
            onDismiss = { viewModel.selectClip(null) }
        )
    }
}
