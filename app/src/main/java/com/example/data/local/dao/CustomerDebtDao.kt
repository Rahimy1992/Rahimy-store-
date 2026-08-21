package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerDebt
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDebtDao {
    @Query("SELECT * FROM customer_debts ORDER BY updatedAt DESC")
    fun getAllDebts(): Flow<List<CustomerDebt>>

    @Query("SELECT * FROM customer_debts WHERE isSettled = 0 ORDER BY totalDebtUsd DESC")
    fun getActiveDebts(): Flow<List<CustomerDebt>>

    @Query("SELECT * FROM customer_debts WHERE customerName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchDebts(query: String): Flow<List<CustomerDebt>>

    @Query("SELECT * FROM customer_debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): CustomerDebt?

    @Query("SELECT * FROM customer_debts WHERE customerName = :customerName LIMIT 1")
    suspend fun getDebtByCustomerName(customerName: String): CustomerDebt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: CustomerDebt): Long

    @Update
    suspend fun updateDebt(debt: CustomerDebt)

    @Delete
    suspend fun deleteDebt(debt: CustomerDebt)

    @Query("DELETE FROM customer_debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)
}
