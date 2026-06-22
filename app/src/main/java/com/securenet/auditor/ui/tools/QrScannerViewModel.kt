package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiQrData(
    val ssid: String,
    val password: String,
    val type: String,
    val isHidden: Boolean = false
)

sealed class QrScanState {
    object Idle : QrScanState()
    object Scanning : QrScanState()
    data class Success(val data: WifiQrData) : QrScanState()
    data class Error(val message: String) : QrScanState()
}

class QrScannerViewModel : ViewModel() {

    private val _scanState = MutableStateFlow<QrScanState>(QrScanState.Idle)
    val scanState: StateFlow<QrScanState> = _scanState.asStateFlow()

    fun onQrScanned(rawContent: String) {
        if (!rawContent.startsWith("WIFI:")) {
            _scanState.value = QrScanState.Error("Invalid Wi-Fi QR code")
            return
        }

        try {
            // WIFI:S:SSID;T:WPA;P:PASSWORD;H:true;;
            val data = parseWifiQr(rawContent)
            _scanState.value = QrScanState.Success(data)
        } catch (e: Exception) {
            _scanState.value = QrScanState.Error("Failed to parse Wi-Fi data")
        }
    }

    private fun parseWifiQr(raw: String): WifiQrData {
        val content = raw.removePrefix("WIFI:")
        val parts = content.split(";")
        
        var ssid = ""
        var password = ""
        var type = "WPA"
        var hidden = false

        parts.forEach { part ->
            when {
                part.startsWith("S:") -> ssid = part.substring(2)
                part.startsWith("P:") -> password = part.substring(2)
                part.startsWith("T:") -> type = part.substring(2)
                part.startsWith("H:") -> hidden = part.substring(2).toBoolean()
            }
        }

        return WifiQrData(ssid, password, type, hidden)
    }

    fun reset() {
        _scanState.value = QrScanState.Idle
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QrScannerViewModel() as T
            }
        }
    }
}
