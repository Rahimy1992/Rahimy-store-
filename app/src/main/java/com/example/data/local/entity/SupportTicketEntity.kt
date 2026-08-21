package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SupportStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    RESOLVED,
    CLOSED
}

enum class SupportPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey
    val ticketId: String,
    val userId: String,
    val userName: String,
    val subject: String,
    val category: String,
    val status: String = SupportStatus.OPEN.name,
    val priority: String = SupportPriority.MEDIUM.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "",
    val unreadCount: Int = 0,
    val assignedTo: String? = null,
    val isSynced: Boolean = false
)
