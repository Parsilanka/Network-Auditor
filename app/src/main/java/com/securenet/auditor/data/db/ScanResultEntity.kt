package com.securenet.auditor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ipAddress: String,        // Now stores comma-separated list of all IPs in the scan
    val hostCount: Int,           // Total number of hosts discovered
    val macAddress: String?,      // Restricted on Android, might be null
    val vendor: String?,
    val openPorts: String,        // Aggregated ports or summary
    val responseTimeMs: Long,     // Average response time for the scan session
    val hostname: String?,
    val timestamp: Long,
    val tag: String?
)
