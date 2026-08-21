package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: Long,
    val username: String,
    val userRole: String,
    val actionType: String, // E.g., "PRODUCT_BULK_DELETE", "CURRENCY_RATE_CHANGE", "USER_ROLE_CHANGE", "SALE_VOID"
    val description: String,
    val detailsJson: String = ""
)
