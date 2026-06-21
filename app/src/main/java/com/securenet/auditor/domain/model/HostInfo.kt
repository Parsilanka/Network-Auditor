package com.securenet.auditor.domain.model

data class HostInfo(
    val ipAddress: String,
    val hostname: String?,
    val macAddress: String?,
    val vendor: String?,
    val openPorts: List<Int>,
    val isReachable: Boolean,
    val responseTimeMs: Long
)
