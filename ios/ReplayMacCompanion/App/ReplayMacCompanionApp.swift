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
                        onClipSelected: { clip in
                            viewModel.selectClip(clip)
                        },
                        onOpenTutorial: {
                            viewModel.openTutorial()
                        }
                    )
                } else {
                    ConnectionScreen(
                        connectionState: viewModel.connectionState,
                        previouslyConnectedItems: viewModel.previouslyConnectedItems,
                        discoveredMacs: viewModel.filteredDiscoveredMacs,
                        isDiscovering: viewModel.isDiscovering,
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
                        onConnectWired: {
                            viewModel.connectWired()
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
            .preferredColorScheme(.dark)
            .sheet(item: $viewModel.selectedClip) { clip in
                ClipDetailsBottomSheet(
                    clip: clip,
                    thumbnailBase64: viewModel.thumbnails[clip.id],
                    onRevealInFinder: { clipId in
                        viewModel.revealClip(clipId)
                    },
                    onDeleteClip: { clipId in
                        viewModel.deleteClip(clipId)
                    },
                    onDismiss: {
                        viewModel.selectClip(nil)
                    }
                )
                .preferredColorScheme(.dark)
            }
        }
    }
}
