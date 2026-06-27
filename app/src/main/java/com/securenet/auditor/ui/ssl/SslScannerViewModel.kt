package com.securenet.auditor.ui.ssl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.network.SslTlsScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SslScannerViewModel(private val scanner: SslTlsScanner) : ViewModel() {

    private val _scanResult = MutableStateFlow<SslTlsScanner.SslScanResult?>(null)
    val scanResult: StateFlow<SslTlsScanner.SslScanResult?> = _scanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun scan(host: String, port: Int = 443) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = scanner.scan(host, port)
                _scanResult.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Scan failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(scanner: SslTlsScanner): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SslScannerViewModel(scanner) as T
            }
        }
    }
}
