package com.securenet.auditor.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface MacVendorApi {
    @GET("{mac}")
    suspend fun getVendor(@Path("mac") mac: String): Response<String>
}
