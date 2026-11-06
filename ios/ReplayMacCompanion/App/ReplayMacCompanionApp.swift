import SwiftUI

@main
struct ReplayCapCompanionApp: App {
    @StateObject private var viewModel: CompanionViewModel

    init() {
        let repo = CompanionRepository()
        _viewModel = StateObject(wrappedValue: CompanionViewModel(repository: repo))
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                if viewModel.connectionState.isConnected {
                    HomeScreen(
                        connectionState: viewModel.connectionState,
                        snapshot: viewModel.snapshot,
                        thumbnails: viewModel.thumbnails,
                        isDemo: viewModel.isDemo,
                        keepScreenAwake: viewModel.keepScreenAwake,
                        onToggleKeepScreenAwake: {
                            viewModel.setKeepScreenAwake(!viewModel.keepScreenAwake)
                        },
                        onDisconnect: {
                            viewModel.disconnect()
                        },
                        onSaveReplay: {
                            viewModel.saveReplay()
                        },
                        onSaveExtendedReplay: {
                            viewModel.saveExtendedReplay()
                        },
                        onToggleBuffer: {
                            viewModel.toggleBuffer()
                        },
                        onToggleSession: {
                            viewModel.toggleSession()
                        },
                        onToggleMicrophone: {
                            viewModel.toggleMicrophone()
                        },
                        onToggleSystemAudio: {
                            viewModel.toggleSystemAudio()
                        },
                        onToggleCamera: {
                            viewModel.toggleCamera()
                        },
                        onOpenAudioMixer: {
                            viewModel.openAudioMixer()
                        },
                        onOpenCaptureSetup: {
                            viewModel.openCaptureSetup()
                        },
                        onClipSelected: { clip in
                            viewModel.selectClip(clip)
                        },
                        onOpenTutorial: {
                            viewModel.openTutorial()
                        },
                        onOpenAppearance: {
                            viewModel.openAppearance()
                        }
                    )
                } else {
                    ConnectionScreen(
                        connectionState: viewModel.connectionState,
                        previouslyConnectedItems: viewModel.previouslyConnectedItems,
                        discoveredMacs: viewModel.filteredDiscoveredMacs,
                        isDiscovering: viewModel.isDiscovering,
                        isLocalNetworkDenied: viewModel.isLocalNetworkDenied,
                        manualHost: viewModel.manualHost,
                        manualPort: viewModel.manualPort,
                        onManualHostChange: { host in
                            viewModel.setManualHost(host)
                        },
                        onManualPortChange: { port in
                            viewModel.setManualPort(port)
                        },
                        onConnect: { host, port in
                            viewModel.connect(host: host, portStr: port)
                        },
                        onConnectRememberedMac: { item in
                            viewModel.connectRememberedMac(item)
                        },
                        onForgetMac: { mac in
                            viewModel.forgetMac(mac)
                        },
                        onOpenSettings: {
                            viewModel.openAppSettings()
                        },
                        onRefreshDiscovery: {
                            viewModel.refreshDiscovery()
                        },
                        onCancelConnection: {
                            viewModel.disconnect()
                        },
                        onDismissError: {
                            viewModel.dismissError()
                        },
                        onOpenTutorial: {
                            viewModel.openTutorial()
                        },
                        onTryDemo: {
                            viewModel.startDemo()
                        }
                    )
                }

                if viewModel.showTutorialDialog {
                    OnboardingDialog {
                        viewModel.dismissTutorial()
                    }
                    .transition(.opacity)
                }
            }
            .themed(viewModel.palette)
            .sheet(item: $viewModel.selectedClip) { clip in
                ClipDetailsBottomSheet(
                    clip: clip,
                    thumbnailBase64: viewModel.thumbnails[clip.id],
                    onRevealInFinder: { clipId in
                        viewModel.revealClip(clipId)
                    },
                    onTrimClip: { clipId in
                        viewModel.trimClip(clipId)
                    },
                    onCopyClip: { clipId in
                        viewModel.copyClip(clipId)
                    },
                    onDeleteClip: { clipId in
                        viewModel.deleteClip(clipId)
                    },
                    onDismiss: {
                        viewModel.selectClip(nil)
                    }
                )
                .themed(viewModel.palette)
            }
            .sheet(isPresented: $viewModel.showAudioMixerSheet) {
                AudioMixerBottomSheet(
                    snapshot: viewModel.snapshot,
                    onToggleMicrophone: {
                        viewModel.toggleMicrophone()
                    },
                    onToggleSystemAudio: {
                        viewModel.toggleSystemAudio()
                    },
                    onSetMicrophoneVolume: { volume in
                        viewModel.setMicrophoneVolume(volume)
                    },
                    onSetSystemAudioVolume: { volume in
                        viewModel.setSystemAudioVolume(volume)
                    },
                    onToggleAppMute: { bundleId in
                        viewModel.toggleAppMute(bundleId)
                    },
                    onUnmuteAllApps: {
                        viewModel.unmuteAllApps()
                    },
                    onDismiss: {
                        viewModel.dismissAudioMixer()
                    }
                )
                .themed(viewModel.palette)
            }
            .sheet(isPresented: $viewModel.showCaptureSetupSheet) {
                CaptureSetupBottomSheet(
                    snapshot: viewModel.snapshot,
                    onSelectDisplay: { displayId in
                        viewModel.selectDisplay(displayId)
                    },
                    onApplyProfile: { profileId in
                        viewModel.applyProfile(profileId)
                    },
                    onDismiss: {
                        viewModel.dismissCaptureSetup()
                    }
                )
                .themed(viewModel.palette)
            }
            .sheet(isPresented: $viewModel.showAppearanceSheet) {
                AppearanceBottomSheet(
                    macTheme: viewModel.macTheme,
                    matchMac: viewModel.matchMacTheme,
                    isDemo: viewModel.isDemo,
                    onSetMatchMac: { match in
                        viewModel.setMatchMacTheme(match)
                    },
                    onSelectAtmosphere: { atmosphereId in
                        viewModel.selectDemoAtmosphere(atmosphereId)
                    },
                    onDismiss: {
                        viewModel.dismissAppearance()
                    }
                )
                .themed(viewModel.palette)
            }
        }
    }
}
