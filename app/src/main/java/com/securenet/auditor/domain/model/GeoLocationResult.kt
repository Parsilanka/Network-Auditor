package com.securenet.auditor.domain.model

data class GeoLocationResult(
    val ipAddress: String,
    val country: String,
    val countryCode: String,
    val regionName: String,
    val city: String,
    val zipCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val isp: String,
    val organization: String,
    val asNumber: String,
    val isProxy: Boolean,
    val isHosting: Boolean,
    val isMobile: Boolean,
    val threatLevel: ThreatLevel,
    val mapUrl: String
)

enum class ThreatLevel { CLEAN, SUSPICIOUS, DANGEROUS }
