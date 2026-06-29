package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.net.SocketTimeoutException

class PortKnocker {

    enum class Protocol { TCP, UDP }

    data class KnockStep(
        val port: Int,
        val protocol: Protocol,
        val delayMs: Long = 100
    )

    data class KnockResult(
        val success: Boolean,
        val message: String
    )

    suspend fun executeSequence(
        host: String,
        sequence: List<KnockStep>
    ): KnockResult = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(host)
            
            sequence.forEachIndexed { index, step ->
                try {
                    when (step.protocol) {
                        Protocol.TCP -> {
                            // For TCP knocking, we just try to connect and immediately close
                            // We don't care if it succeeds (usually it won't as the port is closed)
                            try {
                                val socket = Socket()
                                socket.connect(java.net.InetSocketAddress(address, step.port), 200)
                                socket.close()
                            } catch (e: Exception) {
                                // Expected timeout or connection refused
                            }
                        }
                        Protocol.UDP -> {
                            val socket = DatagramSocket()
                            val data = "knock".toByteArray()
                            val packet = DatagramPacket(data, data.size, address, step.port)
                            socket.send(packet)
                            socket.close()
                        }
                    }
                    
                    if (index < sequence.size - 1) {
                        delay(step.delayMs)
                    }
                } catch (e: Exception) {
                    return@withContext KnockResult(false, "Error at step ${index + 1} (Port ${step.port}): ${e.message}")
                }
            }
            
            KnockResult(true, "Sequence sent successfully to $host")
        } catch (e: Exception) {
            KnockResult(false, "Host resolution failed: ${e.message}")
        }
    }
}
