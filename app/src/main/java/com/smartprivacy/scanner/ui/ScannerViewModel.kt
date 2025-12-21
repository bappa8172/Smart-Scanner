package com.smartprivacy.scanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartprivacy.scanner.analyzer.DeviceSafetyResult
import com.smartprivacy.scanner.analyzer.RiskEngine
import com.smartprivacy.scanner.analyzer.SafetyAnalyzer
import com.smartprivacy.scanner.analyzer.SafetyStatus
import com.smartprivacy.scanner.data.AppDatabase
import com.smartprivacy.scanner.data.AppEntity
import com.smartprivacy.scanner.data.ScannerRepository
import com.smartprivacy.scanner.data.VirusTotalRepository
import com.smartprivacy.scanner.system.AppScanner
import com.smartprivacy.scanner.system.PackageHelper
import com.smartprivacy.scanner.system.UsageStatsHelper
import com.smartprivacy.scanner.system.JunkCleanerHelper
import com.smartprivacy.scanner.system.StorageAnalyzer
import com.smartprivacy.scanner.system.AppStorageStats
import com.smartprivacy.scanner.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

data class ScannerUiState(
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val currentScanningApp: String = "",
    val apps: List<AppEntity> = emptyList(),
    val systemApps: List<AppEntity> = emptyList(),
    val installedApps: List<AppEntity> = emptyList(),
    val safetyResult: DeviceSafetyResult = DeviceSafetyResult(
        safetyScore = 100,
        status = SafetyStatus.SAFE,
        summary = "Scan your device to check for privacy risks.",
        highRiskCount = 0,
        mediumRiskCount = 0,
        lowRiskCount = 0,
        totalThreats = 0,
        userAppThreats = 0
    ),
    val cacheSize: Long = 0L,
    val internalCacheSize: Long = 0L,
    val externalJunkSize: Long = 0L,
    val isCleaning: Boolean = false,
    val cleaningProgress: Float = 0f,
    val appStorageDetails: Map<String, AppStorageStats> = emptyMap(),
    val showStoragePermissionDialog: Boolean = false,
    val permissionTypeNeeded: String = "", // "STORAGE" or "USAGE"
    val vtLoading: Boolean = false
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScannerRepository
    private val vtRepository = VirusTotalRepository()
    private val safetyAnalyzer = SafetyAnalyzer()
    private val appScanner: AppScanner
    private val junkCleaner = JunkCleanerHelper(application)
    private val storageAnalyzer = StorageAnalyzer(application)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val packageHelper = PackageHelper(application)
        val riskEngine = RiskEngine()
        val usageStatsHelper = UsageStatsHelper(application)
        appScanner = AppScanner(application, database.appDao(), packageHelper, riskEngine, usageStatsHelper)
        repository = ScannerRepository(database.appDao(), appScanner)

        viewModelScope.launch {
            repository.allApps.collectLatest { appList ->
                val system = appList.filter { it.isSystemApp }
                val installed = appList.filter { !it.isSystemApp }
                
                _uiState.value = _uiState.value.copy(
                    apps = appList,
                    systemApps = system,
                    installedApps = installed,
                    safetyResult = safetyAnalyzer.calculateOverallSafety(appList.map { it.riskScore to it.isSystemApp })
                )
            }
        }
        
        refreshStorageStats()
    }

    fun checkVirusTotal(app: AppEntity) {
        val sha256 = app.sha256 ?: return
        if (app.vtDetectionRatio != null) return // Already checked

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(vtLoading = true)
            val report = vtRepository.getAppReport(sha256)
            report?.data?.attributes?.lastAnalysisStats?.let { stats ->
                val total = stats.malicious + stats.harmless + stats.undetected + stats.suspicious
                val ratio = "${stats.malicious}/$total"
                
                val updatedApp = app.copy(
                    vtDetectionRatio = ratio,
                    vtMaliciousCount = stats.malicious
                )
                repository.updateApp(updatedApp)
            }
            _uiState.value = _uiState.value.copy(vtLoading = false)
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Get system-reported internal cache first (Manual cleanup needed)
            val internal = if (PermissionUtils.hasUsageStatsPermission(getApplication())) {
                junkCleaner.getAllAppsCacheSize()
            } else 0L
            
            _uiState.value = _uiState.value.copy(internalCacheSize = internal, cacheSize = internal)

            // 2. Get file-system junk (Auto-cleanable)
            if (PermissionUtils.hasAllFilesAccess()) {
                var external: Long = 0
                junkCleaner.scanForDeletableJunkFlow().collect { (_, junkFiles) ->
                    external = junkFiles.sumOf { it.size }
                    _uiState.value = _uiState.value.copy(
                        externalJunkSize = external,
                        cacheSize = internal + external
                    )
                }
            }
            
            // 3. Update per-app stats
            if (PermissionUtils.hasUsageStatsPermission(getApplication())) {
                val stats = storageAnalyzer.fetchCacheReport()
                _uiState.value = _uiState.value.copy(appStorageDetails = stats.associateBy { it.packageName })
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scanProgress = 0f, currentScanningApp = "Initializing HUD...")
            
            appScanner.scanApps().collect { progress ->
                _uiState.value = _uiState.value.copy(
                    scanProgress = progress,
                    currentScanningApp = "Analyzing apps... ${(progress * 100).toInt()}%"
                )
            }

            _uiState.value = _uiState.value.copy(isScanning = false, currentScanningApp = "")
            refreshStorageStats()
        }
    }

    fun onBoostClicked() {
        if (!PermissionUtils.hasUsageStatsPermission(getApplication())) {
            _uiState.value = _uiState.value.copy(showStoragePermissionDialog = true, permissionTypeNeeded = "USAGE")
        } else if (!PermissionUtils.hasAllFilesAccess()) {
            _uiState.value = _uiState.value.copy(showStoragePermissionDialog = true, permissionTypeNeeded = "STORAGE")
        } else {
            cleanCache()
        }
    }

    fun dismissPermissionDialog() {
        _uiState.value = _uiState.value.copy(showStoragePermissionDialog = false)
    }

    fun requestPermission() {
        val type = _uiState.value.permissionTypeNeeded
        if (type == "USAGE") {
            PermissionUtils.openUsageStatsSettings(getApplication())
        } else {
            PermissionUtils.openAllFilesAccessSettings(getApplication())
        }
        dismissPermissionDialog()
    }

    fun cleanCache() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCleaning = true, cleaningProgress = 0f)
            
            // Perform actual cleaning of auto-deletable items
            junkCleaner.scanForDeletableJunkFlow().collect { (progress, junkFiles) ->
                _uiState.value = _uiState.value.copy(cleaningProgress = progress)
                if (progress == 1.0f) {
                    withContext(Dispatchers.IO) {
                        junkCleaner.cleanJunk(junkFiles)
                        delay(1000)
                    }
                }
            }
            
            // After cleaning, reset external junk to 0
            _uiState.value = _uiState.value.copy(
                isCleaning = false, 
                externalJunkSize = 0L, 
                cleaningProgress = 0f
            )
            
            // Refresh stats to show what remains (Internal Cache)
            refreshStorageStats()
        }
    }

    fun openSystemDeepClean() {
        junkCleaner.openSystemCleaner()
    }
}
