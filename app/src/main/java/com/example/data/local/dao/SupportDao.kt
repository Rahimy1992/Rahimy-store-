package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SupportMessageEntity
import com.example.data.local.entity.SupportTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupportDao {

    // --- Support Tickets ---
    @Query("SELECT * FROM support_tickets ORDER BY updatedAt DESC")
    fun getAllTicketsFlow(): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getTicketsForUserFlow(userId: String): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE ticketId = :ticketId")
    suspend fun getTicketById(ticketId: String): SupportTicketEntity?

    @Query("SELECT * FROM support_tickets WHERE isSynced = 0")
    suspend fun getUnsyncedTickets(): List<SupportTicketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<SupportTicketEntity>)

    @Update
    suspend fun updateTicket(ticket: SupportTicketEntity)

    @Query("UPDATE support_tickets SET status = :status, updatedAt = :updatedAt WHERE ticketId = :ticketId")
    suspend fun updateTicketStatus(ticketId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE support_tickets SET unreadCount = 0 WHERE ticketId = :ticketId")
    suspend fun clearUnreadCount(ticketId: String)

    @Query("SELECT COUNT(*) FROM support_tickets WHERE status != 'CLOSED' AND status != 'RESOLVED'")
    fun getOpenTicketsCountFlow(): Flow<Int>

    // --- Support Messages ---
    @Query("SELECT * FROM support_messages WHERE ticketId = :ticketId ORDER BY createdAt ASC")
    fun getMessagesForTicketFlow(ticketId: String): Flow<List<SupportMessageEntity>>

    @Query("SELECT * FROM support_messages WHERE ticketId = :ticketId ORDER BY createdAt ASC")
    suspend fun getMessagesForTicketSync(ticketId: String): List<SupportMessageEntity>

    @Query("SELECT * FROM support_messages WHERE isQueuedOffline = 1")
    suspend fun getQueuedOfflineMessages(): List<SupportMessageEntity>

    @Query("SELECT COUNT(*) FROM support_messages WHERE isQueuedOffline = 1")
    fun getQueuedOfflineMessagesCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SupportMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SupportMessageEntity>)

    @Update
    suspend fun updateMessage(message: SupportMessageEntity)

    @Query("UPDATE support_messages SET isQueuedOffline = 0, deliveredAt = :deliveredAt WHERE messageId = :messageId")
    suspend fun markMessageSynced(messageId: String, deliveredAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM support_messages WHERE readAt IS NULL AND senderId != :currentUserId")
    fun getTotalUnreadMessagesCountFlow(currentUserId: String): Flow<Int>
}
