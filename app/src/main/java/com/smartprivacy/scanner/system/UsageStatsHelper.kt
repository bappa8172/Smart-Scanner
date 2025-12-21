package com.smartprivacy.scanner.system

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.smartprivacy.scanner.utils.PermissionUtils
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Fetches a map of package name to last used time for all apps in one go.
     * This is significantly faster than querying per app.
     */
    fun getAllLastUsedTimes(): Map<String, Long> {
        if (!PermissionUtils.hasUsageStatsPermission(context) || usageStatsManager == null) {
            return emptyMap()
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        return stats?.associate { it.packageName to it.lastTimeUsed } ?: emptyMap()
    }

    fun getLastUsedTime(packageName: String): Long {
        return getAllLastUsedTimes()[packageName] ?: 0L
    }
}
