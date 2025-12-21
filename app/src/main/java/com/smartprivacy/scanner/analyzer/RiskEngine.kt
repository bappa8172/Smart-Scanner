package com.smartprivacy.scanner.analyzer

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build

data class PrivacyAlert(
    val title: String,
    val description: String,
    val severity: RiskSeverity
)

enum class RiskSeverity {
    HIGH, MEDIUM, LOW
}

data class RiskResult(
    val score: Int,
    val level: String,
    val alerts: List<PrivacyAlert>
)

class RiskEngine {

    fun calculateRisk(packageInfo: PackageInfo, isSideloaded: Boolean): RiskResult {
        val appInfo = packageInfo.applicationInfo
        val packageName = packageInfo.packageName
        val isSystemApp = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        
        var score = 0
        val alerts = mutableListOf<PrivacyAlert>()
        val permissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()

        // 1. Tracker Detection (Exodus-style integration)
        val trackers = TrackerDatabase.KNOWN_TRACKERS[packageName] ?: emptyList()
        if (trackers.isNotEmpty()) {
            score += 20
            alerts.add(PrivacyAlert(
                "Integrated Trackers",
                "This app contains ${trackers.size} known data tracking services: ${trackers.joinToString(", ")}.",
                RiskSeverity.MEDIUM
            ))
        }

        // 2. Context-Aware Permission Analysis
        // Rule: Camera/Mic + Internet is risky ONLY if trackers are present or app is not a communication tool
        val hasSurveillance = permissions.contains(Manifest.permission.CAMERA) || permissions.contains(Manifest.permission.RECORD_AUDIO)
        val hasInternet = permissions.contains(Manifest.permission.INTERNET)
        
        if (hasSurveillance && hasInternet) {
            val isCommunicationApp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo?.category == ApplicationInfo.CATEGORY_SOCIAL || appInfo?.category == 2 // CATEGORY_COMMUNICATION
            } else false

            if (!isCommunicationApp && !isSystemApp) {
                score += 30
                alerts.add(PrivacyAlert(
                    "Unusual Hardware Access",
                    "This app can access your camera/mic and the internet, but isn't categorized as a communication tool.",
                    RiskSeverity.HIGH
                ))
            }
        }

        // 3. Location Context
        if (permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            val needsLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo?.category == ApplicationInfo.CATEGORY_MAPS || appInfo?.category == 3 // CATEGORY_NAVIGATION
            } else false

            if (!needsLocation && !isSystemApp) {
                score += 15
                alerts.add(PrivacyAlert(
                    "Unexpected Location Use",
                    "This app requests precise location but does not appear to be a navigation or maps tool.",
                    RiskSeverity.MEDIUM
                ))
            }
        }

        // 4. Critical Logic: SMS + Trackers = High Risk
        if (permissions.contains(Manifest.permission.READ_SMS) && trackers.isNotEmpty()) {
            score += 40
            alerts.add(PrivacyAlert(
                "Sensitive Data Exposure",
                "High Risk: This app can read your private messages and contains active third-party tracking services.",
                RiskSeverity.HIGH
            ))
        }

        // 5. Accessibility Monitoring
        if (permissions.contains(Manifest.permission.BIND_ACCESSIBILITY_SERVICE) && !isSystemApp) {
            score += 50
            alerts.add(PrivacyAlert(
                "Advanced Monitoring",
                "Requests control via Accessibility Services. This allows the app to interact with other apps and read screen content.",
                RiskSeverity.HIGH
            ))
        }

        // Final score normalization
        val finalScore = if (isSystemApp) (score / 5).coerceAtMost(20) else score.coerceAtMost(100)
        
        return RiskResult(
            score = finalScore,
            level = getRiskLevel(finalScore, isSystemApp),
            alerts = alerts
        )
    }

    private fun getRiskLevel(score: Int, isSystemApp: Boolean): String {
        return when {
            score >= 70 -> "High Risk"
            score >= 35 -> "Moderate Concern"
            isSystemApp -> "System Safe"
            else -> "Low Concern"
        }
    }
}
