package com.smartprivacy.scanner.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartprivacy.scanner.analyzer.PrivacyAlert
import com.smartprivacy.scanner.analyzer.RiskEngine
import com.smartprivacy.scanner.analyzer.RiskSeverity
import com.smartprivacy.scanner.data.AppDatabase
import com.smartprivacy.scanner.data.AppEntity
import com.smartprivacy.scanner.data.VirusTotalRepository
import com.smartprivacy.scanner.utils.HashUtils
import com.smartprivacy.scanner.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val vtRepository = VirusTotalRepository()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString("packageName") ?: return@withContext Result.failure()
        val packageHelper = PackageHelper(applicationContext)
        val appName = try {
            val pi = applicationContext.packageManager.getPackageInfo(packageName, 0)
            packageHelper.getAppName(pi)
        } catch (e: Exception) {
            packageName
        }

        // 1. Show scanning notification immediately
        NotificationHelper.createNotificationChannel(applicationContext)
        NotificationHelper.showScanningNotification(applicationContext, packageName, appName)

        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val riskEngine = RiskEngine()
            val usageStatsHelper = UsageStatsHelper(applicationContext)

            val packageManager = applicationContext.packageManager
            val packageInfo = try {
                packageManager.getPackageInfo(packageName, 4096) // GET_PERMISSIONS
            } catch (e: Exception) {
                NotificationHelper.cancelNotification(applicationContext, packageName)
                return@withContext Result.failure()
            }
            
            val isSideloaded = packageHelper.isSideloaded(packageName)
            val riskResult = riskEngine.calculateRisk(packageInfo, isSideloaded)
            
            // 2. Perform VirusTotal Check (Network Call)
            val apkPath = packageInfo.applicationInfo?.sourceDir
            val sha256 = apkPath?.let { HashUtils.getSha256(it) }
            
            var vtRatio: String? = null
            var vtMaliciousCount = 0

            if (sha256 != null) {
                // Check if we already have VT data in DB to avoid API call
                val existing = database.appDao().getAppByPackageName(packageName)
                if (existing?.sha256 == sha256 && existing.vtDetectionRatio != null) {
                    vtRatio = existing.vtDetectionRatio
                    vtMaliciousCount = existing.vtMaliciousCount ?: 0
                } else {
                    // Fetch fresh report
                    val report = vtRepository.getAppReport(sha256)
                    if (report != null) {
                        report.data?.attributes?.lastAnalysisStats?.let { stats ->
                            val total = stats.malicious + stats.harmless + stats.undetected + stats.suspicious
                            vtRatio = "${stats.malicious}/$total"
                            vtMaliciousCount = stats.malicious
                        }
                    }
                }
            }

            // 3. Update Risk based on VT results
            val updatedAlerts = riskResult.alerts.toMutableList()
            var updatedScore = riskResult.score
            
            if (vtMaliciousCount > 0) {
                updatedScore = 100 // Critical
                updatedAlerts.add(0, PrivacyAlert(
                    "Malware Detected",
                    "VirusTotal detected this app as malicious by $vtMaliciousCount engines.",
                    RiskSeverity.HIGH
                ))
            }

            // 4. Save to database
            val lastUsedMap = usageStatsHelper.getAllLastUsedTimes()
            val lastUsed = lastUsedMap[packageName] ?: 0L

            val appEntity = AppEntity(
                packageName = packageName,
                appName = packageHelper.getAppName(packageInfo),
                version = packageInfo.versionName ?: "N/A",
                isSystemApp = packageHelper.isSystemApp(packageInfo),
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                riskScore = updatedScore,
                riskLevel = if (updatedScore >= 70) "High Risk" else riskResult.level,
                riskReasons = updatedAlerts.map { it.title },
                privacyAlerts = updatedAlerts,
                permissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
                lastUsed = lastUsed,
                isSideloaded = isSideloaded,
                sha256 = sha256,
                vtDetectionRatio = vtRatio,
                vtMaliciousCount = vtMaliciousCount
            )

            // Update or Insert
            database.appDao().insertApps(listOf(appEntity))

            // 5. Update Notification
            NotificationHelper.cancelNotification(applicationContext, packageName) // Remove "Scanning..."
            
            if (updatedScore >= 70) {
                val reason = if (vtMaliciousCount > 0) "Malware Detected!" else "High Privacy Risk Detected!"
                NotificationHelper.showRiskNotification(applicationContext, packageName, appName, updatedScore, reason)
            } else {
                // Ideally show safe briefly, but per instruction "ignore if low"
                // Just cancelling the scanning notification is correct.
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            NotificationHelper.cancelNotification(applicationContext, packageName)
            Result.failure()
        }
    }
}
