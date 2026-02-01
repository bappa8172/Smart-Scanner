package com.smartprivacy.scanner.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart ?: return

        // Skip our own package to avoid weird loops (though unlikely for removal)
        if (packageName == context.packageName) return

        val inputData = Data.Builder()
            .putString("packageName", packageName)
            .build()

        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                val workRequest = OneTimeWorkRequest.Builder(AppScanWorker::class.java)
                    .setInputData(inputData)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
            Intent.ACTION_PACKAGE_FULLY_REMOVED -> {
                val workRequest = OneTimeWorkRequest.Builder(AppRemovalWorker::class.java)
                    .setInputData(inputData)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
