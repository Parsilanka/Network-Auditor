package com.securenet.auditor.data.remote

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

data class UrlHausResponse(
    val query_status: String,
    val urlhaus_reference: String?,
    val threat: String?,
    val url_status: String?,
    val blacklists: BlacklistInfo?
)

data class BlacklistInfo(
    val spamhaus_dbl: String?,
    val surbl: String?
)

interface UrlHausApiService {
    @FormUrlEncoded
    @POST("v1/url/")
    suspend fun checkUrl(@Field("url") url: String): Response<UrlHausResponse>
}
