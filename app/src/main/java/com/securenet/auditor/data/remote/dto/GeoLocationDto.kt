package com.securenet.auditor.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GeoLocationDto(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("countryCode") val countryCode: String?,
    @SerializedName("region") val region: String?,
    @SerializedName("regionName") val regionName: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("zip") val zip: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("isp") val isp: String?,
    @SerializedName("org") val org: String?,
    @SerializedName("as") val asNumber: String?,
    @SerializedName("query") val query: String?,
    @SerializedName("proxy") val proxy: Boolean?,
    @SerializedName("hosting") val hosting: Boolean?,
    @SerializedName("mobile") val mobile: Boolean?
)
