package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

class PortScanner {
    val defaultPorts = listOf(
        21, 22, 23, 25, 53, 80, 110, 143,
        443, 445, 3306, 3389, 5900, 8080, 8443
    )

    data class ScanResult(val port: Int, val banner: String?)

    suspend fun scanPortsWithBanners(ip: String, ports: List<Int> = defaultPorts): List<ScanResult> = coroutineScope {
        ports.map { port ->
            async(Dispatchers.IO) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), 400)
                        val banner = if (isBannerPort(port)) {
                            grabBanner(socket)
                        } else {
                            null
                        }
                        ScanResult(port, banner)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun isBannerPort(port: Int): Boolean {
        // Ports that typically send a banner or respond to a simple request
        return port in listOf(21, 22, 23, 25, 80, 110, 143, 8080)
    }

    private suspend fun grabBanner(socket: Socket): String? = withTimeoutOrNull(1000) {
        try {
            socket.soTimeout = 1000
            val inputStream = socket.getInputStream()
            
            // For HTTP ports, we need to send a request
            val port = socket.port
            if (port == 80 || port == 8080) {
                socket.getOutputStream().write("HEAD / HTTP/1.0\r\n\r\n".toByteArray())
            }

            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead != -1) {
                val raw = String(buffer, 0, bytesRead).trim()
                // Extract Server header for HTTP
                if (port == 80 || port == 8080) {
                    raw.lines().firstOrNull { it.startsWith("Server:", ignoreCase = true) }?.substringAfter(":")?.trim()
                } else {
                    raw.lines().first().take(100)
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Keep legacy method for compatibility
    suspend fun scanPorts(ip: String, ports: List<Int> = defaultPorts): List<Int> {
        return scanPortsWithBanners(ip, ports).map { it.port }
    }
}
