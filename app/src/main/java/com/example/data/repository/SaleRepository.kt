package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.ManualSaleImageDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.SaleItemDao
import com.example.data.local.entity.AuditLog
import com.example.data.local.entity.ManualSaleImage
import com.example.data.local.entity.Product
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.data.local.entity.SaleStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.TimeZone

data class CartItem(
    val product: Product,
    val quantity: Int
)

enum class SalesDateFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    SELECTED_MONTH,
    MULTIPLE_MONTHS,
    SELECTED_YEAR,
    CUSTOM_RANGE,
    ALL_TIME
}

data class TopProductMetric(
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class EmployeeSaleMetric(
    val cashierId: Long,
    val cashierName: String,
    val salesCount: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)

data class SalesFinancialMetrics(
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val activeSalesCount: Int = 0,
    val voidedSalesCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val averageDailySales: Double = 0.0,
    val averageWeeklySales: Double = 0.0,
    val averageMonthlySales: Double = 0.0,
    val topSellingProducts: List<TopProductMetric> = emptyList(),
    val mostProfitableProducts: List<TopProductMetric> = emptyList(),
    val employeeSales: List<EmployeeSaleMetric> = emptyList()
)

data class ManualSaleImageInput(
    val uriStr: String,
    val source: String = "GALLERY" // "CAMERA" or "GALLERY"
)

data class ManualSalesSummary(
    val totalTransactions: Int = 0,
    val grossSales: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalTax: Double = 0.0,
    val netSales: Double = 0.0,
    val totalProfit: Double = 0.0,
    val paymentBreakdown: Map<String, Double> = emptyMap()
)

data class DailyManualSaleBreakdown(
    val dayOfMonth: Int,
    val dateEpoch: Long,
    val dateFormatted: String,
    val transactionCount: Int,
    val netSales: Double
)

data class MonthlyManualSaleBreakdown(
    val monthIndex: Int, // 0..11
    val monthName: String,
    val transactionCount: Int,
    val netSales: Double
)

class SaleRepository(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao,
    private val productDao: ProductDao,
    private val auditLogDao: AuditLogDao,
    private val manualSaleImageDao: ManualSaleImageDao? = null
) {
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    fun getImagesForSale(saleId: Long): Flow<List<ManualSaleImage>> {
        return manualSaleImageDao?.getImagesForSale(saleId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun getImagesForSaleSync(saleId: Long): List<ManualSaleImage> {
        return manualSaleImageDao?.getImagesForSaleSync(saleId) ?: emptyList()
    }

    suspend fun deleteManualSaleImage(imageId: Long) {
        manualSaleImageDao?.deleteImage(imageId)
    }

    /**
     * Creates a complete Manual Sale with multiple image attachments, immutable historical snapshots,
     * subtotal-discount+tax calculation, inventory deduction, and audit logging.
     */
    suspend fun createManualSale(
        productId: Long,
        quantity: Int,
        unitPrice: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
        currency: String = "AFN",
        customerName: String? = null,
        customerPhone: String? = null,
        paymentMethod: String = "CASH",
        timestamp: Long = System.currentTimeMillis(),
        notes: String = "",
        cashierId: Long,
        cashierName: String,
        images: List<ManualSaleImageInput> = emptyList()
    ): Long {
        require(quantity > 0) { "Quantity must be greater than zero" }
        require(unitPrice >= 0.0) { "Unit price must be non-negative" }

        val product = productDao.getProductById(productId)
        val productName = product?.name ?: "Manual Product"
        val productSku = product?.sku ?: "MANUAL-SKU"
        val unitCost = product?.costPrice ?: 0.0

        val subtotal = quantity * unitPrice
        val finalTotal = (subtotal - discount + tax).coerceAtLeast(0.0)
        val costTotal = quantity * unitCost
        val grossProfit = finalTotal - costTotal
        val profitMarginPercent = if (finalTotal > 0.0) (grossProfit / finalTotal) * 100.0 else 0.0

        val invoiceNumber = "MSALE-${System.currentTimeMillis().toString().takeLast(8)}"

        val sale = Sale(
            invoiceNumber = invoiceNumber,
            cashierId = cashierId,
            cashierName = cashierName,
            totalRevenue = finalTotal,
            totalCost = costTotal,
            grossProfit = grossProfit,
            profitMarginPercent = profitMarginPercent,
            status = SaleStatus.ACTIVE,
            paymentMethod = paymentMethod,
            notes = notes,
            timestamp = timestamp,
            saleType = "MANUAL",
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            currency = currency,
            customerName = customerName,
            customerPhone = customerPhone
        )

        val saleId = saleDao.insertSale(sale)

        // Insert SaleItem with immutable price & cost snapshot
        val saleItem = SaleItem(
            saleId = saleId,
            productId = productId,
            productName = productName,
            productSku = productSku,
            quantity = quantity,
            unitCostSnapshot = unitCost,
            unitPriceSnapshot = unitPrice,
            subtotal = subtotal,
            costTotal = costTotal,
            grossProfit = grossProfit
        )
        saleItemDao.insertSaleItems(listOf(saleItem))

        // Save image attachments
        if (images.isNotEmpty() && manualSaleImageDao != null) {
            val imageEntities = images.mapIndexed { index, imgInput ->
                ManualSaleImage(
                    saleId = saleId,
                    localUri = imgInput.uriStr,
                    cloudUrl = null,
                    source = imgInput.source,
                    createdAt = System.currentTimeMillis() + index,
                    syncStatus = "PENDING",
                    displayOrder = index
                )
            }
            manualSaleImageDao.insertImages(imageEntities)
        }

        // Deduct Inventory Stock
        productDao.reduceStock(productId, quantity)

        // Audit Log
        auditLogDao.insertLog(
            AuditLog(
                userId = cashierId,
                username = cashierName,
                userRole = "EMPLOYEE",
                actionType = "MANUAL_SALE_CREATED",
                description = "Created Manual Sale #$invoiceNumber for Product $productName (Qty: $quantity, Total: $$finalTotal, Images: ${images.size})",
                detailsJson = "{\"saleId\":$saleId,\"invoice\":\"$invoiceNumber\",\"total\":$finalTotal}"
            )
        )

        return saleId
    }

    /**
     * Checks if a proposed invoice/sale is a duplicate of an existing record.
     */
    suspend fun checkDuplicateSale(
        invoiceNumber: String,
        documentHash: String = ""
    ): com.example.util.DuplicateSaleCheckResult {
        val allSalesList = saleDao.getAllSalesSync()
        return com.example.util.SalesDuplicateDetector.checkDuplicate(
            proposedInvoiceNumber = invoiceNumber,
            proposedDocumentHash = documentHash,
            existingSales = allSalesList
        )
    }

    /**
     * Creates a multi-item Manual Sale with full image attachments, subtotal/tax/discount calculations,
     * duplicate checking, stock deduction, and audit logging.
     */
    suspend fun createManualSaleMulti(
        items: List<CartItem>,
        discount: Double = 0.0,
        tax: Double = 0.0,
        currency: String = "AFN",
        customerName: String? = null,
        customerPhone: String? = null,
        paymentMethod: String = "CASH",
        timestamp: Long = System.currentTimeMillis(),
        notes: String = "",
        cashierId: Long,
        cashierName: String,
        images: List<ManualSaleImageInput> = emptyList(),
        customInvoiceNumber: String? = null,
        documentHash: String = ""
    ): Long {
        require(items.isNotEmpty()) { "Sale items list cannot be empty" }

        val invNum = customInvoiceNumber?.ifBlank { null }
            ?: "MSALE-${System.currentTimeMillis().toString().takeLast(8)}"

        // Idempotency / Duplicate Check
        val dupCheck = checkDuplicateSale(invNum, documentHash)
        if (dupCheck.isDuplicate && dupCheck.existingSale != null) {
            return dupCheck.existingSale.id
        }

        var subtotal = 0.0
        var totalCost = 0.0

        items.forEach { item ->
            subtotal += item.quantity * item.product.sellingPrice
            totalCost += item.quantity * item.product.costPrice
        }

        val finalTotal = (subtotal - discount + tax).coerceAtLeast(0.0)
        val grossProfit = finalTotal - totalCost
        val margin = if (finalTotal > 0.0) (grossProfit / finalTotal) * 100.0 else 0.0

        val notesWithHash = if (documentHash.isNotEmpty()) "$notes [HASH:$documentHash]" else notes

        val sale = Sale(
            invoiceNumber = invNum,
            cashierId = cashierId,
            cashierName = cashierName,
            totalRevenue = finalTotal,
            totalCost = totalCost,
            grossProfit = grossProfit,
            profitMarginPercent = margin,
            status = SaleStatus.ACTIVE,
            paymentMethod = paymentMethod,
            notes = notesWithHash,
            timestamp = timestamp,
            saleType = "MANUAL",
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            currency = currency,
            customerName = customerName,
            customerPhone = customerPhone
        )

        val saleId = saleDao.insertSale(sale)

        val saleItems = items.map { item ->
            val sub = item.quantity * item.product.sellingPrice
            val cst = item.quantity * item.product.costPrice
            SaleItem(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                productSku = item.product.sku,
                quantity = item.quantity,
                unitCostSnapshot = item.product.costPrice,
                unitPriceSnapshot = item.product.sellingPrice,
                subtotal = sub,
                costTotal = cst,
                grossProfit = sub - cst
            )
        }
        saleItemDao.insertSaleItems(saleItems)

        // Save image attachments
        if (images.isNotEmpty() && manualSaleImageDao != null) {
            val imageEntities = images.mapIndexed { index, imgInput ->
                ManualSaleImage(
                    saleId = saleId,
                    localUri = imgInput.uriStr,
                    cloudUrl = null,
                    source = imgInput.source,
                    createdAt = System.currentTimeMillis() + index,
                    syncStatus = "PENDING",
                    displayOrder = index
                )
            }
            manualSaleImageDao.insertImages(imageEntities)
        }

        // Deduct Inventory Stock
        items.forEach { item ->
            productDao.reduceStock(item.product.id, item.quantity)
        }

        // Audit Log
        auditLogDao.insertLog(
            AuditLog(
                userId = cashierId,
                username = cashierName,
                userRole = "EMPLOYEE",
                actionType = "MANUAL_SALE_CREATED",
                description = "Created Multi-Item Manual Sale #$invNum (Total: $$finalTotal, Items: ${items.size}, Images: ${images.size})",
                detailsJson = "{\"saleId\":$saleId,\"invoice\":\"$invNum\",\"total\":$finalTotal}"
            )
        )

        return saleId
    }

    /**
     * Process Purchase / Stock-In from AI Scanned Invoice.
     * - Increases product stock quantity.
     * - Updates cost price and product details.
     * - Preserves historical cost and logs audit trail.
     * - Creates or updates products cleanly.
     */
    suspend fun processInvoicePurchase(
        invoiceData: com.example.data.ai.ExtractedInvoiceData,
        userId: Long,
        username: String,
        userRole: String,
        imageUris: List<String> = emptyList()
    ): Long {
        var createdCount = 0
        var updatedCount = 0

        invoiceData.items.forEach { item ->
            val resolvedId = item.resolvedProductId
            if (resolvedId != null) {
                // Update existing product stock & cost price
                val existing = productDao.getProductById(resolvedId)
                if (existing != null) {
                    val newStock = existing.stockQuantity + item.quantity
                    val newCost = if (item.unitPrice > 0) item.unitPrice else existing.costPrice
                    val newPrice = if (existing.sellingPrice <= newCost) newCost * 1.25 else existing.sellingPrice

                    val updatedProduct = existing.copy(
                        stockQuantity = newStock,
                        costPrice = newCost,
                        sellingPrice = newPrice,
                        updatedAt = System.currentTimeMillis()
                    )
                    productDao.updateProduct(updatedProduct)
                    updatedCount++
                }
            } else {
                // Create new product from invoice line item
                val newProduct = Product(
                    name = item.productName.ifBlank { "Scanned Item" },
                    sku = item.sku.ifBlank { "SKU-${System.currentTimeMillis().toString().takeLast(6)}" },
                    barcode = item.barcode,
                    category = "Invoices & Stock-In",
                    brand = item.brand,
                    costPrice = if (item.unitPrice > 0) item.unitPrice else 5.0,
                    sellingPrice = if (item.unitPrice > 0) (item.unitPrice * 1.30) else 7.50,
                    stockQuantity = item.quantity,
                    description = "Added from Scanned Invoice #${invoiceData.invoiceNumber} (${invoiceData.supplierName})"
                )
                productDao.insertProduct(newProduct)
                createdCount++
            }
        }

        // Audit Log
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = userRole,
                actionType = "PURCHASE_INVOICE_PROCESSED",
                description = "Processed Stock-In Purchase Invoice #${invoiceData.invoiceNumber} from '${invoiceData.supplierName}' (Grand Total: ${invoiceData.grandTotal} ${invoiceData.currency}, Created: $createdCount, Updated: $updatedCount)",
                detailsJson = "{\"invoice\":\"${invoiceData.invoiceNumber}\",\"supplier\":\"${invoiceData.supplierName}\",\"total\":${invoiceData.grandTotal}}"
            )
        )

        return System.currentTimeMillis()
    }

    /**
     * Calculates Manual Sales Summary for a given date range.
     */
    suspend fun getManualSalesSummary(
        startTime: Long,
        endTime: Long
    ): ManualSalesSummary {
        val sales = saleDao.getSalesBetweenDirect(startTime, endTime)
            .filter { it.saleType == "MANUAL" && it.status == SaleStatus.ACTIVE }

        if (sales.isEmpty()) return ManualSalesSummary()

        val gross = sales.sumOf { it.subtotal }
        val discount = sales.sumOf { it.discount }
        val tax = sales.sumOf { it.tax }
        val net = sales.sumOf { it.totalRevenue }
        val profit = sales.sumOf { it.grossProfit ?: 0.0 }

        val pMap = sales.groupBy { it.paymentMethod }
            .mapValues { entry -> entry.value.sumOf { it.totalRevenue } }

        return ManualSalesSummary(
            totalTransactions = sales.size,
            grossSales = gross,
            totalDiscount = discount,
            totalTax = tax,
            netSales = net,
            totalProfit = profit,
            paymentBreakdown = pMap
        )
    }

    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>> = saleItemDao.getItemsForSale(saleId)

    suspend fun getSaleDetails(saleId: Long): Pair<Sale?, List<SaleItem>> {
        val sale = saleDao.getSaleById(saleId)
        val items = saleItemDao.getItemsForSaleDirect(saleId)
        return Pair(sale, items)
    }

    /**
     * Requirement 4, 12, 13: Complete a new sale.
     * Takes snapshots of unit cost and unit price to guarantee cost immutability.
     */
    suspend fun checkout(
        cartItems: List<CartItem>,
        cashierId: Long,
        cashierName: String,
        paymentMethod: String = "CASH",
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        require(cartItems.isNotEmpty()) { "Cart cannot be empty" }

        var totalRevenue = 0.0
        var totalCost = 0.0

        val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(8)}"

        // Pre-calculate revenue and cost from immutable snapshots
        cartItems.forEach { item ->
            val subtotal = item.quantity * item.product.sellingPrice
            val costTotal = item.quantity * item.product.costPrice
            totalRevenue += subtotal
            totalCost += costTotal
        }

        val grossProfit = totalRevenue - totalCost
        val profitMarginPercent = if (totalRevenue > 0.0) (grossProfit / totalRevenue) * 100.0 else 0.0

        val sale = Sale(
            invoiceNumber = invoiceNumber,
            cashierId = cashierId,
            cashierName = cashierName,
            totalRevenue = totalRevenue,
            totalCost = totalCost,
            grossProfit = grossProfit,
            profitMarginPercent = profitMarginPercent,
            status = SaleStatus.ACTIVE,
            paymentMethod = paymentMethod,
            notes = notes,
            timestamp = timestamp
        )

        val saleId = saleDao.insertSale(sale)

        // Insert SaleItems with snapshot costs & prices
        val saleItems = cartItems.map { item ->
            val sub = item.quantity * item.product.sellingPrice
            val cst = item.quantity * item.product.costPrice
            SaleItem(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                productSku = item.product.sku,
                quantity = item.quantity,
                unitCostSnapshot = item.product.costPrice,       // Snapshot unit cost!
                unitPriceSnapshot = item.product.sellingPrice,  // Snapshot unit price!
                subtotal = sub,
                costTotal = cst,
                grossProfit = sub - cst
            )
        }
        saleItemDao.insertSaleItems(saleItems)

        // Reduce inventory stock
        cartItems.forEach { item ->
            productDao.reduceStock(item.product.id, item.quantity)
        }

        // Audit Log
        auditLogDao.insertLog(
            AuditLog(
                userId = cashierId,
                username = cashierName,
                userRole = "EMPLOYEE",
                actionType = "SALE_COMPLETED",
                description = "Completed Sale #$invoiceNumber for $$totalRevenue (Profit: $$grossProfit, Items: ${cartItems.size})",
                detailsJson = "{\"saleId\":$saleId,\"invoice\":\"$invoiceNumber\",\"revenue\":$totalRevenue}"
            )
        )

        return saleId
    }

    /**
     * Requirement 4: Void/Cancel Sale with explicit reason.
     * Restores inventory and preserves audit & financial history.
     */
    suspend fun voidSale(
        saleId: Long,
        reason: String,
        voidedByUserId: Long,
        voidedByUsername: String,
        voidedByUserRole: String
    ) {
        val sale = saleDao.getSaleById(saleId) ?: return
        if (sale.status != SaleStatus.ACTIVE) return

        saleDao.markSaleVoid(
            saleId = saleId,
            status = SaleStatus.VOID,
            reason = reason,
            voidedBy = voidedByUserId
        )

        // Restore inventory stock
        val items = saleItemDao.getItemsForSaleDirect(saleId)
        items.forEach { item ->
            productDao.restoreStock(item.productId, item.quantity)
        }

        // Audit Log
        auditLogDao.insertLog(
            AuditLog(
                userId = voidedByUserId,
                username = voidedByUsername,
                userRole = voidedByUserRole,
                actionType = "SALE_VOIDED",
                description = "Voided sale #${sale.invoiceNumber} (Revenue: $${sale.totalRevenue}). Reason: '$reason'",
                detailsJson = "{\"saleId\":$saleId,\"invoice\":\"${sale.invoiceNumber}\",\"reason\":\"$reason\"}"
            )
        )
    }

    /**
     * Bulk Void / Cancel multiple selected sales
     */
    suspend fun bulkVoidSales(
        saleIds: List<Long>,
        reason: String,
        voidedByUserId: Long,
        voidedByUsername: String,
        voidedByUserRole: String
    ) {
        saleIds.forEach { id ->
            voidSale(id, reason, voidedByUserId, voidedByUsername, voidedByUserRole)
        }
    }

    /**
     * Calculate Time Range bounds for filters based on business timezone (Requirement 11)
     */
    fun getTimeRangeForFilter(
        filter: SalesDateFilter,
        timeZoneId: String = "UTC",
        customStart: Long? = null,
        customEnd: Long? = null,
        selectedYear: Int = 2026,
        selectedMonth: Int = 0, // 0 = Jan, 11 = Dec
        multipleMonths: List<Int> = emptyList()
    ): Pair<Long, Long> {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val cal = Calendar.getInstance(tz)

        return when (filter) {
            SalesDateFilter.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            SalesDateFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            SalesDateFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            SalesDateFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            SalesDateFilter.SELECTED_MONTH -> {
                cal.set(Calendar.YEAR, selectedYear)
                cal.set(Calendar.MONTH, selectedMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            SalesDateFilter.MULTIPLE_MONTHS -> {
                if (multipleMonths.isEmpty()) {
                    Pair(0L, Long.MAX_VALUE)
                } else {
                    val minMonth = multipleMonths.minOrNull() ?: 0
                    val maxMonth = multipleMonths.maxOrNull() ?: 11
                    cal.set(Calendar.YEAR, selectedYear)
                    cal.set(Calendar.MONTH, minMonth)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val start = cal.timeInMillis

                    cal.set(Calendar.MONTH, maxMonth)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, maxDay)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
            }
            SalesDateFilter.SELECTED_YEAR -> {
                cal.set(Calendar.YEAR, selectedYear)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            SalesDateFilter.CUSTOM_RANGE -> {
                Pair(customStart ?: 0L, customEnd ?: System.currentTimeMillis())
            }
            SalesDateFilter.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
        }
    }

    /**
     * Requirement 4 & 12: Comprehensive Financial & Sales Calculation Engine
     * Excludes cancelled/void transactions from averages.
     */
    suspend fun calculateFinancialMetrics(
        startTime: Long,
        endTime: Long,
        timeZoneId: String = "UTC"
    ): SalesFinancialMetrics {
        val sales = saleDao.getSalesBetweenDirect(startTime, endTime)
        val activeSales = sales.filter { it.status == SaleStatus.ACTIVE }
        val voidedCount = sales.count { it.status != SaleStatus.ACTIVE }

        if (activeSales.isEmpty()) {
            return SalesFinancialMetrics(
                voidedSalesCount = voidedCount
            )
        }

        val totalRevenue = activeSales.sumOf { it.totalRevenue }
        val salesWithCost = activeSales.filter { it.totalCost != null }
        val totalCost = if (salesWithCost.isNotEmpty()) salesWithCost.sumOf { it.totalCost ?: 0.0 } else 0.0
        val grossProfit = if (salesWithCost.isNotEmpty()) salesWithCost.sumOf { it.grossProfit ?: 0.0 } else 0.0
        val revenueWithCost = salesWithCost.sumOf { it.totalRevenue }
        val profitMarginPercent = if (revenueWithCost > 0.0) (grossProfit / revenueWithCost) * 100.0 else 0.0

        val activeCount = activeSales.size
        val averageOrderValue = if (activeCount > 0) totalRevenue / activeCount else 0.0

        // Calculate span of days between start and end (or timestamps of sales)
        val effectiveStart = activeSales.minOf { it.timestamp }
        val effectiveEnd = activeSales.maxOf { it.timestamp }
        val daysSpan = ((effectiveEnd - effectiveStart) / 86400000.0).coerceAtLeast(1.0)
        val weeksSpan = (daysSpan / 7.0).coerceAtLeast(1.0)
        val monthsSpan = (daysSpan / 30.4375).coerceAtLeast(1.0)

        val avgDaily = totalRevenue / daysSpan
        val avgWeekly = totalRevenue / weeksSpan
        val avgMonthly = totalRevenue / monthsSpan

        // Fetch sale items to calculate top products and profitability
        val activeSaleIds = activeSales.map { it.id }
        val saleItems = saleItemDao.getItemsForSales(activeSaleIds)

        val productMap = mutableMapOf<Long, MutableList<SaleItem>>()
        saleItems.forEach { item ->
            productMap.getOrPut(item.productId) { mutableListOf() }.add(item)
        }

        val productMetrics = productMap.map { (pId, items) ->
            val name = items.first().productName
            val qty = items.sumOf { it.quantity }
            val rev = items.sumOf { it.subtotal }
            val itemsWithProfit = items.filter { it.grossProfit != null }
            val prof = if (itemsWithProfit.isNotEmpty()) itemsWithProfit.sumOf { it.grossProfit ?: 0.0 } else 0.0
            TopProductMetric(
                productId = pId,
                productName = name,
                quantitySold = qty,
                totalRevenue = rev,
                totalProfit = prof
            )
        }

        val topSelling = productMetrics.sortedByDescending { it.quantitySold }.take(5)
        val mostProfitable = productMetrics.sortedByDescending { it.totalProfit }.take(5)

        // Employee performance
        val employeeMap = activeSales.groupBy { it.cashierId }
        val employeeMetrics = employeeMap.map { (cashierId, empSales) ->
            val name = empSales.first().cashierName
            val count = empSales.size
            val rev = empSales.sumOf { it.totalRevenue }
            val salesWithProf = empSales.filter { it.grossProfit != null }
            val prof = if (salesWithProf.isNotEmpty()) salesWithProf.sumOf { it.grossProfit ?: 0.0 } else 0.0
            EmployeeSaleMetric(
                cashierId = cashierId,
                cashierName = name,
                salesCount = count,
                totalRevenue = rev,
                totalProfit = prof
            )
        }.sortedByDescending { it.totalRevenue }

        return SalesFinancialMetrics(
            totalRevenue = totalRevenue,
            totalCost = totalCost,
            grossProfit = grossProfit,
            profitMarginPercent = profitMarginPercent,
            activeSalesCount = activeCount,
            voidedSalesCount = voidedCount,
            averageOrderValue = averageOrderValue,
            averageDailySales = avgDaily,
            averageWeeklySales = avgWeekly,
            averageMonthlySales = avgMonthly,
            topSellingProducts = topSelling,
            mostProfitableProducts = mostProfitable,
            employeeSales = employeeMetrics
        )
    }
}
