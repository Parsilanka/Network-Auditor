package com.securenet.auditor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class CredentialResult(
    val ip: String,
    val port: Int,
    val protocol: String,
    val isAccessible: Boolean,
    val foundCredentials: Pair<String, String>? = null
)

class CredentialChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val commonCredentials = listOf(
        "admin" to "admin",
        "admin" to "password",
        "root" to "root",
        "admin" to "1234",
        "guest" to "guest"
    )

    suspend fun check(ip: String, port: Int): CredentialResult = withContext(Dispatchers.IO) {
        val protocol = if (port == 443) "https" else "http"
        val url = "$protocol://$ip:$port"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (response.code == 401) {
                // Try credentials
                for ((user, pass) in commonCredentials) {
                    val authRequest = Request.Builder()
                        .url(url)
                        .header("Authorization", Credentials.basic(user, pass))
                        .build()
                    
                    val authResponse = client.newCall(authRequest).execute()
                    if (authResponse.isSuccessful) {
                        return@withContext CredentialResult(ip, port, protocol, true, user to pass)
                    }
                }
                return@withContext CredentialResult(ip, port, protocol, true, null)
            } else if (response.isSuccessful) {
                // Accessible without credentials - might be a risk too
                return@withContext CredentialResult(ip, port, protocol, true, null)
            }
            
            CredentialResult(ip, port, protocol, false)
        } catch (e: Exception) {
            CredentialResult(ip, port, protocol, false)
        }
    }
}
