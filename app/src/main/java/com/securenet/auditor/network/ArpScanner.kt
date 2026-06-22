package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ArpScanner {
    suspend fun getArpTable(): Map<String, String> = withContext(Dispatchers.IO) {
        val arpTable = mutableMapOf<String, String>()
        try {
            val process = Runtime.getRuntime().exec("ip neighbor show") // Modern Android uses ip neighbor
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Example line: 192.168.1.1 dev wlan0 lladdr 00:11:22:33:44:55 REACHABLE
                val parts = line!!.split("\\s+".toRegex())
                if (parts.size >= 5 && parts.contains("lladdr")) {
                    val ip = parts[0]
                    val macIndex = parts.indexOf("lladdr") + 1
                    if (macIndex < parts.size) {
                        val mac = parts[macIndex]
                        if (mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex())) {
                            arpTable[ip] = mac
                        }
                    }
                }
            }
            
            if (arpTable.isEmpty()) {
                // Fallback to /proc/net/arp for older versions
                val procReader = BufferedReader(InputStreamReader(Runtime.getRuntime().exec("cat /proc/net/arp").inputStream))
                procReader.readLine() // skip header
                while (procReader.readLine().also { line = it } != null) {
                    val parts = line!!.split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00") {
                            arpTable[ip] = mac
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        arpTable
    }
}
