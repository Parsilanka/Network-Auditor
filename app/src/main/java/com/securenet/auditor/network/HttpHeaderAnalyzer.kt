package com.securenet.auditor.network

import com.securenet.auditor.domain.model.OsintResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpHeaderAnalyzer {

    data class HeaderAnalysisResult(
        val url: String,
        val overallGrade: String,
        val overallScore: Int,
        val headers: List<HeaderGrade>,
        val serverInfo: String?,
        val responseCode: Int,
        val scanTimeMs: Long
    )

    data class HeaderGrade(
        val headerName: String,
        val value: String?,
        val isPresent: Boolean,
        val grade: String,
        val score: Int,
        val description: String,
        val recommendation: String,
        val isRequired: Boolean,
        val learnMoreUrl: String
    )

    suspend fun analyze(
        url: String
    ): OsintResult<HeaderAnalysisResult> =
        withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val formattedUrl = if (!url.startsWith("http")) "https://$url" else url
            val connection = URL(formattedUrl).openConnection()
                as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = false
            connection.connect()

            val responseCode = connection.responseCode
            val allHeaders = mutableMapOf<String, String?>()

            // Read all response headers
            connection.headerFields.forEach { (key, values) ->
                if (key != null) {
                    allHeaders[key.lowercase()] =
                        values.firstOrNull()
                }
            }
            val serverInfo = allHeaders["server"]
            connection.disconnect()

            val headerGrades = mutableListOf<HeaderGrade>()

            // HSTS Analysis
            val hsts = allHeaders[
                "strict-transport-security"]
            headerGrades.add(HeaderGrade(
                headerName = "Strict-Transport-Security",
                value = hsts,
                isPresent = hsts != null,
                grade = when {
                    hsts == null -> "F"
                    hsts.contains("preload") &&
                    hsts.contains("includeSubDomains") -> "A+"
                    hsts.contains("max-age=") -> "A"
                    else -> "C"
                },
                score = when {
                    hsts == null -> 0
                    hsts.contains("preload") -> 100
                    hsts.contains("includeSubDomains") -> 85
                    else -> 70
                },
                description = "Forces browsers to use " +
                    "HTTPS for all future requests",
                recommendation = if (hsts == null)
                    "Add: Strict-Transport-Security: " +
                    "max-age=31536000; includeSubDomains; " +
                    "preload"
                else "Current value looks good",
                isRequired = true,
                learnMoreUrl = "https://developer.mozilla.org/" +
                    "en-US/docs/Web/HTTP/Headers/" +
                    "Strict-Transport-Security"
            ))

            // CSP Analysis
            val csp = allHeaders["content-security-policy"]
            headerGrades.add(HeaderGrade(
                headerName = "Content-Security-Policy",
                value = csp,
                isPresent = csp != null,
                grade = when {
                    csp == null -> "F"
                    csp.contains("unsafe-inline") ||
                    csp.contains("unsafe-eval") -> "C"
                    csp.contains("default-src") -> "A"
                    else -> "B"
                },
                score = when {
                    csp == null -> 0
                    csp.contains("unsafe-inline") -> 40
                    csp.contains("default-src") -> 90
                    else -> 70
                },
                description = "Prevents XSS and data " +
                    "injection attacks",
                recommendation = if (csp == null)
                    "Add Content-Security-Policy header " +
                    "with appropriate directives"
                else if (csp.contains("unsafe-inline"))
                    "Remove 'unsafe-inline' directive"
                else "Good CSP configuration",
                isRequired = true,
                learnMoreUrl = "https://developer.mozilla.org/" +
                    "en-US/docs/Web/HTTP/CSP"
            ))

            // X-Frame-Options
            val xfo = allHeaders["x-frame-options"]
            headerGrades.add(HeaderGrade(
                headerName = "X-Frame-Options",
                value = xfo,
                isPresent = xfo != null,
                grade = when {
                    xfo == null -> "F"
                    xfo.uppercase() == "DENY" -> "A+"
                    xfo.uppercase() == "SAMEORIGIN" -> "A"
                    else -> "B"
                },
                score = when {
                    xfo == null -> 0
                    xfo.uppercase() == "DENY" -> 100
                    xfo.uppercase() == "SAMEORIGIN" -> 90
                    else -> 60
                },
                description = "Prevents clickjacking " +
                    "attacks by controlling iframe embedding",
                recommendation = if (xfo == null)
                    "Add: X-Frame-Options: SAMEORIGIN"
                else "Good configuration",
                isRequired = true,
                learnMoreUrl = "https://developer.mozilla.org/" +
                    "en-US/docs/Web/HTTP/Headers/" +
                    "X-Frame-Options"
            ))

            // X-Content-Type-Options
            val xcto = allHeaders[
                "x-content-type-options"]
            headerGrades.add(HeaderGrade(
                headerName = "X-Content-Type-Options",
                value = xcto,
                isPresent = xcto != null,
                grade = if (xcto == "nosniff") "A" 
                    else if (xcto != null) "B" 
                    else "F",
                score = if (xcto == "nosniff") 100
                    else if (xcto != null) 60
                    else 0,
                description = "Prevents MIME type " +
                    "sniffing attacks",
                recommendation = if (xcto == null)
                    "Add: X-Content-Type-Options: nosniff"
                else "Good",
                isRequired = true,
                learnMoreUrl = "https://developer.mozilla.org/" +
                    "en-US/docs/Web/HTTP/Headers/" +
                    "X-Content-Type-Options"
            ))

            // Referrer-Policy
            val rp = allHeaders["referrer-policy"]
            headerGrades.add(HeaderGrade(
                headerName = "Referrer-Policy",
                value = rp,
                isPresent = rp != null,
                grade = when (rp) {
                    "no-referrer",
                    "strict-origin-when-cross-origin" -> "A"
                    null -> "D"
                    else -> "B"
                },
                score = when (rp) {
                    "no-referrer" -> 100
                    "strict-origin-when-cross-origin" -> 90
                    null -> 20
                    else -> 60
                },
                description = "Controls how much referrer " +
                    "info is included with requests",
                recommendation = if (rp == null)
                    "Add: Referrer-Policy: " +
                    "strict-origin-when-cross-origin"
                else "Configured",
                isRequired = false,
                learnMoreUrl = "https://developer.mozilla.org/" +
                    "en-US/docs/Web/HTTP/Headers/" +
                    "Referrer-Policy"
            ))

            // Permissions-Policy
            val pp = allHeaders["permissions-policy"]
            headerGrades.add(HeaderGrade(
                headerName = "Permissions-Policy",
                value = pp,
                isPresent = pp != null,
                grade = if (pp != null) "A" else "D",
                score = if (pp != null) 80 else 20,
                description = "Controls browser feature " +
                    "permissions (camera, mic, location)",
                recommendation = if (pp == null)
                    "Add: Permissions-Policy: " +
                    "geolocation=(), camera=(), " +
                    "microphone=()"
                else "Configured",
                isRequired = false,
                learnMoreUrl = "https://developer.mozilla.org/" +
                    "en-US/docs/Web/HTTP/Headers/" +
                    "Feature-Policy"
            ))

            // Calculate overall grade
            val avgScore = headerGrades
                .filter { it.isRequired }
                .map { it.score }
                .average()
                .toInt()

            val overallGrade = when {
                avgScore >= 95 -> "A+"
                avgScore >= 80 -> "A"
                avgScore >= 65 -> "B"
                avgScore >= 50 -> "C"
                avgScore >= 35 -> "D"
                else -> "F"
            }

            OsintResult.Found(HeaderAnalysisResult(
                url = url,
                overallGrade = overallGrade,
                overallScore = avgScore,
                headers = headerGrades,
                serverInfo = serverInfo,
                responseCode = responseCode,
                scanTimeMs = System.currentTimeMillis() - start
            ))
        } catch (e: Exception) {
            OsintResult.Error(e.message ?: "Scan failed")
        }
    }
}
