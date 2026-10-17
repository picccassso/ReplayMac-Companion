import SwiftUI
import UIKit

public struct ConnectionScreen: View {
    public var connectionState: ConnectionState
    public var previouslyConnectedItems: [PreviouslyConnectedItem]
    public var discoveredMacs: [DiscoveredMac]
    public var isDiscovering: Bool
    public var isLocalNetworkDenied: Bool
    public var manualHost: String
    public var manualPort: String
    public var onManualHostChange: (String) -> Void
    public var onManualPortChange: (String) -> Void
    public var onConnect: (String, String) -> Void
    public var onConnectRememberedMac: (PreviouslyConnectedItem) -> Void
    public var onForgetMac: (RememberedMac) -> Void
    public var onOpenSettings: () -> Void
    public var onRefreshDiscovery: () -> Void
    public var onCancelConnection: () -> Void
    public var onDismissError: () -> Void
    public var onOpenTutorial: () -> Void
    public var onTryDemo: () -> Void

    @State private var macToForget: RememberedMac? = nil

    public init(
        connectionState: ConnectionState,
        previouslyConnectedItems: [PreviouslyConnectedItem] = [],
        discoveredMacs: [DiscoveredMac],
        isDiscovering: Bool,
        isLocalNetworkDenied: Bool = false,
        manualHost: String,
        manualPort: String,
        onManualHostChange: @escaping (String) -> Void,
        onManualPortChange: @escaping (String) -> Void,
        onConnect: @escaping (String, String) -> Void,
        onConnectRememberedMac: @escaping (PreviouslyConnectedItem) -> Void = { _ in },
        onForgetMac: @escaping (RememberedMac) -> Void = { _ in },
        onOpenSettings: @escaping () -> Void = {},
        onRefreshDiscovery: @escaping () -> Void,
        onCancelConnection: @escaping () -> Void,
        onDismissError: @escaping () -> Void = {},
        onOpenTutorial: @escaping () -> Void = {},
        onTryDemo: @escaping () -> Void = {}
    ) {
        self.connectionState = connectionState
        self.previouslyConnectedItems = previouslyConnectedItems
        self.discoveredMacs = discoveredMacs
        self.isDiscovering = isDiscovering
        self.isLocalNetworkDenied = isLocalNetworkDenied
        self.manualHost = manualHost
        self.manualPort = manualPort
        self.onManualHostChange = onManualHostChange
        self.onManualPortChange = onManualPortChange
        self.onConnect = onConnect
        self.onConnectRememberedMac = onConnectRememberedMac
        self.onForgetMac = onForgetMac
        self.onOpenSettings = onOpenSettings
        self.onRefreshDiscovery = onRefreshDiscovery
        self.onCancelConnection = onCancelConnection
        self.onDismissError = onDismissError
        self.onOpenTutorial = onOpenTutorial
        self.onTryDemo = onTryDemo
    }

    public var body: some View {
        GlassBackground {
            switch connectionState {
            case .pendingApproval(let sName, _, _, let code):
                CenteredStatus {
                    StatusPanel(
                        title: "Waiting for approval",
                        message: code == nil
                            ? "Click Allow in ReplayCap on “\(sName)” to pair this phone."
                            : "Check that ReplayCap on “\(sName)” shows this code, then click Allow.",
                        onCancel: onCancelConnection
                    ) {
                        if let code = code {
                            PairingCodeView(code: code)
                        } else {
                            Image(systemName: "lock.fill")
                                .font(.system(size: 34))
                                .foregroundColor(AppColors.textSecondary)
                        }
                    }
                }

            case .connecting(let host, let port):
                CenteredStatus {
                    StatusPanel(
                        title: "Connecting",
                        message: "\(host):\(port)",
                        onCancel: onCancelConnection
                    ) {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: AppColors.textSecondary))
                            .scaleEffect(1.5)
                    }
                }

            case .reconnecting(let sName):
                CenteredStatus {
                    StatusPanel(
                        title: "Reconnecting",
                        message: "Looking for “\(sName)” on this network.",
                        onCancel: onCancelConnection
                    ) {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: AppColors.textSecondary))
                            .scaleEffect(1.5)
                    }
                }

            default:
                normalConnectionView
            }
        }
    }

    private var normalConnectionView: some View {
        GeometryReader { geo in
            let isLandscape = geo.size.width > geo.size.height

            ScrollView {
                VStack(spacing: 14) {
                    ConnectionHeader(
                        isDiscovering: isDiscovering,
                        onRefresh: onRefreshDiscovery,
                        onOpenTutorial: onOpenTutorial
                    )

                    if case .failed(let err, _, _, let wasConn, _) = connectionState {
                        ErrorBanner(
                            title: wasConn ? "Connection lost" : "Couldn’t connect",
                            error: err,
                            onDismiss: onDismissError
                        )
                    }

                    if isLandscape {
                        HStack(alignment: .top, spacing: 20) {
                            VStack(spacing: 14) {
                                if !previouslyConnectedItems.isEmpty {
                                    previouslyConnectedSection
                                }
                                networkSection
                            }
                            .frame(maxWidth: .infinity)

                            VStack(spacing: 14) {
                                manualSection
                            }
                            .frame(maxWidth: .infinity)
                        }
                    } else {
                        VStack(spacing: 14) {
                            if !previouslyConnectedItems.isEmpty {
                                previouslyConnectedSection
                            }
                            networkSection
                            manualSection
                        }
                    }

                    TryDemoButton(onClick: onTryDemo)
                        .padding(.top, 10)

                    AppInfoFooter()
                }
                .padding(.horizontal, isLandscape ? 24 : 16)
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .alert(
                "Forget “\(macToForget?.name ?? "this Mac")”?",
                isPresented: Binding(
                    get: { macToForget != nil },
                    set: { if !$0 { macToForget = nil } }
                )
            ) {
                Button("Cancel", role: .cancel) {
                    macToForget = nil
                }
                Button("Forget", role: .destructive) {
                    if let mac = macToForget {
                        onForgetMac(mac)
                        macToForget = nil
                    }
                }
            } message: {
                Text("You will need to allow this device again in ReplayCap if you reconnect.")
            }
        }
    }

    // MARK: - Sections

    private var previouslyConnectedSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Previously Connected")

            GlassCard(cornerRadius: AppRadii.large) {
                VStack(spacing: 0) {
                    ForEach(Array(previouslyConnectedItems.enumerated()), id: \.element.id) { index, item in
                        PreviouslyConnectedRow(
                            item: item,
                            onConnect: {
                                onConnectRememberedMac(item)
                            },
                            onForget: {
                                macToForget = item.mac
                            }
                        )

                        if index < previouslyConnectedItems.count - 1 {
                            RowSeparator(startIndent: 56)
                        }
                    }
                }
            }
        }
    }

    private var networkSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "On This Network") {
                if isDiscovering && !discoveredMacs.isEmpty {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: AppColors.textSecondary))
                        .scaleEffect(0.8)
                }
            }

            if isLocalNetworkDenied {
                LocalNetworkDeniedCard(onOpenSettings: onOpenSettings)
            }

            GlassCard(cornerRadius: AppRadii.large) {
                if discoveredMacs.isEmpty {
                    SearchingRow(isSearching: isDiscovering && !isLocalNetworkDenied)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(discoveredMacs.enumerated()), id: \.element.id) { index, mac in
                            DiscoveredMacRow(mac: mac, onConnect: {
                                onConnect(mac.host, String(mac.port))
                            })

                            if index < discoveredMacs.count - 1 {
                                RowSeparator(startIndent: 56)
                            }
                        }
                    }
                }
            }
        }
    }

    private var manualSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "Enter Manually")

            GlassCard(cornerRadius: AppRadii.large) {
                VStack(spacing: 14) {
                    HStack(spacing: 10) {
                        GlassTextField(
                            value: Binding(get: { manualHost }, set: onManualHostChange),
                            label: "IP address or host",
                            placeholder: "192.168.1.100",
                            keyboardType: .URL
                        )

                        GlassTextField(
                            value: Binding(get: { manualPort }, set: onManualPortChange),
                            label: "Port",
                            placeholder: "7375",
                            keyboardType: .numberPad,
                            onSubmit: {
                                if !manualHost.isEmpty {
                                    onConnect(manualHost, manualPort)
                                }
                            }
                        )
                        .frame(width: 90)
                    }

                    AccentButton(
                        text: "Connect",
                        enabled: !manualHost.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ) {
                        onConnect(manualHost, manualPort)
                    }
                    .frame(maxWidth: .infinity)
                }
                .padding(16)
            }
        }
    }
}

// MARK: - Header & Banner

private struct ConnectionHeader: View {
    let isDiscovering: Bool
    let onRefresh: () -> Void
    let onOpenTutorial: () -> Void

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Connect to your Mac")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(AppColors.textPrimary)

                Text("Turn on Companion Server in ReplayCap’s settings, then choose your Mac.")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textSecondary)
            }
            .padding(.leading, 4)

            Spacer()

            GlassButtonGroup {
                GroupIconButton(
                    systemImage: "questionmark.circle",
                    contentDescription: "Quick guide",
                    tint: AppColors.textSecondary,
                    selected: false,
                    onClick: onOpenTutorial
                )

                GroupIconButton(
                    systemImage: "arrow.clockwise",
                    contentDescription: "Search again",
                    tint: isDiscovering ? AppColors.accent : AppColors.textSecondary,
                    selected: false,
                    onClick: onRefresh
                )
            }
        }
    }
}

private struct PairingCodeView: View {
    let code: String

    var body: some View {
        Text(code.map(String.init).joined(separator: " "))
            .font(.system(size: 40, weight: .semibold, design: .monospaced))
            .foregroundColor(AppColors.textPrimary)
            .accessibilityLabel("Pairing code \(code.map(String.init).joined(separator: " "))")
    }
}

private struct LocalNetworkDeniedCard: View {
    let onOpenSettings: () -> Void

    var body: some View {
        GlassCard(cornerRadius: AppRadii.large) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "wifi.exclamationmark")
                        .font(.system(size: 22))
                        .foregroundColor(.orange)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Local Network access is off")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(AppColors.textPrimary)
                        Text("ReplayCap needs it to find and control your Mac. Turn on Local Network in Settings.")
                            .font(.system(size: 13))
                            .foregroundColor(AppColors.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                AccentButton(text: "Open Settings", onClick: onOpenSettings)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct ErrorBanner: View {
    let title: String
    let error: String
    let onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.circle.fill")
                .font(.system(size: 22))
                .foregroundColor(AppColors.danger)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)

                Text(error)
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            }

            Spacer()

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(AppColors.textSecondary)
                    .frame(width: 32, height: 32)
                    .background(Circle().fill(AppColors.surfaceGlass))
            }
            .buttonStyle(GlassPressableStyle())
        }
        .padding(14)
        .glass(cornerRadius: AppRadii.medium + 4, tint: AppColors.danger.opacity(0.14))
    }
}

private struct DiscoveredMacRow: View {
    let mac: DiscoveredMac
    let onConnect: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "laptopcomputer")
                .font(.system(size: 24))
                .foregroundColor(AppColors.textSecondary)

            VStack(alignment: .leading, spacing: 2) {
                Text(mac.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)

                Text(verbatim: "\(mac.host):\(mac.port)")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            }

            Spacer()

            AccentButton(text: "Connect", onClick: onConnect)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private struct PreviouslyConnectedRow: View {
    let item: PreviouslyConnectedItem
    let onConnect: () -> Void
    let onForget: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "laptopcomputer")
                .font(.system(size: 24))
                .foregroundColor(item.isOnline ? AppColors.accent : AppColors.textSecondary)

            VStack(alignment: .leading, spacing: 3) {
                Text(item.mac.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Circle()
                        .fill(item.isOnline ? AppColors.success : AppColors.textTertiary)
                        .frame(width: 7, height: 7)

                    if item.isOnline {
                        if let endpoint = item.onlineEndpoint {
                            Text(verbatim: "Available • \(endpoint.host):\(endpoint.port)")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        } else {
                            Text("Available on network")
                                .font(.system(size: 12))
                                .foregroundColor(AppColors.textSecondary)
                        }
                    } else {
                        Text("Offline")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.textTertiary)
                    }
                }
            }

            Spacer()

            Button(action: onForget) {
                Image(systemName: "trash")
                    .font(.system(size: 14))
                    .foregroundColor(AppColors.textTertiary)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(GlassPressableStyle())

            AccentButton(
                text: "Connect",
                enabled: item.isOnline,
                onClick: onConnect
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

private struct SearchingRow: View {
    let isSearching: Bool

    var body: some View {
        HStack(spacing: 14) {
            if isSearching {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: AppColors.textSecondary))
                    .scaleEffect(1.1)
            } else {
                Image(systemName: "wifi.slash")
                    .font(.system(size: 22))
                    .foregroundColor(AppColors.textSecondary)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(isSearching ? "Searching…" : "No Macs found")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(AppColors.textPrimary)

                Text("ReplayCap needs to be open, and on the same Wi-Fi as this phone.")
                    .font(.system(size: 13))
                    .foregroundColor(AppColors.textSecondary)
            }
            Spacer()
        }
        .padding(16)
    }
}

// MARK: - Centered Status

private struct CenteredStatus<Content: View>: View {
    let content: () -> Content

    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    var body: some View {
        VStack {
            Spacer()
            content()
                .frame(maxWidth: 420)
                .padding(.horizontal, 24)
            Spacer()
        }
    }
}

private struct StatusPanel<Badge: View>: View {
    let title: String
    let message: String
    let onCancel: () -> Void
    @ViewBuilder let badge: () -> Badge

    var body: some View {
        GlassCard(cornerRadius: AppRadii.large) {
            VStack(spacing: 16) {
                badge()
                    .frame(minHeight: 36)

                VStack(spacing: 4) {
                    Text(title)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(AppColors.textPrimary)
                        .multilineTextAlignment(.center)

                    Text(message)
                        .font(.system(size: 14))
                        .foregroundColor(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                }

                GlassButton(text: "Cancel", onClick: onCancel)
                    .frame(maxWidth: .infinity)
            }
            .padding(28)
        }
    }
}
