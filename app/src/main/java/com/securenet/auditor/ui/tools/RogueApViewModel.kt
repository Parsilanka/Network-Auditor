package com.securenet.auditor.ui.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.RogueApDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class RogueApViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val detector = RogueApDetector(appContext)

    sealed class ScanState {
        object Idle : ScanState()
        object Scanning : ScanState()
        data class Complete(val result: RogueApDetector.RogueApScanResult) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    // Derived states for RogueApScreen
    val isScanning: StateFlow<Boolean> = scanState.map { it is ScanState.Scanning }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val alerts: StateFlow<List<RogueApDetector.RogueApAlert>> = scanState.map { 
        (it as? ScanState.Complete)?.result?.alerts ?: emptyList() 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Compatibility for other screens if needed
    private val _report = MutableStateFlow<RogueApDetector.RogueApReport?>(null)
    val report: StateFlow<RogueApDetector.RogueApReport?> = _report.asStateFlow()

    fun startScan() {
        startScan(appContext)
    }

    fun startScan(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _scanState.value = ScanState.Error(
                "Location permission required. Android requires this permission to scan for nearby Wi-Fi networks."
            )
            return
        }

        viewModelScope.launch {
            _scanState.value = ScanState.Scanning

            try {
                // Hard timeout of 10 seconds maximum
                val result = withTimeoutOrNull(10_000L) {
                    detector.scanForRogueAps(context)
                }

                if (result != null) {
                    _scanState.value = ScanState.Complete(result)
                    
                    // Update legacy report for compatibility
                    val riskLevel = when {
                        result.alerts.any { it.threatType == RogueApDetector.RogueThreatType.EVIL_TWIN } -> "CRITICAL"
                        result.alerts.isNotEmpty() -> "HIGH"
                        else -> "LOW"
                    }
                    _report.value = RogueApDetector.RogueApReport(
                        riskLevel = riskLevel,
                        suspiciousNetworks = emptyList(), // This could be filled if needed
                        nearbyNetworks = emptyList(),
                        connectedSsid = result.currentSsid,
                        connectedBssid = result.currentBssid
                    )
                } else {
                    _scanState.value = ScanState.Error(
                        "Scan timed out. This can happen if location permission is denied or Wi-Fi scanning is throttled by the system. Try again."
                    )
                }
            } catch (e: SecurityException) {
                _scanState.value = ScanState.Error(
                    "Location permission required for Wi-Fi scanning. Grant permission in Settings."
                )
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Scan failed")
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RogueApViewModel(context.applicationContext) as T
            }
        }
    }
}
