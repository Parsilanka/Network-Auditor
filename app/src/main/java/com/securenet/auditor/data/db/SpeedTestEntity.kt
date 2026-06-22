package com.securenet.auditor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_results")
data class SpeedTestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Long,
    val jitterMs: Double,
    val testServer: String,
    val timestamp: Long,
    val isp: String?
)
