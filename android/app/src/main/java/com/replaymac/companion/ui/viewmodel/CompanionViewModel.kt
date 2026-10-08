package com.replaymac.companion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.replaymac.companion.data.model.CompanionClipSummary
import com.replaymac.companion.data.model.CompanionStateSnapshot
import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.network.CompanionWebSocketClient
import com.replaymac.companion.data.repository.CompanionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _showTutorialDialog = MutableStateFlow(!repository.hasSeenOnboarding.value)
    val showTutorialDialog: StateFlow<Boolean> = _showTutorialDialog.asStateFlow()

    private val _selectedClip = MutableStateFlow<CompanionClipSummary?>(null)
    val selectedClip: StateFlow<CompanionClipSummary?> = _selectedClip.asStateFlow()

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

    /** Called when the app comes back on screen; state may have changed while it was away. */
    fun onAppForegrounded() {
        repository.resync()
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

    fun connectAdbUsb() {
        _manualHost.value = "127.0.0.1"
        _manualPort.value = "7375"
        repository.connect("127.0.0.1", 7375)
    }

    fun disconnect() {
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
