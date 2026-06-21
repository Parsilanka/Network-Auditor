package com.securenet.auditor.network

import com.securenet.auditor.data.remote.MacVendorApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class MacVendorLookup {
    private val cache = HashMap<String, String>()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.macvendors.com/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(MacVendorApi::class.java)

    suspend fun lookup(mac: String): String? {
        cache[mac]?.let { return it }
        return try {
            val response = api.getVendor(mac)
            if (response.isSuccessful) {
                response.body()?.also { cache[mac] = it }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
