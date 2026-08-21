package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_messages")
data class SupportMessageEntity(
    @PrimaryKey
    val messageId: String,
    val ticketId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val text: String,
    val attachmentUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val isQueuedOffline: Boolean = false
)
