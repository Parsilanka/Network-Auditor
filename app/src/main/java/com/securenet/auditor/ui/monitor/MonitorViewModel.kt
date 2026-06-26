package com.securenet.auditor.ui.monitor

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.service.NetworkMonitorService
import com.securenet.auditor.worker.NetworkMonitorScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceAlert(
    val ipAddress: String,
    val timestamp: Long,
    val wasAcknowledged: Boolean
)

class MonitorViewModel(private val context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
    private val encryptedPrefs = (context.applicationContext as com.securenet.auditor.SecureNetApp).container.encryptedPrefs

    private val _isMonitorRunning = MutableStateFlow(encryptedPrefs.getBoolSetting("monitoring_enabled") ?: false)
    val isMonitorRunning: StateFlow<Boolean> = _isMonitorRunning.asStateFlow()

    private val _knownDeviceCount = MutableStateFlow(encryptedPrefs.getStringSetting("known_hosts")?.split(",")?.size ?: 0)
    val knownDeviceCount: StateFlow<Int> = _knownDeviceCount.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<DeviceAlert>>(emptyList())
    val alertHistory: StateFlow<List<DeviceAlert>> = _alertHistory.asStateFlow()

    private val _whitelist = MutableStateFlow<Set<String>>(emptySet())
    val whitelist: StateFlow<Set<String>> = _whitelist.asStateFlow()

    private val _scanInterval = MutableStateFlow(prefs.getLong("scan_interval", 15 * 60 * 1000L))
    val scanInterval: StateFlow<Long> = _scanInterval.asStateFlow()

    init {
        loadAlerts()
        loadWhitelist()
    }

    fun startMonitor() {
        encryptedPrefs.saveBoolSetting("monitoring_enabled", true)
        val intervalMinutes = _scanInterval.value / (60 * 1000)
        NetworkMonitorScheduler.schedule(context, intervalMinutes)
        _isMonitorRunning.value = true
    }

    fun stopMonitor() {
        encryptedPrefs.saveBoolSetting("monitoring_enabled", false)
        NetworkMonitorScheduler.cancel(context)
        _isMonitorRunning.value = false
    }

    fun updateBaseline(hosts: List<String>) {
        encryptedPrefs.saveStringSetting("known_hosts", hosts.joinToString(","))
        _knownDeviceCount.value = hosts.size
    }

    fun clearBaseline() {
        encryptedPrefs.saveStringSetting("known_hosts", "")
        _knownDeviceCount.value = 0
    }

    fun clearAlertHistory() {
        prefs.edit().remove("alerts").apply()
        _alertHistory.value = emptyList()
    }

    fun acknowledgeAlert(ip: String) {
        val currentAlerts = prefs.getStringSet("alerts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val updatedAlerts = currentAlerts.map {
            val parts = it.split("|")
            if (parts[0] == ip) {
                "${parts[0]}|${parts[1]}|true"
            } else it
        }.toSet()
        
        prefs.edit().putStringSet("alerts", updatedAlerts).apply()
        loadAlerts()
    }

    fun addToWhitelist(ip: String) {
        val currentWhitelist = prefs.getStringSet("whitelist", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentWhitelist.add(ip)
        prefs.edit().putStringSet("whitelist", currentWhitelist).apply()
        loadWhitelist()
        
        // Also remove from alerts if present
        removeAlert(ip)
    }

    fun removeFromWhitelist(ip: String) {
        val currentWhitelist = prefs.getStringSet("whitelist", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        currentWhitelist.remove(ip)
        prefs.edit().putStringSet("whitelist", currentWhitelist).apply()
        loadWhitelist()
    }

    fun setScanInterval(minutes: Int) {
        val millis = minutes * 60 * 1000L
        prefs.edit().putLong("scan_interval", millis).apply()
        _scanInterval.value = millis
        
        // If monitor is running, restart it to apply new interval
        if (_isMonitorRunning.value) {
            startMonitor()
        }
    }

    private fun loadAlerts() {
        val saved = prefs.getStringSet("alerts", emptySet()) ?: emptySet()
        _alertHistory.value = saved.mapNotNull {
            try {
                val parts = it.split("|")
                DeviceAlert(parts[0], parts[1].toLong(), parts[2].toBoolean())
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    private fun loadWhitelist() {
        _whitelist.value = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
    }

    private fun removeAlert(ip: String) {
        val currentAlerts = prefs.getStringSet("alerts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val updatedAlerts = currentAlerts.filter { !it.startsWith("$ip|") }.toSet()
        prefs.edit().putStringSet("alerts", updatedAlerts).apply()
        loadAlerts()
    }

    fun refreshStats() {
        _knownDeviceCount.value = prefs.getStringSet("known_devices", emptySet())?.size ?: 0
        loadAlerts()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MonitorViewModel(context.applicationContext) as T
            }
        }
    }
}
