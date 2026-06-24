package com.securenet.auditor.data.remote

import com.securenet.auditor.data.remote.dto.GeoLocationDto
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeoLocationService {
    @GET("json/{ip}")
    suspend fun getLocation(
        @Path("ip") ip: String,
        @Query("fields") fields: String = 
            "status,message,country,countryCode,region," +
            "regionName,city,zip,lat,lon,timezone,isp,org," +
            "as,query,proxy,hosting,mobile"
    ): Response<GeoLocationDto>

    companion object {
        fun create(): GeoLocationService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl("http://ip-api.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeoLocationService::class.java)
        }
    }
}
