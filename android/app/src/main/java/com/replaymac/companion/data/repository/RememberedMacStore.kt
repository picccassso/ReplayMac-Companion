package com.replaymac.companion.data.repository

import android.content.SharedPreferences
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac

/** Persists the last paired Mac so the app can find it again on its next launch. */
class RememberedMacStore(private val prefs: SharedPreferences) {
    private companion object {
        const val KEY_SERVER_ID = "remembered_server_id"
        const val KEY_NAME = "remembered_name"
        const val KEY_NETWORK_HOST = "remembered_network_host"
        const val KEY_NETWORK_PORT = "remembered_network_port"
        const val KEY_USB_PORT = "remembered_usb_port"
        const val KEY_LAST_USED_USB = "remembered_last_used_usb"
    }

    fun load(): RememberedMac? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val networkHost = prefs.getString(KEY_NETWORK_HOST, null)
        return RememberedMac(
            serverId = prefs.getString(KEY_SERVER_ID, null),
            name = name,
            networkEndpoint = networkHost?.let { MacEndpoint(it, prefs.getInt(KEY_NETWORK_PORT, 0)) },
            usbPort = prefs.getInt(KEY_USB_PORT, 0).takeIf { it > 0 },
            lastUsedUsb = prefs.getBoolean(KEY_LAST_USED_USB, false)
        )
    }

    fun save(mac: RememberedMac) {
        prefs.edit()
            .putString(KEY_SERVER_ID, mac.serverId)
            .putString(KEY_NAME, mac.name)
            .putString(KEY_NETWORK_HOST, mac.networkEndpoint?.host)
            .putInt(KEY_NETWORK_PORT, mac.networkEndpoint?.port ?: 0)
            .putInt(KEY_USB_PORT, mac.usbPort ?: 0)
            .putBoolean(KEY_LAST_USED_USB, mac.lastUsedUsb)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SERVER_ID)
            .remove(KEY_NAME)
            .remove(KEY_NETWORK_HOST)
            .remove(KEY_NETWORK_PORT)
            .remove(KEY_USB_PORT)
            .remove(KEY_LAST_USED_USB)
            .apply()
    }
}
