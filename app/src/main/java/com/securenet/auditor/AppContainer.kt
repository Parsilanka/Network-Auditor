package com.securenet.auditor

import android.content.Context
import com.securenet.auditor.data.db.SecureNetDatabase
import com.securenet.auditor.data.prefs.EncryptedPrefsManager
import com.securenet.auditor.data.remote.DisifyService
import com.securenet.auditor.data.remote.MailCheckService
import com.securenet.auditor.network.MacVendorLookup
import com.securenet.auditor.data.remote.OsintApiService
import com.securenet.auditor.data.repository.OsintRepository
import com.securenet.auditor.data.repository.ScanRepository
import com.securenet.auditor.network.PortScanner
import com.securenet.auditor.network.SubnetScanner
import com.securenet.auditor.network.WhoisClient
import com.securenet.auditor.security.BiometricHelper

class AppContainer(context: Context) {
    private val db = SecureNetDatabase.getInstance(context)
    val encryptedPrefs = EncryptedPrefsManager(context)
    val scanResultDao = db.scanResultDao()
    val speedTestDao = db.speedTestDao()
    val arpDao = db.arpDao()
    val scanRepository = ScanRepository(scanResultDao)
    val subnetScanner = SubnetScanner(context)
    val portScanner = PortScanner()
    val macVendorLookup = MacVendorLookup()
    val biometricHelper = BiometricHelper(context)

    // Retrofit services
    private val hibpService = OsintApiService.createHibpService()
    private val hunterService = OsintApiService.createHunterService()
    private val disifyService = DisifyService.create()
    private val mailCheckService = MailCheckService.create()
    private val ipApiService = OsintApiService.createIpApiService()
    private val urlHausService = OsintApiService.createUrlHausService()
    private val whoisClient = WhoisClient()

    val osintRepository = OsintRepository(
        hibpService, 
        hunterService, 
        disifyService, 
        mailCheckService, 
        ipApiService,
        urlHausService,
        whoisClient,
        encryptedPrefs
    )
}
