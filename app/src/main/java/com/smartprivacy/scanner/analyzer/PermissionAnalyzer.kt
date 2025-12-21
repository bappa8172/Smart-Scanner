package com.smartprivacy.scanner.analyzer

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build

data class AppPermission(
    val name: String,
    val riskLevel: PermissionRiskLevel,
    val isGranted: Boolean
)

enum class PermissionRiskLevel {
    DANGEROUS,
    NORMAL,
    SPECIAL
}

class PermissionAnalyzer(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getPermissionsForApp(packageName: String): List<AppPermission> {
        val permissionsList = mutableListOf<AppPermission>()
        
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }

            val requestedPermissions = packageInfo.requestedPermissions
            val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags

            if (requestedPermissions != null && requestedPermissionsFlags != null) {
                for (i in requestedPermissions.indices) {
                    if (i < requestedPermissionsFlags.size) {
                        val permissionName = requestedPermissions[i]
                        val isGranted = (requestedPermissionsFlags[i] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                        
                        val riskLevel = categorizePermission(permissionName)
                        
                        permissionsList.add(
                            AppPermission(
                                name = permissionName,
                                riskLevel = riskLevel,
                                isGranted = isGranted
                            )
                        )
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return permissionsList
    }

    private fun categorizePermission(permissionName: String): PermissionRiskLevel {
        val specialPermissions = setOf(
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.WRITE_SETTINGS",
            "android.permission.REQUEST_INSTALL_PACKAGES"
        )

        if (specialPermissions.contains(permissionName)) {
            return PermissionRiskLevel.SPECIAL
        }

        return try {
            val permissionInfo = packageManager.getPermissionInfo(permissionName, 0)
            val protectionLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permissionInfo.protection
            } else {
                @Suppress("DEPRECATION")
                permissionInfo.protectionLevel
            }

            val baseLevel = protectionLevel and PermissionInfo.PROTECTION_MASK_BASE

            when (baseLevel) {
                PermissionInfo.PROTECTION_DANGEROUS -> PermissionRiskLevel.DANGEROUS
                else -> PermissionRiskLevel.NORMAL
            }
        } catch (e: Exception) {
            if (MalwareRules.DANGEROUS_PERMISSIONS.contains(permissionName)) {
                PermissionRiskLevel.DANGEROUS
            } else {
                PermissionRiskLevel.NORMAL
            }
        }
    }
}
