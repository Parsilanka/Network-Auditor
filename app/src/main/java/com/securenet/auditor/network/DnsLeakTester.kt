package com.securenet.auditor.network

import com.securenet.auditor.data.repository.GeoLocationRepository
import com.securenet.auditor.domain.model.OsintResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DnsLeakTester(
    private val geoLocationRepository: GeoLocationRepository
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class DnsServer(
        val ip: String,
        val geo: String,
        val isp: String?,
        val country: String?
    )

    data class LeakResult(
        val publicIp: String,
        val publicIsp: String?,
        val publicCountry: String?,
        val dnsServers: List<DnsServer>,
        val isLeaking: Boolean,
        val conclusion: String
    )

    suspend fun runTest(): LeakResult? = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                // 1. Get Public IP and Info
                val publicIpDeferred = async { fetchPublicIpInfo() }
                
                // 2. Get DNS Resolvers IP from multiple sources
                val dnsIpsDeferred = async { fetchDnsResolverIps() }

                val publicInfo = publicIpDeferred.await() ?: return@coroutineScope null
                val dnsIps = dnsIpsDeferred.await()

                // 3. Analyze DNS Resolvers using GeoLocationRepository
                val dnsServers = analyzeDnsResolvers(dnsIps)
                
                // Analyze for leaks
                val leakingServers = dnsServers.filter { it.isp == publicInfo.isp && it.isp != null }
                val isLeaking = leakingServers.isNotEmpty()
                
                val conclusion = if (isLeaking) {
                    "DNS Leak Detected! Your DNS queries are being handled by your ISP (${publicInfo.isp}), bypassing your VPN/Proxy."
                } else if (dnsServers.isEmpty()) {
                    "Could not retrieve DNS information."
                } else {
                    val primaryGeo = dnsServers.firstOrNull()?.geo ?: "Unknown"
                    "No DNS leak detected. Your DNS queries are being resolved by $primaryGeo. No third-party DNS leaks detected."
                }

                LeakResult(
                    publicIp = publicInfo.ip,
                    publicIsp = publicInfo.isp,
                    publicCountry = publicInfo.country,
                    dnsServers = dnsServers,
                    isLeaking = isLeaking,
                    conclusion = conclusion
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchPublicIpInfo(): PublicIpInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("http://ip-api.com/json").build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                if (json.getString("status") == "success") {
                    PublicIpInfo(
                        ip = json.getString("query"),
                        isp = json.getString("isp"),
                        country = json.getString("country")
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchDnsResolverIps(): List<String> = withContext(Dispatchers.IO) {
        val ips = mutableSetOf<String>()
        val urls = listOf(
            "https://edns.ip-api.com/json",
            "https://bash.ws/dnsleak/test" // This might need parsing if it returns plain text or HTML
        )

        urls.forEach { url ->
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (url.contains("ip-api")) {
                        val json = JSONObject(body)
                        // The edns.ip-api.com response structure can vary, handle both
                        val dnsIp = json.optString("dns", "")
                        if (dnsIp.isNotEmpty()) {
                            // Sometimes 'dns' is a string IP
                            ips.add(dnsIp)
                        } else {
                            // Sometimes it's a nested object {"dns": {"ip": "...", "geo": "..."}}
                            val dnsObj = json.optJSONObject("dns")
                            dnsObj?.optString("ip")?.let { if (it.isNotEmpty()) ips.add(it) }
                        }
                    } else if (url.contains("bash.ws")) {
                        // bash.ws often returns plain IPs line by line or similar
                        body.lines().forEach { line ->
                            val ip = line.trim()
                            if (isValidIp(ip)) ips.add(ip)
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        ips.toList()
    }

    private suspend fun analyzeDnsResolvers(ips: List<String>): List<DnsServer> {
        return ips.map { ip ->
            try {
                val geoResult = geoLocationRepository.lookupIp(ip)
                when (geoResult) {
                    is OsintResult.Found -> DnsServer(
                        ip = ip,
                        geo = "${geoResult.data.country}${if (geoResult.data.organization != null) " - ${geoResult.data.organization}" else ""}",
                        isp = geoResult.data.isp,
                        country = geoResult.data.country
                    )
                    else -> DnsServer(
                        ip = ip,
                        geo = "Location unknown",
                        isp = null,
                        country = null
                    )
                }
            } catch (e: Exception) {
                DnsServer(ip = ip, geo = "Lookup failed", isp = null, country = null)
            }
        }
    }

    private fun isValidIp(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            parts.size == 4 && parts.all { it.toInt() in 0..255 }
        } catch (e: Exception) {
            false
        }
    }

    data class PublicIpInfo(val ip: String, val isp: String, val country: String)
}
