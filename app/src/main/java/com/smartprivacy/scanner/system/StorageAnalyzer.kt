package com.smartprivacy.scanner.system

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.util.Log
import com.smartprivacy.scanner.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppStorageStats(
    val packageName: String,
    val appName: String,
    val cacheSize: Long,
    val dataSize: Long,
    val appSize: Long,
    val totalSize: Long,
    val isSystemApp: Boolean
)

class StorageAnalyzer(private val context: Context) {

    private val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
    private val packageManager: PackageManager = context.packageManager

    /**
     * DIAGNOSIS OF CACHE DETECTION FAILURE (0 BYTES):
     * 
     * 1. Usage Access Permission: Manifest declaration is NOT enough. User must 
     *    manually enable "Usage Access" in Settings. Without this, API returns 0.
     * 2. Thread Blocking: StorageStatsManager calls are IPC-heavy. If called 
     *    on Main thread, they might be dropped or return incomplete results.
     * 3. App Visibility (Android 11+): Must have QUERY_ALL_PACKAGES to "see" apps.
     * 4. Cache Calculation: Modern Android often summarizes "Cache" differently. 
     *    We must query per-package using the default Internal Storage UUID.
     */

    suspend fun getAppStorageStats(packageName: String): AppStorageStats? = withContext(Dispatchers.IO) {
        // Critical: Exit if user hasn't granted the "Usage Access" permission
        if (storageStatsManager == null || !PermissionUtils.hasUsageStatsPermission(context)) {
            return@withContext null
        }

        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }

            // Querying Internal Storage stats specifically
            val storageStats = storageStatsManager.queryStatsForPackage(
                StorageManager.UUID_DEFAULT,
                packageName,
                Process.myUserHandle()
            )

            AppStorageStats(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                cacheSize = storageStats.cacheBytes,
                dataSize = storageStats.dataBytes,
                appSize = storageStats.appBytes,
                totalSize = storageStats.appBytes + storageStats.dataBytes + storageStats.cacheBytes,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        } catch (e: Exception) {
            Log.e("StorageAnalyzer", "Critical failure querying package: $packageName", e)
            null
        }
    }

    /**
     * Fetches storage data for all third-party apps with actual cache.
     */
    suspend fun fetchCacheReport(): List<AppStorageStats> = withContext(Dispatchers.IO) {
        if (!PermissionUtils.hasUsageStatsPermission(context)) return@withContext emptyList()

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val report = mutableListOf<AppStorageStats>()

        for (appInfo in installedApps) {
            // Filter: Production-ready cleanup focus on non-system apps
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                try {
                    val stats = storageStatsManager?.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        appInfo.packageName,
                        Process.myUserHandle()
                    )
                    
                    // Production-ready: Only report apps that have actual cache to clean
                    if (stats != null && stats.cacheBytes > 0) {
                        report.add(AppStorageStats(
                            packageName = appInfo.packageName,
                            appName = packageManager.getApplicationLabel(appInfo).toString(),
                            cacheSize = stats.cacheBytes,
                            dataSize = stats.dataBytes,
                            appSize = stats.appBytes,
                            totalSize = stats.appBytes + stats.dataBytes + stats.cacheBytes,
                            isSystemApp = false
                        ))
                    }
                } catch (e: Exception) {
                    continue // Skip apps that are currently inaccessible
                }
            }
        }
        report.sortedByDescending { it.cacheSize }
    }
}
