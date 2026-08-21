package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.LocalizationState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintInvoiceManager {

    fun generateReceiptText(
        sale: Sale,
        items: List<SaleItem>,
        customerName: String = "",
        paymentType: String = "Cash",
        localization: LocalizationState
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(sale.timestamp))
        val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

        val storeTitle = "==== Rahimy Smart Commerce ===="
        val invLine = if (isFa) "شماره فاکتور: ${sale.invoiceNumber}" else "Invoice #: ${sale.invoiceNumber}"
        val dateLine = if (isFa) "تاریخ و زمان: $dateStr" else "Date: $dateStr"
        val cashierLine = if (isFa) "تحویل‌دار: ${sale.cashierName}" else "Cashier: ${sale.cashierName}"
        val custLine = if (customerName.isNotBlank()) (if (isFa) "مشتری: $customerName" else "Customer: $customerName") else ""
        val payLine = if (isFa) "روش پرداخت: $paymentType" else "Payment: $paymentType"

        val divider = "--------------------------------"
        val itemHeaders = if (isFa) "جنس | تعداد | فی | مجموع" else "Item | Qty | Price | Total"

        val itemLines = items.joinToString("\n") { item ->
            val localPrice = LocalizationManager.formatDualCurrency(
                item.subtotal,
                localization.selectedCurrencyCode,
                localization.selectedCurrencySymbol,
                localization.exchangeRateToUSD,
                localization.markupPercent
            )
            "• ${item.productName}\n  ${item.quantity}x @ $localPrice"
        }

        val totalLocal = LocalizationManager.formatDualCurrency(
            sale.totalRevenue,
            localization.selectedCurrencyCode,
            localization.selectedCurrencySymbol,
            localization.exchangeRateToUSD,
            localization.markupPercent
        )
        val totalLine = if (isFa) "مجموع کل قابل پرداخت: $totalLocal" else "Total Amount: $totalLocal"
        val footer = if (isFa) "تشکر از خرید شما! / Rahimy Commerce" else "Thank you for your business!"

        return listOfNotNull(
            storeTitle,
            invLine,
            dateLine,
            cashierLine,
            if (custLine.isNotBlank()) custLine else null,
            payLine,
            divider,
            itemHeaders,
            divider,
            itemLines,
            divider,
            totalLine,
            divider,
            footer
        ).joinToString("\n")
    }

    fun shareReceiptText(context: Context, receiptText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rahimy Smart Commerce - Invoice")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        context.startActivity(Intent.createChooser(intent, "ارسال فاکتور / Share Invoice"))
    }

    fun printInvoiceHtml(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        customerName: String = "",
        paymentType: String = "نقدی",
        localization: LocalizationState
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(sale.timestamp))
        val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

        val totalLocal = LocalizationManager.formatDualCurrency(
            sale.totalRevenue,
            localization.selectedCurrencyCode,
            localization.selectedCurrencySymbol,
            localization.exchangeRateToUSD,
            localization.markupPercent
        )

        val itemsHtml = items.joinToString("") { item ->
            val itemLocalPrice = LocalizationManager.formatDualCurrency(
                item.subtotal,
                localization.selectedCurrencyCode,
                localization.selectedCurrencySymbol,
                localization.exchangeRateToUSD,
                localization.markupPercent
            )
            val unitLocalPrice = LocalizationManager.formatDualCurrency(
                item.unitPriceSnapshot,
                localization.selectedCurrencyCode,
                localization.selectedCurrencySymbol,
                localization.exchangeRateToUSD,
                localization.markupPercent
            )
            """
            <tr>
                <td style="padding: 6px; border-bottom: 1px solid #eee;">${item.productName}</td>
                <td style="padding: 6px; border-bottom: 1px solid #eee; text-align: center;">${item.quantity}</td>
                <td style="padding: 6px; border-bottom: 1px solid #eee; text-align: right;">$unitLocalPrice</td>
                <td style="padding: 6px; border-bottom: 1px solid #eee; text-align: right; font-weight: bold;">$itemLocalPrice</td>
            </tr>
            """.trimIndent()
        }

        val htmlContent = """
        <!DOCTYPE html>
        <html dir="${if (isFa) "rtl" else "ltr"}">
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: sans-serif; margin: 20px; color: #222; font-size: 14px; }
                .receipt-card { border: 2px solid #1a237e; border-radius: 12px; padding: 20px; max-width: 480px; margin: auto; }
                .header { text-align: center; border-bottom: 2px dashed #1a237e; padding-bottom: 12px; margin-bottom: 16px; }
                .store-name { font-size: 22px; font-weight: bold; color: #1a237e; }
                .info-row { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; }
                table { width: 100%; border-collapse: collapse; margin-top: 12px; margin-bottom: 16px; }
                th { background-color: #f0f4f9; padding: 8px; text-align: right; font-size: 12px; }
                .total-box { background: #e8eaf6; border-radius: 8px; padding: 12px; text-align: center; font-size: 18px; font-weight: bold; color: #1a237e; }
                .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
            </style>
        </head>
        <body>
            <div class="receipt-card">
                <div class="header">
                    <div class="store-name">Rahimy Smart Commerce</div>
                    <div>فروشگاه و صندوق هوشمند رحیمی</div>
                </div>
                
                <div class="info-row">
                    <span>${if (isFa) "شماره فاکتور:" else "Invoice #:"} <b>${sale.invoiceNumber}</b></span>
                    <span>${dateStr}</span>
                </div>
                <div class="info-row">
                    <span>${if (isFa) "تحویل‌دار:" else "Cashier:"} ${sale.cashierName}</span>
                    <span>${if (isFa) "نوع پرداخت:" else "Payment:"} <b>$paymentType</b></span>
                </div>
                ${if (customerName.isNotBlank()) "<div class=\"info-row\"><span>${if (isFa) "مشتری:" else "Customer:"} <b>$customerName</b></span></div>" else ""}

                <table>
                    <thead>
                        <tr>
                            <th style="text-align: right;">${if (isFa) "نام جنس" else "Item"}</th>
                            <th style="text-align: center;">${if (isFa) "تعداد" else "Qty"}</th>
                            <th style="text-align: right;">${if (isFa) "فی" else "Price"}</th>
                            <th style="text-align: right;">${if (isFa) "مجموع" else "Total"}</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>

                <div class="total-box">
                    ${if (isFa) "مجموع کل قابل پرداخت:" else "Total Amount:"}<br>
                    <span>$totalLocal</span>
                </div>

                <div class="footer">
                    ${if (isFa) "تشکر از خرید و اعتماد شما! • Rahimy POS System" else "Thank you for shopping with us!"}
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter("Invoice_${sale.invoiceNumber}")
                val builder = PrintAttributes.Builder()
                builder.setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                printManager.print("Invoice_${sale.invoiceNumber}", printAdapter, builder.build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }
}
