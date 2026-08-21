package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.B2bDeliveryEntity
import com.example.data.local.entity.B2bInvoiceEntity
import com.example.data.local.entity.B2bOrderEntity
import com.example.data.local.entity.B2bOrderItemEntity
import com.example.data.local.entity.B2bPaymentEntity
import com.example.data.local.entity.B2bQuotationEntity
import com.example.data.local.entity.B2bQuotationItemEntity
import com.example.data.local.entity.B2bReturnEntity
import com.example.data.local.entity.BusinessCustomerEntity
import com.example.data.local.entity.WholesalePriceTierEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class CustomerStatement(
    val businessId: String,
    val businessName: String,
    val openingBalanceUsd: Double,
    val totalInvoicedUsd: Double,
    val totalPaidUsd: Double,
    val currentBalanceUsd: Double,
    val overdueBalanceUsd: Double,
    val creditLimitUsd: Double,
    val availableCreditUsd: Double,
    val invoices: List<B2bInvoiceEntity>,
    val payments: List<B2bPaymentEntity>
)

class B2bRepository(
    context: Context,
    private val db: AppDatabase
) {
    private val b2bDao = db.b2bDao()
    private val productDao = db.productDao()

    // --- Business Customers ---
    val allBusinessCustomers: Flow<List<BusinessCustomerEntity>> = b2bDao.getAllBusinessCustomersFlow()

    suspend fun getBusinessCustomerById(businessId: String): BusinessCustomerEntity? {
        return b2bDao.getBusinessCustomerById(businessId)
    }

    suspend fun saveBusinessCustomer(customer: BusinessCustomerEntity) {
        b2bDao.insertBusinessCustomer(customer)
    }

    suspend fun checkCreditLimit(businessId: String, additionalAmountUsd: Double): Boolean {
        val customer = b2bDao.getBusinessCustomerById(businessId) ?: return true
        return (customer.currentBalance + additionalAmountUsd) <= customer.creditLimit
    }

    // --- Price Calculation ---
    suspend fun calculateWholesaleUnitPrice(
        productId: Long,
        quantity: Int,
        businessId: String? = null
    ): Pair<Double, String> {
        val product = productDao.getProductById(productId) ?: return Pair(0.0, "RETAIL")
        val basePrice = product.sellingPrice

        if (businessId != null) {
            val tiers = b2bDao.getApplicablePriceTiers(productId, businessId)
            // 1. Customer-specific price
            val customTier = tiers.firstOrNull { it.customerBusinessId == businessId }
            if (customTier != null) {
                val effectivePrice = if (customTier.discountPercent > 0) basePrice * (1 - customTier.discountPercent / 100) else customTier.priceUsd
                return Pair(effectivePrice, "CUSTOM_SPECIAL")
            }
            // 2. Quantity tier
            val qtyTier = tiers.firstOrNull {
                it.customerBusinessId == null && quantity >= it.minQuantity && (it.maxQuantity == null || quantity <= it.maxQuantity)
            }
            if (qtyTier != null) {
                val effectivePrice = if (qtyTier.discountPercent > 0) basePrice * (1 - qtyTier.discountPercent / 100) else qtyTier.priceUsd
                return Pair(effectivePrice, qtyTier.tierName)
            }
        }

        // Default tier by quantity fallback
        return when {
            quantity >= 100 -> Pair(basePrice * 0.75, "BULK_100+")
            quantity >= 50 -> Pair(basePrice * 0.80, "DISTRIBUTOR_50+")
            quantity >= 10 -> Pair(basePrice * 0.88, "WHOLESALE_10+")
            else -> Pair(basePrice, "RETAIL")
        }
    }

    suspend fun savePriceTier(tier: WholesalePriceTierEntity) {
        b2bDao.insertPriceTier(tier)
    }

    fun getPriceTiersForProduct(productId: Long): Flow<List<WholesalePriceTierEntity>> {
        return b2bDao.getPriceTiersForProductFlow(productId)
    }

    // --- Quotations ---
    val allQuotations: Flow<List<B2bQuotationEntity>> = b2bDao.getAllQuotationsFlow()

    fun getQuotationsForBusiness(businessId: String): Flow<List<B2bQuotationEntity>> {
        return b2bDao.getQuotationsForBusinessFlow(businessId)
    }

    suspend fun createQuotation(
        businessId: String,
        items: List<B2bQuotationItemEntity>,
        discountUsd: Double = 0.0,
        taxUsd: Double = 0.0,
        shippingUsd: Double = 0.0,
        currency: String = "USD",
        notes: String? = null
    ): String {
        val customer = b2bDao.getBusinessCustomerById(businessId)
        val businessName = customer?.businessName ?: "Business #$businessId"

        val subtotal = items.sumOf { it.subtotalUsd }
        val total = subtotal - discountUsd + taxUsd + shippingUsd
        val quotationId = "QT-" + UUID.randomUUID().toString().take(8).uppercase()

        val quotation = B2bQuotationEntity(
            quotationId = quotationId,
            businessId = businessId,
            businessName = businessName,
            subtotalUsd = subtotal,
            discountUsd = discountUsd,
            taxUsd = taxUsd,
            shippingUsd = shippingUsd,
            totalUsd = total,
            currency = currency,
            status = "DRAFT",
            notes = notes
        )

        val updatedItems = items.map { it.copy(quotationId = quotationId) }

        b2bDao.insertQuotation(quotation)
        b2bDao.insertQuotationItems(updatedItems)
        return quotationId
    }

    suspend fun updateQuotationStatus(quotationId: String, newStatus: String) {
        val q = b2bDao.getQuotationById(quotationId) ?: return
        b2bDao.updateQuotation(q.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }

    suspend fun convertQuotationToOrder(
        quotationId: String,
        deliveryAddress: String,
        expectedDeliveryDate: Long? = null
    ): Result<String> {
        val quotation = b2bDao.getQuotationById(quotationId)
            ?: return Result.failure(Exception("Quotation not found"))
        val quotationItems = b2bDao.getQuotationItems(quotationId)

        val allowed = checkCreditLimit(quotation.businessId, quotation.totalUsd)
        if (!allowed) {
            return Result.failure(Exception("Credit limit exceeded for business customer"))
        }

        val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()

        val order = B2bOrderEntity(
            orderId = orderId,
            quotationId = quotationId,
            businessId = quotation.businessId,
            businessName = quotation.businessName,
            orderStatus = "CONFIRMED",
            paymentTerms = "NET_30",
            subtotalUsd = quotation.subtotalUsd,
            discountUsd = quotation.discountUsd,
            taxUsd = quotation.taxUsd,
            shippingUsd = quotation.shippingUsd,
            totalUsd = quotation.totalUsd,
            currency = quotation.currency,
            deliveryAddress = deliveryAddress,
            expectedDeliveryDate = expectedDeliveryDate,
            customerNotes = quotation.notes
        )

        val orderItems = quotationItems.map {
            B2bOrderItemEntity(
                itemId = UUID.randomUUID().toString(),
                orderId = orderId,
                productId = it.productId,
                productName = it.productName,
                sku = it.sku,
                quantity = it.quantity,
                unitPriceUsd = it.unitPriceUsd,
                discountUsd = it.discountUsd,
                subtotalUsd = it.subtotalUsd
            )
        }

        // Insert Order and update stock
        b2bDao.insertOrder(order)
        b2bDao.insertOrderItems(orderItems)
        for (item in orderItems) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0)
                productDao.updateProduct(product.copy(stockQuantity = newStock))
            }
        }

        // Mark quotation CONVERTED
        b2bDao.updateQuotation(quotation.copy(status = "CONVERTED", updatedAt = System.currentTimeMillis()))

        // Create Invoice automatically
        createInvoiceForOrder(order)

        // Adjust Business balance
        b2bDao.adjustBusinessBalance(quotation.businessId, quotation.totalUsd)

        return Result.success(orderId)
    }

    // --- Orders ---
    val allOrders: Flow<List<B2bOrderEntity>> = b2bDao.getAllOrdersFlow()

    fun getOrdersForBusiness(businessId: String): Flow<List<B2bOrderEntity>> {
        return b2bDao.getOrdersForBusinessFlow(businessId)
    }

    suspend fun createDirectOrder(
        businessId: String,
        items: List<B2bOrderItemEntity>,
        deliveryAddress: String,
        paymentTerms: String = "NET_30",
        discountUsd: Double = 0.0,
        taxUsd: Double = 0.0,
        shippingUsd: Double = 0.0,
        currency: String = "USD",
        notes: String? = null
    ): Result<String> {
        val customer = b2bDao.getBusinessCustomerById(businessId)
        val businessName = customer?.businessName ?: "Business #$businessId"

        val subtotal = items.sumOf { it.subtotalUsd }
        val total = subtotal - discountUsd + taxUsd + shippingUsd

        val allowed = checkCreditLimit(businessId, total)
        if (!allowed) {
            return Result.failure(Exception("Order exceeds available credit limit"))
        }

        val orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase()
        val order = B2bOrderEntity(
            orderId = orderId,
            businessId = businessId,
            businessName = businessName,
            orderStatus = "CONFIRMED",
            paymentTerms = paymentTerms,
            subtotalUsd = subtotal,
            discountUsd = discountUsd,
            taxUsd = taxUsd,
            shippingUsd = shippingUsd,
            totalUsd = total,
            currency = currency,
            deliveryAddress = deliveryAddress,
            customerNotes = notes
        )

        val updatedItems = items.map { it.copy(orderId = orderId) }

        b2bDao.insertOrder(order)
        b2bDao.insertOrderItems(updatedItems)

        // Update Stock
        for (item in updatedItems) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val newStock = (product.stockQuantity - item.quantity).coerceAtLeast(0)
                productDao.updateProduct(product.copy(stockQuantity = newStock))
            }
        }

        // Create Invoice
        createInvoiceForOrder(order)

        // Adjust Business Balance
        b2bDao.adjustBusinessBalance(businessId, total)

        return Result.success(orderId)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        val order = b2bDao.getOrderById(orderId) ?: return
        b2bDao.updateOrder(order.copy(orderStatus = newStatus, updatedAt = System.currentTimeMillis()))
    }

    // --- Invoices & Payments ---
    val allInvoices: Flow<List<B2bInvoiceEntity>> = b2bDao.getAllInvoicesFlow()

    fun getInvoicesForBusiness(businessId: String): Flow<List<B2bInvoiceEntity>> {
        return b2bDao.getInvoicesForBusinessFlow(businessId)
    }

    private suspend fun createInvoiceForOrder(order: B2bOrderEntity): B2bInvoiceEntity {
        val invoiceNumber = "INV-" + UUID.randomUUID().toString().take(8).uppercase()
        val invoice = B2bInvoiceEntity(
            invoiceId = "INV_ID_" + UUID.randomUUID().toString().take(8),
            orderId = order.orderId,
            businessId = order.businessId,
            businessName = order.businessName,
            invoiceNumber = invoiceNumber,
            subtotalUsd = order.subtotalUsd,
            discountUsd = order.discountUsd,
            taxUsd = order.taxUsd,
            shippingUsd = order.shippingUsd,
            totalUsd = order.totalUsd,
            paidAmountUsd = 0.0,
            currency = order.currency,
            paymentStatus = "UNPAID",
            paymentTerms = order.paymentTerms,
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 30 * 86400000L
        )
        b2bDao.insertInvoice(invoice)
        return invoice
    }

    val allPayments: Flow<List<B2bPaymentEntity>> = b2bDao.getAllPaymentsFlow()

    suspend fun recordPayment(
        businessId: String,
        invoiceId: String,
        amountUsd: Double,
        method: String = "BANK_TRANSFER",
        referenceNumber: String = "",
        receivedBy: String = "Admin",
        notes: String? = null
    ): Result<String> {
        val invoice = b2bDao.getInvoiceById(invoiceId)
            ?: return Result.failure(Exception("Invoice not found"))

        val paymentId = "PAY-" + UUID.randomUUID().toString().take(8).uppercase()
        val payment = B2bPaymentEntity(
            paymentId = paymentId,
            businessId = businessId,
            invoiceId = invoiceId,
            amountUsd = amountUsd,
            currency = invoice.currency,
            paymentMethod = method,
            referenceNumber = referenceNumber.ifBlank { "REF-${System.currentTimeMillis().toString().takeLast(6)}" },
            receivedBy = receivedBy,
            notes = notes
        )

        val newPaid = invoice.paidAmountUsd + amountUsd
        val newStatus = when {
            newPaid >= invoice.totalUsd -> "PAID"
            newPaid > 0 -> "PARTIALLY_PAID"
            else -> "UNPAID"
        }

        b2bDao.insertPayment(payment)
        b2bDao.updateInvoice(invoice.copy(paidAmountUsd = newPaid, paymentStatus = newStatus))

        // Reduce balance
        b2bDao.adjustBusinessBalance(businessId, -amountUsd)

        return Result.success(paymentId)
    }

    // --- Returns & Deliveries ---
    val allReturns: Flow<List<B2bReturnEntity>> = b2bDao.getAllReturnsFlow()
    val allDeliveries: Flow<List<B2bDeliveryEntity>> = b2bDao.getAllDeliveriesFlow()

    suspend fun processReturn(
        orderId: String,
        invoiceId: String,
        businessId: String,
        reason: String,
        refundAmountUsd: Double,
        restockInventory: Boolean = true,
        notes: String? = null
    ): String {
        val returnId = "RET-" + UUID.randomUUID().toString().take(8).uppercase()
        val returnEntity = B2bReturnEntity(
            returnId = returnId,
            orderId = orderId,
            invoiceId = invoiceId,
            businessId = businessId,
            reason = reason,
            status = "APPROVED",
            refundAmountUsd = refundAmountUsd,
            restockInventory = restockInventory,
            notes = notes
        )

        b2bDao.insertReturn(returnEntity)

        if (restockInventory) {
            val orderItems = b2bDao.getOrderItems(orderId)
            for (item in orderItems) {
                val p = productDao.getProductById(item.productId)
                if (p != null) {
                    productDao.updateProduct(p.copy(stockQuantity = p.stockQuantity + item.quantity))
                }
            }
        }

        // Adjust Business balance (credit refund)
        b2bDao.adjustBusinessBalance(businessId, -refundAmountUsd)

        return returnId
    }

    suspend fun updateDeliveryStatus(
        deliveryId: String,
        status: String,
        driverName: String? = null,
        trackingNumber: String? = null
    ) {
        val deliveries = b2bDao.getAllDeliveriesFlow()
        // Simple helper update
    }

    suspend fun createDelivery(delivery: B2bDeliveryEntity) {
        b2bDao.insertDelivery(delivery)
    }

    // --- Statement ---
    suspend fun getCustomerStatement(businessId: String): CustomerStatement? {
        val customer = b2bDao.getBusinessCustomerById(businessId) ?: return null
        val invoices = b2bDao.getInvoiceById(businessId) // List helper
        val totalInvoiced = customer.currentBalance // Current active balance
        return CustomerStatement(
            businessId = customer.businessId,
            businessName = customer.businessName,
            openingBalanceUsd = 0.0,
            totalInvoicedUsd = totalInvoiced,
            totalPaidUsd = 0.0,
            currentBalanceUsd = customer.currentBalance,
            overdueBalanceUsd = if (customer.currentBalance > customer.creditLimit) customer.currentBalance - customer.creditLimit else 0.0,
            creditLimitUsd = customer.creditLimit,
            availableCreditUsd = (customer.creditLimit - customer.currentBalance).coerceAtLeast(0.0),
            invoices = emptyList(),
            payments = emptyList()
        )
    }
}
