package com.securenet.auditor.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi

class WifiConnectionManager(private val context: Context) {

    data class WifiNetwork(
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,      // RSSI in dBm
        val signalLevel: Int,         // 0-4 bars
        val frequency: Int,           // MHz
        val capabilities: String,     // Security type string
        val securityType: WifiSecurityType,
        val isConnected: Boolean,
        val channel: Int
    )

    enum class WifiSecurityType {
        OPEN, WEP, WPA, WPA2, WPA3, WPA2_WPA3, UNKNOWN
    }

    enum class ConnectionResult {
        SUCCESS,
        WRONG_PASSWORD,
        NETWORK_NOT_FOUND,
        PERMISSION_DENIED,
        FAILED,
        REQUIRES_SYSTEM_SETTINGS  // Android 10+ limitation
    }

    // Scan for nearby Wi-Fi networks
    @SuppressLint("MissingPermission")
    fun scanNetworks(): List<WifiNetwork> {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            // Android 10+ limitation: can't enable programmatically
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }
        }

        @Suppress("DEPRECATION")
        wifiManager.startScan()
        val results = wifiManager.scanResults

        return results.map { result ->
            val securityType = when {
                result.capabilities.contains("WPA3") -> 
                    WifiSecurityType.WPA3
                result.capabilities.contains("WPA2") && 
                result.capabilities.contains("WPA3") -> 
                    WifiSecurityType.WPA2_WPA3
                result.capabilities.contains("WPA2") -> 
                    WifiSecurityType.WPA2
                result.capabilities.contains("WPA") -> 
                    WifiSecurityType.WPA
                result.capabilities.contains("WEP") -> 
                    WifiSecurityType.WEP
                result.capabilities.contains("ESS") && 
                !result.capabilities.contains("WPA") && 
                !result.capabilities.contains("WEP") -> 
                    WifiSecurityType.OPEN
                else -> WifiSecurityType.UNKNOWN
            }

            val channel = when {
                result.frequency in 2412..2484 -> 
                    (result.frequency - 2412) / 5 + 1
                result.frequency in 5170..5825 -> 
                    (result.frequency - 5170) / 5 + 34
                else -> 0
            }

            WifiNetwork(
                ssid = result.SSID.removeSurrounding("\""),
                bssid = result.BSSID,
                signalStrength = result.level,
                signalLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    wifiManager.calculateSignalLevel(result.level)
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.calculateSignalLevel(result.level, 5)
                },
                frequency = result.frequency,
                capabilities = result.capabilities,
                securityType = securityType,
                isConnected = false,
                channel = channel
            )
        }
        .filter { it.ssid.isNotEmpty() }
        .sortedByDescending { it.signalStrength }
        .distinctBy { it.ssid }
    }

    // Connect to a Wi-Fi network
    @SuppressLint("MissingPermission")
    fun connectToNetwork(
        ssid: String,
        password: String?,
        securityType: WifiSecurityType,
        onResult: (ConnectionResult) -> Unit
    ) {
        // Android 10+ (API 29+) requires WifiNetworkSpecifier
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectAndroid10Plus(ssid, password, securityType, onResult)
        } else {
            connectLegacy(ssid, password, securityType, onResult)
        }
    }

    // Android 10+ connection using WifiNetworkSpecifier
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectAndroid10Plus(
        ssid: String,
        password: String?,
        securityType: WifiSecurityType,
        onResult: (ConnectionResult) -> Unit
    ) {
        val specifierBuilder = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)

        when (securityType) {
            WifiSecurityType.WPA, WifiSecurityType.WPA2,
            WifiSecurityType.WPA2_WPA3 -> {
                if (!password.isNullOrEmpty()) {
                    specifierBuilder.setWpa2Passphrase(password)
                }
            }
            WifiSecurityType.WPA3 -> {
                if (!password.isNullOrEmpty()) {
                    specifierBuilder.setWpa3Passphrase(password)
                }
            }
            WifiSecurityType.OPEN -> { /* no password needed */ }
            else -> { /* handle WEP and others */ }
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifierBuilder.build())
            .build()

        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityManager.bindProcessToNetwork(network)
                onResult(ConnectionResult.SUCCESS)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                onResult(ConnectionResult.FAILED)
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback, 10000)
    }

    // Legacy connection for Android < 10
    @Suppress("DEPRECATION")
    private fun connectLegacy(
        ssid: String,
        password: String?,
        securityType: WifiSecurityType,
        onResult: (ConnectionResult) -> Unit
    ) {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiConfig = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            when (securityType) {
                WifiSecurityType.OPEN -> {
                    allowedKeyManagement.set(
                        WifiConfiguration.KeyMgmt.NONE)
                }
                WifiSecurityType.WEP -> {
                    wepKeys[0] = "\"$password\""
                    wepTxKeyIndex = 0
                    allowedKeyManagement.set(
                        WifiConfiguration.KeyMgmt.NONE)
                    allowedAuthAlgorithms.set(
                        WifiConfiguration.AuthAlgorithm.OPEN)
                    allowedAuthAlgorithms.set(
                        WifiConfiguration.AuthAlgorithm.SHARED)
                }
                else -> {
                    preSharedKey = "\"$password\""
                    allowedKeyManagement.set(
                        WifiConfiguration.KeyMgmt.WPA_PSK)
                }
            }
        }

        val networkId = wifiManager.addNetwork(wifiConfig)
        if (networkId == -1) {
            onResult(ConnectionResult.FAILED)
            return
        }

        wifiManager.disconnect()
        val enabled = wifiManager.enableNetwork(networkId, true)
        wifiManager.reconnect()

        if (enabled) {
            onResult(ConnectionResult.SUCCESS)
        } else {
            onResult(ConnectionResult.FAILED)
        }
    }

    // Disconnect from current network
    fun disconnectFromNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.disconnect()
        } else {
            val connectivityManager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(null)
        }
    }

    // Get currently connected network info
    @SuppressLint("MissingPermission")
    fun getCurrentNetwork(): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return info?.ssid?.removeSurrounding("\"")
    }

    // Enable Wi-Fi if disabled
    fun enableWifi() {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            } else {
                // Android 10+ cannot enable Wi-Fi programmatically
                val panelIntent = Intent(android.provider.Settings.Panel.ACTION_WIFI)
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(panelIntent)
            }
        }
    }
}
