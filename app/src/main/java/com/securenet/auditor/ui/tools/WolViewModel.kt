package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WolViewModel : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun sendMagicPacket(macAddress: String, ipAddress: String = "255.255.255.255") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val macBytes = getMacBytes(macAddress)
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0..5) {
                    bytes[i] = 0xff.toByte()
                }
                for (i in 6 until bytes.size step macBytes.size) {
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                }

                val address = InetAddress.getByName(ipAddress)
                val packet = DatagramPacket(bytes, bytes.size, address, 9)
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.send(packet)
                }
                _status.value = "Magic Packet sent to $macAddress"
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
            }
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.split(":", "-")
        if (hex.size != 6) throw IllegalArgumentException("Invalid MAC address")
        try {
            for (i in 0..5) {
                bytes[i] = hex[i].toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid hex in MAC address")
        }
        return bytes
    }

    fun clearStatus() {
        _status.value = null
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WolViewModel() as T
            }
        }
    }
}
