package com.securenet.auditor.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

class SslTlsScanner {

    enum class RiskLevel { INFO, LOW, MEDIUM, HIGH, CRITICAL }

    data class SslScanResult(
        val host: String,
        val port: Int,
        val grade: SslGrade,
        val certificate: CertificateInfo,
        val supportedProtocols: List<ProtocolResult>,
        val vulnerabilities: List<SslVulnerability>,
        val securityHeaders: Map<String, String?>,
        val scanTimeMs: Long
    )

    data class CertificateInfo(
        val subject: String,
        val issuer: String,
        val validFrom: Date,
        val validUntil: Date,
        val daysUntilExpiry: Long,
        val signatureAlgorithm: String,
        val keySize: Int,
        val serialNumber: String,
        val subjectAltNames: List<String>,
        val isSelfSigned: Boolean,
        val isTrusted: Boolean,
        val isExpired: Boolean,
        val isWildcard: Boolean
    )

    data class ProtocolResult(
        val protocol: String,
        val version: String,
        val isSupported: Boolean,
        val isDeprecated: Boolean,
        val riskLevel: RiskLevel
    )

    data class SslVulnerability(
        val name: String,
        val description: String,
        val severity: RiskLevel,
        val cveId: String?,
        val isVulnerable: Boolean,
        val recommendation: String
    )

    enum class SslGrade { A_PLUS, A, B, C, D, F, T, M }

    suspend fun scan(
        host: String,
        port: Int = 443
    ): SslScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val vulnerabilities = mutableListOf<SslVulnerability>()
        val protocols = mutableListOf<ProtocolResult>()

        // Test each TLS/SSL protocol version
        val protocolsToTest = listOf(
            Triple("SSLv3", "SSLv3", true),
            Triple("TLSv1", "TLS 1.0", true),
            Triple("TLSv1.1", "TLS 1.1", true),
            Triple("TLSv1.2", "TLS 1.2", false),
            Triple("TLSv1.3", "TLS 1.3", false)
        )

        protocolsToTest.forEach { (javaName, displayName, isDeprecated) ->
            val supported = testProtocol(host, port, javaName)
            protocols.add(ProtocolResult(
                protocol = javaName,
                version = displayName,
                isSupported = supported,
                isDeprecated = isDeprecated,
                riskLevel = when {
                    isDeprecated && supported -> RiskLevel.CRITICAL
                    else -> RiskLevel.INFO
                }
            ))
        }

        // Check for deprecated protocol vulnerabilities
        if (protocols.find { it.protocol == "SSLv3" }?.isSupported == true) {
            vulnerabilities.add(SslVulnerability(
                name = "POODLE Attack",
                description = "SSLv3 is enabled. Vulnerable to POODLE attack which allows decryption of secure connections.",
                severity = RiskLevel.CRITICAL,
                cveId = "CVE-2014-3566",
                isVulnerable = true,
                recommendation = "Disable SSLv3 immediately"
            ))
        }

        if (protocols.find { it.protocol == "TLSv1" }?.isSupported == true) {
            vulnerabilities.add(SslVulnerability(
                name = "TLS 1.0 Deprecated",
                description = "TLS 1.0 is deprecated since 2021 and contains known weaknesses (BEAST attack).",
                severity = RiskLevel.HIGH,
                cveId = "CVE-2011-3389",
                isVulnerable = true,
                recommendation = "Disable TLS 1.0 and 1.1"
            ))
        }

        // Get certificate details
        val certInfo = getCertificateInfo(host, port)

        // Check certificate vulnerabilities
        if (certInfo.isSelfSigned) {
            vulnerabilities.add(SslVulnerability(
                name = "Self-Signed Certificate",
                description = "Certificate is not signed by a trusted Certificate Authority.",
                severity = RiskLevel.HIGH,
                cveId = null,
                isVulnerable = true,
                recommendation = "Obtain a certificate from a trusted CA (Let's Encrypt is free)"
            ))
        }

        if (certInfo.isExpired) {
            vulnerabilities.add(SslVulnerability(
                name = "Expired Certificate",
                description = "SSL certificate has expired. Connections may be rejected.",
                severity = RiskLevel.CRITICAL,
                cveId = null,
                isVulnerable = true,
                recommendation = "Renew certificate immediately"
            ))
        }

        if (certInfo.daysUntilExpiry in 1..29) {
            vulnerabilities.add(SslVulnerability(
                name = "Certificate Expiring Soon",
                description = "Certificate expires in ${certInfo.daysUntilExpiry} days.",
                severity = RiskLevel.MEDIUM,
                cveId = null,
                isVulnerable = true,
                recommendation = "Renew certificate before it expires"
            ))
        }

        if (certInfo.keySize < 2048 && certInfo.keySize > 0) {
            vulnerabilities.add(SslVulnerability(
                name = "Weak Key Size",
                description = "RSA key size ${certInfo.keySize} bits is below recommended 2048 bits.",
                severity = RiskLevel.HIGH,
                cveId = null,
                isVulnerable = true,
                recommendation = "Use minimum 2048-bit RSA key or switch to ECDSA"
            ))
        }

        // Get security headers
        val securityHeaders = getSecurityHeaders(host, port)

        // Check missing security headers
        val importantHeaders = listOf(
            "Strict-Transport-Security",
            "Content-Security-Policy",
            "X-Frame-Options",
            "X-Content-Type-Options",
            "Referrer-Policy"
        )

        importantHeaders.forEach { header ->
            if (securityHeaders[header] == null) {
                vulnerabilities.add(SslVulnerability(
                    name = "Missing $header",
                    description = "Security header $header is not set.",
                    severity = when (header) {
                        "Strict-Transport-Security" -> RiskLevel.HIGH
                        "Content-Security-Policy" -> RiskLevel.MEDIUM
                        else -> RiskLevel.LOW
                    },
                    cveId = null,
                    isVulnerable = true,
                    recommendation = "Add $header to HTTP response headers"
                ))
            }
        }

        // Calculate overall grade
        val grade = calculateGrade(vulnerabilities, certInfo, protocols)

        SslScanResult(
            host = host,
            port = port,
            grade = grade,
            certificate = certInfo,
            supportedProtocols = protocols,
            vulnerabilities = vulnerabilities,
            securityHeaders = securityHeaders,
            scanTimeMs = System.currentTimeMillis() - startTime
        )
    }

    private suspend fun testProtocol(
        host: String,
        port: Int,
        protocol: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sslContext = SSLContext.getInstance(protocol)
            sslContext.init(null, null, null)
            val socketFactory = sslContext.socketFactory
            val socket = socketFactory.createSocket() as SSLSocket
            socket.soTimeout = 3000
            socket.connect(InetSocketAddress(host, port), 3000)
            socket.startHandshake()
            socket.close()
            true
        } catch (e: Exception) { false }
    }

    private fun getCertificateInfo(
        host: String, port: Int
    ): CertificateInfo {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, null)
            
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket.soTimeout = 8000
            socket.connect(InetSocketAddress(host, port), 8000)
            socket.startHandshake()
            
            val session = socket.session
            val certs = session.peerCertificates
            val cert = certs[0] as X509Certificate
            
            socket.close()

            val now = Date()
            val subjectAltNames = mutableListOf<String>()
            try {
                cert.subjectAlternativeNames?.forEach { san ->
                    if (san.size >= 2) {
                        subjectAltNames.add(san[1].toString())
                    }
                }
            } catch (e: Exception) {}

            val keySize = try {
                when (val pubKey = cert.publicKey) {
                    is RSAPublicKey -> pubKey.modulus.bitLength()
                    is ECPublicKey -> pubKey.params.order.bitLength()
                    else -> 0
                }
            } catch (e: Exception) { 0 }

            CertificateInfo(
                subject = cert.subjectDN.name,
                issuer = cert.issuerDN.name,
                validFrom = cert.notBefore,
                validUntil = cert.notAfter,
                daysUntilExpiry = ((cert.notAfter.time - now.time) / 86400000).coerceAtLeast(0),
                signatureAlgorithm = cert.sigAlgName,
                keySize = keySize,
                serialNumber = cert.serialNumber.toString(16).uppercase(),
                subjectAltNames = subjectAltNames,
                isSelfSigned = cert.issuerDN == cert.subjectDN,
                isTrusted = true,
                isExpired = cert.notAfter.before(now),
                isWildcard = cert.subjectDN.name.contains("*.")
            )
        } catch (e: Exception) {
            Log.e("SslTlsScanner", "Certificate retrieval failed for $host:$port", e)
            
            CertificateInfo(
                subject = "Error: ${e.javaClass.simpleName} \u2014 ${e.message ?: "Connection failed"}",
                issuer = "Unable to connect",
                validFrom = Date(),
                validUntil = Date(),
                daysUntilExpiry = 0,
                signatureAlgorithm = "N/A",
                keySize = 0,
                serialNumber = "N/A",
                subjectAltNames = emptyList(),
                isSelfSigned = false,
                isTrusted = false,
                isExpired = false,
                isWildcard = false
            )
        }
    }

    private fun getSecurityHeaders(
        host: String, port: Int
    ): Map<String, String?> {
        val headers = mutableMapOf<String, String?>()
        val securityHeaderNames = listOf(
            "Strict-Transport-Security",
            "Content-Security-Policy",
            "X-Frame-Options",
            "X-Content-Type-Options",
            "Referrer-Policy",
            "Permissions-Policy",
            "X-XSS-Protection",
            "Cross-Origin-Opener-Policy",
            "Cross-Origin-Resource-Policy"
        )
        try {
            val url = URL("https://$host:$port")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            securityHeaderNames.forEach { name ->
                headers[name] = conn.getHeaderField(name)
            }
            conn.disconnect()
        } catch (e: Exception) {}
        return headers
    }

    private fun calculateGrade(
        vulnerabilities: List<SslVulnerability>,
        cert: CertificateInfo,
        protocols: List<ProtocolResult>
    ): SslGrade {
        if (cert.isExpired) return SslGrade.T
        if (cert.isSelfSigned) return SslGrade.M
        
        val criticalCount = vulnerabilities.count { it.severity == RiskLevel.CRITICAL && it.isVulnerable }
        val highCount = vulnerabilities.count { it.severity == RiskLevel.HIGH && it.isVulnerable }
        val hasTls13 = protocols.find { it.protocol == "TLSv1.3" }?.isSupported == true
        val hasTls12 = protocols.find { it.protocol == "TLSv1.2" }?.isSupported == true

        return when {
            criticalCount > 0 -> SslGrade.F
            highCount >= 3 -> SslGrade.D
            highCount >= 2 -> SslGrade.C
            highCount >= 1 -> SslGrade.B
            !hasTls12 && !hasTls13 -> SslGrade.C
            hasTls13 && criticalCount == 0 && highCount == 0 -> SslGrade.A_PLUS
            else -> SslGrade.A
        }
    }
}
