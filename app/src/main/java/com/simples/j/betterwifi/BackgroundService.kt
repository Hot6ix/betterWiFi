package com.simples.j.betterwifi

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION
import android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
import android.os.IBinder
import android.util.Log.i

class BackgroundService : Service() {

    private lateinit var wifiManager: WifiManager
    private var scanList = ArrayList<ScanResult>()
    private var filteredConfiguredList = ArrayList<WifiConfiguration>()
    private lateinit var receiver: BroadcastReceiver
    private var looper = Looper()

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        i(applicationContext.packageName, "Sticky service started")

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        receiver = object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                when(p1?.action) {
                    SCAN_RESULTS_AVAILABLE_ACTION -> {
                        i(applicationContext.packageName, "Scanning complete")
                        scanList = wifiManager.scanResults as ArrayList<ScanResult>

                        compare(sort(filter(scanList, wifiManager.configuredNetworks)))

                        if(wifiManager.connectionInfo.rssi > -60) // If current wifi is poor
                            wifiManager.startScan()

                    }
                    NETWORK_STATE_CHANGED_ACTION -> sendBroadcast(Intent("wifi.ON_NETWORK_STATE_CHANGED"))
                }
            }
        }

        val iFilter = IntentFilter(SCAN_RESULTS_AVAILABLE_ACTION)
        iFilter.addAction(NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(receiver, iFilter)

        looper.start()

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        looper.quit()
        i(applicationContext.packageName, "Sticky service stopped")
    }

    // Filtering that scanned wifi is configured
    private fun filter(scanned: ArrayList<ScanResult>, configured: List<WifiConfiguration>): ArrayList<ScanResult> {
        val list: ArrayList<ScanResult> = ArrayList()
        for(i in scanned) {
            configured.filter { Regex("\"").replace(it.SSID, "") == i.SSID }.map { list.add(i); filteredConfiguredList.add(it) }
        }

        return list
    }

    // Sorted by level
    private fun sort(scanned: ArrayList<ScanResult>): ArrayList<ScanResult> {
        return ArrayList(scanned.sortedByDescending { it.level })
    }

    private fun compare(filtered: List<ScanResult>) {
        // Get connected wifi
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val ssid: String = Regex("\"").replace(wifiInfo.ssid, "") // Remove double quotes

        // Compare filteredList and connected wifi

        for((i, item) in filtered.withIndex()) {
            if(ssid != item.SSID) {
                if (wifiInfo.rssi < item.level) {
                    i(applicationContext.packageName, "${item.SSID}(${item.level}) is stronger than ${wifiInfo.ssid}(${wifiInfo.rssi})")
                    val wifiConf: WifiConfiguration = filteredConfiguredList[i]
                    i(applicationContext.packageName, "${wifiConf.SSID}")

                    wifiManager.disconnect()
                    wifiManager.enableNetwork(wifiConf.networkId, true)
                    wifiManager.reconnect()

                    if(check()) break
                }
            }
        }
    }

    private fun check(): Boolean {
        return if(wifiManager.isWifiEnabled) {
            wifiManager.connectionInfo.networkId == -1
        }
        else {
            false
        }
    }

    private inner class Looper: Thread() {

        var isRunning = true

        override fun run() {
            super.run()

            while(isRunning){
                if(wifiManager.connectionInfo.rssi < -60)
                    wifiManager.startScan()

                sleep(1000)
            }

        }

        fun quit() {
            isRunning = false
        }
    }
}
