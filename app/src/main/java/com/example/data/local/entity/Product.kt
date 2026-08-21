package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String = "",
    val category: String,
    val brand: String = "",
    val costPrice: Double,       // Current purchase cost (USD base)
    val sellingPrice: Double,    // Selling price (USD base)
    val stockQuantity: Int,
    val minStockThreshold: Int = 5,
    val isActive: Boolean = true,
    val imageUris: List<String> = emptyList(), // Multi-image URIs or local file paths
    val primaryImageIndex: Int = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
