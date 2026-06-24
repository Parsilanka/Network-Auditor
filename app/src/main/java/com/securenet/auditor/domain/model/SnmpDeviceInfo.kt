package com.securenet.auditor.domain.model

data class SnmpDeviceInfo(
    val ipAddress: String,
    val community: String,
    val systemDescription: String?,
    val systemName: String?,
    val systemLocation: String?,
    val systemContact: String?,
    val uptime: String?,
    val interfaceCount: String?,
    val inboundTraffic: String?,
    val outboundTraffic: String?,
    val tcpConnections: String?,
    val totalMemoryKb: Long?,
    val freeMemoryKb: Long?,
    val memoryUsagePercent: Int?,
    val cpuLoad1Min: String?,
    val storageSize: String?,
    val storageUsed: String?,
    val storageUsagePercent: Int?,
    val isReachable: Boolean,
    val scanTimeMs: Long,
    val deviceType: SnmpDeviceType
)

enum class SnmpDeviceType {
    ROUTER, SWITCH, SERVER, LINUX_HOST,
    WINDOWS_HOST, PRINTER, UNKNOWN
}
