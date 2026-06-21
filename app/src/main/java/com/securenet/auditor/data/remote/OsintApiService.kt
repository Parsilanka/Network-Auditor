package com.securenet.auditor.data.remote

import com.securenet.auditor.BuildConfig
import com.securenet.auditor.data.remote.dto.BreachDto
import com.securenet.auditor.data.remote.dto.DisifyDto
import com.securenet.auditor.data.remote.dto.HunterResponseDto
import com.securenet.auditor.data.remote.dto.MailCheckDto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HibpApiService {
    @GET("breachedaccount/{email}")
    suspend fun checkEmailBreach(
        @Path("email") email: String,
        @Header("hibp-api-key") apiKey: String,
        @Query("truncateResponse") truncate: Boolean = false
    ): Response<List<BreachDto>>
}

interface HunterApiService {
    @GET("domain-search")
    suspend fun searchDomain(
        @Query("domain") domain: String,
        @Query("api_key") apiKey: String
    ): Response<HunterResponseDto>
}

interface DisifyService {
    @GET("api/email/{email}")
    suspend fun checkEmail(
        @Path("email") email: String
    ): Response<DisifyDto>
    
    companion object {
        fun create(): DisifyService {
            return Retrofit.Builder()
                .baseUrl("https://www.disify.com/")
                .client(OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DisifyService::class.java)
        }
    }
}

interface MailCheckService {
    @GET("email/{email}")
    suspend fun checkEmail(
        @Path("email") email: String
    ): Response<MailCheckDto>
    
    companion object {
        fun create(): MailCheckService {
            return Retrofit.Builder()
                .baseUrl("https://api.mailcheck.ai/")
                .client(OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MailCheckService::class.java)
        }
    }
}

object OsintApiService {
    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun createHibpService(): HibpApiService {
        return Retrofit.Builder()
            .baseUrl("https://haveibeenpwned.com/api/v3/")
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HibpApiService::class.java)
    }

    fun createHunterService(): HunterApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.hunter.io/v2/")
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HunterApiService::class.java)
    }
}
