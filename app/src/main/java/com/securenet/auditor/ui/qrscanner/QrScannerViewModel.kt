package com.securenet.auditor.ui.qrscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.network.QrWifiParser
import com.securenet.auditor.network.WifiConnectionManager
import com.securenet.auditor.ui.wifi.WifiScannerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QrScannerViewModel(
    private val wifiManager: WifiConnectionManager
) : ViewModel() {

    private val _scannedResult = MutableStateFlow<QrWifiParser.WifiCredentials?>(null)
    val scannedResult: StateFlow<QrWifiParser.WifiCredentials?> = _scannedResult.asStateFlow()

    private val _connectionState = MutableStateFlow<WifiScannerViewModel.ConnectionState>(
        WifiScannerViewModel.ConnectionState.Idle)
    val connectionState: StateFlow<WifiScannerViewModel.ConnectionState> = _connectionState.asStateFlow()

    fun processQrCode(rawContent: String) {
        val credentials = QrWifiParser.parse(rawContent)
        if (credentials != null) {
            _scannedResult.value = credentials
        } else {
            // Not a Wi-Fi QR code
            _connectionState.value = WifiScannerViewModel.ConnectionState.Failed(
                "Not a valid Wi-Fi QR code.")
        }
    }

    fun connectToScannedNetwork(credentials: QrWifiParser.WifiCredentials) {
        viewModelScope.launch {
            _connectionState.value = WifiScannerViewModel.ConnectionState.Connecting(credentials.ssid)

            wifiManager.connectToNetwork(
                ssid = credentials.ssid,
                password = credentials.password,
                securityType = credentials.securityType
            ) { result ->
                _connectionState.value = when (result) {
                    WifiConnectionManager.ConnectionResult.SUCCESS ->
                        WifiScannerViewModel.ConnectionState.Connected(credentials.ssid)
                    else ->
                        WifiScannerViewModel.ConnectionState.Failed(
                            "Connection failed. Try connecting manually via Android Wi-Fi settings.")
                }
            }
        }
    }

    fun clearResult() {
        _scannedResult.value = null
        _connectionState.value = WifiScannerViewModel.ConnectionState.Idle
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QrScannerViewModel(container.wifiConnectionManager) as T
            }
        }
    }
}
