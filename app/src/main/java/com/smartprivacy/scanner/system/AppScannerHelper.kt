package com.smartprivacy.scanner.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class AppScannerHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getAppName(packageName: String): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    fun getInstalledApps(): List<InstalledApp> {
        val apps = mutableListOf<InstalledApp>()
        val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }

        for (packageInfo in installedPackages) {
            val appInfo = packageInfo.applicationInfo ?: continue
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val packageName = packageInfo.packageName
            val icon = packageManager.getApplicationIcon(appInfo)
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            
            val installSource = getInstallSource(packageName, isSystemApp)

            apps.add(
                InstalledApp(
                    name = appName,
                    packageName = packageName,
                    icon = icon,
                    isSystemApp = isSystemApp,
                    installSource = installSource
                )
            )
        }
        return apps
    }

    private fun getInstallSource(packageName: String, isSystemApp: Boolean): String {
        if (isSystemApp) return "System"
        
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            
            when (installer) {
                "com.android.vending" -> "Play Store"
                "com.amazon.venezia" -> "Amazon Appstore"
                null -> "Sideloaded"
                else -> "Other ($installer)"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
