package com.securenet.auditor.network.snmp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SnmpClient {
    
    companion object {
        const val SNMP_PORT = 161
        const val DEFAULT_TIMEOUT = 3000
        const val DEFAULT_RETRIES = 2
        
        // Standard SNMP OIDs for common device info
        val OID_SYSTEM_DESCR = "1.3.6.1.2.1.1.1.0"
        val OID_SYSTEM_UPTIME = "1.3.6.1.2.1.1.3.0"
        val OID_SYSTEM_NAME = "1.3.6.1.2.1.1.5.0"
        val OID_SYSTEM_LOCATION = "1.3.6.1.2.1.1.6.0"
        val OID_SYSTEM_CONTACT = "1.3.6.1.2.1.1.4.0"
        val OID_IF_NUMBER = "1.3.6.1.2.1.2.1.0"
        val OID_IF_IN_OCTETS = "1.3.6.1.2.1.2.2.1.10.1"
        val OID_IF_OUT_OCTETS = "1.3.6.1.2.1.2.2.1.16.1"
        val OID_TCP_CURR_ESTAB = "1.3.6.1.2.1.6.9.0"
        val OID_TOTAL_MEM = "1.3.6.1.4.1.2021.4.5.0"
        val OID_FREE_MEM = "1.3.6.1.4.1.2021.4.11.0"
        val OID_CPU_LOAD_1MIN = "1.3.6.1.4.1.2021.10.1.3.1"
        val OID_STORAGE_SIZE = "1.3.6.1.2.1.25.2.3.1.5.1"
        val OID_STORAGE_USED = "1.3.6.1.2.1.25.2.3.1.6.1"
    }
    
    // Build SNMP v1 GET request packet manually
    private fun buildSnmpGetRequest(
        community: String,
        oid: String,
        requestId: Int
    ): ByteArray {
        // Encode OID to BER format
        fun encodeOid(oid: String): ByteArray {
            val parts = oid.split(".").map { it.toInt() }
            val encoded = mutableListOf<Byte>()
            encoded.add(((parts[0] * 40) + parts[1]).toByte())
            for (i in 2 until parts.size) {
                val value = parts[i]
                if (value < 128) {
                    encoded.add(value.toByte())
                } else {
                    val bytes = mutableListOf<Byte>()
                    var v = value
                    bytes.add(0, (v and 0x7F).toByte())
                    v = v shr 7
                    while (v > 0) {
                        bytes.add(0, ((v and 0x7F) or 0x80).toByte())
                        v = v shr 7
                    }
                    encoded.addAll(bytes)
                }
            }
            return encoded.toByteArray()
        }
        
        fun tlv(tag: Byte, value: ByteArray): ByteArray {
            return byteArrayOf(tag, value.size.toByte()) + value
        }
        
        fun integer(value: Int): ByteArray {
            return tlv(0x02, byteArrayOf(
                (value shr 24).toByte(),
                (value shr 16).toByte(),
                (value shr 8).toByte(),
                value.toByte()
            ))
        }
        
        fun octetString(value: String): ByteArray {
            return tlv(0x04, value.toByteArray())
        }
        
        val encodedOid = encodeOid(oid)
        val oidTlv = tlv(0x06, encodedOid)
        val nullValue = byteArrayOf(0x05, 0x00)
        val varBind = tlv(0x30, oidTlv + nullValue)
        val varBindList = tlv(0x30, varBind)
        
        val pdu = tlv(
            0xA0.toByte(),  // GET-REQUEST PDU type
            integer(requestId) +
            integer(0) +    // error status
            integer(0) +    // error index
            varBindList
        )
        
        val message = tlv(
            0x30,
            integer(0) +    // SNMP version 1 = 0
            octetString(community) +
            pdu
        )
        
        return message
    }
    
    // Parse SNMP response and extract value string
    private fun parseSnmpResponse(data: ByteArray): String? {
        return try {
            var pos = 0
            
            fun readLength(): Int {
                val len = data[pos++].toInt() and 0xFF
                return if (len < 0x80) len
                else {
                    var result = 0
                    repeat(len and 0x7F) { 
                        result = (result shl 8) or 
                            (data[pos++].toInt() and 0xFF) 
                    }
                    result
                }
            }
            
            fun skipTlv() {
                pos++ // skip tag
                val len = readLength()
                pos += len
            }
            
            // SEQUENCE wrapper
            pos++; readLength()
            // Version
            skipTlv()
            // Community string
            skipTlv()
            // GET-RESPONSE PDU
            pos++; readLength()
            // Request ID, error status, error index
            skipTlv(); skipTlv(); skipTlv()
            // VarBindList SEQUENCE
            pos++; readLength()
            // First VarBind SEQUENCE  
            pos++; readLength()
            // OID - skip it
            skipTlv()
            
            // Now we're at the value
            val valueTag = data[pos++].toInt() and 0xFF
            val valueLen = readLength()
            val valueBytes = data.copyOfRange(pos, pos + valueLen)
            
            when (valueTag) {
                0x02 -> { // INTEGER
                    var intVal = 0L
                    valueBytes.forEach { b ->
                        intVal = (intVal shl 8) or 
                            (b.toLong() and 0xFF)
                    }
                    intVal.toString()
                }
                0x04 -> String(valueBytes) // OCTET STRING
                0x43 -> { // TimeTicks
                    var ticks = 0L
                    valueBytes.forEach { b ->
                        ticks = (ticks shl 8) or 
                            (b.toLong() and 0xFF)
                    }
                    val totalSeconds = ticks / 100
                    val days = totalSeconds / 86400
                    val hours = (totalSeconds % 86400) / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    "${days}d ${hours}h ${minutes}m"
                }
                0x41 -> { // Counter32
                    var counter = 0L
                    valueBytes.forEach { b ->
                        counter = (counter shl 8) or 
                            (b.toLong() and 0xFF)
                    }
                    counter.toString()
                }
                else -> valueBytes.toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getOid(
        host: String,
        community: String,
        oid: String,
        timeoutMs: Int = DEFAULT_TIMEOUT
    ): String? = withContext(Dispatchers.IO) {
        repeat(DEFAULT_RETRIES) { attempt ->
            try {
                val socket = DatagramSocket()
                socket.soTimeout = timeoutMs
                
                val requestId = (Math.random() * Int.MAX_VALUE).toInt()
                val packet = buildSnmpGetRequest(community, oid, requestId)
                
                val address = InetAddress.getByName(host)
                val sendPacket = DatagramPacket(
                    packet, packet.size, address, SNMP_PORT)
                socket.send(sendPacket)
                
                val buffer = ByteArray(4096)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)
                socket.close()
                
                val response = buffer.copyOf(receivePacket.length)
                return@withContext parseSnmpResponse(response)
            } catch (e: Exception) {
                if (attempt == DEFAULT_RETRIES - 1) return@withContext null
            }
        }
        null
    }
    
    // Get multiple OIDs efficiently
    suspend fun getMultipleOids(
        host: String,
        community: String,
        oids: Map<String, String>
    ): Map<String, String?> {
        val results = mutableMapOf<String, String?>()
        oids.forEach { (name, oid) ->
            results[name] = getOid(host, community, oid)
            delay(50) // Small delay between requests
        }
        return results
    }
}
