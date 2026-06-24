package com.securenet.auditor.data.repository

import com.securenet.auditor.data.remote.GeoLocationService
import com.securenet.auditor.domain.model.GeoLocationResult
import com.securenet.auditor.domain.model.OsintResult
import com.securenet.auditor.domain.model.ThreatLevel
import kotlinx.coroutines.delay
import java.io.IOException

class GeoLocationRepository(
    private val service: GeoLocationService
) {
    suspend fun lookupIp(ip: String): OsintResult<GeoLocationResult> {
        return try {
            val response = service.getLocation(ip)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                if (dto.status == "fail") {
                    return OsintResult.Error(
                        dto.message ?: "Lookup failed for $ip")
                }
                
                val threatLevel = when {
                    (dto.proxy == true) && (dto.hosting == true) -> 
                        ThreatLevel.DANGEROUS
                    (dto.proxy == true) || (dto.hosting == true) -> 
                        ThreatLevel.SUSPICIOUS
                    else -> ThreatLevel.CLEAN
                }
                
                // Generate OpenStreetMap static map URL
                val mapUrl = "https://www.openstreetmap.org/" +
                    "?mlat=${dto.lat}&mlon=${dto.lon}" +
                    "#map=12/${dto.lat}/${dto.lon}"
                
                val result = GeoLocationResult(
                    ipAddress = dto.query ?: ip,
                    country = dto.country ?: "Unknown",
                    countryCode = dto.countryCode ?: "??",
                    regionName = dto.regionName ?: "Unknown",
                    city = dto.city ?: "Unknown",
                    zipCode = dto.zip ?: "",
                    latitude = dto.lat ?: 0.0,
                    longitude = dto.lon ?: 0.0,
                    timezone = dto.timezone ?: "Unknown",
                    isp = dto.isp ?: "Unknown",
                    organization = dto.org ?: "Unknown",
                    asNumber = dto.asNumber ?: "Unknown",
                    isProxy = dto.proxy ?: false,
                    isHosting = dto.hosting ?: false,
                    isMobile = dto.mobile ?: false,
                    threatLevel = threatLevel,
                    mapUrl = mapUrl
                )
                OsintResult.Found(result)
            } else {
                OsintResult.Error("HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            OsintResult.Error("No internet connection")
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Unknown error")
        }
    }
    
    // Batch lookup for multiple IPs from network scan
    suspend fun lookupMultipleIps(
        ips: List<String>
    ): Map<String, GeoLocationResult> {
        val results = mutableMapOf<String, GeoLocationResult>()
        ips.forEach { ip ->
            delay(200) // Respect rate limit: 45 req/min free tier
            val result = lookupIp(ip)
            if (result is OsintResult.Found) {
                results[ip] = result.data
            }
        }
        return results
    }
}
