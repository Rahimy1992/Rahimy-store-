package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.entity.CustomerDebt
import com.example.data.local.entity.Product
import com.example.data.local.entity.Sale
import com.example.data.repository.SalesFinancialMetrics
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.LocalizationState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelCsvExporter {

    private fun getBOM(): ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun exportProductsToCsv(context: Context, products: List<Product>, localization: LocalizationState): File? {
        return try {
            val file = File(context.cacheDir, "Rahimy_Inventory_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(getBOM()) // UTF-8 BOM for Excel Farasi/Arabic compatibility

            val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")
            val header = if (isFa) {
                "کد (ID),نام جنس,بارکد,کد SKU,دسته‌بندی,برند,قیمت خرید (USD),قیمت فروش (USD),قیمت فروش ($),موجودی,حد مجاز کمبود,توضیحات\n"
            } else {
                "ID,Name,Barcode,SKU,Category,Brand,Cost Price (USD),Selling Price (USD),Local Selling Price,Stock Qty,Min Threshold,Description\n"
            }
            fos.write(header.toByteArray(Charsets.UTF_8))

            products.forEach { p ->
                val localPrice = LocalizationManager.formatDualCurrency(
                    p.sellingPrice,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ).replace(",", " ")

                val row = "\"${p.id}\",\"${clean(p.name)}\",\"${p.barcode}\",\"${p.sku}\",\"${clean(p.category)}\",\"${clean(p.brand)}\",\"${p.costPrice}\",\"${p.sellingPrice}\",\"$localPrice\",\"${p.stockQuantity}\",\"${p.minStockThreshold}\",\"${clean(p.description)}\"\n"
                fos.write(row.toByteArray(Charsets.UTF_8))
            }
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportSalesToCsv(context: Context, sales: List<Sale>, localization: LocalizationState): File? {
        return try {
            val file = File(context.cacheDir, "Rahimy_Sales_Report_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(getBOM())

            val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val header = if (isFa) {
                "شماره فاکتور,تاریخ و زمان,نام تحویل‌دار,مجموع درآمد (USD),درآمد ارزی,مجموع هزینه (USD),سود ناخالص (USD),حاشیه سود %,وضعیت فاکتور,علت ابطال\n"
            } else {
                "Invoice #,Date & Time,Cashier Name,Revenue (USD),Local Revenue,Cost (USD),Gross Profit (USD),Profit Margin %,Status,Void Reason\n"
            }
            fos.write(header.toByteArray(Charsets.UTF_8))

            sales.forEach { s ->
                val dateStr = sdf.format(Date(s.timestamp))
                val localRevenue = LocalizationManager.formatDualCurrency(
                    s.totalRevenue,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ).replace(",", " ")

                val row = "\"${s.invoiceNumber}\",\"$dateStr\",\"${clean(s.cashierName)}\",\"${s.totalRevenue}\",\"$localRevenue\",\"${s.totalCost}\",\"${s.grossProfit}\",\"${String.format("%.2f", s.profitMarginPercent)}\",\"${s.status.name}\",\"${clean(s.voidReason)}\"\n"
                fos.write(row.toByteArray(Charsets.UTF_8))
            }
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportDebtsToCsv(context: Context, debts: List<CustomerDebt>, localization: LocalizationState): File? {
        return try {
            val file = File(context.cacheDir, "Rahimy_Customer_Debts_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(getBOM())

            val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val header = if (isFa) {
                "کد,نام مشتری,شماره تماس,کل بدهی (USD),بدهی ارزی,آخرین تراکنش,آخرین مبلغ (USD),شماره فاکتور,وضعیت حساب,ملاحظات\n"
            } else {
                "ID,Customer Name,Phone Number,Total Debt (USD),Local Debt,Last Transaction,Last Amount (USD),Invoice #,Status,Notes\n"
            }
            fos.write(header.toByteArray(Charsets.UTF_8))

            debts.forEach { d ->
                val localDebt = LocalizationManager.formatDualCurrency(
                    d.totalDebtUsd,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ).replace(",", " ")
                val statusStr = if (d.isSettled) (if (isFa) "تسویه شده" else "Settled") else (if (isFa) "بدهکار" else "Outstanding")

                val row = "\"${d.id}\",\"${clean(d.customerName)}\",\"${d.phoneNumber}\",\"${d.totalDebtUsd}\",\"$localDebt\",\"${d.lastTransactionType}\",\"${d.lastAmountUsd}\",\"${d.saleInvoiceNumber}\",\"$statusStr\",\"${clean(d.notes)}\"\n"
                fos.write(row.toByteArray(Charsets.UTF_8))
            }
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportFinancialsToCsv(context: Context, metrics: SalesFinancialMetrics, localization: LocalizationState): File? {
        return try {
            val file = File(context.cacheDir, "Rahimy_Financial_P&L_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            fos.write(getBOM())

            val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")
            val header = if (isFa) {
                "شاخص مالی,مقدار (USD),مقدار با نرخ ارز اصلی\n"
            } else {
                "Metric,Value (USD),Local Currency Value\n"
            }
            fos.write(header.toByteArray(Charsets.UTF_8))

            val formatLocal: (Double) -> String = { amount ->
                LocalizationManager.formatDualCurrency(
                    amount,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ).replace(",", " ")
            }

            val rows = listOf(
                Pair(if (isFa) "مجموع کل درآمد" else "Total Revenue", metrics.totalRevenue),
                Pair(if (isFa) "هزینه تمام‌شده اجناس (COGS)" else "Total Cost (COGS)", metrics.totalCost),
                Pair(if (isFa) "سود ناخالص" else "Gross Profit", metrics.grossProfit),
                Pair(if (isFa) "میانگین ارزش فاکتور (AOV)" else "Average Order Value", metrics.averageOrderValue),
                Pair(if (isFa) "میانگین فروش روزانه" else "Avg Daily Sales", metrics.averageDailySales),
                Pair(if (isFa) "میانگین فروش هفتگی" else "Avg Weekly Sales", metrics.averageWeeklySales),
                Pair(if (isFa) "میانگین فروش ماهانه" else "Avg Monthly Sales", metrics.averageMonthlySales)
            )

            rows.forEach { (title, valUsd) ->
                val line = "\"$title\",\"$valUsd\",\"${formatLocal(valUsd)}\"\n"
                fos.write(line.toByteArray(Charsets.UTF_8))
            }

            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun clean(str: String): String {
        return str.replace("\"", "'").replace("\n", " ")
    }
}
