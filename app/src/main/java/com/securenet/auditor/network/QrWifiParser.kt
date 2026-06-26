package com.securenet.auditor.network

object QrWifiParser {

    data class WifiCredentials(
        val ssid: String,
        val password: String?,
        val securityType: WifiConnectionManager.WifiSecurityType,
        val isHidden: Boolean
    )

    // Parse Wi-Fi QR code format:
    // WIFI:T:WPA;S:NetworkName;P:password123;H:false;;
    fun parse(qrContent: String): WifiCredentials? {
        if (!qrContent.startsWith("WIFI:")) return null

        try {
            val params = mutableMapOf<String, String>()
            // Remove "WIFI:" prefix and trailing ";;"
            val content = qrContent
                .removePrefix("WIFI:")
                .removeSuffix(";;")
                .removeSuffix(";")

            // Split by ";" but handle escaped semicolons "\;"
            val parts = content.split(Regex("(?<!\\\\);"))

            parts.forEach { part ->
                val colonIndex = part.indexOf(':')
                if (colonIndex > 0) {
                    val key = part.substring(0, colonIndex)
                    val value = part.substring(colonIndex + 1)
                        .replace("\\;", ";")
                        .replace("\\\\", "\\")
                        .replace("\\\"", "\"")
                        .replace("\\,", ",")
                    params[key] = value
                }
            }

            val ssid = params["S"] ?: return null
            val password = params["P"]
            val secType = params["T"] ?: "nopass"
            val hidden = params["H"]?.lowercase() == "true"

            val securityType = when (secType.uppercase()) {
                "WPA", "WPA2" -> 
                    WifiConnectionManager.WifiSecurityType.WPA2
                "WPA3" -> 
                    WifiConnectionManager.WifiSecurityType.WPA3
                "WEP" -> 
                    WifiConnectionManager.WifiSecurityType.WEP
                "NOPASS", "" -> 
                    WifiConnectionManager.WifiSecurityType.OPEN
                else -> 
                    WifiConnectionManager.WifiSecurityType.WPA2
            }

            return WifiCredentials(
                ssid = ssid,
                password = password,
                securityType = securityType,
                isHidden = hidden
            )
        } catch (e: Exception) {
            return null
        }
    }
}
