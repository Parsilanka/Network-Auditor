package com.securenet.auditor.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import com.securenet.auditor.data.repository.ScanRepository
import com.securenet.auditor.domain.model.HostInfo
import com.securenet.auditor.domain.model.ScanProgress
import com.securenet.auditor.network.PortScanner
import com.securenet.auditor.network.SubnetScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val scanner: SubnetScanner,
    private val portScanner: PortScanner,
    private val repository: ScanRepository
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<HostInfo>>(emptyList())
    val discoveredHosts: StateFlow<List<HostInfo>> = _discoveredHosts.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _currentSubnet = MutableStateFlow<String?>(null)
    val currentSubnet: StateFlow<String?> = _currentSubnet.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        val subnet = scanner.detectSubnet()
        if (subnet == null) {
            _scanProgress.value = ScanProgress.Error("Connect to Wi-Fi to scan")
            return
        }
        _currentSubnet.value = subnet
        stopScan()
        _discoveredHosts.value = emptyList()
        _isScanning.value = true
        
        scanJob = viewModelScope.launch {
            scanner.scanSubnet(subnet).collect { progress ->
                _scanProgress.value = progress
                when (progress) {
                    is ScanProgress.HostFound -> {
                        _discoveredHosts.value = _discoveredHosts.value + progress.host
                    }
                    is ScanProgress.Complete -> {
                        _isScanning.value = false
                        repository.saveHosts(progress.results)
                    }
                    is ScanProgress.Error -> {
                        _isScanning.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
        if (_scanProgress.value is ScanProgress.Scanning) {
            _scanProgress.value = ScanProgress.Idle
        }
    }

    fun runPortScan(host: HostInfo) {
        viewModelScope.launch {
            val openPorts = portScanner.scanPorts(host.ipAddress)
            _discoveredHosts.value = _discoveredHosts.value.map {
                if (it.ipAddress == host.ipAddress) it.copy(openPorts = openPorts) else it
            }
        }
    }

    fun clearResults() {
        _discoveredHosts.value = emptyList()
        _scanProgress.value = ScanProgress.Idle
        _isScanning.value = false
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ScannerViewModel(
                    container.subnetScanner,
                    container.portScanner,
                    container.scanRepository
                ) as T
            }
        }
    }
}
