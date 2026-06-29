package com.securenet.auditor.ui.packet

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.PacketAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PacketAnalyzerViewModel(private val analyzer: PacketAnalyzer) : ViewModel() {

    private val _appUsage = MutableStateFlow<List<PacketAnalyzer.NetworkSession>>(emptyList())
    val appUsage: StateFlow<List<PacketAnalyzer.NetworkSession>> = _appUsage.asStateFlow()

    private val _activeConnections = MutableStateFlow<List<PacketAnalyzer.ConnectionInfo>>(emptyList())
    val activeConnections: StateFlow<List<PacketAnalyzer.ConnectionInfo>> = _activeConnections.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.M)
    fun loadStats(hours: Int = 24) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _appUsage.value = analyzer.getNetworkStats(hours)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadActiveConnections(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _activeConnections.value = analyzer.getActiveConnectionInfo(context)
        }
    }

    fun startConnectionMonitoring() {
        // Monitoring is handled by the UI or loadActiveConnections
    }

    companion object {
        fun provideFactory(analyzer: PacketAnalyzer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PacketAnalyzerViewModel(analyzer) as T
            }
        }
    }
}
