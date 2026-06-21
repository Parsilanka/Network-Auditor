package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.InetSocketAddress
import java.net.Socket

class PortScanner {
    val defaultPorts = listOf(
        21, 22, 23, 25, 53, 80, 110, 143,
        443, 445, 3306, 3389, 5900, 8080, 8443
    )

    suspend fun scanPorts(ip: String, ports: List<Int> = defaultPorts): List<Int> = coroutineScope {
        ports.map { port ->
            async(Dispatchers.IO) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), 300)
                        port
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }
}
