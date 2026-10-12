package com.replaymac.companion.data.repository

import android.annotation.SuppressLint
import android.content.SharedPreferences

/**
 * The secret each Mac handed this phone when the user clicked Allow, keyed by
 * the Mac's serverId. Kept in its own prefs file, which is excluded from
 * backups and device transfer (see res/xml/data_extraction_rules.xml).
 */
class PairingSecretStore(private val prefs: SharedPreferences) {
    companion object {
        const val PREFS_NAME = "replaymac_companion_secrets"
    }

    fun secret(serverId: String): String? = prefs.getString(serverId, null)

    /** Written synchronously: the Mac only sends a secret once, so it must not be lost. */
    @SuppressLint("ApplySharedPref")
    fun save(serverId: String, secret: String) {
        prefs.edit().putString(serverId, secret).commit()
    }

    fun remove(serverId: String) {
        prefs.edit().remove(serverId).apply()
    }
}
