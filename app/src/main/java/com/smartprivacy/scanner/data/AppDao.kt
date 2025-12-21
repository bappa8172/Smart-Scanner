package com.smartprivacy.scanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY riskScore DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps")
    suspend fun getAllAppsOnce(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("DELETE FROM apps")
    suspend fun deleteAll()
}
