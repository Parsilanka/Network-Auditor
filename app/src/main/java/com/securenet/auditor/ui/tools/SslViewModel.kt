package com.securenet.auditor.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class SslInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validUntil: String,
    val algorithm: String,
    val serialNumber: String,
    val version: Int,
    val isExpired: Boolean
)

class SslViewModel : ViewModel() {

    private val _sslInfo = MutableStateFlow<SslInfo?>(null)
    val sslInfo: StateFlow<SslInfo?> = _sslInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun inspectCertificate(domain: String) {
        if (domain.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _sslInfo.value = null
            
            try {
                val info = withContext(Dispatchers.IO) {
                    val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    val socket = factory.createSocket() as SSLSocket
                    socket.connect(InetSocketAddress(domain, 443), 5000)
                    socket.startHandshake()
                    
                    val session = socket.session
                    val certs = session.peerCertificates
                    if (certs.isEmpty()) throw Exception("No certificates found")
                    
                    val x509 = certs[0] as X509Certificate
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    
                    SslInfo(
                        subject = x509.subjectX500Principal.name,
                        issuer = x509.issuerX500Principal.name,
                        validFrom = dateFormat.format(x509.notBefore),
                        validUntil = dateFormat.format(x509.notAfter),
                        algorithm = x509.sigAlgName,
                        serialNumber = x509.serialNumber.toString(16).uppercase(),
                        version = x509.version,
                        isExpired = Date().after(x509.notAfter) || Date().before(x509.notBefore)
                    ).also {
                        socket.close()
                    }
                }
                _sslInfo.value = info
            } catch (e: Exception) {
                _error.value = "Failed to connect: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SslViewModel() as T
            }
        }
    }
}
