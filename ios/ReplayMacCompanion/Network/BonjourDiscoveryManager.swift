import Foundation
import Network
import Combine

public final class BonjourDiscoveryManager: NSObject, @unchecked Sendable, NetServiceDelegate {
    public static let serviceType = "_replaymac._tcp"
    public static let txtServerId = "id"

    private let discoveredSubject = CurrentValueSubject<[DiscoveredMac], Never>([])
    public var discoveredMacs: AnyPublisher<[DiscoveredMac], Never> {
        discoveredSubject.eraseToAnyPublisher()
    }
    public var currentDiscoveredMacs: [DiscoveredMac] {
        discoveredSubject.value
    }

    private let isDiscoveringSubject = CurrentValueSubject<Bool, Never>(false)
    public var isDiscovering: AnyPublisher<Bool, Never> {
        isDiscoveringSubject.eraseToAnyPublisher()
    }
    public var currentIsDiscovering: Bool {
        isDiscoveringSubject.value
    }

    private let lock = NSLock()
    private var browser: NWBrowser?
    private var discoveredMap: [String: DiscoveredMac] = [:]
    private var serverIdMap: [String: String] = [:]
    private var activeResolvers: [String: NetService] = [:]

    public override init() {
        super.init()
    }

    deinit {
        stopDiscovery()
    }

    public func startDiscovery() {
        lock.lock()
        guard browser == nil else {
            lock.unlock()
            return
        }

        let descriptor = NWBrowser.Descriptor.bonjour(type: Self.serviceType, domain: nil)
        let parameters = NWParameters()
        let nwBrowser = NWBrowser(for: descriptor, using: parameters)
        browser = nwBrowser
        isDiscoveringSubject.send(true)
        lock.unlock()

        nwBrowser.stateUpdateHandler = { [weak self] state in
            guard let self = self else { return }
            switch state {
            case .ready:
                self.isDiscoveringSubject.send(true)
            case .failed, .cancelled:
                self.lock.lock()
                self.isDiscoveringSubject.send(false)
                self.lock.unlock()
            default:
                break
            }
        }

        nwBrowser.browseResultsChangedHandler = { [weak self] results, changes in
            guard let self = self else { return }
            self.handleBrowseResults(results: results, changes: changes)
        }

        nwBrowser.start(queue: .main)
    }

    public func stopDiscovery() {
        lock.lock()
        guard let b = browser else {
            lock.unlock()
            return
        }
        browser = nil
        for (_, service) in activeResolvers {
            service.stop()
        }
        activeResolvers.removeAll()
        isDiscoveringSubject.send(false)
        lock.unlock()

        b.cancel()
    }

    public func restartDiscovery() {
        lock.lock()
        discoveredMap.removeAll()
        serverIdMap.removeAll()
        for (_, service) in activeResolvers {
            service.stop()
        }
        activeResolvers.removeAll()
        let list = Array(discoveredMap.values)
        lock.unlock()

        discoveredSubject.send(list)

        stopDiscovery()
        startDiscovery()
    }

    public func addManualTarget(name: String, host: String, port: Int) {
        lock.lock()
        let mac = DiscoveredMac(
            id: "\(host):\(port)",
            name: name,
            host: host,
            port: port
        )
        discoveredMap[name] = mac
        let list = Array(discoveredMap.values)
        lock.unlock()

        discoveredSubject.send(list)
    }

    private func handleBrowseResults(results: Set<NWBrowser.Result>, changes: Set<NWBrowser.Result.Change>) {
        var shouldNotify = false

        lock.lock()
        for result in results {
            if case let .service(name: name, type: type, domain: domain, interface: _) = result.endpoint {
                // Parse serverId from TXT record
                var serverId: String? = nil
                if case let .bonjour(txt) = result.metadata {
                    if let rawId = txt[Self.txtServerId]?.trimmingCharacters(in: .whitespacesAndNewlines), !rawId.isEmpty {
                        serverId = rawId
                    }
                }
                if let serverId = serverId {
                    serverIdMap[name] = serverId
                }

                // If already resolved, update serverId if newly available
                if let existing = discoveredMap[name] {
                    if existing.serverId == nil, let serverId = serverIdMap[name] {
                        discoveredMap[name] = DiscoveredMac(
                            id: existing.id,
                            name: existing.name,
                            host: existing.host,
                            port: existing.port,
                            serverId: serverId,
                            lastSeenTimestamp: Date()
                        )
                        shouldNotify = true
                    }
                } else if activeResolvers[name] == nil {
                    // Resolve service address via NetService
                    let netService = NetService(domain: domain, type: type, name: name)
                    netService.delegate = self
                    activeResolvers[name] = netService
                    netService.resolve(withTimeout: 5.0)
                }
            }
        }

        // Remove any services that were removed
        for change in changes {
            if case let .removed(result) = change {
                if case let .service(name: name, _, _, _) = result.endpoint {
                    discoveredMap.removeValue(forKey: name)
                    serverIdMap.removeValue(forKey: name)
                    activeResolvers[name]?.stop()
                    activeResolvers.removeValue(forKey: name)
                    shouldNotify = true
                }
            }
        }

        let list = shouldNotify ? Array(discoveredMap.values) : nil
        lock.unlock()

        if let list = list {
            discoveredSubject.send(list)
        }
    }

    // MARK: - NetServiceDelegate

    public func netServiceDidResolveAddress(_ sender: NetService) {
        lock.lock()
        let name = sender.name
        activeResolvers.removeValue(forKey: name)

        let port = Int(sender.port)
        guard port > 0, let addresses = sender.addresses, !addresses.isEmpty else {
            lock.unlock()
            return
        }

        var resolvedHost: String? = nil

        // Prefer IPv4 address
        for data in addresses {
            if let ip = parseIP(from: data, family: AF_INET) {
                resolvedHost = ip
                break
            }
        }

        // Fallback to IPv6 address
        if resolvedHost == nil {
            for data in addresses {
                if let ip = parseIP(from: data, family: AF_INET6) {
                    resolvedHost = ip
                    break
                }
            }
        }

        // Fallback to hostName
        if resolvedHost == nil, let hostName = sender.hostName {
            var clean = hostName.trimmingCharacters(in: .whitespacesAndNewlines)
            if clean.hasSuffix(".") {
                clean = String(clean.dropLast())
            }
            if !clean.isEmpty {
                resolvedHost = clean
            }
        }

        guard let host = resolvedHost else {
            lock.unlock()
            return
        }

        var serverId = serverIdMap[name]
        if serverId == nil, let txtData = sender.txtRecordData() {
            let dict = NetService.dictionary(fromTXTRecord: txtData)
            if let data = dict[Self.txtServerId],
               let idStr = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
               !idStr.isEmpty {
                serverId = idStr
                serverIdMap[name] = idStr
            }
        }

        let mac = DiscoveredMac(
            id: "\(host):\(port)",
            name: name,
            host: host,
            port: port,
            serverId: serverId,
            lastSeenTimestamp: Date()
        )
        discoveredMap[name] = mac
        let list = Array(discoveredMap.values)
        lock.unlock()

        discoveredSubject.send(list)
    }

    public func netService(_ sender: NetService, didNotResolve errorDict: [String : NSNumber]) {
        lock.lock()
        activeResolvers.removeValue(forKey: sender.name)
        lock.unlock()
    }

    private func parseIP(from data: Data, family: Int32) -> String? {
        data.withUnsafeBytes { rawBufferPointer -> String? in
            guard let sockaddrPtr = rawBufferPointer.baseAddress?.assumingMemoryBound(to: sockaddr.self) else { return nil }
            if sockaddrPtr.pointee.sa_family == sa_family_t(family) {
                if family == AF_INET {
                    let sockaddrInPtr = rawBufferPointer.baseAddress!.assumingMemoryBound(to: sockaddr_in.self)
                    var addr = sockaddrInPtr.pointee.sin_addr
                    var buffer = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
                    if inet_ntop(AF_INET, &addr, &buffer, socklen_t(INET_ADDRSTRLEN)) != nil {
                        return String(cString: buffer)
                    }
                } else if family == AF_INET6 {
                    let sockaddrIn6Ptr = rawBufferPointer.baseAddress!.assumingMemoryBound(to: sockaddr_in6.self)
                    var addr = sockaddrIn6Ptr.pointee.sin6_addr
                    var buffer = [CChar](repeating: 0, count: Int(INET6_ADDRSTRLEN))
                    if inet_ntop(AF_INET6, &addr, &buffer, socklen_t(INET6_ADDRSTRLEN)) != nil {
                        return String(cString: buffer)
                    }
                }
            }
            return nil
        }
    }
}
