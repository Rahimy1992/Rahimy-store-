package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.B2bDeliveryEntity
import com.example.data.local.entity.B2bInvoiceEntity
import com.example.data.local.entity.B2bOrderEntity
import com.example.data.local.entity.B2bOrderItemEntity
import com.example.data.local.entity.B2bOrderWithItems
import com.example.data.local.entity.B2bPaymentEntity
import com.example.data.local.entity.B2bQuotationEntity
import com.example.data.local.entity.B2bQuotationItemEntity
import com.example.data.local.entity.B2bReturnEntity
import com.example.data.local.entity.BusinessCustomerEntity
import com.example.data.local.entity.BusinessCustomerWithOrders
import com.example.data.local.entity.WholesalePriceListEntity
import com.example.data.local.entity.WholesalePriceListWithTiers
import com.example.data.local.entity.WholesalePriceTierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface B2bDao {

    // --- Business Customers ---
    @Query("SELECT * FROM business_customers ORDER BY businessName ASC")
    fun getAllBusinessCustomersFlow(): Flow<List<BusinessCustomerEntity>>

    @Query("SELECT * FROM business_customers WHERE businessId = :businessId")
    suspend fun getBusinessCustomerById(businessId: String): BusinessCustomerEntity?

    @Transaction
    @Query("SELECT * FROM business_customers WHERE businessId = :businessId")
    suspend fun getBusinessCustomerWithOrders(businessId: String): BusinessCustomerWithOrders?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessCustomer(customer: BusinessCustomerEntity)

    @Update
    suspend fun updateBusinessCustomer(customer: BusinessCustomerEntity)

    @Query("UPDATE business_customers SET currentBalance = currentBalance + :amount WHERE businessId = :businessId")
    suspend fun adjustBusinessBalance(businessId: String, amount: Double)

    // --- Price Lists & Price Tiers ---
    @Query("SELECT * FROM wholesale_price_lists ORDER BY name ASC")
    fun getAllPriceListsFlow(): Flow<List<WholesalePriceListEntity>>

    @Transaction
    @Query("SELECT * FROM wholesale_price_lists WHERE priceListId = :priceListId")
    suspend fun getPriceListWithTiers(priceListId: String): WholesalePriceListWithTiers?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceList(priceList: WholesalePriceListEntity)

    @Query("SELECT * FROM wholesale_price_tiers WHERE productId = :productId ORDER BY minQuantity ASC")
    fun getPriceTiersForProductFlow(productId: Long): Flow<List<WholesalePriceTierEntity>>

    @Query("SELECT * FROM wholesale_price_tiers WHERE productId = :productId AND (customerBusinessId IS NULL OR customerBusinessId = :businessId) ORDER BY customerBusinessId DESC, minQuantity ASC")
    suspend fun getApplicablePriceTiers(productId: Long, businessId: String): List<WholesalePriceTierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceTier(tier: WholesalePriceTierEntity)

    @Query("DELETE FROM wholesale_price_tiers WHERE tierId = :tierId")
    suspend fun deletePriceTier(tierId: String)

    // --- Quotations ---
    @Query("SELECT * FROM b2b_quotations ORDER BY createdAt DESC")
    fun getAllQuotationsFlow(): Flow<List<B2bQuotationEntity>>

    @Query("SELECT * FROM b2b_quotations WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getQuotationsForBusinessFlow(businessId: String): Flow<List<B2bQuotationEntity>>

    @Query("SELECT * FROM b2b_quotations WHERE quotationId = :quotationId")
    suspend fun getQuotationById(quotationId: String): B2bQuotationEntity?

    @Query("SELECT * FROM b2b_quotation_items WHERE quotationId = :quotationId")
    suspend fun getQuotationItems(quotationId: String): List<B2bQuotationItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: B2bQuotationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotationItems(items: List<B2bQuotationItemEntity>)

    @Update
    suspend fun updateQuotation(quotation: B2bQuotationEntity)

    // --- Orders ---
    @Query("SELECT * FROM b2b_orders ORDER BY createdAt DESC")
    fun getAllOrdersFlow(): Flow<List<B2bOrderEntity>>

    @Query("SELECT * FROM b2b_orders WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getOrdersForBusinessFlow(businessId: String): Flow<List<B2bOrderEntity>>

    @Query("SELECT * FROM b2b_orders WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: String): B2bOrderEntity?

    @Transaction
    @Query("SELECT * FROM b2b_orders WHERE orderId = :orderId")
    suspend fun getOrderWithItems(orderId: String): B2bOrderWithItems?

    @Query("SELECT * FROM b2b_order_items WHERE orderId = :orderId")
    suspend fun getOrderItems(orderId: String): List<B2bOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: B2bOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<B2bOrderItemEntity>)

    @Update
    suspend fun updateOrder(order: B2bOrderEntity)

    // --- Invoices ---
    @Query("SELECT * FROM b2b_invoices ORDER BY issueDate DESC")
    fun getAllInvoicesFlow(): Flow<List<B2bInvoiceEntity>>

    @Query("SELECT * FROM b2b_invoices WHERE businessId = :businessId ORDER BY issueDate DESC")
    fun getInvoicesForBusinessFlow(businessId: String): Flow<List<B2bInvoiceEntity>>

    @Query("SELECT * FROM b2b_invoices WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceById(invoiceId: String): B2bInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: B2bInvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: B2bInvoiceEntity)

    // --- Payments ---
    @Query("SELECT * FROM b2b_payments ORDER BY createdAt DESC")
    fun getAllPaymentsFlow(): Flow<List<B2bPaymentEntity>>

    @Query("SELECT * FROM b2b_payments WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getPaymentsForBusinessFlow(businessId: String): Flow<List<B2bPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: B2bPaymentEntity)

    // --- Returns ---
    @Query("SELECT * FROM b2b_returns ORDER BY createdAt DESC")
    fun getAllReturnsFlow(): Flow<List<B2bReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(b2bReturn: B2bReturnEntity)

    // --- Deliveries ---
    @Query("SELECT * FROM b2b_deliveries ORDER BY deliveryId DESC")
    fun getAllDeliveriesFlow(): Flow<List<B2bDeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: B2bDeliveryEntity)

    @Update
    suspend fun updateDelivery(delivery: B2bDeliveryEntity)
}
