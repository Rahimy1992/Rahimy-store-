package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Product
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import com.example.data.repository.CartItem
import com.example.data.repository.ProductRepository
import com.example.data.repository.SaleRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HistoricalProfitTest {

    private lateinit var db: AppDatabase
    private lateinit var productRepo: ProductRepository
    private lateinit var saleRepo: SaleRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        productRepo = ProductRepository(db.productDao(), db.auditLogDao())
        saleRepo = SaleRepository(db.saleDao(), db.saleItemDao(), db.productDao(), db.auditLogDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testHistoricalSaleProfitImmutability() {
        runBlocking {
            // Step 1: Create Product A with buyPrice (cost) = 10, sellPrice = 15
            val productA = Product(
                name = "Product A",
                sku = "SKU-PROD-A",
                category = "Electronics",
                brand = "RahimyBrand",
                costPrice = 10.0,
                sellingPrice = 15.0,
                stockQuantity = 50
            )
            val prodId = productRepo.addProduct(productA, 1L, "admin", "SUPER_ADMIN")
            val savedProduct = productRepo.getProductById(prodId)
            assertNotNull(savedProduct)
            assertEquals(10.0, savedProduct!!.costPrice, 0.001)
            assertEquals(15.0, savedProduct.sellingPrice, 0.001)

            // Step 2: Complete a sale for 1 unit of Product A
            val cashier = User(id = 3, username = "cashier1", displayName = "Cashier 1", role = UserRole.EMPLOYEE)
            val cartItems = listOf(CartItem(product = savedProduct, quantity = 1))
            val saleId = saleRepo.checkout(
                cartItems = cartItems,
                cashierId = cashier.id,
                cashierName = cashier.displayName
            )

            // Verify sale record immediately after creation
            val (saleBefore, itemsBefore) = saleRepo.getSaleDetails(saleId)
            assertNotNull(saleBefore)
            assertEquals(15.0, saleBefore!!.totalRevenue, 0.001)
            assertEquals(10.0, saleBefore.totalCost ?: 0.0, 0.001)
            assertEquals(5.0, saleBefore.grossProfit ?: 0.0, 0.001)
            assertEquals(1, itemsBefore.size)
            assertEquals(10.0, itemsBefore[0].unitCostSnapshot ?: 0.0, 0.001)
            assertEquals(15.0, itemsBefore[0].unitPriceSnapshot, 0.001)
            assertEquals(5.0, itemsBefore[0].grossProfit ?: 0.0, 0.001)

            // Step 3: Change Product A: buyPrice = 13, sellPrice = 20
            val updatedProductA = savedProduct.copy(
                costPrice = 13.0,
                sellingPrice = 20.0
            )
            productRepo.updateProduct(updatedProductA, 1L, "admin", "SUPER_ADMIN")

            val currentProd = productRepo.getProductById(prodId)
            assertNotNull(currentProd)
            assertEquals(13.0, currentProd!!.costPrice, 0.001)
            assertEquals(20.0, currentProd.sellingPrice, 0.001)

            // Step 4: CRITICAL HISTORICAL PROFIT VERIFICATION:
            // Verify that the previous sale STILL reports: buyPrice = 10, grossProfit = 5
            val (saleAfter, itemsAfter) = saleRepo.getSaleDetails(saleId)
            assertNotNull(saleAfter)
            assertEquals("Historical revenue must remain unchanged at 15.0", 15.0, saleAfter!!.totalRevenue, 0.001)
            assertEquals("Historical total cost must remain unchanged at 10.0", 10.0, saleAfter.totalCost ?: 0.0, 0.001)
            assertEquals("Historical gross profit must remain unchanged at 5.0", 5.0, saleAfter.grossProfit ?: 0.0, 0.001)

            assertEquals(1, itemsAfter.size)
            assertEquals("Historical snapshot unit cost must remain 10.0", 10.0, itemsAfter[0].unitCostSnapshot ?: 0.0, 0.001)
            assertEquals("Historical snapshot unit price must remain 15.0", 15.0, itemsAfter[0].unitPriceSnapshot, 0.001)
            assertEquals("Historical item gross profit must remain 5.0", 5.0, itemsAfter[0].grossProfit ?: 0.0, 0.001)
        }
    }
}
