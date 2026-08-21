package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CurrencyConfigDao
import com.example.data.local.entity.AuditLog
import com.example.data.local.entity.CurrencyConfig
import com.example.data.local.entity.User
import kotlinx.coroutines.flow.Flow

class CurrencyRepository(
    private val currencyConfigDao: CurrencyConfigDao,
    private val auditLogDao: AuditLogDao
) {
    val allCurrencies: Flow<List<CurrencyConfig>> = currencyConfigDao.getAllCurrencies()
    val primaryRegionalCurrency: Flow<CurrencyConfig?> = currencyConfigDao.getPrimaryRegionalCurrency()

    suspend fun getCurrencyByCode(code: String): CurrencyConfig? = currencyConfigDao.getCurrencyByCode(code)

    /**
     * Requirement 10: Manager manually configures USD reference rate, exchange rate, and percentage markup.
     * Every rate or percentage change must be recorded in Audit Logs.
     */
    suspend fun updateCurrencyConfig(
        currencyCode: String,
        newExchangeRate: Double,
        newMarkupPercent: Double,
        actingUser: User
    ): SecurityResult<Unit> {
        val existing = currencyConfigDao.getCurrencyByCode(currencyCode)
            ?: return SecurityResult.Denied("Currency $currencyCode not found")

        val oldRate = existing.exchangeRateToUSD
        val oldMarkup = existing.markupPercent

        val updated = existing.copy(
            exchangeRateToUSD = newExchangeRate,
            markupPercent = newMarkupPercent,
            updatedAt = System.currentTimeMillis()
        )
        currencyConfigDao.updateCurrency(updated)

        // Audit Log (Mandatory for Requirement 10)
        auditLogDao.insertLog(
            AuditLog(
                userId = actingUser.id,
                username = actingUser.username,
                userRole = actingUser.role.name,
                actionType = "CURRENCY_RATE_CHANGED",
                description = "Updated currency $currencyCode: Rate ($oldRate -> $newExchangeRate), Markup ($oldMarkup% -> $newMarkupPercent%)",
                detailsJson = "{\"currency\":\"$currencyCode\",\"oldRate\":$oldRate,\"newRate\":$newExchangeRate,\"oldMarkup\":$oldMarkup,\"newMarkup\":$newMarkupPercent}"
            )
        )

        return SecurityResult.Success(Unit)
    }

    suspend fun setPrimaryRegionalCurrency(currencyCode: String, actingUser: User) {
        currencyConfigDao.clearPrimaryFlags()
        currencyConfigDao.setPrimaryRegional(currencyCode)
        auditLogDao.insertLog(
            AuditLog(
                userId = actingUser.id,
                username = actingUser.username,
                userRole = actingUser.role.name,
                actionType = "PRIMARY_CURRENCY_CHANGED",
                description = "Set $currencyCode as primary regional currency",
                detailsJson = "{\"currency\":\"$currencyCode\"}"
            )
        )
    }
}
