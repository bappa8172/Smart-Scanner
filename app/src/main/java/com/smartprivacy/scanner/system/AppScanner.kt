package com.smartprivacy.scanner.system

import android.content.Context
import com.smartprivacy.scanner.analyzer.RiskEngine
import com.smartprivacy.scanner.data.AppDao
import com.smartprivacy.scanner.data.AppEntity
import com.smartprivacy.scanner.utils.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AppScanner(
    context: Context, // Removed 'private val' since it's unused property
    private val appDao: AppDao,
    private val packageHelper: PackageHelper,
    private val riskEngine: RiskEngine,
    private val usageStatsHelper: UsageStatsHelper
) {

    /**
     * Scans apps and returns a Flow of progress (0.0 to 1.0)
     * Preserves existing VirusTotal data to avoid unnecessary re-fetching.
     */
    fun scanApps(): Flow<Float> = flow {
        val installedPackages = packageHelper.getInstalledApps()
        val total = installedPackages.size
        
        // Load existing data to preserve VirusTotal results
        val existingAppsMap = appDao.getAllAppsOnce().associateBy { it.packageName }
        
        // Batch query usage stats once at the start for massive performance gain
        val lastUsedMap = usageStatsHelper.getAllLastUsedTimes()
        
        val appEntities = mutableListOf<AppEntity>()

        installedPackages.forEachIndexed { index, packageInfo ->
            val packageName = packageInfo.packageName
            val isSideloaded = packageHelper.isSideloaded(packageName)
            val riskResult = riskEngine.calculateRisk(packageInfo, isSideloaded)
            val lastUsed = lastUsedMap[packageName] ?: 0L
            
            val apkPath = packageInfo.applicationInfo?.sourceDir
            val sha256 = apkPath?.let { HashUtils.getSha256(it) }
            
            // Preserve existing VT data if SHA256 matches
            val existing = existingAppsMap[packageName]
            // Fix: Added safe calls ?. to resolve errors
            val vtRatio = if (existing?.sha256 != null && existing.sha256 == sha256) existing.vtDetectionRatio else null
            val vtMalicious = if (existing?.sha256 != null && existing.sha256 == sha256) existing.vtMaliciousCount else 0
            
            appEntities.add(AppEntity(
                packageName = packageName,
                appName = packageHelper.getAppName(packageInfo),
                version = packageInfo.versionName ?: "N/A",
                isSystemApp = packageHelper.isSystemApp(packageInfo),
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                riskScore = riskResult.score,
                riskLevel = riskResult.level,
                riskReasons = riskResult.alerts.map { it.title },
                privacyAlerts = riskResult.alerts,
                permissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
                lastUsed = lastUsed,
                isSideloaded = isSideloaded,
                sha256 = sha256,
                vtDetectionRatio = vtRatio,
                vtMaliciousCount = vtMalicious
            ))
            
            // Emit progress
            if (index % 5 == 0 || index == total - 1) {
                emit((index + 1).toFloat() / total)
            }
        }
        
        // Using a transaction would be better, but deleteAll + insert works for now
        // if we preserved the critical data above.
        appDao.deleteAll()
        appDao.insertApps(appEntities)
    }.flowOn(Dispatchers.IO)
}
