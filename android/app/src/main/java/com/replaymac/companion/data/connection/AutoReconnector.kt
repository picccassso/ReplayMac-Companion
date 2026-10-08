package com.replaymac.companion.data.connection

import com.replaymac.companion.data.model.ConnectionState
import com.replaymac.companion.data.model.DiscoveredMac
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Finds the remembered Mac again and connects to it. Tries every address it
 * might be at, and jumps straight to a new one as soon as discovery sees the
 * Mac somewhere else, e.g. after the phone moves to another network.
 */
class AutoReconnector(
    private val scope: CoroutineScope,
    private val connectionState: StateFlow<ConnectionState>,
    private val discoveredMacs: StateFlow<List<DiscoveredMac>>,
    private val connect: (MacEndpoint) -> Unit,
    private val onPairingDenied: () -> Unit,
    private val attemptTimeoutMs: Long = ATTEMPT_TIMEOUT_MS
) {
    companion object {
        /** Covers the socket's 5 s connect timeout plus the hello round trip. */
        const val ATTEMPT_TIMEOUT_MS = 8_000L
    }

    private sealed interface Outcome {
        /** Connected, or the Mac is asking the user to approve this phone. */
        data object Settled : Outcome
        data object Denied : Outcome
        data object Failed : Outcome
        data class FoundElsewhere(val endpoint: MacEndpoint) : Outcome
    }

    private val _target = MutableStateFlow<RememberedMac?>(null)
    /** The Mac being looked for, or null when idle. */
    val target: StateFlow<RememberedMac?> = _target.asStateFlow()

    private var job: Job? = null

    @Synchronized
    fun start(mac: RememberedMac) {
        if (job?.isActive == true) return
        _target.value = mac
        job = scope.launch {
            try {
                run(mac)
            } finally {
                // Only clear our own run; a stop() then start() may already own the target.
                if (_target.value === mac) _target.value = null
            }
        }
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        _target.value = null
    }

    private suspend fun run(mac: RememberedMac) {
        var round = 0
        while (currentCoroutineContext().isActive) {
            val tried = mutableSetOf<MacEndpoint>()
            var next = untried(mac, tried)
            while (next != null) {
                tried += next
                connect(next)
                when (val outcome = awaitOutcome(mac, next, tried)) {
                    Outcome.Settled -> return
                    Outcome.Denied -> {
                        onPairingDenied()
                        return
                    }
                    is Outcome.FoundElsewhere -> next = outcome.endpoint
                    Outcome.Failed -> next = untried(mac, tried)
                }
            }
            // Every known address failed. Wait, but wake as soon as discovery
            // sees the Mac at an address this round hasn't tried.
            withTimeoutOrNull(ReconnectPlan.backoffMillis(round++)) {
                discoveredMacs.first { untried(mac, tried, it) != null }
            }
        }
    }

    private fun untried(
        mac: RememberedMac,
        tried: Set<MacEndpoint>,
        discovered: List<DiscoveredMac> = discoveredMacs.value
    ): MacEndpoint? = ReconnectPlan.candidates(mac, discovered).firstOrNull { it !in tried }

    private suspend fun awaitOutcome(mac: RememberedMac, endpoint: MacEndpoint, tried: Set<MacEndpoint>): Outcome {
        val settled: Flow<Outcome> = connectionState
            .filter { it !is ConnectionState.Connecting }
            .map { state ->
                when {
                    state is ConnectionState.Connected || state is ConnectionState.PendingApproval -> Outcome.Settled
                    state is ConnectionState.Failed && state.pairingDenied -> Outcome.Denied
                    else -> Outcome.Failed
                }
            }
        // While a dead address times out, discovery may already know the new one.
        val seenElsewhere: Flow<Outcome> = discoveredMacs.mapNotNull { discovered ->
            discovered
                .filter { ReconnectPlan.isSameMac(it, mac) }
                .map { MacEndpoint(it.host, it.port) }
                .firstOrNull { it != endpoint && it !in tried }
                ?.let { Outcome.FoundElsewhere(it) }
        }
        return withTimeoutOrNull(attemptTimeoutMs) { merge(settled, seenElsewhere).first() } ?: Outcome.Failed
    }
}
