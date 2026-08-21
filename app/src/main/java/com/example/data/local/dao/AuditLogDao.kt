package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AuditLog
import kotlinx.coroutines.flow.Flow

/**
 * AuditLogDao strictly enforces append-only logging (No DELETE, No UPDATE).
 */
@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE actionType = :actionType ORDER BY timestamp DESC")
    fun getLogsByAction(actionType: String): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: AuditLog): Long
}
