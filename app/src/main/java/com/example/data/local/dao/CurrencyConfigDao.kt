package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CurrencyConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyConfigDao {
    @Query("SELECT * FROM currency_configs ORDER BY id ASC")
    fun getAllCurrencies(): Flow<List<CurrencyConfig>>

    @Query("SELECT * FROM currency_configs")
    suspend fun getAllCurrenciesDirect(): List<CurrencyConfig>

    @Query("SELECT * FROM currency_configs WHERE currencyCode = :code LIMIT 1")
    suspend fun getCurrencyByCode(code: String): CurrencyConfig?

    @Query("SELECT * FROM currency_configs WHERE isPrimaryRegional = 1 LIMIT 1")
    fun getPrimaryRegionalCurrency(): Flow<CurrencyConfig?>

    @Query("SELECT * FROM currency_configs WHERE isPrimaryRegional = 1 LIMIT 1")
    suspend fun getPrimaryRegionalCurrencyDirect(): CurrencyConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrency(config: CurrencyConfig): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(configs: List<CurrencyConfig>): List<Long>

    @Update
    suspend fun updateCurrency(config: CurrencyConfig)

    @Query("UPDATE currency_configs SET isPrimaryRegional = 0")
    suspend fun clearPrimaryFlags()

    @Query("UPDATE currency_configs SET isPrimaryRegional = 1 WHERE currencyCode = :code")
    suspend fun setPrimaryRegional(code: String)
}
