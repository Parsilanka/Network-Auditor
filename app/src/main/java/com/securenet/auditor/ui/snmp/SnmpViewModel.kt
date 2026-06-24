package com.securenet.auditor.ui.snmp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.securenet.auditor.domain.model.SnmpDeviceInfo
import com.securenet.auditor.domain.model.SnmpDeviceType
import com.securenet.auditor.network.snmp.SnmpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SnmpViewModel(
    private val snmpClient: SnmpClient
) : ViewModel() {

    private val _deviceInfo = MutableStateFlow<SnmpScanState>(
        SnmpScanState.Idle)
    val deviceInfo: StateFlow<SnmpScanState> = 
        _deviceInfo.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scannedDevices = MutableStateFlow<List<SnmpDeviceInfo>>(
        emptyList())
    val scannedDevices: StateFlow<List<SnmpDeviceInfo>> = 
        _scannedDevices.asStateFlow()
    
    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()

    fun scanDevice(
        host: String, 
        community: String = "public"
    ) {
        viewModelScope.launch {
            _deviceInfo.value = SnmpScanState.Loading
            _isScanning.value = true
            _progress.value = "Connecting to $host..."
            
            val startTime = System.currentTimeMillis()
            
            val oidsToQuery = mapOf(
                "sysDescr" to SnmpClient.OID_SYSTEM_DESCR,
                "sysUptime" to SnmpClient.OID_SYSTEM_UPTIME,
                "sysName" to SnmpClient.OID_SYSTEM_NAME,
                "sysLocation" to SnmpClient.OID_SYSTEM_LOCATION,
                "sysContact" to SnmpClient.OID_SYSTEM_CONTACT,
                "ifNumber" to SnmpClient.OID_IF_NUMBER,
                "ifInOctets" to SnmpClient.OID_IF_IN_OCTETS,
                "ifOutOctets" to SnmpClient.OID_IF_OUT_OCTETS,
                "tcpCurrEstab" to SnmpClient.OID_TCP_CURR_ESTAB,
                "totalMem" to SnmpClient.OID_TOTAL_MEM,
                "freeMem" to SnmpClient.OID_FREE_MEM,
                "cpuLoad" to SnmpClient.OID_CPU_LOAD_1MIN,
                "storageSize" to SnmpClient.OID_STORAGE_SIZE,
                "storageUsed" to SnmpClient.OID_STORAGE_USED
            )
            
            _progress.value = "Querying SNMP OIDs..."
            val results = snmpClient.getMultipleOids(
                host, community, oidsToQuery)
            
            val scanTime = System.currentTimeMillis() - startTime
            _progress.value = "Processing results..."
            
            val totalMem = results["totalMem"]?.toLongOrNull()
            val freeMem = results["freeMem"]?.toLongOrNull()
            val memUsage = if (totalMem != null && freeMem != null 
                && totalMem > 0) {
                ((totalMem - freeMem) * 100 / totalMem).toInt()
            } else null
            
            val storSize = results["storageSize"]?.toLongOrNull()
            val storUsed = results["storageUsed"]?.toLongOrNull()
            val storUsage = if (storSize != null && storUsed != null
                && storSize > 0) {
                (storUsed * 100 / storSize).toInt()
            } else null
            
            val sysDescr = results["sysDescr"] ?: ""
            val deviceType = when {
                sysDescr.contains("Router", true) ||
                sysDescr.contains("Cisco", true) ||
                sysDescr.contains("Juniper", true) -> 
                    SnmpDeviceType.ROUTER
                sysDescr.contains("Switch", true) -> 
                    SnmpDeviceType.SWITCH
                sysDescr.contains("Linux", true) -> 
                    SnmpDeviceType.LINUX_HOST
                sysDescr.contains("Windows", true) -> 
                    SnmpDeviceType.WINDOWS_HOST
                sysDescr.contains("printer", true) ||
                sysDescr.contains("HP", true) -> 
                    SnmpDeviceType.PRINTER
                sysDescr.contains("Server", true) -> 
                    SnmpDeviceType.SERVER
                else -> SnmpDeviceType.UNKNOWN
            }
            
            val isReachable = results.values.any { it != null }
            
            if (!isReachable) {
                _deviceInfo.value = SnmpScanState.Error(
                    "SNMP not responding on $host. " +
                    "Ensure SNMP is enabled and community " +
                    "'$community' is correct.")
                _isScanning.value = false
                return@launch
            }
            
            val info = SnmpDeviceInfo(
                ipAddress = host,
                community = community,
                systemDescription = results["sysDescr"],
                systemName = results["sysName"],
                systemLocation = results["sysLocation"],
                systemContact = results["sysContact"],
                uptime = results["sysUptime"],
                interfaceCount = results["ifNumber"],
                inboundTraffic = results["ifInOctets"],
                outboundTraffic = results["ifOutOctets"],
                tcpConnections = results["tcpCurrEstab"],
                totalMemoryKb = totalMem,
                freeMemoryKb = freeMem,
                memoryUsagePercent = memUsage,
                cpuLoad1Min = results["cpuLoad"],
                storageSize = results["storageSize"],
                storageUsed = results["storageUsed"],
                storageUsagePercent = storUsage,
                isReachable = isReachable,
                scanTimeMs = scanTime,
                deviceType = deviceType
            )
            
            val currentList = _scannedDevices.value.toMutableList()
            currentList.removeAll { it.ipAddress == host }
            currentList.add(0, info)
            _scannedDevices.value = currentList
            
            _deviceInfo.value = SnmpScanState.Success(info)
            _isScanning.value = false
        }
    }
    
    fun clearResult() { _deviceInfo.value = SnmpScanState.Idle }

    companion object {
        fun factory(snmpClient: SnmpClient) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return SnmpViewModel(snmpClient) as T
                }
            }
    }
}

sealed class SnmpScanState {
    object Idle : SnmpScanState()
    object Loading : SnmpScanState()
    data class Success(val data: SnmpDeviceInfo) : SnmpScanState()
    data class Error(val message: String) : SnmpScanState()
}
