package com.replaymac.companion.data.network

import com.replaymac.companion.data.model.CompanionServerEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * What the repository drives: commands to a Mac and the events it sends back.
 * The socket client talks to a real ReplayMac; the demo session plays one with
 * sample data. Every command returns whether it went out, like a socket send.
 */
interface CompanionLink {
    val events: SharedFlow<CompanionServerEvent>

    /** Asks for a fresh snapshot of the Mac's state. */
    fun resync(): Boolean
    fun sendAction(actionName: String): Boolean
    fun getThumbnail(clipId: String): Boolean
    fun revealClip(clipId: String): Boolean
    fun deleteClip(clipId: String): Boolean
    fun trimClip(clipId: String): Boolean
    fun copyClip(clipId: String): Boolean
    /** [target] is one of [com.replaymac.companion.data.model.CompanionVolumeTargets]. */
    fun setVolume(target: String, volume: Double): Boolean
    fun toggleAppMute(bundleId: String): Boolean
    fun unmuteAllApps(): Boolean
    fun selectDisplay(displayId: String): Boolean
    fun applyProfile(profileId: String): Boolean
}
