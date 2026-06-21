package com.securenet.auditor.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "securenet_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(service: String, key: String) {
        sharedPreferences.edit().putString(service, key).apply()
    }

    fun getApiKey(service: String): String? {
        return sharedPreferences.getString(service, null)
    }

    fun hasApiKey(service: String): Boolean {
        return sharedPreferences.contains(service)
    }

    fun clearApiKey(service: String) {
        sharedPreferences.edit().remove(service).apply()
    }

    companion object {
        const val HIBP_KEY = "hibp_api_key"
        const val HUNTER_KEY = "hunter_api_key"
    }
}
