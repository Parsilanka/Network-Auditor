package com.securenet.auditor.data.repository

import com.securenet.auditor.data.db.ScanResultDao
import com.securenet.auditor.data.db.ScanResultEntity
import com.securenet.auditor.domain.model.HostInfo
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val dao: ScanResultDao) {

    fun getAllScansFlow(): Flow<List<ScanResultEntity>> = dao.getAllAsFlow()

    suspend fun saveHosts(hosts: List<HostInfo>) {
        if (hosts.isEmpty()) return

        val allIps = hosts.joinToString(",") { it.ipAddress }
        val totalPorts = hosts.flatMap { it.openPorts }.distinct().joinToString(",")
        val avgResponseTime = if (hosts.isNotEmpty()) hosts.map { it.responseTimeMs }.average().toLong() else 0L
        
        val aggregateEntity = ScanResultEntity(
            ipAddress = allIps,
            hostCount = hosts.size,
            macAddress = null,
            vendor = null,
            openPorts = totalPorts,
            responseTimeMs = avgResponseTime,
            hostname = "Multiple Hosts",
            timestamp = System.currentTimeMillis(),
            tag = null
        )
        dao.insert(aggregateEntity)
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun updateTag(id: Long, tag: String) = dao.updateTag(id, tag)

    suspend fun deleteAll() = dao.deleteAll()
}
