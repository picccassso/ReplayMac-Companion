package com.replaymac.companion

import android.content.SharedPreferences
import com.replaymac.companion.data.model.MacEndpoint
import com.replaymac.companion.data.model.RememberedMac
import com.replaymac.companion.data.repository.RememberedMacStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RememberedMacStoreTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: RememberedMacStore

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = RememberedMacStore(prefs)
    }

    @Test
    fun testEmptyStoreLoadsEmptyListAndNullLatest() {
        assertTrue(store.loadAll().isEmpty())
        assertNull(store.loadLatest())
        assertNull(store.load())
    }

    @Test
    fun testSaveAndLoadSingleMac() {
        val mac = RememberedMac(
            serverId = "server-1",
            name = "Alex’s MacBook Pro",
            networkEndpoint = MacEndpoint("192.168.1.10", 7375),
            usbPort = 7375,
            lastUsedUsb = false
        )
        store.save(mac)

        val all = store.loadAll()
        assertEquals(1, all.size)
        assertEquals(mac, all[0])
        assertEquals(mac, store.loadLatest())
        assertEquals(mac, store.load())
    }

    @Test
    fun testMultiDeviceOrderingNewestFirst() {
        val mac1 = RememberedMac(
            serverId = "id-1",
            name = "Mac 1",
            networkEndpoint = MacEndpoint("192.168.1.11", 7375),
            usbPort = null,
            lastUsedUsb = false
        )
        val mac2 = RememberedMac(
            serverId = "id-2",
            name = "Mac 2",
            networkEndpoint = MacEndpoint("192.168.1.12", 7375),
            usbPort = 7375,
            lastUsedUsb = true
        )
        val mac3 = RememberedMac(
            serverId = "id-3",
            name = "Mac 3",
            networkEndpoint = MacEndpoint("192.168.1.13", 7375),
            usbPort = null,
            lastUsedUsb = false
        )

        store.save(mac1)
        store.save(mac2)
        store.save(mac3)

        // mac3 was saved last, so order must be [mac3, mac2, mac1]
        val all = store.loadAll()
        assertEquals(3, all.size)
        assertEquals("Mac 3", all[0].name)
        assertEquals("Mac 2", all[1].name)
        assertEquals("Mac 1", all[2].name)
        assertEquals(mac3, store.loadLatest())

        // Re-saving mac1 moves it back to index 0
        store.save(mac1)
        val updated = store.loadAll()
        assertEquals(3, updated.size)
        assertEquals("Mac 1", updated[0].name)
        assertEquals("Mac 3", updated[1].name)
        assertEquals("Mac 2", updated[2].name)
        assertEquals(mac1, store.loadLatest())
    }

    @Test
    fun testDeduplicationByNameWhenNoServerId() {
        val macA = RememberedMac(
            serverId = null,
            name = "MacBook Air",
            networkEndpoint = MacEndpoint("10.0.0.1", 7375),
            usbPort = null,
            lastUsedUsb = false
        )
        val macB = RememberedMac(
            serverId = null,
            name = "MacBook Air",
            networkEndpoint = MacEndpoint("10.0.0.2", 7375),
            usbPort = 7375,
            lastUsedUsb = true
        )

        store.save(macA)
        store.save(macB)

        val all = store.loadAll()
        assertEquals(1, all.size)
        assertEquals(macB, all[0])
    }

    @Test
    fun testDeduplicationByServerId() {
        val macA = RememberedMac(
            serverId = "stable-uuid",
            name = "Work Mac",
            networkEndpoint = MacEndpoint("192.168.1.5", 7375),
            usbPort = null,
            lastUsedUsb = false
        )
        val macB = RememberedMac(
            serverId = "stable-uuid",
            name = "Work Mac Renamed",
            networkEndpoint = MacEndpoint("192.168.1.9", 7375),
            usbPort = 7375,
            lastUsedUsb = false
        )

        store.save(macA)
        store.save(macB)

        val all = store.loadAll()
        assertEquals(1, all.size)
        assertEquals("Work Mac Renamed", all[0].name)
        assertEquals(macB, all[0])
    }

    @Test
    fun testRemoveMac() {
        val mac1 = RememberedMac("id-1", "Mac 1", MacEndpoint("10.0.0.1", 7375), null, false)
        val mac2 = RememberedMac("id-2", "Mac 2", MacEndpoint("10.0.0.2", 7375), null, false)
        val mac3 = RememberedMac("id-3", "Mac 3", MacEndpoint("10.0.0.3", 7375), null, false)

        store.save(mac1)
        store.save(mac2)
        store.save(mac3)

        store.remove(mac2)
        val afterRemove2 = store.loadAll()
        assertEquals(2, afterRemove2.size)
        assertEquals("Mac 3", afterRemove2[0].name)
        assertEquals("Mac 1", afterRemove2[1].name)

        store.remove(serverId = "id-3", name = "Mac 3")
        val afterRemove3 = store.loadAll()
        assertEquals(1, afterRemove3.size)
        assertEquals("Mac 1", afterRemove3[0].name)
    }

    @Test
    fun testClear() {
        val mac1 = RememberedMac("id-1", "Mac 1", MacEndpoint("10.0.0.1", 7375), null, false)
        store.save(mac1)

        store.clear()
        assertTrue(store.loadAll().isEmpty())
        assertNull(store.loadLatest())
        assertNull(prefs.getString(RememberedMacStore.KEY_NAME, null))
    }

    @Test
    fun testLegacyMigrationPreservesExistingSingleDevice() {
        prefs.edit()
            .putString(RememberedMacStore.KEY_NAME, "Studio Mac")
            .putString(RememberedMacStore.KEY_SERVER_ID, "legacy-srv-99")
            .putString(RememberedMacStore.KEY_NETWORK_HOST, "192.168.50.12")
            .putInt(RememberedMacStore.KEY_NETWORK_PORT, 7375)
            .putInt(RememberedMacStore.KEY_USB_PORT, 7375)
            .putBoolean(RememberedMacStore.KEY_LAST_USED_USB, true)
            .apply()

        assertFalse(prefs.contains(RememberedMacStore.KEY_MACS_LIST))

        val loaded = store.loadAll()
        assertEquals(1, loaded.size)
        val migrated = loaded[0]
        assertEquals("Studio Mac", migrated.name)
        assertEquals("legacy-srv-99", migrated.serverId)
        assertNotNull(migrated.networkEndpoint)
        assertEquals("192.168.50.12", migrated.networkEndpoint?.host)
        assertEquals(7375, migrated.networkEndpoint?.port)
        assertEquals(7375, migrated.usbPort)
        assertTrue(migrated.lastUsedUsb)

        // After migration, the list key must be written
        assertTrue(prefs.contains(RememberedMacStore.KEY_MACS_LIST))
    }

    @Test
    fun testLegacySyncKeepsHeadSynchronized() {
        val mac1 = RememberedMac("id-1", "Mac 1", MacEndpoint("10.0.0.1", 7375), null, false)
        val mac2 = RememberedMac("id-2", "Mac 2", MacEndpoint("10.0.0.2", 7375), null, false)

        store.save(mac1)
        assertEquals("Mac 1", prefs.getString(RememberedMacStore.KEY_NAME, null))

        store.save(mac2)
        assertEquals("Mac 2", prefs.getString(RememberedMacStore.KEY_NAME, null))

        store.remove(mac2)
        assertEquals("Mac 1", prefs.getString(RememberedMacStore.KEY_NAME, null))

        store.remove(mac1)
        assertNull(prefs.getString(RememberedMacStore.KEY_NAME, null))
    }
}

/** In-memory implementation of SharedPreferences for JVM tests. */
private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = HashMap(values)
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = values[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    inner class FakeEditor : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clearFlag = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) toRemove.add(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearFlag = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearFlag) values.clear()
            for (key in toRemove) values.remove(key)
            values.putAll(temp)
        }
    }
}
