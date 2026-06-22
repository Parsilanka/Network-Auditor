package com.securenet.auditor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arp_records")
data class ArpEntity(
    @PrimaryKey val ipAddress: String,
    val macAddress: String,
    val lastUpdated: Long
)
