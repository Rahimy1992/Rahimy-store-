package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getAllSalesSync(): List<Sale>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getSalesBetweenDirect(startTime: Long, endTime: Long): List<Sale>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE id IN (:ids)")
    suspend fun getSalesByIds(ids: List<Long>): List<Sale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("UPDATE sales SET status = :status, voidReason = :reason, voidedByUserId = :voidedBy, voidedAt = :timestamp WHERE id = :saleId")
    suspend fun markSaleVoid(
        saleId: Long,
        status: SaleStatus,
        reason: String,
        voidedBy: Long?,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE sales SET status = :status, voidReason = :reason, voidedByUserId = :voidedBy, voidedAt = :timestamp WHERE id IN (:saleIds)")
    suspend fun markMultipleSalesVoid(
        saleIds: List<Long>,
        status: SaleStatus,
        reason: String,
        voidedBy: Long?,
        timestamp: Long = System.currentTimeMillis()
    )
    @Query("SELECT COUNT(*) FROM sales WHERE status = 'ACTIVE'")
    suspend fun getUnsyncedSalesCount(): Int

    @Query("SELECT * FROM sales WHERE status = 'ACTIVE'")
    suspend fun getUnsyncedSales(): List<Sale>

    @Query("UPDATE sales SET notes = :note WHERE id = :saleId")
    suspend fun markSaleSynced(saleId: Long, note: String = "SYNCED")
}
