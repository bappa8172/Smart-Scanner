package com.smartprivacy.scanner.data

import com.smartprivacy.scanner.system.AppScanner
import kotlinx.coroutines.flow.Flow

class ScannerRepository(
    private val appDao: AppDao,
    private val appScanner: AppScanner
) {
    val allApps: Flow<List<AppEntity>> = appDao.getAllApps()

    suspend fun performScan() {
        // AppScanner.scanApps() returns a Flow. 
        // We collect it here to perform the scan.
        appScanner.scanApps().collect {}
    }

    suspend fun getAppDetails(packageName: String): AppEntity? {
        return appDao.getAppByPackageName(packageName)
    }

    suspend fun updateApp(app: AppEntity) {
        appDao.updateApp(app)
    }
}
