package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.data.db.SpeedTestDao
import com.securenet.auditor.data.db.SpeedTestEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long,
    val jitterMs: Double,
    val testServer: String,
    val timestamp: Long,
    val isp: String?
)

sealed class SpeedTestState {
    object Idle : SpeedTestState()
    data class TestingPing(val pingMs: Long) : SpeedTestState()
    data class TestingDownload(
        val progress: Float,
        val currentMbps: Double
    ) : SpeedTestState()
    data class TestingUpload(
        val progress: Float,
        val currentMbps: Double
    ) : SpeedTestState()
    data class Complete(val result: SpeedTestResult) : SpeedTestState()
    data class Error(val message: String) : SpeedTestState()
}

class SpeedTestViewModel(private val speedTestDao: SpeedTestDao) : ViewModel() {

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    val history = speedTestDao.getLastResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startSpeedTest() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = SpeedTestState.TestingPing(0)

                // Step 1: Ping test (average of 5 pings)
                val pingTimes = (1..5).map {
                    val start = System.currentTimeMillis()
                    try {
                        val conn = URL("https://www.cloudflare.com").openConnection() as HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.connect()
                        val time = System.currentTimeMillis() - start
                        conn.disconnect()
                        time
                    } catch (e: Exception) {
                        3000L
                    }
                }
                val avgPing = pingTimes.average().toLong()
                val jitter = if (pingTimes.size > 1) {
                    pingTimes.zipWithNext { a, b -> abs(a - b).toDouble() }.average()
                } else 0.0
                _state.value = SpeedTestState.TestingPing(avgPing)

                delay(500) // Small pause for UI

                // Step 2: Download test
                val downloadUrl = "https://speed.cloudflare.com/__down?bytes=10000000"
                var finalDownloadMbps = 0.0
                val downloadStartTime = System.currentTimeMillis()
                try {
                    val conn = URL(downloadUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    val input = conn.inputStream
                    val buffer = ByteArray(8192)
                    var totalBytes = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                        val elapsed = (System.currentTimeMillis() - downloadStartTime) / 1000.0
                        val currentMbps = if (elapsed > 0) (totalBytes * 8.0) / (elapsed * 1_000_000.0) else 0.0
                        val progress = totalBytes / 10_000_000f
                        _state.value = SpeedTestState.TestingDownload(minOf(progress, 1f), currentMbps)
                    }
                    val totalTime = (System.currentTimeMillis() - downloadStartTime) / 1000.0
                    finalDownloadMbps = (totalBytes * 8.0) / (totalTime * 1_000_000.0)
                    input.close()
                    conn.disconnect()
                } catch (e: Exception) {
                    _state.value = SpeedTestState.Error("Download failed: ${e.message}")
                    return@launch
                }

                delay(500)

                // Step 3: Upload test
                var finalUploadMbps = 0.0
                val uploadDataSize = 5_000_000
                val uploadData = ByteArray(uploadDataSize) { (it % 256).toByte() }
                val uploadStartTime = System.currentTimeMillis()
                try {
                    val conn = URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 10000
                    conn.setRequestProperty("Content-Length", uploadDataSize.toString())
                    val out = conn.outputStream
                    var offset = 0
                    val chunkSize = 8192
                    while (offset < uploadDataSize) {
                        val length = minOf(chunkSize, uploadDataSize - offset)
                        out.write(uploadData, offset, length)
                        offset += length
                        val elapsed = (System.currentTimeMillis() - uploadStartTime) / 1000.0
                        val currentMbps = if (elapsed > 0) (offset * 8.0) / (elapsed * 1_000_000.0) else 0.0
                        _state.value = SpeedTestState.TestingUpload(offset.toFloat() / uploadDataSize, currentMbps)
                    }
                    val totalTime = (System.currentTimeMillis() - uploadStartTime) / 1000.0
                    finalUploadMbps = (uploadDataSize * 8.0) / (totalTime * 1_000_000.0)
                    out.close()
                    conn.disconnect()
                } catch (e: Exception) {
                    _state.value = SpeedTestState.Error("Upload failed: ${e.message}")
                    return@launch
                }

                val result = SpeedTestResult(
                    downloadMbps = finalDownloadMbps,
                    uploadMbps = finalUploadMbps,
                    pingMs = avgPing,
                    jitterMs = jitter,
                    testServer = "Cloudflare",
                    timestamp = System.currentTimeMillis(),
                    isp = null
                )
                
                speedTestDao.insert(
                    SpeedTestEntity(
                        downloadMbps = result.downloadMbps,
                        uploadMbps = result.uploadMbps,
                        pingMs = result.pingMs,
                        jitterMs = result.jitterMs,
                        testServer = result.testServer,
                        timestamp = result.timestamp,
                        isp = result.isp
                    )
                )
                speedTestDao.trimHistory()
                
                _state.value = SpeedTestState.Complete(result)
            } catch (e: Exception) {
                _state.value = SpeedTestState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        fun provideFactory(speedTestDao: SpeedTestDao): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpeedTestViewModel(speedTestDao) as T
            }
        }
    }
}
