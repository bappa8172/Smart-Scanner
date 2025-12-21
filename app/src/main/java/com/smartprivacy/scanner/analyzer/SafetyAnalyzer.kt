package com.smartprivacy.scanner.analyzer

data class DeviceSafetyResult(
    val safetyScore: Int,
    val status: SafetyStatus,
    val summary: String,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
    val totalThreats: Int,
    val userAppThreats: Int
)

enum class SafetyStatus {
    SAFE,
    WARNING,
    DANGER
}

class SafetyAnalyzer {

    fun calculateOverallSafety(appRiskScores: List<Pair<Int, Boolean>>): DeviceSafetyResult {
        if (appRiskScores.isEmpty()) {
            return DeviceSafetyResult(100, SafetyStatus.SAFE, "Scan your device to detect privacy threats.", 0, 0, 0, 0, 0)
        }

        // 1. Identify threats with higher threshold for system apps
        val highRiskApps = appRiskScores.filter { (score, isSystem) -> 
            if (isSystem) score >= 90 else score >= 75 
        }
        val mediumRiskApps = appRiskScores.filter { (score, isSystem) -> 
            if (isSystem) score in 70..89 else score in 40..74 
        }

        val highRiskCount = highRiskApps.size
        val mediumRiskCount = mediumRiskApps.size
        val totalThreats = highRiskCount + mediumRiskCount
        val userAppThreats = appRiskScores.count { (score, isSystem) -> !isSystem && score >= 40 }

        // Status determination
        val status = when {
            highRiskCount > 0 -> SafetyStatus.DANGER
            mediumRiskCount > 0 -> SafetyStatus.WARNING
            else -> SafetyStatus.SAFE
        }

        val summary = when (status) {
            SafetyStatus.SAFE -> "Your device is well protected. No major privacy threats found."
            SafetyStatus.WARNING -> "Action recommended. $mediumRiskCount moderate risks detected."
            SafetyStatus.DANGER -> "Critical risks detected! $highRiskCount high-risk apps found."
        }

        // Final score calculation
        val maxRisk = appRiskScores.map { it.first }.maxOrNull() ?: 0
        var score = 100 - maxRisk
        score -= (highRiskCount * 5)
        score -= (mediumRiskCount * 2)
        val finalScore = score.coerceIn(0, 100)

        return DeviceSafetyResult(
            safetyScore = finalScore,
            status = status,
            summary = summary,
            highRiskCount = highRiskCount,
            mediumRiskCount = mediumRiskCount,
            lowRiskCount = appRiskScores.size - totalThreats,
            totalThreats = totalThreats,
            userAppThreats = userAppThreats
        )
    }
}
