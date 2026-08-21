package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "business_customers")
data class BusinessCustomerEntity(
    @PrimaryKey
    val businessId: String,
    val businessName: String,
    val businessType: String = "WHOLESALE", // WHOLESALE, DISTRIBUTOR, RETAILER, COMPANY
    val ownerName: String,
    val contactPerson: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String,
    val country: String = "Afghanistan",
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val customerCode: String,
    val currency: String = "USD",
    val paymentTerms: String = "NET_30", // CASH, NET_7, NET_15, NET_30, NET_45, NET_60, CUSTOM
    val creditLimit: Double = 5000.0,
    val currentBalance: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED, BLOCKED, PENDING_APPROVAL
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "wholesale_price_lists")
data class WholesalePriceListEntity(
    @PrimaryKey
    val priceListId: String,
    val name: String,
    val businessType: String = "WHOLESALE", // WHOLESALE, DISTRIBUTOR, RETAILER, COMPANY, VIP
    val currency: String = "USD",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "wholesale_price_tiers")
data class WholesalePriceTierEntity(
    @PrimaryKey
    val tierId: String,
    val priceListId: String? = null,
    val productId: Long,
    val customerBusinessId: String? = null, // null for general tier, non-null for customer-specific
    val minQuantity: Int = 1,
    val maxQuantity: Int? = null,
    val tierName: String = "WHOLESALE", // RETAIL, WHOLESALE, DISTRIBUTOR, VIP, CUSTOM
    val priceUsd: Double,
    val discountPercent: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "b2b_quotations")
data class B2bQuotationEntity(
    @PrimaryKey
    val quotationId: String,
    val businessId: String,
    val businessName: String,
    val subtotalUsd: Double,
    val discountUsd: Double = 0.0,
    val taxUsd: Double = 0.0,
    val shippingUsd: Double = 0.0,
    val totalUsd: Double,
    val currency: String = "USD",
    val exchangeRate: Double = 1.0,
    val status: String = "DRAFT", // DRAFT, PENDING_APPROVAL, SENT, ACCEPTED, REJECTED, EXPIRED, CONVERTED
    val validUntil: Long = System.currentTimeMillis() + 14 * 86400000L,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "b2b_quotation_items")
data class B2bQuotationItemEntity(
    @PrimaryKey
    val itemId: String,
    val quotationId: String,
    val productId: Long,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val unitPriceUsd: Double,
    val discountUsd: Double = 0.0,
    val subtotalUsd: Double
)

@Entity(tableName = "b2b_orders")
data class B2bOrderEntity(
    @PrimaryKey
    val orderId: String,
    val quotationId: String? = null,
    val businessId: String,
    val businessName: String,
    val orderStatus: String = "DRAFT", // DRAFT, PENDING_APPROVAL, CONFIRMED, PROCESSING, READY, SHIPPED, DELIVERED, CANCELLED, RETURNED
    val paymentTerms: String = "NET_30",
    val subtotalUsd: Double,
    val discountUsd: Double = 0.0,
    val taxUsd: Double = 0.0,
    val shippingUsd: Double = 0.0,
    val totalUsd: Double,
    val currency: String = "USD",
    val exchangeRate: Double = 1.0,
    val deliveryAddress: String,
    val expectedDeliveryDate: Long? = null,
    val customerNotes: String? = null,
    val internalNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "b2b_order_items")
data class B2bOrderItemEntity(
    @PrimaryKey
    val itemId: String,
    val orderId: String,
    val productId: Long,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val unitPriceUsd: Double,
    val discountUsd: Double = 0.0,
    val subtotalUsd: Double
)

@Entity(tableName = "b2b_invoices")
data class B2bInvoiceEntity(
    @PrimaryKey
    val invoiceId: String,
    val orderId: String,
    val businessId: String,
    val businessName: String,
    val invoiceNumber: String,
    val subtotalUsd: Double,
    val discountUsd: Double = 0.0,
    val taxUsd: Double = 0.0,
    val shippingUsd: Double = 0.0,
    val totalUsd: Double,
    val paidAmountUsd: Double = 0.0,
    val currency: String = "USD",
    val exchangeRate: Double = 1.0,
    val paymentStatus: String = "UNPAID", // UNPAID, PARTIALLY_PAID, PAID, OVERDUE, VOID
    val paymentTerms: String = "NET_30",
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 30 * 86400000L,
    val notes: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "b2b_payments")
data class B2bPaymentEntity(
    @PrimaryKey
    val paymentId: String,
    val businessId: String,
    val invoiceId: String,
    val amountUsd: Double,
    val currency: String = "USD",
    val exchangeRate: Double = 1.0,
    val paymentMethod: String = "BANK_TRANSFER", // CASH, BANK_TRANSFER, CARD, CHEQUE, OTHER
    val referenceNumber: String,
    val receivedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "b2b_returns")
data class B2bReturnEntity(
    @PrimaryKey
    val returnId: String,
    val orderId: String,
    val invoiceId: String,
    val businessId: String,
    val reason: String = "DAMAGED", // DAMAGED, WRONG_PRODUCT, CUSTOMER_CANCEL, QUALITY_ISSUE
    val status: String = "APPROVED", // PENDING, APPROVED, REJECTED, COMPLETED
    val refundAmountUsd: Double,
    val restockInventory: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "b2b_deliveries")
data class B2bDeliveryEntity(
    @PrimaryKey
    val deliveryId: String,
    val orderId: String,
    val businessId: String,
    val deliveryAddress: String,
    val contactPerson: String,
    val phone: String,
    val deliveryStatus: String = "PENDING", // PENDING, PREPARING, SHIPPED, IN_TRANSIT, DELIVERED, FAILED, RETURNED
    val shippingCostUsd: Double = 0.0,
    val trackingNumber: String? = null,
    val driverName: String? = null,
    val expectedDeliveryDate: Long? = null,
    val deliveredAt: Long? = null,
    val isSynced: Boolean = false
)

// --- Relation POJOs ---

data class B2bOrderWithItems(
    @Embedded val order: B2bOrderEntity,
    @Relation(
        parentColumn = "orderId",
        entityColumn = "orderId"
    )
    val items: List<B2bOrderItemEntity>
)

data class BusinessCustomerWithOrders(
    @Embedded val customer: BusinessCustomerEntity,
    @Relation(
        parentColumn = "businessId",
        entityColumn = "businessId"
    )
    val orders: List<B2bOrderEntity>
)

data class WholesalePriceListWithTiers(
    @Embedded val priceList: WholesalePriceListEntity,
    @Relation(
        parentColumn = "priceListId",
        entityColumn = "priceListId"
    )
    val tiers: List<WholesalePriceTierEntity>
)

typealias BusinessCustomer = BusinessCustomerEntity
typealias WholesalePriceList = WholesalePriceListEntity
typealias B2bOrder = B2bOrderEntity
typealias B2BOrder = B2bOrderEntity

