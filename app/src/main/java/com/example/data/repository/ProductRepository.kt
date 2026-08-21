package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.AuditLog
import com.example.data.local.entity.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val auditLogDao: AuditLogDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val activeProducts: Flow<List<Product>> = productDao.getActiveProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    val allBrands: Flow<List<String>> = productDao.getAllBrands()

    fun searchProducts(query: String): Flow<List<Product>> = productDao.searchProducts(query)

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)

    suspend fun getProductsByIds(ids: List<Long>): List<Product> = productDao.getProductsByIds(ids)

    suspend fun addProduct(product: Product, userId: Long, username: String, role: String): Long {
        val id = productDao.insertProduct(product)
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = "PRODUCT_CREATED",
                description = "Created product '${product.name}' (SKU: ${product.sku}, Stock: ${product.stockQuantity}, Price: $${product.sellingPrice})",
                detailsJson = "{\"productId\":$id,\"name\":\"${product.name}\",\"sellingPrice\":${product.sellingPrice}}"
            )
        )
        return id
    }

    suspend fun addProductsBulk(products: List<Product>, userId: Long, username: String, role: String): List<Long> {
        val ids = productDao.insertProducts(products)
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = "PRODUCT_BULK_CREATE",
                description = "Bulk created ${products.size} products",
                detailsJson = "{\"count\":${products.size}}"
            )
        )
        return ids
    }

    suspend fun updateProduct(product: Product, userId: Long, username: String, role: String) {
        productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = "PRODUCT_UPDATED",
                description = "Updated product #${product.id} '${product.name}' (Price: $${product.sellingPrice}, Cost: $${product.costPrice}, Stock: ${product.stockQuantity})",
                detailsJson = "{\"productId\":${product.id},\"name\":\"${product.name}\"}"
            )
        )
    }

    /**
     * Requirement 3: Bulk Product Management - edit multiple selected products
     */
    suspend fun bulkEditProducts(
        productIds: List<Long>,
        priceAdjustmentPercent: Double? = null, // e.g. +10% or -5%
        newCategory: String? = null,
        stockAdjustment: Int? = null,           // e.g. +10 units
        userId: Long,
        username: String,
        role: String
    ) {
        val currentProducts = productDao.getProductsByIds(productIds)
        val updated = currentProducts.map { p ->
            var price = p.sellingPrice
            if (priceAdjustmentPercent != null) {
                price = (price * (1.0 + priceAdjustmentPercent / 100.0)).coerceAtLeast(0.01)
                // round to 2 decimals
                price = Math.round(price * 100.0) / 100.0
            }
            var category = p.category
            if (!newCategory.isNullOrBlank()) {
                category = newCategory
            }
            var stock = p.stockQuantity
            if (stockAdjustment != null) {
                stock = (stock + stockAdjustment).coerceAtLeast(0)
            }
            p.copy(
                sellingPrice = price,
                category = category,
                stockQuantity = stock,
                updatedAt = System.currentTimeMillis()
            )
        }
        productDao.updateProducts(updated)
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = "PRODUCT_BULK_EDIT",
                description = "Bulk updated ${productIds.size} products (PriceAdj: $priceAdjustmentPercent%, NewCat: $newCategory, StockAdj: $stockAdjustment)",
                detailsJson = "{\"productIds\":$productIds}"
            )
        )
    }

    /**
     * Requirement 3: Bulk activate/deactivate multiple products
     */
    suspend fun bulkSetStatus(
        productIds: List<Long>,
        isActive: Boolean,
        userId: Long,
        username: String,
        role: String
    ) {
        productDao.setProductsActiveStatus(productIds, isActive)
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = if (isActive) "PRODUCT_BULK_ACTIVATE" else "PRODUCT_BULK_DEACTIVATE",
                description = "${if (isActive) "Activated" else "Deactivated"} ${productIds.size} products",
                detailsJson = "{\"productIds\":$productIds,\"isActive\":$isActive}"
            )
        )
    }

    /**
     * Requirement 3: Delete one product (requires confirmation)
     */
    suspend fun deleteProduct(product: Product, userId: Long, username: String, role: String) {
        productDao.deleteProduct(product)
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = "PRODUCT_DELETED",
                description = "Deleted product #${product.id} '${product.name}'",
                detailsJson = "{\"productId\":${product.id},\"name\":\"${product.name}\"}"
            )
        )
    }

    /**
     * Requirement 3: Delete multiple selected products with explicit confirmation & audit log
     */
    suspend fun deleteProductsBulk(productIds: List<Long>, userId: Long, username: String, role: String) {
        productDao.deleteProductsByIds(productIds)
        auditLogDao.insertLog(
            AuditLog(
                userId = userId,
                username = username,
                userRole = role,
                actionType = "PRODUCT_BULK_DELETE",
                description = "Bulk deleted ${productIds.size} products: $productIds",
                detailsJson = "{\"deletedIds\":$productIds}"
            )
        )
    }
}
