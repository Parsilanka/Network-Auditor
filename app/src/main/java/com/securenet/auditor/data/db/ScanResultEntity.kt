package com.securenet.auditor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ipAddress: String,        // Stores comma-separated list of all IPs
    val hostCount: Int,           
    val macAddress: String?,      
    val vendor: String?,
    val openPorts: String,        
    val responseTimeMs: Long,     
    val hostname: String?,
    val timestamp: Long,
    val tag: String?,
    val detailedHostsJson: String? = null // New field to store full HostInfo list as JSON
)
