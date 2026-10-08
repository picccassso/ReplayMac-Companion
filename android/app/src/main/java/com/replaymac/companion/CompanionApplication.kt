package com.replaymac.companion

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.replaymac.companion.data.repository.CompanionRepository

class CompanionApplication : Application() {
    companion object {
        private const val TAG = "CompanionApp"
    }

    lateinit var repository: CompanionRepository
        private set

    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate() {
        super.onCreate()
        repository = CompanionRepository(this)
        acquireMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("ReplayMacCompanionMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
            Log.d(TAG, "Multicast lock acquired for NSD discovery")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            multicastLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release multicast lock: ${e.message}")
        }
    }
}
