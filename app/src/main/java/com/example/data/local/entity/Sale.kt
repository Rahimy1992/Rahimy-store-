package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SaleStatus {
    ACTIVE,
    CANCELLED,
    VOID
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val cashierId: Long,
    val cashierName: String,
    val totalRevenue: Double,
    val totalCost: Double? = null,             // Null when cost is unavailable for historical records
    val grossProfit: Double? = null,           // Null when profit is unavailable for historical records
    val profitMarginPercent: Double? = null,   // Null when margin is unavailable for historical records
    val status: SaleStatus = SaleStatus.ACTIVE,
    val voidReason: String = "",
    val voidedByUserId: Long? = null,
    val voidedAt: Long? = null,
    val paymentMethod: String = "CASH", // CASH, CARD, DIGITAL
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(), // Reliable UTC epoch
    val saleType: String = "POS", // POS, B2B, MANUAL
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val currency: String = "USD",
    val customerName: String? = null,
    val customerPhone: String? = null
)
