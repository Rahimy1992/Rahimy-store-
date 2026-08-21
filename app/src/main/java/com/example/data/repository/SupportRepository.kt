package com.example.data.repository

import android.content.Context
import com.example.data.cloud.FirestoreCollections
import com.example.data.cloud.RahimyFirebaseMessagingService
import com.example.data.local.dao.SupportDao
import com.example.data.local.entity.SupportMessageEntity
import com.example.data.local.entity.SupportTicketEntity
import com.example.data.local.entity.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class SupportRepository(
    private val context: Context,
    private val supportDao: SupportDao
) {
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun getTicketsForUser(userId: String, userRole: String): Flow<List<SupportTicketEntity>> {
        return when (userRole) {
            UserRole.CUSTOMER.name, UserRole.EMPLOYEE.name -> {
                supportDao.getTicketsForUserFlow(userId)
            }
            else -> { // MANAGER, SUPER_ADMIN, VIEWER
                supportDao.getAllTicketsFlow()
            }
        }
    }

    fun getMessagesForTicket(ticketId: String): Flow<List<SupportMessageEntity>> {
        return supportDao.getMessagesForTicketFlow(ticketId)
    }

    suspend fun createTicket(
        userId: String,
        userName: String,
        userRole: String,
        subject: String,
        category: String,
        priority: String = "MEDIUM",
        initialMessageText: String,
        attachmentUrl: String? = null,
        isOnline: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val ticketId = "TICK-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()

        val ticket = SupportTicketEntity(
            ticketId = ticketId,
            userId = userId,
            userName = userName,
            subject = subject,
            category = category,
            status = "OPEN",
            priority = priority,
            createdAt = now,
            updatedAt = now,
            lastMessage = initialMessageText,
            unreadCount = 0,
            assignedTo = null,
            isSynced = isOnline
        )

        supportDao.insertTicket(ticket)

        val messageId = "MSG-" + UUID.randomUUID().toString().take(8).uppercase()
        val firstMsg = SupportMessageEntity(
            messageId = messageId,
            ticketId = ticketId,
            senderId = userId,
            senderName = userName,
            senderRole = userRole,
            text = initialMessageText,
            attachmentUrl = attachmentUrl,
            createdAt = now,
            deliveredAt = if (isOnline) now else null,
            readAt = null,
            isQueuedOffline = !isOnline
        )

        supportDao.insertMessage(firstMsg)

        if (isOnline) {
            try {
                firestore.collection(FirestoreCollections.SUPPORT_TICKETS)
                    .document(ticketId)
                    .set(
                        mapOf(
                            "ticketId" to ticketId,
                            "userId" to userId,
                            "userName" to userName,
                            "subject" to subject,
                            "category" to category,
                            "status" to "OPEN",
                            "priority" to priority,
                            "createdAt" to now,
                            "updatedAt" to now,
                            "lastMessage" to initialMessageText,
                            "unreadCount" to 0,
                            "assignedTo" to null
                        )
                    )

                firestore.collection(FirestoreCollections.SUPPORT_TICKETS)
                    .document(ticketId)
                    .collection("messages")
                    .document(messageId)
                    .set(
                        mapOf(
                            "messageId" to messageId,
                            "ticketId" to ticketId,
                            "senderId" to userId,
                            "senderName" to userName,
                            "senderRole" to userRole,
                            "text" to initialMessageText,
                            "attachmentUrl" to attachmentUrl,
                            "createdAt" to now,
                            "deliveredAt" to now,
                            "readAt" to null
                        )
                    )
            } catch (e: Exception) {
                // Keep local Room state saved
            }
        }

        ticketId
    }

    suspend fun sendMessage(
        ticketId: String,
        senderId: String,
        senderName: String,
        senderRole: String,
        text: String,
        attachmentUrl: String? = null,
        isOnline: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val messageId = "MSG-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()

        val msg = SupportMessageEntity(
            messageId = messageId,
            ticketId = ticketId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            text = text,
            attachmentUrl = attachmentUrl,
            createdAt = now,
            deliveredAt = if (isOnline) now else null,
            readAt = null,
            isQueuedOffline = !isOnline
        )

        supportDao.insertMessage(msg)

        val ticket = supportDao.getTicketById(ticketId)
        if (ticket != null) {
            val updatedTicket = ticket.copy(
                lastMessage = text,
                updatedAt = now,
                unreadCount = if (senderRole == UserRole.CUSTOMER.name) ticket.unreadCount + 1 else ticket.unreadCount
            )
            supportDao.updateTicket(updatedTicket)
        }

        if (isOnline) {
            try {
                firestore.collection(FirestoreCollections.SUPPORT_TICKETS)
                    .document(ticketId)
                    .collection("messages")
                    .document(messageId)
                    .set(
                        mapOf(
                            "messageId" to messageId,
                            "ticketId" to ticketId,
                            "senderId" to senderId,
                            "senderName" to senderName,
                            "senderRole" to senderRole,
                            "text" to text,
                            "attachmentUrl" to attachmentUrl,
                            "createdAt" to now,
                            "deliveredAt" to now,
                            "readAt" to null
                        )
                    )

                firestore.collection(FirestoreCollections.SUPPORT_TICKETS)
                    .document(ticketId)
                    .update("lastMessage", text, "updatedAt", now)
            } catch (e: Exception) {
                // Handled gracefully via Room queue
            }
        }

        // Trigger FCM local notification if response is from support agent/staff to customer
        if (senderRole == UserRole.SUPER_ADMIN.name || senderRole == UserRole.MANAGER.name) {
            RahimyFirebaseMessagingService.showLocalNotification(
                context = context,
                title = "پاسخ پشتیبانی رحیمی (Support Reply)",
                message = "$senderName: $text"
            )
        }

        messageId
    }

    suspend fun updateTicketStatus(
        ticketId: String,
        newStatus: String,
        assignedTo: String? = null
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val ticket = supportDao.getTicketById(ticketId)
        if (ticket != null) {
            val updated = ticket.copy(
                status = newStatus,
                assignedTo = assignedTo ?: ticket.assignedTo,
                updatedAt = now
            )
            supportDao.updateTicket(updated)

            try {
                firestore.collection(FirestoreCollections.SUPPORT_TICKETS)
                    .document(ticketId)
                    .update(
                        mapOf(
                            "status" to newStatus,
                            "assignedTo" to (assignedTo ?: ticket.assignedTo),
                            "updatedAt" to now
                        )
                    )
            } catch (e: Exception) {
                // Room update preserved
            }
        }
    }

    suspend fun markMessagesAsRead(ticketId: String) = withContext(Dispatchers.IO) {
        supportDao.clearUnreadCount(ticketId)
    }

    suspend fun syncQueuedOfflineMessages(): Int = withContext(Dispatchers.IO) {
        val queued = supportDao.getQueuedOfflineMessages()
        var syncedCount = 0
        val now = System.currentTimeMillis()

        for (msg in queued) {
            try {
                firestore.collection(FirestoreCollections.SUPPORT_TICKETS)
                    .document(msg.ticketId)
                    .collection("messages")
                    .document(msg.messageId)
                    .set(
                        mapOf(
                            "messageId" to msg.messageId,
                            "ticketId" to msg.ticketId,
                            "senderId" to msg.senderId,
                            "senderName" to msg.senderName,
                            "senderRole" to msg.senderRole,
                            "text" to msg.text,
                            "attachmentUrl" to msg.attachmentUrl,
                            "createdAt" to msg.createdAt,
                            "deliveredAt" to now,
                            "readAt" to null
                        )
                    )

                supportDao.markMessageSynced(msg.messageId, now)
                syncedCount++
            } catch (e: Exception) {
                // Will retry on next sync interval
            }
        }
        syncedCount
    }
}
