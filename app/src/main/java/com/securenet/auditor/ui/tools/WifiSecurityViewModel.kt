package com.securenet.auditor.ui.tools

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.WifiScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WifiSecurityInfo(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int, // 0-4
    val frequency: Int, // MHz
    val linkSpeed: Int, // Mbps
    val encryption: String,
    val gateway: String,
    val dns1: String,
    val dns2: String,
    val ipAddress: String,
    val isSecure: Boolean
)

data class AvailableWifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val capabilities: String,
    val frequency: Int
)

class WifiSecurityViewModel(
    private val context: Context,
    private val scanner: WifiScanner = WifiScanner(context)
) : ViewModel() {

    private val _wifiInfo = MutableStateFlow<WifiSecurityInfo?>(null)
    val wifiInfo: StateFlow<WifiSecurityInfo?> = _wifiInfo.asStateFlow()

    private val _availableNetworks = MutableStateFlow<List<AvailableWifiNetwork>>(emptyList())
    val availableNetworks: StateFlow<List<AvailableWifiNetwork>> = _availableNetworks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    @SuppressLint("MissingPermission")
    fun refreshWifiInfo() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        
        val connectionInfo: WifiInfo? = wifiManager.connectionInfo
        val dhcpInfo = wifiManager.dhcpInfo
        
        if (connectionInfo == null || connectionInfo.networkId == -1) {
            _wifiInfo.value = null
            return
        }

        val ssid = connectionInfo.ssid.removeSurrounding("\"")
        val bssid = connectionInfo.bssid
        val signalLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wifiManager.calculateSignalLevel(connectionInfo.rssi)
        } else {
            @Suppress("DEPRECATION")
            WifiManager.calculateSignalLevel(connectionInfo.rssi, 5)
        }
        
        val encryption = getEncryptionType(wifiManager, connectionInfo)
        
        _wifiInfo.value = WifiSecurityInfo(
            ssid = if (ssid == "<unknown ssid>") "Connected" else ssid,
            bssid = bssid ?: "N/A",
            signalLevel = signalLevel,
            frequency = connectionInfo.frequency,
            linkSpeed = connectionInfo.linkSpeed,
            encryption = encryption,
            gateway = Formatter.formatIpAddress(dhcpInfo.gateway),
            dns1 = Formatter.formatIpAddress(dhcpInfo.dns1),
            dns2 = Formatter.formatIpAddress(dhcpInfo.dns2),
            ipAddress = Formatter.formatIpAddress(connectionInfo.ipAddress),
            isSecure = !encryption.contains("None", true) && !encryption.contains("Open", true)
        )
        
        startWifiScan()
    }

    @SuppressLint("MissingPermission")
    fun startWifiScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        viewModelScope.launch {
            scanner.scanNetworks()
                .onCompletion { _isScanning.value = false }
                .collect { networks ->
                    _availableNetworks.value = networks
                    _isScanning.value = false
                }
        }
    }

    private fun getEncryptionType(wifiManager: WifiManager, info: WifiInfo): String {
        // This is a simplified check. Accurate encryption detection usually requires 
        // ScanResults matching the current BSSID.
        val scanResults = wifiManager.scanResults
        val match = scanResults.find { it.BSSID == info.bssid }
        return match?.capabilities ?: "Unknown (Requires Location)"
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WifiSecurityViewModel(context) as T
            }
        }
    }
}
