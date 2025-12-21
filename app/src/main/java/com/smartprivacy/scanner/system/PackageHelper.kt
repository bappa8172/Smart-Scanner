package com.smartprivacy.scanner.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

class PackageHelper(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getInstalledApps(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
    }

    fun getAppName(packageInfo: PackageInfo): String {
        val appInfo = packageInfo.applicationInfo
        return if (appInfo != null) {
            packageManager.getApplicationLabel(appInfo).toString()
        } else {
            packageInfo.packageName
        }
    }

    fun isSystemApp(packageInfo: PackageInfo): Boolean {
        val appInfo = packageInfo.applicationInfo
        return if (appInfo != null) {
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } else {
            false
        }
    }

    fun isSideloaded(packageName: String): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            installer == null || (installer != "com.android.vending" && installer != "com.amazon.venezia")
        } catch (e: Exception) {
            true
        }
    }
}
