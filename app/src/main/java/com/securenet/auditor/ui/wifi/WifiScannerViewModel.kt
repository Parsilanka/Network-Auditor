package com.securenet.auditor.ui.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.network.WifiConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WifiScannerViewModel(
    private val wifiManager: WifiConnectionManager
) : ViewModel() {

    private val _networks = MutableStateFlow<List<WifiConnectionManager.WifiNetwork>>(emptyList())
    val networks: StateFlow<List<WifiConnectionManager.WifiNetwork>> = _networks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentSsid = MutableStateFlow<String?>(null)
    val currentSsid: StateFlow<String?> = _currentSsid.asStateFlow()

    sealed class ConnectionState {
        object Idle : ConnectionState()
        data class Connecting(val ssid: String) : ConnectionState()
        data class Connected(val ssid: String) : ConnectionState()
        data class Failed(val message: String) : ConnectionState()
        object RequiresPassword : ConnectionState()
    }

    fun scanNetworks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            delay(500) // Allow scan to start
            val results = wifiManager.scanNetworks()
            _networks.value = results
            _currentSsid.value = wifiManager.getCurrentNetwork()
            _isScanning.value = false
        }
    }

    fun connectToNetwork(
        network: WifiConnectionManager.WifiNetwork,
        password: String?
    ) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting(network.ssid)
            
            wifiManager.connectToNetwork(
                ssid = network.ssid,
                password = password,
                securityType = network.securityType
            ) { result ->
                _connectionState.value = when (result) {
                    WifiConnectionManager.ConnectionResult.SUCCESS ->
                        ConnectionState.Connected(network.ssid)
                    WifiConnectionManager.ConnectionResult.WRONG_PASSWORD ->
                        ConnectionState.Failed("Wrong password for ${network.ssid}")
                    WifiConnectionManager.ConnectionResult.REQUIRES_SYSTEM_SETTINGS ->
                        ConnectionState.Failed("Please connect via Android Settings on Android 10+")
                    else ->
                        ConnectionState.Failed("Could not connect to ${network.ssid}")
                }
            }
        }
    }

    fun disconnect() {
        wifiManager.disconnectFromNetwork()
        _connectionState.value = ConnectionState.Idle
        _currentSsid.value = null
    }

    fun refreshCurrentNetwork() {
        _currentSsid.value = wifiManager.getCurrentNetwork()
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WifiScannerViewModel(container.wifiConnectionManager) as T
            }
        }
    }
}
