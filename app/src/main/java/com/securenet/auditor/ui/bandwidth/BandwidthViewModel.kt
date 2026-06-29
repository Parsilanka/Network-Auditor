package com.securenet.auditor.ui.bandwidth

import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.BandwidthMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    private val _showDownload = MutableStateFlow(true)
    val showDownload: StateFlow<Boolean> = 
        _showDownload.asStateFlow()

    private val _showUpload = MutableStateFlow(true)
    val showUpload: StateFlow<Boolean> = 
        _showUpload.asStateFlow()

    private val _showSpeedometer = MutableStateFlow(true)
    val showSpeedometer: StateFlow<Boolean> = 
        _showSpeedometer.asStateFlow()

    private val _peakDownload = MutableStateFlow(0L)
    val peakDownload: StateFlow<Long> = 
        _peakDownload.asStateFlow()

    private val _peakUpload = MutableStateFlow(0L)
    val peakUpload: StateFlow<Long> = 
        _peakUpload.asStateFlow()

    private var maxHistorySize = 60

    data class AppNetworkUsage(
        val appName: String,
        val packageName: String,
        val downloadBytes: Long,
        val uploadBytes: Long,
        val icon: Any?
    )

    private var monitoringJob: Job? = null

    fun startMonitoring(intervalMs: Long = 1000L) {
        _isMonitoring.value = true
        monitoringJob = viewModelScope.launch {
            // Initialize baseline
            monitor.getCurrentSnapshot()
            delay(intervalMs)
            
            while (isActive) {
                val snapshot = monitor.getCurrentSnapshot()
                _currentSpeed.value = snapshot

                // Update peak tracking
                if (snapshot.downloadBytesPerSec > _peakDownload.value) {
                    _peakDownload.value = snapshot.downloadBytesPerSec
                }
                if (snapshot.uploadBytesPerSec > _peakUpload.value) {
                    _peakUpload.value = snapshot.uploadBytesPerSec
                }

                val currentList =
                    _snapshots.value.toMutableList()
                currentList.add(snapshot)
                
                if (currentList.size > maxHistorySize) {
                    currentList.removeAt(0)
                }
                _snapshots.value = currentList
                delay(intervalMs)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        _isMonitoring.value = false
    }

    fun setUpdateInterval(ms: Long) {
        // Cancel current job and restart with new interval
        monitoringJob?.cancel()
        if (_isMonitoring.value) {
            startMonitoring(intervalMs = ms)
        }
    }

    fun setHistoryDuration(seconds: Int) {
        maxHistorySize = seconds
    }

    fun setShowDownload(show: Boolean) {
        _showDownload.value = show
    }

    fun setShowUpload(show: Boolean) {
        _showUpload.value = show
    }

    fun setShowSpeedometer(show: Boolean) {
        _showSpeedometer.value = show
    }

    fun resetPeakValues() {
        _peakDownload.value = 0L
        _peakUpload.value = 0L
    }

    fun clearHistory() {
        _snapshots.value = emptyList()
    }

    fun exportToCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshots = _snapshots.value
            if (snapshots.isEmpty()) return@launch

            val csv = buildString {
                appendLine(
                    "Timestamp,Download_Bps,Upload_Bps," +
                    "Download_Formatted,Upload_Formatted")
                snapshots.forEach { s ->
                    appendLine(
                        "${s.timestamp}," +
                        "${s.downloadBytesPerSec}," +
                        "${s.uploadBytesPerSec}," +
                        "${s.downloadFormatted}," +
                        "${s.uploadFormatted}")
                }
            }

            val fileName = "bandwidth_${
                SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault())
                .format(Date())}.csv"

            val downloadsDir = Environment
                .getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)
            File(downloadsDir, fileName).writeText(csv)
        }
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
