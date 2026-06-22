package com.securenet.auditor.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.securenet.auditor.ui.tools.AvailableWifiNetwork
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiScanner(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    fun scanNetworks(): Flow<List<AvailableWifiNetwork>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    val results = wifiManager.scanResults
                    trySend(mapResults(results))
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        
        // Send current cached results immediately
        trySend(mapResults(wifiManager.scanResults))
        
        // Trigger a fresh scan
        wifiManager.startScan()

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun mapResults(results: List<ScanResult>): List<AvailableWifiNetwork> {
        return results.map {
            val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.calculateSignalLevel(it.level)
            } else {
                @Suppress("DEPRECATION")
                WifiManager.calculateSignalLevel(it.level, 5)
            }
            AvailableWifiNetwork(
                ssid = it.SSID,
                bssid = it.BSSID,
                signalLevel = level,
                capabilities = it.capabilities,
                frequency = it.frequency
            )
        }.filter { it.ssid.isNotBlank() }.sortedByDescending { it.signalLevel }
    }
}
