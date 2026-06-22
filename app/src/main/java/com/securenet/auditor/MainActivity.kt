package com.securenet.auditor

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.securenet.auditor.ui.navigation.NavGraph
import com.securenet.auditor.ui.theme.SecureNetTheme
import com.securenet.auditor.ui.theme.ThemeViewModel

class MainActivity : FragmentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModel.Factory((application as SecureNetApp).container.encryptedPrefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
            SecureNetTheme(isDarkTheme = isDarkTheme) {
                NavGraph()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this, permissions, 1001)
    }
}
