package com.smartprivacy.scanner.data

import androidx.room.TypeConverter
import com.smartprivacy.scanner.analyzer.PrivacyAlert
import com.smartprivacy.scanner.analyzer.RiskSeverity

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromAlertList(value: String): List<PrivacyAlert> {
        if (value.isEmpty()) return emptyList()
        return value.split("||").map {
            val parts = it.split("|")
            PrivacyAlert(parts[0], parts[1], RiskSeverity.valueOf(parts[2]))
        }
    }

    @TypeConverter
    fun toAlertList(list: List<PrivacyAlert>): String {
        return list.joinToString("||") { "${it.title}|${it.description}|${it.severity.name}" }
    }
}
