package com.securenet.auditor.ui.geolocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.data.repository.GeoLocationRepository
import com.securenet.auditor.domain.model.GeoLocationResult
import com.securenet.auditor.domain.model.HostInfo
import com.securenet.auditor.domain.model.OsintResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeoLocationViewModel(
    private val repository: GeoLocationRepository
) : ViewModel() {

    private val _lookupResult = MutableStateFlow<OsintResult<GeoLocationResult>>(
        OsintResult.Idle)
    val lookupResult: StateFlow<OsintResult<GeoLocationResult>> = 
        _lookupResult.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<GeoLocationResult>>(
        emptyList())
    val searchHistory: StateFlow<List<GeoLocationResult>> = 
        _searchHistory.asStateFlow()

    private val _batchResults = MutableStateFlow<Map<String, GeoLocationResult>>(
        emptyMap())
    val batchResults: StateFlow<Map<String, GeoLocationResult>> = 
        _batchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun lookupIp(ip: String) {
        if (isPrivateIp(ip)) {
            _lookupResult.value = OsintResult.Error("Private IP — Geolocation not available for LAN addresses. This IP is on your local network.")
            return
        }
        viewModelScope.launch {
            _lookupResult.value = OsintResult.Loading
            _isLoading.value = true
            val result = repository.lookupIp(ip)
            _lookupResult.value = result
            if (result is OsintResult.Found) {
                val currentHistory = _searchHistory.value.toMutableList()
                currentHistory.add(0, result.data)
                if (currentHistory.size > 20) currentHistory.removeLast()
                _searchHistory.value = currentHistory
            }
            _isLoading.value = false
        }
    }

    private fun isPrivateIp(ip: String): Boolean {
        if (ip.isBlank() || ip == "localhost" || ip == "127.0.0.1") return false
        
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        return try {
            val p1 = parts[0].toInt()
            val p2 = parts[1].toInt()
            
            p1 == 10 || 
            (p1 == 172 && p2 in 16..31) || 
            (p1 == 192 && p2 == 168)
        } catch (e: Exception) {
            false
        }
    }

    fun lookupMyIp() {
        lookupIp("") // empty string returns caller's own IP
    }

    fun batchLookupFromScan(hosts: List<HostInfo>) {
        viewModelScope.launch {
            _isLoading.value = true
            val ips = hosts.map { it.ipAddress }
            _batchResults.value = repository.lookupMultipleIps(ips)
            _isLoading.value = false
        }
    }

    fun clearResult() {
        _lookupResult.value = OsintResult.Idle
    }

    companion object {
        fun factory(repository: GeoLocationRepository) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return GeoLocationViewModel(repository) as T
                }
            }
    }
}
