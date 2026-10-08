package com.replaymac.companion.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.replaymac.companion.data.model.DiscoveredMac
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NsdDiscoveryManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "NsdDiscoveryManager"
        const val SERVICE_TYPE = "_replaymac._tcp"
        private const val RESOLVE_TIMEOUT_MS = 5000L
        /** TXT record key carrying ReplayMac's stable server identity. */
        const val TXT_SERVER_ID = "id"
    }

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private val _discoveredMacs = MutableStateFlow<List<DiscoveredMac>>(emptyList())
    val discoveredMacs: StateFlow<List<DiscoveredMac>> = _discoveredMacs.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val discoveredMap = ConcurrentHashMap<String, DiscoveredMac>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isResolving = false
    private var isStopping = false
    private var isRestartPending = false
    private var resolveTimeoutJob: Job? = null
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()

    @Synchronized
    fun startDiscovery() {
        if (isStopping) {
            isRestartPending = true
            return
        }
        if (_isDiscovering.value || discoveryListener != null) return

        val manager = nsdManager ?: return

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started for $regType")
                _isDiscovering.value = true
                isStopping = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}, type: ${serviceInfo.serviceType}")
                queueResolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                discoveredMap.remove(serviceInfo.serviceName)
                updateState()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped for $serviceType")
                synchronized(this@NsdDiscoveryManager) {
                    _isDiscovering.value = false
                    isStopping = false
                    discoveryListener = null
                    if (isRestartPending) {
                        isRestartPending = false
                        startDiscovery()
                    }
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                synchronized(this@NsdDiscoveryManager) {
                    _isDiscovering.value = false
                    isStopping = false
                    discoveryListener = null
                    isRestartPending = false
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
                synchronized(this@NsdDiscoveryManager) {
                    _isDiscovering.value = false
                    isStopping = false
                    discoveryListener = null
                    if (isRestartPending) {
                        isRestartPending = false
                        startDiscovery()
                    }
                }
            }
        }

        discoveryListener = listener

        try {
            manager.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                listener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            _isDiscovering.value = false
            discoveryListener = null
        }
    }

    @Synchronized
    fun stopDiscovery() {
        if (!_isDiscovering.value && discoveryListener == null) return
        val manager = nsdManager ?: return
        val listener = discoveryListener ?: return

        if (isStopping) return
        isStopping = true

        clearResolveQueue()

        try {
            manager.stopServiceDiscovery(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop discovery", e)
            _isDiscovering.value = false
            isStopping = false
            discoveryListener = null
        }
    }

    @Synchronized
    fun restartDiscovery() {
        discoveredMap.clear()
        updateState()
        clearResolveQueue()

        if (_isDiscovering.value && discoveryListener != null) {
            isRestartPending = true
            stopDiscovery()
        } else {
            startDiscovery()
        }
    }

    @Synchronized
    private fun clearResolveQueue() {
        resolveTimeoutJob?.cancel()
        resolveTimeoutJob = null
        resolveQueue.clear()
        isResolving = false
    }

    @Synchronized
    private fun queueResolve(serviceInfo: NsdServiceInfo) {
        val name = serviceInfo.serviceName ?: return
        // Don't enqueue if already queued
        if (resolveQueue.any { it.serviceName == name }) return
        resolveQueue.addLast(serviceInfo)
        processNextResolve()
    }

    @Synchronized
    private fun processNextResolve() {
        if (isResolving || resolveQueue.isEmpty()) return
        val manager = nsdManager ?: return
        val next = resolveQueue.removeFirst()
        isResolving = true

        resolveTimeoutJob?.cancel()
        resolveTimeoutJob = scope.launch {
            delay(RESOLVE_TIMEOUT_MS)
            synchronized(this@NsdDiscoveryManager) {
                if (isResolving) {
                    Log.w(TAG, "Resolve timed out for ${next.serviceName}")
                    isResolving = false
                    processNextResolve()
                }
            }
        }

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                synchronized(this@NsdDiscoveryManager) {
                    resolveTimeoutJob?.cancel()
                    isResolving = false
                    processNextResolve()
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val rawHost = serviceInfo.host?.hostAddress ?: ""
                val host = if (rawHost.contains("%")) rawHost.substringBefore("%") else rawHost
                val port = serviceInfo.port
                val name = serviceInfo.serviceName ?: "ReplayMac"
                Log.d(TAG, "Resolved: $name at $host:$port")

                if (host.isNotEmpty() && port > 0) {
                    val mac = DiscoveredMac(
                        id = "$host:$port",
                        name = name,
                        host = host,
                        port = port,
                        serverId = serverIdOf(serviceInfo)
                    )
                    discoveredMap[name] = mac
                    updateState()
                }

                synchronized(this@NsdDiscoveryManager) {
                    resolveTimeoutJob?.cancel()
                    isResolving = false
                    processNextResolve()
                }
            }
        }

        try {
            manager.resolveService(next, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during resolveService", e)
            resolveTimeoutJob?.cancel()
            isResolving = false
            processNextResolve()
        }
    }

    private fun serverIdOf(serviceInfo: NsdServiceInfo): String? =
        serviceInfo.attributes[TXT_SERVER_ID]
            ?.toString(Charsets.UTF_8)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun updateState() {
        _discoveredMacs.update { discoveredMap.values.toList() }
    }

    fun addManualTarget(name: String, host: String, port: Int) {
        val mac = DiscoveredMac(
            id = "$host:$port",
            name = name,
            host = host,
            port = port
        )
        discoveredMap[name] = mac
        updateState()
    }
}
