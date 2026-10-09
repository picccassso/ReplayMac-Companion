package com.replaymac.companion.data.repository

import android.content.SharedPreferences
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists paired Macs so the app can find them again, maintaining an ordered history
 * with the most recently used Mac first.
 */
class RememberedMacStore(private val prefs: SharedPreferences) {
    companion object {
        const val KEY_SERVER_ID = "remembered_server_id"
        const val KEY_NAME = "remembered_name"
        const val KEY_NETWORK_HOST = "remembered_network_host"
        const val KEY_NETWORK_PORT = "remembered_network_port"
        const val KEY_USB_PORT = "remembered_usb_port"
        const val KEY_LAST_USED_USB = "remembered_last_used_usb"
        const val KEY_MACS_LIST = "remembered_macs_list"
    }

    /**
     * Loads all remembered Macs in order (most recently used first).
     * Automatically migrates from the legacy single-Mac preferences if needed.
     */
    fun loadAll(): List<RememberedMac> {
        val jsonString = prefs.getString(KEY_MACS_LIST, null)
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<RememberedMac>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(deserializeMac(obj))
                }
                return list
            } catch (_: Exception) {
                // If corrupted, fall back to legacy or empty
            }
        }

        // Migrate legacy single-device format if present
        val legacy = loadLegacy()
        if (legacy != null) {
            val list = listOf(legacy)
            persistList(list)
            return list
        }

        return emptyList()
    }

    /** The most recently connected Mac, or null if none saved. */
    fun loadLatest(): RememberedMac? = loadAll().firstOrNull()

    /** Backward-compatible load for auto-reconnect, returning the latest Mac. */
    fun load(): RememberedMac? = loadLatest()

    fun save(mac: RememberedMac) {
        val macs = loadAll().toMutableList()
        // Deduplicate any existing entry for this Mac
        macs.removeAll { it.isSameMacAs(mac.serverId, mac.name) }
        // Insert as most recently used at top
        macs.add(0, mac)
        persistList(macs)
        syncLegacy(mac)
    }

    fun remove(serverId: String?, name: String) {
        val macs = loadAll().toMutableList()
        macs.removeAll { it.isSameMacAs(serverId, name) }
        persistList(macs)
        val first = macs.firstOrNull()
        if (first != null) {
            syncLegacy(first)
        } else {
            clearLegacy()
        }
    }

    fun remove(mac: RememberedMac) {
        remove(mac.serverId, mac.name)
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_MACS_LIST)
            .apply()
        clearLegacy()
    }

    // MARK: - Private Helpers

    private fun persistList(list: List<RememberedMac>) {
        val array = JSONArray()
        for (mac in list) {
            array.put(serializeMac(mac))
        }
        prefs.edit().putString(KEY_MACS_LIST, array.toString()).apply()
    }

    private fun serializeMac(mac: RememberedMac): JSONObject {
        return JSONObject().apply {
            if (mac.serverId != null) {
                put("serverId", mac.serverId)
            }
            put("name", mac.name)
            mac.networkEndpoint?.let { ep ->
                val epObj = JSONObject().apply {
                    put("host", ep.host)
                    put("port", ep.port)
                }
                put("networkEndpoint", epObj)
                put("networkHost", ep.host)
                put("networkPort", ep.port)
            }
            mac.usbPort?.let { put("usbPort", it) }
            put("lastUsedUsb", mac.lastUsedUsb)
        }
    }

    private fun deserializeMac(obj: JSONObject): RememberedMac {
        val serverId = if (obj.has("serverId") && !obj.isNull("serverId")) obj.getString("serverId") else null
        val name = obj.getString("name")
        val networkEndpoint = if (obj.has("networkEndpoint") && !obj.isNull("networkEndpoint")) {
            val epObj = obj.getJSONObject("networkEndpoint")
            MacEndpoint(epObj.getString("host"), epObj.getInt("port"))
        } else if (obj.has("networkHost") && !obj.isNull("networkHost")) {
            val host = obj.getString("networkHost")
            val port = obj.optInt("networkPort", 0)
            if (port > 0) MacEndpoint(host, port) else null
        } else null
        val usbPort = if (obj.has("usbPort") && !obj.isNull("usbPort")) obj.optInt("usbPort").takeIf { it > 0 } else null
        val lastUsedUsb = obj.optBoolean("lastUsedUsb", false)

        return RememberedMac(
            serverId = serverId,
            name = name,
            networkEndpoint = networkEndpoint,
            usbPort = usbPort,
            lastUsedUsb = lastUsedUsb
        )
    }

    private fun loadLegacy(): RememberedMac? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val networkHost = prefs.getString(KEY_NETWORK_HOST, null)
        val networkPort = prefs.getInt(KEY_NETWORK_PORT, 0)
        val networkEndpoint = if (!networkHost.isNullOrEmpty() && networkPort > 0) {
            MacEndpoint(networkHost, networkPort)
        } else null

        val usbPort = prefs.getInt(KEY_USB_PORT, 0).takeIf { it > 0 }
        val lastUsedUsb = prefs.getBoolean(KEY_LAST_USED_USB, false)

        return RememberedMac(
            serverId = prefs.getString(KEY_SERVER_ID, null),
            name = name,
            networkEndpoint = networkEndpoint,
            usbPort = usbPort,
            lastUsedUsb = lastUsedUsb
        )
    }

    private fun syncLegacy(mac: RememberedMac) {
        prefs.edit()
            .putString(KEY_SERVER_ID, mac.serverId)
            .putString(KEY_NAME, mac.name)
            .putString(KEY_NETWORK_HOST, mac.networkEndpoint?.host)
            .putInt(KEY_NETWORK_PORT, mac.networkEndpoint?.port ?: 0)
            .putInt(KEY_USB_PORT, mac.usbPort ?: 0)
            .putBoolean(KEY_LAST_USED_USB, mac.lastUsedUsb)
            .apply()
    }

    private fun clearLegacy() {
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
