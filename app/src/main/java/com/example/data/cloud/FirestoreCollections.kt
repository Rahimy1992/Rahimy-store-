package com.example.data.cloud

import com.example.data.local.entity.Product
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.data.local.entity.User

/**
 * Cloud Firestore Collection Names and Schema DTOs for Rahimy Smart Commerce.
 */
object FirestoreCollections {
    const val USERS = "users"
    const val PRODUCTS = "products"
    const val CATEGORIES = "categories"
    const val BRANDS = "brands"
    const val INVENTORY = "inventory"
    const val CUSTOMERS = "customers"
    const val EMPLOYEES = "employees"
    const val SALES = "sales"
    const val SALE_ITEMS = "saleItems"
    const val ORDERS = "orders"
    const val ORDER_ITEMS = "orderItems"
    const val PAYMENTS = "payments"
    const val CURRENCY_CONFIGS = "currencyConfigs"
    const val SETTINGS = "settings"
    const val AUDIT_LOGS = "auditLogs"
    const val NOTIFICATIONS = "notifications"
    const val AI_CONVERSATIONS = "aiConversations"
    const val CATALOG_SCANS = "catalogScans"
    const val BACKUPS = "backups"
    const val SUPPORT_TICKETS = "supportTickets"
}

data class CloudSupportTicket(
    val ticketId: String = "",
    val userId: String = "",
    val userName: String = "",
    val subject: String = "",
    val category: String = "GENERAL",
    val status: String = "OPEN",
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "",
    val unreadCount: Int = 0,
    val assignedTo: String? = null
)

data class CloudSupportMessage(
    val messageId: String = "",
    val ticketId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "CUSTOMER",
    val text: String = "",
    val attachmentUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = System.currentTimeMillis(),
    val readAt: Long? = null
)

data class CloudProduct(
    val id: String = "",
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val category: String = "",
    val brand: String = "",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val minStockThreshold: Int = 5,
    val description: String = "",
    val imageUris: List<String> = emptyList(),
    val primaryImageIndex: Int = 0,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class CloudSale(
    val id: String = "",
    val invoiceNumber: String = "",
    val cashierId: Long = 0L,
    val cashierName: String = "",
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val status: String = "ACTIVE",
    val voidReason: String? = null,
    val voidedByUserId: Long? = null,
    val voidedAt: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncedAt: Long = System.currentTimeMillis()
)

data class CloudSaleItem(
    val id: String = "",
    val saleId: Long = 0L,
    val productId: Long = 0L,
    val productName: String = "",
    val productSku: String = "",
    val quantity: Int = 0,
    val unitCostSnapshot: Double = 0.0,
    val unitPriceSnapshot: Double = 0.0,
    val subtotal: Double = 0.0,
    val costTotal: Double = 0.0,
    val grossProfit: Double = 0.0
)
