package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

class SubnetCalculatorViewModel : ViewModel() {

    data class SubnetResult(
        val ip: String,
        val mask: String,
        val cidr: Int,
        val networkAddress: String,
        val broadcastAddress: String,
        val hostRange: String,
        val totalHosts: Long,
        val usableHosts: Long,
        val wildcardMask: String,
        val binaryMask: String
    )

    private val _result = MutableStateFlow<SubnetResult?>(null)
    val result: StateFlow<SubnetResult?> = _result.asStateFlow()

    fun calculate(ip: String, maskOrCidr: String) {
        try {
            val ipInt = ipToLong(ip)
            val cidr = if (maskOrCidr.contains(".")) {
                maskToCidr(maskOrCidr)
            } else {
                maskOrCidr.toIntOrNull() ?: 24
            }

            if (cidr !in 0..32) throw Exception("Invalid CIDR")

            val maskInt = (0xFFFFFFFFL shl (32 - cidr)) and 0xFFFFFFFFL
            val networkInt = ipInt and maskInt
            val broadcastInt = networkInt or (maskInt xor 0xFFFFFFFFL)
            
            val totalHosts = 2.0.pow(32 - cidr).toLong()
            val usableHosts = if (cidr < 31) totalHosts - 2 else if (cidr == 31) 2 else 1

            val firstHost = if (cidr < 31) networkInt + 1 else networkInt
            val lastHost = if (cidr < 31) broadcastInt - 1 else broadcastInt

            val wildcard = maskInt xor 0xFFFFFFFFL

            _result.value = SubnetResult(
                ip = ip,
                mask = longToIp(maskInt),
                cidr = cidr,
                networkAddress = longToIp(networkInt),
                broadcastAddress = longToIp(broadcastInt),
                hostRange = "${longToIp(firstHost)} - ${longToIp(lastHost)}",
                totalHosts = totalHosts,
                usableHosts = usableHosts,
                wildcardMask = longToIp(wildcard),
                binaryMask = maskInt.toString(2).padStart(32, '0').chunked(8).joinToString(".")
            )
        } catch (e: Exception) {
            _result.value = null
        }
    }

    private fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) throw Exception("Invalid IP")
        return parts.map { it.toLong() }.fold(0L) { acc, part -> (acc shl 8) + part }
    }

    private fun longToIp(long: Long): String {
        return "${(long shr 24) and 0xFF}.${(long shr 16) and 0xFF}.${(long shr 8) and 0xFF}.${long and 0xFF}"
    }

    private fun maskToCidr(mask: String): Int {
        val maskInt = ipToLong(mask)
        var count = 0
        var m = maskInt
        while (m and 0x80000000L != 0L) {
            count++
            m = (m shl 1) and 0xFFFFFFFFL
        }
        return count
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SubnetCalculatorViewModel() as T
            }
        }
    }
}
