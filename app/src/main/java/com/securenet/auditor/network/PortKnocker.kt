package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class PortKnocker {

    data class UIKnockStep(
        val port: String,
        val protocol: String,
        val delayMs: String = "200"
    )

    data class KnockResult(
        val step: Int,
        val port: Int,
        val protocol: String,
        val success: Boolean,
        val timestamp: Long
    )

    fun executeKnockSequence(
        host: String,
        steps: List<UIKnockStep>
    ): Flow<KnockResult> = flow {
        steps.forEachIndexed { index, uiStep ->
            val port = uiStep.port.toIntOrNull() ?: return@forEachIndexed
            val delayValue = uiStep.delayMs.toLongOrNull() ?: 200L
            
            val success = when (uiStep.protocol) {
                "TCP" -> knockTcp(host, port)
                "UDP" -> knockUdp(host, port)
                else -> false
            }
            
            emit(KnockResult(
                step = index + 1,
                port = port,
                protocol = uiStep.protocol,
                success = success,
                timestamp = System.currentTimeMillis()
            ))
            
            if (index < steps.size - 1) {
                delay(delayValue)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun knockTcp(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(host, port), 1000)
            }
            true
        } catch (e: Exception) {
            // Connection refused is actually expected
            // for a knock - the port doesn't need to 
            // accept, just receive the SYN packet
            true
        }
    }

    private fun knockUdp(host: String, port: Int): Boolean {
        return try {
            DatagramSocket().use { socket ->
                val data = ByteArray(1)
                val packet = DatagramPacket(
                    data, data.size,
                    InetAddress.getByName(host), port)
                socket.send(packet)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
