package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val productSku: String,
    val quantity: Int,
    val unitCostSnapshot: Double? = null,     // Unit cost at time of sale (IMMUTABLE, null if unavailable)
    val unitPriceSnapshot: Double,            // Unit selling price at time of sale (IMMUTABLE)
    val subtotal: Double,                     // quantity * unitPriceSnapshot
    val costTotal: Double? = null,            // quantity * unitCostSnapshot (null if cost unavailable)
    val grossProfit: Double? = null           // subtotal - costTotal (null if cost unavailable)
)
