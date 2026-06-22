package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress

data class PingResult(
    val sequenceNumber: Int,
    val host: String,
    val responseTimeMs: Long?,
    val isSuccess: Boolean,
    val timestamp: Long
)

class PingViewModel : ViewModel() {

    private val _pingResults = MutableStateFlow<List<PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingResult>> = _pingResults.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun startPing(host: String, count: Int = 4) {
        if (_isRunning.value) return
        
        viewModelScope.launch {
            _isRunning.value = true
            _pingResults.value = emptyList()
            
            val results = mutableListOf<PingResult>()
            
            for (i in 1..count) {
                val start = System.currentTimeMillis()
                val reachable = try {
                    InetAddress.getByName(host).isReachable(3000)
                } catch (e: Exception) {
                    false
                }
                val time = System.currentTimeMillis() - start
                
                val result = PingResult(
                    sequenceNumber = i,
                    host = host,
                    responseTimeMs = if (reachable) time else null,
                    isSuccess = reachable,
                    timestamp = System.currentTimeMillis()
                )
                
                results.add(result)
                _pingResults.value = results.toList()
                
                if (i < count) delay(1000)
            }
            
            _isRunning.value = false
        }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PingViewModel() as T
            }
        }
    }
}
