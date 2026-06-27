package com.securenet.auditor.network

import android.net.TrafficStats

class BandwidthMonitor {

    data class BandwidthSnapshot(
        val timestamp: Long,
        val downloadBytesPerSec: Long,
        val uploadBytesPerSec: Long,
        val totalDownloadMb: Float,
        val totalUploadMb: Float,
        val downloadFormatted: String,
        val uploadFormatted: String
    )

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTimestamp = 0L

    fun getCurrentSnapshot(): BandwidthSnapshot {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDelta = (currentTime - lastTimestamp)
            .coerceAtLeast(1L)
        val rxDelta = (currentRx - lastRxBytes)
            .coerceAtLeast(0L)
        val txDelta = (currentTx - lastTxBytes)
            .coerceAtLeast(0L)

        val downloadBps = (rxDelta * 1000L) / timeDelta
        val uploadBps = (txDelta * 1000L) / timeDelta

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTimestamp = currentTime

        return BandwidthSnapshot(
            timestamp = currentTime,
            downloadBytesPerSec = downloadBps,
            uploadBytesPerSec = uploadBps,
            totalDownloadMb = currentRx / 1048576f,
            totalUploadMb = currentTx / 1048576f,
            downloadFormatted = formatSpeed(downloadBps),
            uploadFormatted = formatSpeed(uploadBps)
        )
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec < 1024 -> "${bytesPerSec} B/s"
            bytesPerSec < 1048576 ->
                "${"%.1f".format(bytesPerSec/1024f)} KB/s"
            bytesPerSec < 1073741824 ->
                "${"%.2f".format(bytesPerSec/1048576f)} MB/s"
            else ->
                "${"%.2f".format(
                    bytesPerSec/1073741824f)} GB/s"
        }
    }
}
