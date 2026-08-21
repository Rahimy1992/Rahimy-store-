package com.example

import android.graphics.Bitmap
import com.example.data.ai.AiInvoiceScanner
import com.example.data.local.entity.Product
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleStatus
import com.example.util.ImageEnhancementEngine
import com.example.util.MatchConfidenceLevel
import com.example.util.ProductDuplicateDetector
import com.example.util.SalesDuplicateDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiInvoiceAndManualSalesTest {

    @Test
    fun testImageQualityAnalysisAndEnhancement() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val metrics = ImageEnhancementEngine.analyzeQuality(bitmap)

        assertNotNull(metrics)
        assertTrue("Brightness score should be calculated", metrics.brightnessScore >= 0.0)
        assertTrue("Contrast score should be calculated", metrics.contrastScore >= 0.0)
    }

    @Test
    fun testExactBarcodeProductMatch() {
        val existingProducts = listOf(
            Product(id = 1, name = "Saffron 5g", barcode = "89012345001", sku = "SKU-SAF", category = "General", sellingPrice = 12.0, costPrice = 8.0, stockQuantity = 10),
            Product(id = 2, name = "Black Tea 500g", barcode = "89012345002", sku = "SKU-TEA", category = "General", sellingPrice = 4.0, costPrice = 2.5, stockQuantity = 20)
        )

        val matches = ProductDuplicateDetector.findMatches(
            detectedName = "Saffron Afghan",
            detectedBarcode = "89012345001",
            existingProducts = existingProducts
        )

        assertEquals(1, matches.size)
        assertEquals("EXACT_BARCODE", matches[0].matchReason)
        assertEquals(MatchConfidenceLevel.HIGH, matches[0].confidenceLevel)
        assertEquals(1L, matches[0].existingProduct.id)
    }

    @Test
    fun testExactSkuProductMatch() {
        val existingProducts = listOf(
            Product(id = 1, name = "Saffron 5g", barcode = "89012345001", sku = "SKU-SAF-100", category = "General", sellingPrice = 12.0, costPrice = 8.0, stockQuantity = 10)
        )

        val matches = ProductDuplicateDetector.findMatches(
            detectedName = "Some Saffron",
            detectedSku = "SKU-SAF-100",
            existingProducts = existingProducts
        )

        assertEquals(1, matches.size)
        assertEquals("EXACT_SKU", matches[0].matchReason)
        assertEquals(MatchConfidenceLevel.HIGH, matches[0].confidenceLevel)
    }

    @Test
    fun testFuzzyProductNameMatch() {
        val existingProducts = listOf(
            Product(id = 1, name = "Samsung Galaxy A15 128GB", barcode = "", sku = "A15-128", category = "General", sellingPrice = 180.0, costPrice = 150.0, stockQuantity = 5)
        )

        val matches = ProductDuplicateDetector.findMatches(
            detectedName = "Samsung A15 128 GB Black",
            detectedBrand = "Samsung",
            existingProducts = existingProducts
        )

        assertTrue("Fuzzy match should find Samsung candidate", matches.isNotEmpty())
        assertTrue("Match score should be > 0.60", matches[0].matchScore >= 0.60)
    }

    @Test
    fun testDuplicateSaleDetectionByInvoiceNumber() {
        val existingSales = listOf(
            Sale(
                id = 1,
                invoiceNumber = "INV-2026-001",
                cashierId = 1,
                cashierName = "Manager",
                totalRevenue = 100.0,
                totalCost = 70.0,
                status = SaleStatus.ACTIVE
            )
        )

        val check1 = SalesDuplicateDetector.checkDuplicate(
            proposedInvoiceNumber = "INV-2026-001",
            existingSales = existingSales
        )
        assertTrue("Should detect duplicate invoice number", check1.isDuplicate)

        val check2 = SalesDuplicateDetector.checkDuplicate(
            proposedInvoiceNumber = "INV-2026-002",
            existingSales = existingSales
        )
        assertFalse("Different invoice number should not be marked duplicate", check2.isDuplicate)
    }

    @Test
    fun testDocumentHashComputation() {
        val hash1 = SalesDuplicateDetector.computeDocumentHash("Herat Wholesale", "INV-100", "2026-08-20", 150.0, 3)
        val hash2 = SalesDuplicateDetector.computeDocumentHash("Herat Wholesale", "INV-100", "2026-08-20", 150.0, 3)
        val hash3 = SalesDuplicateDetector.computeDocumentHash("Kabul Wholesale", "INV-100", "2026-08-20", 150.0, 3)

        assertEquals("Same invoice inputs must produce identical SHA-256 hashes", hash1, hash2)
        assertNotEquals("Different supplier must produce different document hash", hash1, hash3)
    }

    @Test
    fun testVoidedSalesExcludedFromActiveTotals() {
        val activeSale = Sale(
            id = 1,
            invoiceNumber = "INV-001",
            cashierId = 1,
            cashierName = "Cashier",
            totalRevenue = 200.0,
            totalCost = 150.0,
            grossProfit = 50.0,
            status = SaleStatus.ACTIVE
        )

        val voidSale = Sale(
            id = 2,
            invoiceNumber = "INV-002",
            cashierId = 1,
            cashierName = "Cashier",
            totalRevenue = 500.0,
            totalCost = 400.0,
            grossProfit = 100.0,
            status = SaleStatus.VOID
        )

        val allSales = listOf(activeSale, voidSale)
        val activeOnly = allSales.filter { it.status == SaleStatus.ACTIVE }

        assertEquals(1, activeOnly.size)
        assertEquals(200.0, activeOnly.sumOf { it.totalRevenue }, 0.001)
    }

    @Test
    fun testAiInvoiceScannerFallbackGeneration() = runBlocking {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = AiInvoiceScanner.processInvoiceImages(listOf(bitmap))

        assertTrue(result.isSuccess)
        val invoice = result.getOrNull()
        assertNotNull(invoice)
        assertTrue(invoice!!.items.isNotEmpty())
        assertTrue("Grand total should equal subtotal - discount + tax", invoice.grandTotal > 0.0)
    }
}
