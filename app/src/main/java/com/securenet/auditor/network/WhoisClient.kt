package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class WhoisClient {
    suspend fun lookup(query: String): String = withContext(Dispatchers.IO) {
        try {
            Socket("whois.iana.org", 43).use { socket ->
                socket.soTimeout = 5000
                val out = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                out.println(query)
                
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                }
                
                // If it points to another whois server, we might need to follow it,
                // but for a simple implementation, returning the IANA response is a start.
                val responseStr = response.toString()
                val referMatch = Regex("refer:\\s+(.+)").find(responseStr)
                if (referMatch != null) {
                    val nextServer = referMatch.groupValues[1].trim()
                    return@withContext lookupFrom(nextServer, query)
                }
                
                responseStr
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun lookupFrom(server: String, query: String): String = withContext(Dispatchers.IO) {
        try {
            Socket(server, 43).use { socket ->
                socket.soTimeout = 5000
                val out = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                out.println(query)
                
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                }
                response.toString()
            }
        } catch (e: Exception) {
            "Error querying $server: ${e.message}"
        }
    }
}
