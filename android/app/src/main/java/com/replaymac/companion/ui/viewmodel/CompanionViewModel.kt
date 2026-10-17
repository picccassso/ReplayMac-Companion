package com.replaymac.companion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.PreviouslyConnectedItem
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.data.network.CompanionWebSocketClient
import com.replaymac.companion.data.repository.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

typealias PreviouslyConnectedItem = PreviouslyConnectedItem

class CompanionViewModel(
    private val repository: CompanionRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val snapshot: StateFlow<CompanionStateSnapshot> = repository.snapshot
    val thumbnails: StateFlow<Map<String, String>> = repository.thumbnails
    val discoveredMacs: StateFlow<List<DiscoveredMac>> = repository.discoveredMacs
    val isDiscovering: StateFlow<Boolean> = repository.isDiscovering
    val keepScreenAwake: StateFlow<Boolean> = repository.keepScreenAwake
    val hasSeenOnboarding: StateFlow<Boolean> = repository.hasSeenOnboarding
    val previouslyConnectedMacs: StateFlow<List<RememberedMac>> = repository.previouslyConnectedMacs
    /** True while the app shows the demo Mac's sample data instead of a real Mac. */
    val isDemo: StateFlow<Boolean> = repository.isDemoActive

    val previouslyConnectedItems: StateFlow<List<PreviouslyConnectedItem>> =
        combine(repository.previouslyConnectedMacs, repository.discoveredMacs) { saved, discovered ->
            saved.map { mac ->
                val match = discovered.firstOrNull { mac.isSameMacAs(it) }
                val onlineEndpoint = match?.let { MacEndpoint(it.host, it.port) }
                PreviouslyConnectedItem(
                    mac = mac,
                    isOnline = match != null,
                    onlineEndpoint = onlineEndpoint
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repository.previouslyConnectedMacs.value.map { mac ->
                val match = repository.discoveredMacs.value.firstOrNull { mac.isSameMacAs(it) }
                val onlineEndpoint = match?.let { MacEndpoint(it.host, it.port) }
                PreviouslyConnectedItem(
                    mac = mac,
                    isOnline = match != null,
                    onlineEndpoint = onlineEndpoint
                )
            }
        )

    val filteredDiscoveredMacs: StateFlow<List<DiscoveredMac>> =
        combine(repository.discoveredMacs, repository.previouslyConnectedMacs) { discovered, saved ->
            discovered.filter { d -> saved.none { it.isSameMacAs(d) } }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repository.discoveredMacs.value.filter { d -> repository.previouslyConnectedMacs.value.none { it.isSameMacAs(d) } }
        )

    private val _showTutorialDialog = MutableStateFlow(!repository.hasSeenOnboarding.value)
    val showTutorialDialog: StateFlow<Boolean> = _showTutorialDialog.asStateFlow()

    private val _selectedClip = MutableStateFlow<CompanionClipSummary?>(null)
    val selectedClip: StateFlow<CompanionClipSummary?> = _selectedClip.asStateFlow()

    private val _showAudioMixerSheet = MutableStateFlow(false)
    val showAudioMixerSheet: StateFlow<Boolean> = _showAudioMixerSheet.asStateFlow()

    private val _showCaptureSetupSheet = MutableStateFlow(false)
    val showCaptureSetupSheet: StateFlow<Boolean> = _showCaptureSetupSheet.asStateFlow()

    private val _manualHost = MutableStateFlow(repository.getLastConnectedHost() ?: "")
    val manualHost: StateFlow<String> = _manualHost.asStateFlow()

    private val _manualPort = MutableStateFlow(repository.getLastConnectedPort().toString())
    val manualPort: StateFlow<String> = _manualPort.asStateFlow()

    fun openTutorial() {
        _showTutorialDialog.value = true
    }

    fun dismissTutorial() {
        _showTutorialDialog.value = false
        repository.setHasSeenOnboarding(true)
    }

    fun openAudioMixer() {
        _showAudioMixerSheet.value = true
    }

    fun dismissAudioMixer() {
        _showAudioMixerSheet.value = false
    }

    fun openCaptureSetup() {
        _showCaptureSetupSheet.value = true
    }

    fun dismissCaptureSetup() {
        _showCaptureSetupSheet.value = false
    }

    init {
        repository.startDiscovery()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopDiscovery()
    }

    fun setManualHost(host: String) {
        _manualHost.value = host
    }

    fun setManualPort(port: String) {
        _manualPort.value = port
    }

    fun dismissError() {
        repository.clearError()
    }

    /** Called when the app comes on screen: refreshes the Mac's state, or reconnects to it. */
    fun onAppForegrounded() {
        repository.onAppForegrounded()
    }

    fun onAppBackgrounded() {
        repository.onAppBackgrounded()
    }

    fun refreshDiscovery() {
        repository.clearError()
        repository.restartDiscovery()
    }

    fun connect(host: String, portStr: String) {
        val port = portStr.trim().toIntOrNull()?.takeIf { it in 1..65535 } ?: CompanionWebSocketClient.DEFAULT_PORT
        val trimmedHost = host.trim()
        _manualHost.value = trimmedHost
        _manualPort.value = port.toString()
        repository.connect(trimmedHost, port)
    }

    /** Tries every control against a pretend Mac; leaving goes through [disconnect]. */
    fun startDemo() {
        repository.startDemo()
    }

    fun connectAdbUsb() {
        _manualHost.value = "127.0.0.1"
        _manualPort.value = "7375"
        repository.connect("127.0.0.1", 7375)
    }

    fun connectRememberedMac(item: PreviouslyConnectedItem) {
        val endpoint = item.onlineEndpoint ?: item.mac.networkEndpoint
        if (endpoint != null) {
            connect(endpoint.host, endpoint.port.toString())
        } else if (item.mac.usbPort != null) {
            connect("127.0.0.1", item.mac.usbPort.toString())
        }
    }

    fun forgetMac(mac: RememberedMac) {
        repository.forgetMac(mac)
    }

    fun disconnect() {
        _selectedClip.value = null
        _showAudioMixerSheet.value = false
        _showCaptureSetupSheet.value = false
        repository.disconnect()
    }

    fun saveReplay() {
        repository.saveReplay()
    }

    fun saveExtendedReplay() {
        repository.saveExtendedReplay()
    }

    fun toggleBuffer() {
        repository.toggleBuffer()
    }

    fun toggleSession() {
        repository.toggleSession()
    }

    fun toggleMicrophone() {
        repository.toggleMicrophone()
    }

    fun toggleSystemAudio() {
        repository.toggleSystemAudio()
    }

    fun selectClip(clip: CompanionClipSummary?) {
        _selectedClip.value = clip
        if (clip != null) {
            repository.requestThumbnail(clip.id)
        }
    }

    fun revealClip(clipId: String) {
        repository.revealClip(clipId)
    }

    fun deleteClip(clipId: String) {
        repository.deleteClip(clipId)
        _selectedClip.value = null
    }

    fun trimClip(clipId: String) {
        repository.trimClip(clipId)
    }

    fun copyClip(clipId: String) {
        repository.copyClip(clipId)
    }

    fun setMicrophoneVolume(volume: Double) {
        repository.setMicrophoneVolume(volume)
    }

    fun setSystemAudioVolume(volume: Double) {
        repository.setSystemAudioVolume(volume)
    }

    fun toggleAppMute(bundleId: String) {
        repository.toggleAppMute(bundleId)
    }

    fun unmuteAllApps() {
        repository.unmuteAllApps()
    }

    fun selectDisplay(displayId: String) {
        repository.selectDisplay(displayId)
    }

    fun applyProfile(profileId: String) {
        repository.applyProfile(profileId)
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        repository.setKeepScreenAwake(enabled)
    }
}

class CompanionViewModelFactory(
    private val repository: CompanionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompanionViewModel::class.java)) {
            return CompanionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
