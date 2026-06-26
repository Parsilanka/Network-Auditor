package com.securenet.auditor.worker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import java.util.concurrent.TimeUnit

object NetworkMonitorScheduler {

    fun schedule(
        context: Context,
        intervalMinutes: Long = 15
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NetworkMonitorWorker>(
            intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "network_monitor",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("network_monitor")
    }

    fun getStatus(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("network_monitor")
    }
}
