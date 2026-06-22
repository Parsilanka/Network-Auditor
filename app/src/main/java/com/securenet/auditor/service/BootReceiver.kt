package com.securenet.auditor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("monitor_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("monitor_enabled", false)) {
                NetworkMonitorService.start(context)
            }
        }
    }
}
