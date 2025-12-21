package com.smartprivacy.scanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartprivacy.scanner.analyzer.PrivacyAlert

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val version: String,
    val isSystemApp: Boolean,
    val installTime: Long,
    val updateTime: Long,
    val riskScore: Int,
    val riskLevel: String, // Low, Medium, High
    val riskReasons: List<String>,
    val privacyAlerts: List<PrivacyAlert>,
    val permissions: List<String>,
    val lastUsed: Long = 0L,
    val isSideloaded: Boolean = false,
    val sha256: String? = null,
    val vtDetectionRatio: String? = null,
    val vtMaliciousCount: Int = 0
)
