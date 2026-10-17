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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.ui.components.CompanionHaptics
import com.replaymac.companion.ui.components.OnboardingDialog
import com.replaymac.companion.ui.screens.AudioMixerBottomSheet
import com.replaymac.companion.ui.screens.CaptureSetupBottomSheet
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
    val showAudioMixerSheet by viewModel.showAudioMixerSheet.collectAsState()
    val showCaptureSetupSheet by viewModel.showCaptureSetupSheet.collectAsState()
    val manualHost by viewModel.manualHost.collectAsState()
    val manualPort by viewModel.manualPort.collectAsState()
    val showTutorialDialog by viewModel.showTutorialDialog.collectAsState()
    val isDemo by viewModel.isDemo.collectAsState()

    val view = LocalView.current
    var previousSaveStatus by remember { mutableStateOf(snapshot.saveStatus) }
    LaunchedEffect(snapshot.saveStatus) {
        val outcome = CompanionHaptics.saveTransitionOutcome(previousSaveStatus, snapshot.saveStatus)
        previousSaveStatus = snapshot.saveStatus
        if (outcome != null) {
            CompanionHaptics.performOutcome(view, outcome)
        }
    }

    val anySheetOpen = selectedClip != null || showAudioMixerSheet || showCaptureSetupSheet
    BackHandler(enabled = connectionState is ConnectionState.Connected && !anySheetOpen) {
        viewModel.disconnect()
    }

    if (connectionState is ConnectionState.Connected) {
        HomeScreen(
            connectionState = connectionState,
            snapshot = snapshot,
            thumbnails = thumbnails,
            isDemo = isDemo,
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
            onOpenTutorial = { viewModel.openTutorial() },
            onOpenAudioMixer = { viewModel.openAudioMixer() },
            onOpenCaptureSetup = { viewModel.openCaptureSetup() }
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
            onOpenTutorial = { viewModel.openTutorial() },
            onTryDemo = { viewModel.startDemo() }
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
            onTrimClip = { clipId -> viewModel.trimClip(clipId) },
            onCopyClip = { clipId -> viewModel.copyClip(clipId) },
            onDismiss = { viewModel.selectClip(null) }
        )
    }

    if (showAudioMixerSheet && connectionState is ConnectionState.Connected) {
        AudioMixerBottomSheet(
            snapshot = snapshot,
            onToggleMicrophone = { viewModel.toggleMicrophone() },
            onToggleSystemAudio = { viewModel.toggleSystemAudio() },
            onMicrophoneVolumeChange = { volume -> viewModel.setMicrophoneVolume(volume) },
            onSystemAudioVolumeChange = { volume -> viewModel.setSystemAudioVolume(volume) },
            onToggleAppMute = { bundleId -> viewModel.toggleAppMute(bundleId) },
            onUnmuteAllApps = { viewModel.unmuteAllApps() },
            onDismiss = { viewModel.dismissAudioMixer() }
        )
    }

    if (showCaptureSetupSheet && connectionState is ConnectionState.Connected) {
        CaptureSetupBottomSheet(
            snapshot = snapshot,
            onSelectDisplay = { displayId -> viewModel.selectDisplay(displayId) },
            onApplyProfile = { profileId -> viewModel.applyProfile(profileId) },
            onDismiss = { viewModel.dismissCaptureSetup() }
        )
    }
}
