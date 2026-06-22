package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.*

data class PingResult(
    val sequenceNumber: Int,
    val host: String,
    val responseTimeMs: Long?,
    val isSuccess: Boolean,
    val timestamp: Long
)

data class PingSummary(
    val minTime: Long = 0,
    val maxTime: Long = 0,
    val avgTime: Long = 0,
    val lossPercent: Int = 0
)

class PingViewModel : ViewModel() {

    private val _pingResults = MutableStateFlow<List<PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingResult>> = _pingResults.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _summary = MutableStateFlow(PingSummary())
    val summary: StateFlow<PingSummary> = _summary.asStateFlow()

    fun startPing(host: String, count: Int = 4) {
        if (_isRunning.value) return
        
        viewModelScope.launch {
            _isRunning.value = true
            _pingResults.value = emptyList()
            _summary.value = PingSummary()
            
            val results = mutableListOf<PingResult>()
            
            for (i in 1..count) {
                val result = pingHost(host, i)
                results.add(result)
                _pingResults.value = results.toList()
                
                if (i < count) delay(1000)
            }
            
            // Calculate summary
            val successfulPings = results.filter { it.isSuccess }
            val minTime = successfulPings.minOfOrNull { it.responseTimeMs ?: 0 } ?: 0
            val maxTime = successfulPings.maxOfOrNull { it.responseTimeMs ?: 0 } ?: 0
            val avgTime = if (successfulPings.isEmpty()) 0L else
                successfulPings.sumOf { it.responseTimeMs ?: 0 } / successfulPings.size
            val lossPercent = ((results.size - successfulPings.size) * 100) / results.size
            
            _summary.value = PingSummary(minTime, maxTime, avgTime, lossPercent.toInt())
            _isRunning.value = false
        }
    }

    private suspend fun pingHost(host: String, sequenceNumber: Int): PingResult {
        // Try multiple TCP ports that are commonly open
        val portsToTry = listOf(80, 443, 53)
        
        return withContext(Dispatchers.IO) {
            var success = false
            var responseTime = 0L
            
            // First try HTTP connection for domains
            try {
                val url = URL("http://$host")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "HEAD"
                val connectStart = System.currentTimeMillis()
                connection.connect()
                responseTime = System.currentTimeMillis() - connectStart
                success = true
                connection.disconnect()
            } catch (e: Exception) {
                // Try TCP socket on common ports
                for (port in portsToTry) {
                    try {
                        val socketStart = System.currentTimeMillis()
                        Socket().use { socket ->
                            socket.connect(
                                InetSocketAddress(host, port), 
                                3000
                            )
                            responseTime = System.currentTimeMillis() - socketStart
                            success = true
                        }
                        break
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
            
            // If TCP fails, try DNS resolution as last resort
            if (!success) {
                try {
                    val dnsStart = System.currentTimeMillis()
                    InetAddress.getByName(host)
                    responseTime = System.currentTimeMillis() - dnsStart
                    success = true
                } catch (e: Exception) {
                    success = false
                }
            }
            
            PingResult(
                sequenceNumber = sequenceNumber,
                host = host,
                responseTimeMs = if (success) responseTime else null,
                isSuccess = success,
                timestamp = System.currentTimeMillis()
            )
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
