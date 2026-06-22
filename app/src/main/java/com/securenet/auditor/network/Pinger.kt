package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class Pinger {
    suspend fun ping(host: String, timeout: Int = 1000): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        return@withContext try {
            if (InetAddress.getByName(host).isReachable(timeout)) {
                System.currentTimeMillis() - start
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }
}
