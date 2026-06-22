package com.securenet.auditor.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

data class IpGeoDto(
    val status: String,
    val country: String?,
    val city: String?,
    val isp: String?,
    val org: String?,
    val lat: Double?,
    val lon: Double?,
    val proxy: Boolean?,
    val hosting: Boolean?,
    val mobile: Boolean?,
    val query: String
)

interface IpApiService {
    @GET("json/{ip}?fields=status,message,country,city,isp,org,lat,lon,proxy,hosting,mobile,query")
    suspend fun getGeo(@Path("ip") ip: String): Response<IpGeoDto>
}
