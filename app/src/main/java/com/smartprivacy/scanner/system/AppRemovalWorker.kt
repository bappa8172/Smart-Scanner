package com.smartprivacy.scanner.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartprivacy.scanner.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRemovalWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString("packageName") ?: return@withContext Result.failure()

        try {
            val database = AppDatabase.getDatabase(applicationContext)
            database.appDao().deleteApp(packageName)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
