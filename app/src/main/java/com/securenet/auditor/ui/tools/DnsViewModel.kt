package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class DnsRecord(
    val type: String,
    val value: String
)

class DnsViewModel : ViewModel() {

    private val _records = MutableStateFlow<List<DnsRecord>>(emptyList())
    val records: StateFlow<List<DnsRecord>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun performLookup(domain: String) {
        if (domain.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _records.value = emptyList()
            
            try {
                val results = withContext(Dispatchers.IO) {
                    val addresses = InetAddress.getAllByName(domain)
                    addresses.map { addr ->
                        DnsRecord(
                            type = if (addr.address.size == 4) "A" else "AAAA",
                            value = addr.hostAddress ?: ""
                        )
                    }
                }
                
                if (results.isEmpty()) {
                    _error.value = "No records found"
                } else {
                    _records.value = results
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DnsViewModel() as T
            }
        }
    }
}
