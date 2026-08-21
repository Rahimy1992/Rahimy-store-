package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CatalogScan
import com.example.data.local.entity.ScanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogScanDao {
    @Query("SELECT * FROM catalog_scans ORDER BY scanTimestamp DESC")
    fun getAllScans(): Flow<List<CatalogScan>>

    @Query("SELECT * FROM catalog_scans WHERE id = :id")
    suspend fun getScanById(id: Long): CatalogScan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: CatalogScan): Long

    @Update
    suspend fun updateScan(scan: CatalogScan)

    @Query("UPDATE catalog_scans SET status = :status WHERE id = :id")
    suspend fun updateScanStatus(id: Long, status: ScanStatus)

    @Delete
    suspend fun deleteScan(scan: CatalogScan)
}
