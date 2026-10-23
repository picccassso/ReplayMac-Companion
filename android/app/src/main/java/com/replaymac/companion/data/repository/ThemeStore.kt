package com.replaymac.companion.data.repository

import android.content.SharedPreferences
import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.RememberedMac
import kotlinx.serialization.json.Json

/**
 * Remembers each Mac's last theme, so the app can wear it on a cold launch
 * and on the Connect screen before the Mac says anything, and whether the
 * phone follows the Mac's look at all.
 */
class ThemeStore(private val prefs: SharedPreferences) {
    companion object {
        const val KEY_MATCH_MAC = "theme.matchMac"
        const val KEY_THEME_PREFIX = "theme.byMac."

        /** A Mac is known by its server id, or by its name when an older ReplayMac sends no id. */
        fun macKey(serverId: String?, name: String): String = serverId ?: name

        fun macKey(mac: RememberedMac): String = macKey(mac.serverId, mac.name)

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    /** "Match my Mac": on until the user turns it off. */
    var matchMac: Boolean
        get() = prefs.getBoolean(KEY_MATCH_MAC, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MATCH_MAC, value).apply()
        }

    fun save(theme: CompanionTheme, macKey: String) {
        prefs.edit().putString(KEY_THEME_PREFIX + macKey, json.encodeToString(CompanionTheme.serializer(), theme)).apply()
    }

    /** The Mac's last theme; null if it never sent one or what was stored can't be read. */
    fun load(macKey: String): CompanionTheme? {
        val stored = prefs.getString(KEY_THEME_PREFIX + macKey, null) ?: return null
        return try {
            json.decodeFromString(CompanionTheme.serializer(), stored)
        } catch (_: Exception) {
            null
        }
    }

    fun clear(macKey: String) {
        prefs.edit().remove(KEY_THEME_PREFIX + macKey).apply()
    }
}
