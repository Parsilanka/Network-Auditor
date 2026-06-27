package com.securenet.auditor.ui.bandwidth

import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.BandwidthMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BandwidthViewModel(
    private val monitor: BandwidthMonitor
) : ViewModel() {

    private val _snapshots = MutableStateFlow<List<BandwidthMonitor.BandwidthSnapshot>>(
        emptyList())
    val snapshots: StateFlow<List<BandwidthMonitor.BandwidthSnapshot>> =
        _snapshots.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> =
        _isMonitoring.asStateFlow()

    private val _currentSpeed = MutableStateFlow<BandwidthMonitor.BandwidthSnapshot?>(null)
    val currentSpeed: StateFlow<BandwidthMonitor.BandwidthSnapshot?> =
        _currentSpeed.asStateFlow()

    private val _appUsage = MutableStateFlow<List<AppNetworkUsage>>(emptyList())
    val appUsage: StateFlow<List<AppNetworkUsage>> =
        _appUsage.asStateFlow()

    data class AppNetworkUsage(
        val appName: String,
        val packageName: String,
        val downloadBytes: Long,
        val uploadBytes: Long,
        val icon: Any?
    )

    private var monitoringJob: Job? = null

    fun startMonitoring() {
        _isMonitoring.value = true
        monitoringJob = viewModelScope.launch {
            // Initialize baseline
            monitor.getCurrentSnapshot()
            delay(1000)
            
            while (isActive) {
                val snapshot = monitor.getCurrentSnapshot()
                _currentSpeed.value = snapshot

                val currentList =
                    _snapshots.value.toMutableList()
                currentList.add(snapshot)
                // Keep last 60 seconds of data
                if (currentList.size > 60) {
                    currentList.removeAt(0)
                }
                _snapshots.value = currentList
                delay(1000)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        _isMonitoring.value = false
    }

    fun loadAppUsageStats(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val packageManager = context.packageManager
            val installedApps = packageManager
                .getInstalledApplications(
                    PackageManager.GET_META_DATA)

            val usageList = installedApps.mapNotNull { app ->
                val uid = app.uid
                val rxBytes = TrafficStats.getUidRxBytes(uid)
                val txBytes = TrafficStats.getUidTxBytes(uid)

                if (rxBytes > 0 || txBytes > 0) {
                    AppNetworkUsage(
                        appName = packageManager
                            .getApplicationLabel(app)
                            .toString(),
                        packageName = app.packageName,
                        downloadBytes = rxBytes,
                        uploadBytes = txBytes,
                        icon = packageManager
                            .getApplicationIcon(app)
                    )
                } else null
            }.sortedByDescending {
                it.downloadBytes + it.uploadBytes
            }.take(20)

            _appUsage.value = usageList
        }
    }

    companion object {
        fun provideFactory(monitor: BandwidthMonitor): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BandwidthViewModel(monitor) as T
            }
        }
    }
}
