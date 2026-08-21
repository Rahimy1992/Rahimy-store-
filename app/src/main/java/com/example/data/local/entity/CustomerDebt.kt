package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_debts")
data class CustomerDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phoneNumber: String = "",
    val totalDebtUsd: Double,
    val lastTransactionType: String = "CREDIT_SALE", // "CREDIT_SALE", "PAYMENT_RECEIVED", "MANUAL_ADJUSTMENT"
    val lastAmountUsd: Double,
    val notes: String = "",
    val saleInvoiceNumber: String = "",
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
