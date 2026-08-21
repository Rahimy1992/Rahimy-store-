package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.B2bOrderItemEntity
import com.example.data.local.entity.B2bQuotationItemEntity
import com.example.data.local.entity.BusinessCustomerEntity
import com.example.data.local.entity.Product
import com.example.data.local.entity.WholesalePriceTierEntity
import com.example.data.repository.B2bRepository
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
@Config(sdk = [33])
class B2bWholesaleTest {

    private lateinit var db: AppDatabase
    private lateinit var b2bRepo: B2bRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        b2bRepo = B2bRepository(context, db)

        runBlocking {
            // Seed a test product
            db.productDao().insertProduct(
                Product(
                    id = 101L,
                    name = "Wholesale Wheat Sack 50kg",
                    category = "Grains",
                    brand = "Kabul Mills",
                    sku = "WHEAT-50KG",
                    barcode = "893001122",
                    sellingPrice = 25.0,
                    costPrice = 18.0,
                    stockQuantity = 500,
                    minStockThreshold = 50
                )
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testBusinessCustomerCrudAndCreditLimit() = runBlocking {
        val customer = BusinessCustomerEntity(
            businessId = "BUS-TEST-1",
            businessName = "Herat General Trading",
            ownerName = "Ahmad Herati",
            contactPerson = "Ahmad",
            phone = "+93700111222",
            email = "info@herattrading.af",
            address = "Main Bazaar",
            city = "Herat",
            customerCode = "CUST-HRT-01",
            creditLimit = 10000.0,
            currentBalance = 8000.0
        )

        b2bRepo.saveBusinessCustomer(customer)
        val fetched = b2bRepo.getBusinessCustomerById("BUS-TEST-1")
        assertNotNull(fetched)
        assertEquals("Herat General Trading", fetched?.businessName)

        // Check Credit Limit Enforcement
        val allow500 = b2bRepo.checkCreditLimit("BUS-TEST-1", 500.0)
        assertTrue(allow500) // 8000 + 500 <= 10000

        val allow3000 = b2bRepo.checkCreditLimit("BUS-TEST-1", 3000.0)
        assertFalse(allow3000) // 8000 + 3000 > 10000
    }

    @Test
    fun testWholesalePriceTiersAndCustomerSpecificPricing() = runBlocking {
        // General Tier
        b2bRepo.savePriceTier(
            WholesalePriceTierEntity(
                tierId = "TIER-BULK-100",
                productId = 101L,
                minQuantity = 100,
                maxQuantity = 500,
                tierName = "BULK",
                priceUsd = 20.0
            )
        )

        // Customer-Specific Tier
        b2bRepo.savePriceTier(
            WholesalePriceTierEntity(
                tierId = "TIER-VIP-CUST",
                productId = 101L,
                customerBusinessId = "BUS-VIP",
                minQuantity = 1,
                tierName = "CUSTOM_SPECIAL",
                priceUsd = 18.50
            )
        )

        // 1. Regular Customer buying 5 sacks -> Retail ($25.0)
        val res1 = b2bRepo.calculateWholesaleUnitPrice(101L, 5, "BUS-REGULAR")
        assertEquals(25.0, res1.first, 0.01)

        // 2. Regular Customer buying 120 sacks -> Quantity Tier ($20.0)
        val res2 = b2bRepo.calculateWholesaleUnitPrice(101L, 120, "BUS-REGULAR")
        assertEquals(20.0, res2.first, 0.01)

        // 3. VIP Customer buying 1 sack -> Customer-Specific Price ($18.50)
        val res3 = b2bRepo.calculateWholesaleUnitPrice(101L, 1, "BUS-VIP")
        assertEquals(18.50, res3.first, 0.01)
        assertEquals("CUSTOM_SPECIAL", res3.second)
    }

    @Test
    fun testQuotationToOrderToInvoiceAndPaymentFlow() = runBlocking {
        val customer = BusinessCustomerEntity(
            businessId = "BUS-FLOW-1",
            businessName = "Mazar Wholesale Mart",
            ownerName = "Jan Mazar",
            contactPerson = "Jan",
            phone = "+93799887766",
            email = "jan@mazarmart.af",
            address = "Darwaza Balkh",
            city = "Mazar-i-Sharif",
            customerCode = "CUST-MZR-01",
            creditLimit = 20000.0,
            currentBalance = 0.0
        )
        b2bRepo.saveBusinessCustomer(customer)

        // 1. Create Quotation
        val item = B2bQuotationItemEntity(
            itemId = "QI-1",
            quotationId = "",
            productId = 101L,
            productName = "Wholesale Wheat Sack 50kg",
            sku = "WHEAT-50KG",
            quantity = 50,
            unitPriceUsd = 20.0,
            subtotalUsd = 1000.0
        )
        val qId = b2bRepo.createQuotation("BUS-FLOW-1", listOf(item))
        assertNotNull(qId)

        // 2. Convert Quotation to Order
        val result = b2bRepo.convertQuotationToOrder(qId, "Balkh Street, Mazar")
        assertTrue(result.isSuccess)
        val orderId = result.getOrNull()!!

        // Verify Stock Deducted
        val p = db.productDao().getProductById(101L)
        assertEquals(450, p?.stockQuantity) // 500 - 50 = 450

        // Verify Invoice Created
        val invoices = b2bRepo.allInvoices.first()
        val invoice = invoices.firstOrNull { it.orderId == orderId }
        assertNotNull(invoice)
        assertEquals(1000.0, invoice?.totalUsd!!, 0.01)
        assertEquals("UNPAID", invoice.paymentStatus)

        // 3. Record Partial Payment
        val payRes = b2bRepo.recordPayment(
            businessId = "BUS-FLOW-1",
            invoiceId = invoice.invoiceId,
            amountUsd = 400.0,
            method = "BANK_TRANSFER",
            referenceNumber = "TRX-88412"
        )
        assertTrue(payRes.isSuccess)

        // Verify Invoice Status updated to PARTIALLY_PAID
        val invUpdated = db.b2bDao().getInvoiceById(invoice.invoiceId)
        assertEquals("PARTIALLY_PAID", invUpdated?.paymentStatus)
        assertEquals(400.0, invUpdated?.paidAmountUsd!!, 0.01)

        // Verify Business Balance is $600 ($1000 order - $400 paid)
        val custUpdated = b2bRepo.getBusinessCustomerById("BUS-FLOW-1")
        assertEquals(600.0, custUpdated?.currentBalance!!, 0.01)
    }

    @Test
    fun testB2bReturnsAndInventoryRestoration() = runBlocking {
        val customer = BusinessCustomerEntity(
            businessId = "BUS-RET-1",
            businessName = "Kandahar Retailers Co.",
            ownerName = "Khan",
            contactPerson = "Khan",
            phone = "+93700554433",
            email = "khan@kandahar.af",
            address = "Aino Mina",
            city = "Kandahar",
            customerCode = "CUST-KND-01",
            creditLimit = 15000.0,
            currentBalance = 1000.0
        )
        b2bRepo.saveBusinessCustomer(customer)

        val item = B2bOrderItemEntity(
            itemId = "OI-RET-1",
            orderId = "",
            productId = 101L,
            productName = "Wholesale Wheat Sack 50kg",
            sku = "WHEAT-50KG",
            quantity = 10,
            unitPriceUsd = 20.0,
            subtotalUsd = 200.0
        )

        val orderRes = b2bRepo.createDirectOrder(
            businessId = "BUS-RET-1",
            items = listOf(item),
            deliveryAddress = "Aino Mina, Kandahar"
        )
        assertTrue(orderRes.isSuccess)
        val orderId = orderRes.getOrNull()!!

        // Initial stock was 500 - 10 = 490
        val pAfterOrder = db.productDao().getProductById(101L)
        assertEquals(490, pAfterOrder?.stockQuantity)

        // Process Return with Restock = true
        val retId = b2bRepo.processReturn(
            orderId = orderId,
            invoiceId = "INV-DUMMY",
            businessId = "BUS-RET-1",
            reason = "DAMAGED_PACKAGING",
            refundAmountUsd = 200.0,
            restockInventory = true
        )
        assertNotNull(retId)

        // Stock restored to 500
        val pAfterReturn = db.productDao().getProductById(101L)
        assertEquals(500, pAfterReturn?.stockQuantity)
    }
}
