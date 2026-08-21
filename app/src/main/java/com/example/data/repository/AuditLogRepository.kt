package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.entity.AuditLog
import kotlinx.coroutines.flow.Flow

class AuditLogRepository(
    private val auditLogDao: AuditLogDao
) {
    val allLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()

    fun getLogsByAction(action: String): Flow<List<AuditLog>> = auditLogDao.getLogsByAction(action)

    suspend fun logAction(
        userId: Long,
        username: String,
        role: String,
        actionType: String,
        description: String,
        detailsJson: String = ""
    ): Long {
        return auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = actionType,
                description = description,
                detailsJson = detailsJson
            )
        )
    }
}
