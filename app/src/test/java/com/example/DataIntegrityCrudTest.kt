package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CurrencyConfig
import com.example.data.local.entity.Product
import com.example.data.local.entity.SaleStatus
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import com.example.data.repository.CartItem
import com.example.data.repository.CurrencyRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SaleRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.first
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
class DataIntegrityCrudTest {

    private lateinit var db: AppDatabase
    private lateinit var productRepo: ProductRepository
    private lateinit var saleRepo: SaleRepository
    private lateinit var userRepo: UserRepository
    private lateinit var currencyRepo: CurrencyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        productRepo = ProductRepository(db.productDao(), db.auditLogDao())
        saleRepo = SaleRepository(db.saleDao(), db.saleItemDao(), db.productDao(), db.auditLogDao())
        userRepo = UserRepository(db.userDao(), db.auditLogDao())
        currencyRepo = CurrencyRepository(db.currencyConfigDao(), db.auditLogDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testProductCrudAndStockManagement() {
        runBlocking {
            val product = Product(
                name = "Test Item",
                sku = "SKU-TEST-1",
                category = "General",
                brand = "TestBrand",
                costPrice = 20.0,
                sellingPrice = 30.0,
                stockQuantity = 100
            )
            val id = productRepo.addProduct(product, 1L, "admin", "SUPER_ADMIN")
            val fetched = productRepo.getProductById(id)
            assertNotNull(fetched)
            assertEquals("Test Item", fetched!!.name)
            assertEquals(100, fetched.stockQuantity)

            // Checkout 10 units
            val saleId = saleRepo.checkout(
                cartItems = listOf(CartItem(product = fetched, quantity = 10)),
                cashierId = 1L,
                cashierName = "admin"
            )
            val productAfterSale = productRepo.getProductById(id)
            assertEquals("Stock should be reduced by 10", 90, productAfterSale!!.stockQuantity)

            // Void the sale -> Stock must be restored
            saleRepo.voidSale(
                saleId = saleId,
                reason = "Customer cancelled before collection",
                voidedByUserId = 1L,
                voidedByUsername = "admin",
                voidedByUserRole = "SUPER_ADMIN"
            )
            val productAfterVoid = productRepo.getProductById(id)
            assertEquals("Stock must be restored to 100 after voiding", 100, productAfterVoid!!.stockQuantity)

            val (voidedSale, _) = saleRepo.getSaleDetails(saleId)
            assertNotNull(voidedSale)
            assertEquals(SaleStatus.VOID, voidedSale!!.status)
        }
    }

    @Test
    fun testCurrencyConfigurationDisplayOnly() {
        runBlocking {
            val config = CurrencyConfig(
                currencyCode = "AFN",
                currencySymbol = "؋",
                displayName = "Afghan Afghani",
                exchangeRateToUSD = 71.5,
                markupPercent = 2.0,
                isPrimaryRegional = true
            )
            db.currencyConfigDao().insertCurrency(config)

            val fetched = currencyRepo.getCurrencyByCode("AFN")
            assertNotNull(fetched)
            assertEquals(71.5, fetched!!.exchangeRateToUSD, 0.001)
            assertEquals(2.0, fetched.markupPercent, 0.001)

            val managerUser = User(id = 2, username = "manager", displayName = "Manager", role = UserRole.MANAGER)
            currencyRepo.updateCurrencyConfig("AFN", 72.0, 2.5, managerUser)

            val updated = currencyRepo.getCurrencyByCode("AFN")
            assertEquals(72.0, updated!!.exchangeRateToUSD, 0.001)
            assertEquals(2.5, updated.markupPercent, 0.001)

            // Verify audit log has the currency change record
            val auditLogs = db.auditLogDao().getAllLogs().first()
            assertTrue(auditLogs.any { it.actionType == "CURRENCY_RATE_CHANGED" })
        }
    }
}
