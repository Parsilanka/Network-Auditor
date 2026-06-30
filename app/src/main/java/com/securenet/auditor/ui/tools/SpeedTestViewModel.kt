package com.securenet.auditor.ui.tools

import android.os.Environment
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
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

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

                // Step 1: Ping test
                val pingResults = mutableListOf<Long>()
                repeat(5) {
                    val start = System.currentTimeMillis()
                    try {
                        val conn = URL("https://www.cloudflare.com").openConnection() as HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.connect()
                        val time = System.currentTimeMillis() - start
                        if (time in 1..5000) {
                            pingResults.add(time)
                        }
                        conn.disconnect()
                    } catch (e: Exception) {
                        pingResults.add(3000L)
                    }
                    delay(100)
                }
                
                val avgPing = if (pingResults.isNotEmpty()) pingResults.average().toLong() else 0L
                val jitter = calculateJitter(pingResults).toDouble()
                _state.value = SpeedTestState.TestingPing(avgPing)

                delay(500)

                // Step 2: Download test
                val finalDownloadMbps = testDownloadSpeed()
                if (finalDownloadMbps == -1.0) {
                    _state.value = SpeedTestState.Error("Download failed")
                    return@launch
                }

                delay(500)

                // Step 3: Upload test
                val finalUploadMbps = testUploadSpeed()
                if (finalUploadMbps == -1.0) {
                    _state.value = SpeedTestState.Error("Upload failed")
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

    private suspend fun testDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            val testUrl = "https://speed.cloudflare.com/__down?bytes=10000000"
            val connection = URL(testUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            val startTime = System.nanoTime()
            var totalBytes = 0L
            
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    val currentTime = System.nanoTime()
                    val elapsedSeconds = (currentTime - startTime) / 1_000_000_000.0
                    val currentMbps = if (elapsedSeconds > 0) (totalBytes * 8.0) / (elapsedSeconds * 1_000_000.0) else 0.0
                    _state.value = SpeedTestState.TestingDownload(minOf(totalBytes / 10_000_000f, 1f), currentMbps.coerceIn(0.0, 1000.0))
                }
            }
            
            val endTime = System.nanoTime()
            connection.disconnect()
            
            val elapsedSeconds = (endTime - startTime) / 1_000_000_000.0
            if (elapsedSeconds < 0.05 || totalBytes == 0L) return@withContext 0.0
            
            val mbps = (totalBytes * 8.0 / elapsedSeconds) / 1_000_000.0
            mbps.coerceIn(0.0, 1000.0)
        } catch (e: Exception) {
            -1.0
        }
    }

    private suspend fun testUploadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            val testData = ByteArray(2_000_000)
            Random.nextBytes(testData)
            
            val url = URL("https://speed.cloudflare.com/__up")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(testData.size)
            
            val startTime = System.nanoTime()
            
            connection.outputStream.use { os ->
                val chunkSize = 8192
                var uploaded = 0
                while (uploaded < testData.size) {
                    val length = minOf(chunkSize, testData.size - uploaded)
                    os.write(testData, uploaded, length)
                    uploaded += length
                    val currentTime = System.nanoTime()
                    val elapsedSeconds = (currentTime - startTime) / 1_000_000_000.0
                    val currentMbps = if (elapsedSeconds > 0) (uploaded * 8.0) / (elapsedSeconds * 1_000_000.0) else 0.0
                    _state.value = SpeedTestState.TestingUpload(uploaded.toFloat() / testData.size, currentMbps.coerceIn(0.0, 1000.0))
                }
                os.flush()
            }
            
            val responseCode = connection.responseCode
            val endTime = System.nanoTime()
            connection.disconnect()
            
            val elapsedSeconds = (endTime - startTime) / 1_000_000_000.0
            if (elapsedSeconds < 0.05) return@withContext 0.0
            
            val mbps = (testData.size * 8.0 / elapsedSeconds) / 1_000_000.0
            mbps.coerceIn(0.0, 1000.0)
        } catch (e: Exception) {
            -1.0
        }
    }

    private fun calculateJitter(pingResults: List<Long>): Long {
        if (pingResults.size < 2) return 0L
        val differences = pingResults.zipWithNext { a, b -> abs(b - a) }
        val avgJitter = differences.average().toLong()
        return avgJitter.coerceIn(0L, 500L)
    }

    fun clearHistory() {
        viewModelScope.launch {
            speedTestDao.deleteAll()
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            speedTestDao.deleteById(id)
        }
    }

    fun exportHistoryToCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            val historyList = history.value
            val csv = buildString {
                appendLine("Date,Server,Ping_ms,Download_Mbps,Upload_Mbps,Jitter_ms")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                historyList.forEach { test ->
                    appendLine("${dateFormat.format(Date(test.timestamp))},${test.testServer},${test.pingMs},${test.downloadMbps},${test.uploadMbps},${test.jitterMs}")
                }
            }
            val fileName = "speedtest_history_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.csv"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            try {
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                File(downloadsDir, fileName).writeText(csv)
            } catch (e: Exception) {
                e.printStackTrace()
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
