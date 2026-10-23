package com.replaymac.companion.data.repository

import com.replaymac.companion.data.model.CompanionTheme
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.ui.theme.ThemePalette
import com.replaymac.companion.ui.theme.ThemeResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides which theme the app wears and keeps [ThemeStore] up to date:
 * the connected Mac's latest theme, the remembered Mac's stored one before
 * a Mac has said anything, or the demo's. The demo always shows its theme,
 * whatever "Match my Mac" says, and never writes to the store.
 */
class ThemeController(private val store: ThemeStore) {
    private var theme: CompanionTheme? = null
    private var isDemo = false
    /** The theme last written for a Mac, so repeated snapshots don't rewrite it. */
    private var lastSaved: Pair<String, CompanionTheme>? = null

    private val _matchMac = MutableStateFlow(store.matchMac)
    val matchMac: StateFlow<Boolean> = _matchMac.asStateFlow()

    private val _palette = MutableStateFlow(ThemePalette.Default)
    val palette: StateFlow<ThemePalette> = _palette.asStateFlow()

    /** Before a Mac speaks: [mac]'s stored theme, or today's look when there is none. */
    @Synchronized
    fun showRemembered(mac: RememberedMac?) {
        apply(mac?.let { store.load(ThemeStore.macKey(it)) }, demo = false)
    }

    /**
     * A snapshot from the Mac known as [macKey]. Its theme is worn and stored;
     * a snapshot without one (an older ReplayMac) shows today's look but keeps
     * what was stored.
     */
    @Synchronized
    fun onMacSnapshot(theme: CompanionTheme?, macKey: String?) {
        if (theme != null && macKey != null && lastSaved != (macKey to theme)) {
            store.save(theme, macKey)
            lastSaved = macKey to theme
        }
        apply(theme, demo = false)
    }

    @Synchronized
    fun onDemoSnapshot(theme: CompanionTheme?) {
        apply(theme, demo = true)
    }

    /** Forgetting a Mac forgets its theme too. */
    @Synchronized
    fun forget(mac: RememberedMac) {
        val key = ThemeStore.macKey(mac)
        store.clear(key)
        if (lastSaved?.first == key) lastSaved = null
    }

    @Synchronized
    fun setMatchMac(enabled: Boolean) {
        store.matchMac = enabled
        _matchMac.value = enabled
        publish()
    }

    private fun apply(theme: CompanionTheme?, demo: Boolean) {
        this.theme = theme
        isDemo = demo
        publish()
    }

    private fun publish() {
        _palette.value = ThemeResolver.resolve(theme, matchMac = isDemo || _matchMac.value)
    }
}
