package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    suspend fun getAllProductsSync(): List<Product>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getProductsByIds(ids: List<Long>): List<Product>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockThreshold AND isActive = 1 ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT brand FROM products WHERE brand != '' ORDER BY brand ASC")
    fun getAllBrands(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>): List<Long>

    @Update
    suspend fun updateProduct(product: Product)

    @Update
    suspend fun updateProducts(products: List<Product>)

    @Query("UPDATE products SET isActive = :isActive, updatedAt = :timestamp WHERE id IN (:ids)")
    suspend fun setProductsActiveStatus(ids: List<Long>, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity, updatedAt = :timestamp WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = stockQuantity + :quantity, updatedAt = :timestamp WHERE id = :productId")
    suspend fun restoreStock(productId: Long, quantity: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<Long>)
}
