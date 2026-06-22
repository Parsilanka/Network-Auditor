package com.securenet.auditor

import android.app.Application

class SecureNetApp : Application() {
    lateinit var container: AppContainer
    
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.securenet.auditor.util.NotificationChannelManager.createNotificationChannels(this)
    }
}
