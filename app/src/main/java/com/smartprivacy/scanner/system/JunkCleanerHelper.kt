package com.smartprivacy.scanner.system

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class JunkFile(
    val name: String,
    val path: String,
    val size: Long,
    val type: JunkType
)

enum class JunkType {
    INTERNAL_CACHE, // System-protected (Manual clean only)
    EXTERNAL_CACHE, // Deletable app cache
    TEMP_FILE,
    LOG_FILE,
    THUMBNAIL,
    ORPHAN_APK
}

class JunkCleanerHelper(private val context: Context) {

    private val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager

    /**
     * Reports total internal cache size of all apps.
     * This is protected by the OS and requires manual user action to clear.
     */
    fun getAllAppsCacheSize(): Long {
        var totalCache: Long = 0
        if (storageStatsManager == null) return 0L
        
        try {
            val packageManager = context.packageManager
            val installedApps = packageManager.getInstalledApplications(0)

            for (app in installedApps) {
                if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    try {
                        val storageStats = storageStatsManager.queryStatsForPackage(
                            StorageManager.UUID_DEFAULT,
                            app.packageName,
                            Process.myUserHandle()
                        )
                        totalCache += storageStats.cacheBytes
                    } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) {}
        return totalCache
    }

    /**
     * Scans for files that the app actually has PERMISSION to delete.
     * Targets temp folders, thumbnails, and accessible external caches.
     */
    fun scanForDeletableJunkFlow(): Flow<Pair<Float, List<JunkFile>>> = flow {
        val junkList = mutableListOf<JunkFile>()
        val externalDir = Environment.getExternalStorageDirectory() ?: return@flow
        
        val junkFolders = setOf("temp", "cache", ".cache", "logs", ".thumbnails", "thumbnails")
        val junkExtensions = setOf("tmp", "log", "temp", "apk", "cache", "bak")

        val scanPaths = mutableListOf<File>()
        externalDir.listFiles()?.let { scanPaths.addAll(it) }
        
        val androidData = File(externalDir, "Android/data")
        if (androidData.exists()) {
            androidData.listFiles()?.let { scanPaths.addAll(it) }
        }

        val total = scanPaths.size.toFloat()
        if (total == 0f) {
            emit(1.0f to emptyList())
            return@flow
        }

        scanPaths.forEachIndexed { index, root ->
            try {
                root.walkTopDown().maxDepth(4).forEach { file ->
                    val name = file.name.lowercase()
                    if (file.isDirectory) {
                        if (name == "cache" || name == "code_cache" || junkFolders.contains(name)) {
                            val size = getFolderSize(file)
                            if (size > 0) junkList.add(JunkFile(file.name, file.absolutePath, size, JunkType.EXTERNAL_CACHE))
                        }
                    } else {
                        if (junkExtensions.contains(file.extension.lowercase()) || name.startsWith(".thumb")) {
                            junkList.add(JunkFile(file.name, file.absolutePath, file.length(), JunkType.TEMP_FILE))
                        }
                    }
                }
            } catch (e: Exception) {}
            
            if (index % 10 == 0 || index == scanPaths.size - 1) {
                emit((index + 1) / total to junkList.toList())
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        try {
            file.walkTopDown().maxDepth(10).forEach { if (it.isFile) size += it.length() }
        } catch (e: Exception) {}
        return size
    }

    /**
     * Deletes identified junk files.
     */
    fun cleanJunk(files: List<JunkFile>): Boolean {
        var deletedAny = false
        files.forEach { junk ->
            try {
                val file = File(junk.path)
                if (file.exists()) {
                    if (file.deleteRecursively()) deletedAny = true
                }
            } catch (e: Exception) {}
        }
        
        // Also clear this app's own cache
        try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (e: Exception) {}
        
        return deletedAny
    }

    fun openSystemCleaner() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
