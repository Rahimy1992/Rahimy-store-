package com.example.data.repository

import com.example.data.local.dao.CustomerDebtDao
import com.example.data.local.entity.CustomerDebt
import kotlinx.coroutines.flow.Flow

class CustomerDebtRepository(private val dao: CustomerDebtDao) {
    val allDebts: Flow<List<CustomerDebt>> = dao.getAllDebts()
    val activeDebts: Flow<List<CustomerDebt>> = dao.getActiveDebts()

    fun searchDebts(query: String): Flow<List<CustomerDebt>> = dao.searchDebts(query)

    suspend fun getDebtById(id: Long): CustomerDebt? = dao.getDebtById(id)

    suspend fun getDebtByCustomerName(name: String): CustomerDebt? = dao.getDebtByCustomerName(name)

    suspend fun insertOrUpdateCreditSale(
        customerName: String,
        phoneNumber: String,
        saleAmountUsd: Double,
        invoiceNumber: String,
        notes: String
    ): Long {
        val existing = dao.getDebtByCustomerName(customerName)
        if (existing != null) {
            val updated = existing.copy(
                phoneNumber = phoneNumber.ifBlank { existing.phoneNumber },
                totalDebtUsd = existing.totalDebtUsd + saleAmountUsd,
                lastTransactionType = "CREDIT_SALE",
                lastAmountUsd = saleAmountUsd,
                notes = if (notes.isNotBlank()) notes else existing.notes,
                saleInvoiceNumber = invoiceNumber,
                isSettled = false,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateDebt(updated)
            return existing.id
        } else {
            val newDebt = CustomerDebt(
                customerName = customerName,
                phoneNumber = phoneNumber,
                totalDebtUsd = saleAmountUsd,
                lastTransactionType = "CREDIT_SALE",
                lastAmountUsd = saleAmountUsd,
                notes = notes,
                saleInvoiceNumber = invoiceNumber,
                isSettled = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            return dao.insertDebt(newDebt)
        }
    }

    suspend fun recordPayment(debtId: Long, paymentAmountUsd: Double, notes: String): Boolean {
        val debt = dao.getDebtById(debtId) ?: return false
        val newTotal = (debt.totalDebtUsd - paymentAmountUsd).coerceAtLeast(0.0)
        val updated = debt.copy(
            totalDebtUsd = newTotal,
            lastTransactionType = "PAYMENT_RECEIVED",
            lastAmountUsd = paymentAmountUsd,
            notes = if (notes.isNotBlank()) notes else debt.notes,
            isSettled = newTotal <= 0.01,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateDebt(updated)
        return true
    }

    suspend fun deleteDebt(debt: CustomerDebt) = dao.deleteDebt(debt)
}
