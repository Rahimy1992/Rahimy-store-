package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_configs")
data class CurrencyConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val currencyCode: String,      // e.g. "AFN", "SAR", "TRY", "EUR", "USD"
    val currencySymbol: String,    // e.g. "؋", "﷼", "₺", "€", "$"
    val displayName: String,       // e.g. "Afghan Afghani", "Saudi Riyal", "Turkish Lira", "Euro", "US Dollar"
    val exchangeRateToUSD: Double, // 1 USD = X in this currency (e.g. 71.5 AFN, 3.75 SAR, 33.2 TRY, 0.92 EUR, 1.0 USD)
    val markupPercent: Double = 0.0, // Manager configurable percentage adjustment
    val isPrimaryRegional: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
