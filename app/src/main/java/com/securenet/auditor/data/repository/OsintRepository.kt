package com.securenet.auditor.data.repository

import com.securenet.auditor.data.prefs.EncryptedPrefsManager
import com.securenet.auditor.data.remote.*
import com.securenet.auditor.data.remote.dto.HunterResponseDto
import com.securenet.auditor.domain.model.*
import com.securenet.auditor.network.WhoisClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.MessageDigest

class OsintRepository(
    private val hibpService: HibpApiService,
    private val pwnedPasswordsService: PwnedPasswordsService,
    private val hunterService: HunterApiService,
    private val disifyService: DisifyService,
    private val mailCheckService: MailCheckService,
    private val ipApiService: IpApiService,
    private val urlHausService: UrlHausApiService,
    private val whoisClient: WhoisClient,
    private val prefs: EncryptedPrefsManager
) {

    suspend fun checkEmail(email: String): OsintResult<List<BreachResult>> {
        val apiKey = prefs.getApiKey(EncryptedPrefsManager.HIBP_KEY)
            ?: return OsintResult.Error("API key not configured")

        return try {
            val response = hibpService.checkEmailBreach(email, apiKey)
            if (response.isSuccessful) {
                val breaches = response.body()?.map { dto ->
                    BreachResult(
                        name = dto.name,
                        domain = dto.domain,
                        breachDate = dto.breachDate,
                        pwnCount = dto.pwnCount,
                        description = dto.description,
                        severity = when {
                            dto.pwnCount < 10_000 -> BreachSeverity.LOW
                            dto.pwnCount < 100_000 -> BreachSeverity.MEDIUM
                            dto.pwnCount < 1_000_000 -> BreachSeverity.HIGH
                            else -> BreachSeverity.CRITICAL
                        }
                    )
                } ?: emptyList()
                if (breaches.isEmpty()) OsintResult.NotFound else OsintResult.Found(breaches)
            } else {
                if (response.code() == 404) OsintResult.NotFound
                else handleHttpError(response.code())
            }
        } catch (e: SocketTimeoutException) {
            OsintResult.Error("Request timed out")
        } catch (e: IOException) {
            OsintResult.Error("No internet connection")
        } catch (e: Exception) {
            OsintResult.Error("Unknown error: ${e.message}")
        }
    }

    suspend fun searchDomain(domain: String): OsintResult<HunterResponseDto> {
        val apiKey = prefs.getApiKey(EncryptedPrefsManager.HUNTER_KEY)
            ?: return OsintResult.Error("API key not configured")

        return try {
            val response = hunterService.searchDomain(domain, apiKey)
            if (response.isSuccessful) {
                response.body()?.let { OsintResult.Found(it) } ?: OsintResult.NotFound
            } else {
                if (response.code() == 404) OsintResult.NotFound
                else handleHttpError(response.code())
            }
        } catch (e: SocketTimeoutException) {
            OsintResult.Error("Request timed out")
        } catch (e: IOException) {
            OsintResult.Error("No internet connection")
        } catch (e: Exception) {
            OsintResult.Error("Unknown error: ${e.message}")
        }
    }

    suspend fun checkEmailReputation(email: String): OsintResult<CombinedEmailCheckResult> {
        return try {
            // Call both APIs simultaneously
            val disifyResult = try {
                disifyService.checkEmail(email).body()
            } catch (e: Exception) { null }
            
            val mailCheckResult = try {
                mailCheckService.checkEmail(email).body()
            } catch (e: Exception) { null }
            
            if (disifyResult == null && mailCheckResult == null) {
                return OsintResult.Error("Could not reach verification servers")
            }
            
            val combined = CombinedEmailCheckResult(
                email = email,
                domain = disifyResult?.domain ?: mailCheckResult?.domain,
                isDisposable = disifyResult?.disposable 
                    ?: mailCheckResult?.disposable ?: false,
                isFormatValid = disifyResult?.format ?: true,
                hasDns = disifyResult?.dns ?: true,
                hasMx = mailCheckResult?.mx ?: true,
                isAlias = mailCheckResult?.alias ?: false
            )
            OsintResult.Found(combined)
        } catch (e: IOException) {
            OsintResult.Error("No internet connection")
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getIpGeo(ip: String): OsintResult<IpGeoDto> {
        return try {
            val response = ipApiService.getGeo(ip)
            if (response.isSuccessful) {
                val data = response.body()
                if (data?.status == "success") OsintResult.Found(data)
                else OsintResult.Error(data?.status ?: "Unknown API error")
            } else {
                handleHttpError(response.code())
            }
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getWhois(query: String): OsintResult<String> {
        return try {
            val result = whoisClient.lookup(query)
            if (result.startsWith("Error")) OsintResult.Error(result)
            else OsintResult.Found(result)
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Whois error")
        }
    }

    suspend fun checkMalwareUrl(url: String): OsintResult<UrlHausResponse> {
        return try {
            val response = urlHausService.checkUrl(url)
            if (response.isSuccessful) {
                response.body()?.let { OsintResult.Found(it) } ?: OsintResult.NotFound
            } else {
                handleHttpError(response.code())
            }
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Malware check error")
        }
    }

    suspend fun checkPasswordBreach(password: String): OsintResult<Int> {
        return try {
            val sha1 = MessageDigest.getInstance("SHA-1")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .uppercase()
            
            val prefix = sha1.take(5)
            val suffix = sha1.substring(5)
            
            val response = pwnedPasswordsService.getPwnedHashes(prefix)
            if (response.isSuccessful) {
                val body = response.body() ?: return OsintResult.NotFound
                val lines = body.lines()
                val match = lines.find { it.startsWith(suffix) }
                if (match != null) {
                    val count = match.split(":")[1].trim().toInt()
                    OsintResult.Found(count)
                } else {
                    OsintResult.NotFound
                }
            } else {
                OsintResult.Error("API error: ${response.code()}")
            }
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun handleHttpError(code: Int): OsintResult<Nothing> {
        return when (code) {
            401, 403 -> OsintResult.Error("Invalid API key — check settings")
            429 -> OsintResult.Error("Rate limited. Wait before retrying.")
            else -> OsintResult.Error("Server error: $code")
        }
    }
}
