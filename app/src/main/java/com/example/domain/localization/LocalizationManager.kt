package com.example.domain.localization

import androidx.compose.ui.unit.LayoutDirection
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val layoutDirection: LayoutDirection,
    val defaultCurrency: String,
    val defaultTimeZone: String
) {
    DARI("prs", "دری (افغانستان)", "Dari (Persian)", LayoutDirection.Rtl, "AFN", "Asia/Kabul"),
    PERSIAN("fa", "فارسی", "Persian", LayoutDirection.Rtl, "AFN", "Asia/Kabul"),
    PASHTO("ps", "پښتو", "Pashto", LayoutDirection.Rtl, "AFN", "Asia/Kabul"),
    ENGLISH("en", "English", "English", LayoutDirection.Ltr, "USD", "America/New_York"),
    ARABIC("ar", "العربية", "Arabic", LayoutDirection.Rtl, "SAR", "Asia/Riyadh"),
    TURKISH("tr", "Türkçe", "Turkish", LayoutDirection.Ltr, "TRY", "Europe/Istanbul"),
    SPANISH("es", "Español", "Spanish", LayoutDirection.Ltr, "EUR", "Europe/Madrid")
}

data class LocalizationState(
    val currentLanguage: SupportedLanguage = SupportedLanguage.DARI,
    val isRtl: Boolean = true,
    val selectedCurrencyCode: String = "AFN",
    val selectedCurrencySymbol: String = "؋",
    val exchangeRateToUSD: Double = 71.50,
    val markupPercent: Double = 2.0,
    val currentTimeZoneId: String = "Asia/Kabul",
    val numberFormat: String = "STANDARD" // STANDARD or LOCALE_SPECIFIC
)

object LocalizationManager {

    fun getLocaleForLanguage(language: SupportedLanguage): Locale {
        return when (language) {
            SupportedLanguage.DARI -> Locale("fa", "AF")
            SupportedLanguage.PERSIAN -> Locale("fa", "IR")
            SupportedLanguage.PASHTO -> Locale("ps", "AF")
            SupportedLanguage.ARABIC -> Locale("ar")
            SupportedLanguage.TURKISH -> Locale("tr")
            SupportedLanguage.SPANISH -> Locale("es")
            SupportedLanguage.ENGLISH -> Locale.ENGLISH
        }
    }

    fun updateAppLocale(context: android.content.Context, language: SupportedLanguage) {
        val locale = getLocaleForLanguage(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Converts base USD price into display formatted string containing BOTH regional currency and USD.
     * Requirement 10: "The application is NOT a currency-exchange system. Currency conversion exists only
     * for product-price and business-data DISPLAY... USD must remain available alongside the regional currency."
     */
    fun formatDualCurrency(
        usdAmount: Double,
        currencyCode: String,
        currencySymbol: String,
        exchangeRateToUSD: Double,
        markupPercent: Double = 0.0,
        locale: Locale = Locale.US
    ): String {
        val effectiveRate = exchangeRateToUSD * (1.0 + markupPercent / 100.0)
        val regionalValue = usdAmount * effectiveRate

        val symbols = DecimalFormatSymbols(locale)
        val regionalFormatter = DecimalFormat("#,##0.00", symbols)
        val usdFormatter = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

        val formattedRegional = "$currencySymbol ${regionalFormatter.format(regionalValue)}"
        val formattedUsd = "$${usdFormatter.format(usdAmount)} USD"

        return if (currencyCode.equals("USD", ignoreCase = true)) {
            formattedUsd
        } else {
            "$formattedRegional  (~$formattedUsd)"
        }
    }

    /**
     * Formats single amount with currency code/symbol
     */
    fun formatSingleCurrency(
        amount: Double,
        currencySymbol: String,
        locale: Locale = Locale.US
    ): String {
        val symbols = DecimalFormatSymbols(locale)
        val formatter = DecimalFormat("#,##0.00", symbols)
        return "$currencySymbol ${formatter.format(amount)}"
    }

    /**
     * Formats timestamp according to selected language, locale, and timezone.
     * Requirement 11: "Display date/time according to selected language, locale, timezone."
     */
    fun formatDateTime(
        epochMillis: Long,
        pattern: String = "yyyy-MM-dd HH:mm",
        timeZoneId: String = "UTC",
        locale: Locale = Locale.US
    ): String {
        val sdf = SimpleDateFormat(pattern, locale)
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(epochMillis))
    }

    fun formatDateOnly(
        epochMillis: Long,
        timeZoneId: String = "UTC",
        locale: Locale = Locale.US
    ): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", locale)
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(epochMillis))
    }

    fun formatMonthYear(
        epochMillis: Long,
        timeZoneId: String = "UTC",
        locale: Locale = Locale.US
    ): String {
        val sdf = SimpleDateFormat("MMM yyyy", locale)
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(epochMillis))
    }

    fun getString(key: String, language: SupportedLanguage): String {
        return when (key) {
            "manual_sales" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "فروش دستی"
                SupportedLanguage.PASHTO -> "لاسي پلورنه"
                SupportedLanguage.ARABIC -> "المبيعات اليدوية"
                SupportedLanguage.TURKISH -> "Manuel Satış"
                SupportedLanguage.SPANISH -> "Venta Manual"
                else -> "Manual Sales"
            }
            "online_services" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "خدمات آنلاین"
                SupportedLanguage.PASHTO -> "آنلاين خدمتونه"
                SupportedLanguage.ARABIC -> "الخدمات عبر الإنترنت"
                SupportedLanguage.TURKISH -> "Çevrimiçi Hizmetler"
                SupportedLanguage.SPANISH -> "Servicios en Línea"
                else -> "Online Services"
            }
            "support" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "پشتیبانی"
                SupportedLanguage.PASHTO -> "ملاتړ او مرسته"
                SupportedLanguage.ARABIC -> "الدعم الفني"
                SupportedLanguage.TURKISH -> "Destek"
                SupportedLanguage.SPANISH -> "Soporte"
                else -> "Support"
            }
            "contact_support" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تماس با پشتیبانی"
                SupportedLanguage.PASHTO -> "له ملاتړ سره اړیکه"
                SupportedLanguage.ARABIC -> "الاتصال بالدعم"
                SupportedLanguage.TURKISH -> "Destekle İletişime Geç"
                SupportedLanguage.SPANISH -> "Contactar a Soporte"
                else -> "Contact Support"
            }
            "support_chat" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "گفتوگوی پشتیبانی"
                SupportedLanguage.PASHTO -> "د ملاتړ چیټ"
                SupportedLanguage.ARABIC -> "دردشة الدعم"
                SupportedLanguage.TURKISH -> "Destek Sohbeti"
                SupportedLanguage.SPANISH -> "Chat de Soporte"
                else -> "Support Chat"
            }
            "my_tickets" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "درخواست‌های من"
                SupportedLanguage.PASHTO -> "زما غوښتنې"
                SupportedLanguage.ARABIC -> "تذاكري"
                SupportedLanguage.TURKISH -> "Destek Taleplerim"
                SupportedLanguage.SPANISH -> "Mis Tickets"
                else -> "My Tickets"
            }
            "send_message" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "ارسال پیام"
                SupportedLanguage.PASHTO -> "پیغام واستوئ"
                SupportedLanguage.ARABIC -> "إرسال رسالة"
                SupportedLanguage.TURKISH -> "Mesaj Gönder"
                SupportedLanguage.SPANISH -> "Enviar Mensaje"
                else -> "Send Message"
            }
            "sync_now" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "همگام‌سازی اکنون"
                SupportedLanguage.PASHTO -> "همدا اوس همګام کړئ"
                SupportedLanguage.ARABIC -> "المزامنة الآن"
                SupportedLanguage.TURKISH -> "Şimdi Senkronize Et"
                SupportedLanguage.SPANISH -> "Sincronizar Ahora"
                else -> "Sync Now"
            }
            "online" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "آنلاین"
                SupportedLanguage.PASHTO -> "آنلاین"
                SupportedLanguage.ARABIC -> "متصل"
                SupportedLanguage.TURKISH -> "Çevrimiçi"
                SupportedLanguage.SPANISH -> "En línea"
                else -> "Online"
            }
            "offline" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "آفلاین"
                SupportedLanguage.PASHTO -> "آفلاین"
                SupportedLanguage.ARABIC -> "غیر متصل"
                SupportedLanguage.TURKISH -> "Çevrimdışı"
                SupportedLanguage.SPANISH -> "Fuera de línea"
                else -> "Offline"
            }
            "syncing" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "در حال همگام‌سازی"
                SupportedLanguage.PASHTO -> "د همګام کولو په حال کې"
                SupportedLanguage.ARABIC -> "جاري المزامنة"
                SupportedLanguage.TURKISH -> "Senkronize Ediliyor"
                SupportedLanguage.SPANISH -> "Sincronizando"
                else -> "Syncing"
            }
            "pending" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "در انتظار"
                SupportedLanguage.PASHTO -> "په تمه کې"
                SupportedLanguage.ARABIC -> "قيد الانتظار"
                SupportedLanguage.TURKISH -> "Beklemede"
                SupportedLanguage.SPANISH -> "Pendiente"
                else -> "Pending"
            }
            "failed" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "ناموفق"
                SupportedLanguage.PASHTO -> "ناامیده"
                SupportedLanguage.ARABIC -> "فشلت"
                SupportedLanguage.TURKISH -> "Başarısız"
                SupportedLanguage.SPANISH -> "Fallido"
                else -> "Failed"
            }
            "synced" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "همگام‌شده"
                SupportedLanguage.PASHTO -> "همګام شوی"
                SupportedLanguage.ARABIC -> "تمت المزامنة"
                SupportedLanguage.TURKISH -> "Senkronize Edildi"
                SupportedLanguage.SPANISH -> "Sincronizado"
                else -> "Synced"
            }
            "connected" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "متصل"
                SupportedLanguage.PASHTO -> "متصل"
                SupportedLanguage.ARABIC -> "متصل"
                SupportedLanguage.TURKISH -> "Bağlandı"
                SupportedLanguage.SPANISH -> "Conectado"
                else -> "Connected"
            }
            "disconnected" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "قطع"
                SupportedLanguage.PASHTO -> "پرې شوی"
                SupportedLanguage.ARABIC -> "غير متصل"
                SupportedLanguage.TURKISH -> "Bağlantı Kesildi"
                SupportedLanguage.SPANISH -> "Desconectado"
                else -> "Disconnected"
            }
            "resolved" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "حل‌شده"
                SupportedLanguage.PASHTO -> "حل شوی"
                SupportedLanguage.ARABIC -> "تم الحل"
                SupportedLanguage.TURKISH -> "Çözüldü"
                SupportedLanguage.SPANISH -> "Resuelto"
                else -> "Resolved"
            }
            "closed" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "بسته‌شده"
                SupportedLanguage.PASHTO -> "تړل شوی"
                SupportedLanguage.ARABIC -> "مغلق"
                SupportedLanguage.TURKISH -> "Kapatıldı"
                SupportedLanguage.SPANISH -> "Cerrado"
                else -> "Closed"
            }
            "open" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "باز"
                SupportedLanguage.PASHTO -> "خلاص"
                SupportedLanguage.ARABIC -> "مفتوح"
                SupportedLanguage.TURKISH -> "Açık"
                SupportedLanguage.SPANISH -> "Abierto"
                else -> "Open"
            }
            "in_progress" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "در حال پیگیری"
                SupportedLanguage.PASHTO -> "د پرمختګ په حال کې"
                SupportedLanguage.ARABIC -> "قيد التنفيذ"
                SupportedLanguage.TURKISH -> "İşlemde"
                SupportedLanguage.SPANISH -> "En Progreso"
                else -> "In Progress"
            }
            "waiting_for_customer" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "در انتظار پاسخ مشتری"
                SupportedLanguage.PASHTO -> "د پیرودونکي ځواب په تمه"
                SupportedLanguage.ARABIC -> "بانتظار العميل"
                SupportedLanguage.TURKISH -> "Müşteri Yanıtı Bekleniyor"
                SupportedLanguage.SPANISH -> "Esperando Respuesta"
                else -> "Waiting for Customer"
            }
            "internet" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "اینترنت"
                SupportedLanguage.PASHTO -> "انټرنیټ"
                SupportedLanguage.ARABIC -> "الإنترنت"
                SupportedLanguage.TURKISH -> "İnternet"
                SupportedLanguage.SPANISH -> "Internet"
                else -> "Internet"
            }
            "firebase" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "فایربیس"
                SupportedLanguage.PASHTO -> "فایربیس"
                SupportedLanguage.ARABIC -> "فايربيس"
                SupportedLanguage.TURKISH -> "Firebase"
                SupportedLanguage.SPANISH -> "Firebase"
                else -> "Firebase"
            }
            "cloud_sync" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "همگام‌سازی ابری"
                SupportedLanguage.PASHTO -> "ورېځ همګام کول"
                SupportedLanguage.ARABIC -> "المزامنة السحابية"
                SupportedLanguage.TURKISH -> "Bulut Senkronizasyonu"
                SupportedLanguage.SPANISH -> "Sincronización en la Nube"
                else -> "Cloud Sync"
            }
            "pending_transactions" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "معاملات در انتظار"
                SupportedLanguage.PASHTO -> "په تمه مناملې"
                SupportedLanguage.ARABIC -> "المعاملات المعلقة"
                SupportedLanguage.TURKISH -> "Bekleyen İşlemler"
                SupportedLanguage.SPANISH -> "Transacciones Pendientes"
                else -> "Pending Transactions"
            }
            "open_tickets" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "درخواست‌های باز"
                SupportedLanguage.PASHTO -> "خلاصې غوښتنې"
                SupportedLanguage.ARABIC -> "التذاكر المفتوحة"
                SupportedLanguage.TURKISH -> "Açık Talepler"
                SupportedLanguage.SPANISH -> "Tickets Abiertos"
                else -> "Open Tickets"
            }
            "unread_messages" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "پیام‌های خوانده‌نشده"
                SupportedLanguage.PASHTO -> "نه‌لوستل شوي پیغامونه"
                SupportedLanguage.ARABIC -> "الرسائل غير المقروءة"
                SupportedLanguage.TURKISH -> "Okunmamış Mesajlar"
                SupportedLanguage.SPANISH -> "Mensajes No Leídos"
                else -> "Unread Messages"
            }
            "b2b_title" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تجارت عمده B2B"
                SupportedLanguage.PASHTO -> "په لویه کچه تجارت"
                SupportedLanguage.ARABIC -> "التجارة بالجملة B2B"
                SupportedLanguage.TURKISH -> "B2B Toptan Ticaret"
                SupportedLanguage.SPANISH -> "Comercio B2B al por Mayor"
                else -> "B2B Wholesale Commerce"
            }
            "b2b_customers" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "مشتریان عمده"
                SupportedLanguage.PASHTO -> "عمده پیرودونکي"
                SupportedLanguage.ARABIC -> "عملاء الجملة"
                SupportedLanguage.TURKISH -> "Toptan Müşteriler"
                SupportedLanguage.SPANISH -> "Clientes B2B"
                else -> "Wholesale Customers"
            }
            "b2b_businesses" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "شرکت‌ها / کسب‌وکارها"
                SupportedLanguage.PASHTO -> "شرکتونه او سوداګرۍ"
                SupportedLanguage.ARABIC -> "الشركات والأعمال"
                SupportedLanguage.TURKISH -> "Şirketler ve İşletmeler"
                SupportedLanguage.SPANISH -> "Empresas y Negocios"
                else -> "Business Accounts"
            }
            "b2b_prices" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "قیمت‌های عمده"
                SupportedLanguage.PASHTO -> "عمده قیمتونه"
                SupportedLanguage.ARABIC -> "أسعار الجملة"
                SupportedLanguage.TURKISH -> "Toptan Fiyatlar"
                SupportedLanguage.SPANISH -> "Precios al por Mayor"
                else -> "Wholesale Prices"
            }
            "b2b_orders" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "سفارش‌های عمده"
                SupportedLanguage.PASHTO -> "عمده سپارښتنې"
                SupportedLanguage.ARABIC -> "طلبات الجملة"
                SupportedLanguage.TURKISH -> "Toptan Siparişler"
                SupportedLanguage.SPANISH -> "Pedidos B2B"
                else -> "Wholesale Orders"
            }
            "b2b_quotations" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "پیش‌فاکتورها"
                SupportedLanguage.PASHTO -> "مخکې فاکتورونه"
                SupportedLanguage.ARABIC -> "عروض الأسعار"
                SupportedLanguage.TURKISH -> "Teklifler"
                SupportedLanguage.SPANISH -> "Cotizaciones"
                else -> "Proforma Invoices / Quotations"
            }
            "b2b_invoices" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "فاکتورها"
                SupportedLanguage.PASHTO -> "فاکتورونه"
                SupportedLanguage.ARABIC -> "الفواتير"
                SupportedLanguage.TURKISH -> "Faturalar"
                SupportedLanguage.SPANISH -> "Facturas B2B"
                else -> "Invoices"
            }
            "b2b_payments" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "پرداخت‌ها"
                SupportedLanguage.PASHTO -> "تادیات"
                SupportedLanguage.ARABIC -> "المدفوعات"
                SupportedLanguage.TURKISH -> "Ödemeler"
                SupportedLanguage.SPANISH -> "Pagos"
                else -> "Payments"
            }
            "b2b_credit_accounts" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "حساب‌های اعتباری"
                SupportedLanguage.PASHTO -> "اعتباري حسابونه"
                SupportedLanguage.ARABIC -> "حسابات الائتمان"
                SupportedLanguage.TURKISH -> "Kredi Hesapları"
                SupportedLanguage.SPANISH -> "Cuentas de Crédito"
                else -> "Credit Accounts"
            }
            "b2b_accounts_receivable" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "بدهی مشتریان"
                SupportedLanguage.PASHTO -> "د پیرودونکو پورونه"
                SupportedLanguage.ARABIC -> "الحسابات المدينة"
                SupportedLanguage.TURKISH -> "Müşteri Alacakları"
                SupportedLanguage.SPANISH -> "Cuentas por Cobrar"
                else -> "Accounts Receivable"
            }
            "b2b_discounts" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تخفیف‌های عمده"
                SupportedLanguage.PASHTO -> "عمده تخفیفونه"
                SupportedLanguage.ARABIC -> "خصومات الجملة"
                SupportedLanguage.TURKISH -> "Toptan İndirimler"
                SupportedLanguage.SPANISH -> "Descuentos B2B"
                else -> "Wholesale Discounts"
            }
            "b2b_reports" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "گزارش‌های B2B"
                SupportedLanguage.PASHTO -> "B2B راپورونه"
                SupportedLanguage.ARABIC -> "تقارير B2B"
                SupportedLanguage.TURKISH -> "B2B Raporları"
                SupportedLanguage.SPANISH -> "Reportes B2B"
                else -> "B2B Reports"
            }
            "b2b_support" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "پشتیبانی B2B"
                SupportedLanguage.PASHTO -> "B2B ملاتړ"
                SupportedLanguage.ARABIC -> "دعم B2B"
                SupportedLanguage.TURKISH -> "B2B Destek"
                SupportedLanguage.SPANISH -> "Soporte B2B"
                else -> "B2B Support"
            }
            "about_us" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "درباره ما"
                SupportedLanguage.PASHTO -> "زموږ په اړه"
                SupportedLanguage.ARABIC -> "من نحن"
                SupportedLanguage.TURKISH -> "Hakkımızda"
                SupportedLanguage.SPANISH -> "Acerca de Nosotros"
                else -> "ABOUT US"
            }
            "company_name" -> "Rahimy Smart Commerce"
            "phone_whatsapp" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تلفن / واتساپ"
                SupportedLanguage.PASHTO -> "تلیفون / واټساپ"
                SupportedLanguage.ARABIC -> "الهاتف / واتساب"
                SupportedLanguage.TURKISH -> "Telefon / WhatsApp"
                SupportedLanguage.SPANISH -> "Teléfono / WhatsApp"
                else -> "Phone / WhatsApp"
            }
            "telegram" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تلگرام"
                SupportedLanguage.PASHTO -> "ټلګرام"
                SupportedLanguage.ARABIC -> "تيليجرام"
                SupportedLanguage.TURKISH -> "Telegram"
                SupportedLanguage.SPANISH -> "Telegram"
                else -> "Telegram"
            }
            "call_button" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تماس"
                SupportedLanguage.PASHTO -> "تلیفون وکړئ"
                SupportedLanguage.ARABIC -> "اتصال"
                SupportedLanguage.TURKISH -> "Ara"
                SupportedLanguage.SPANISH -> "Llamar"
                else -> "Call"
            }
            "whatsapp_button" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "واتساپ"
                SupportedLanguage.PASHTO -> "واټساپ"
                SupportedLanguage.ARABIC -> "واتساب"
                SupportedLanguage.TURKISH -> "WhatsApp"
                SupportedLanguage.SPANISH -> "WhatsApp"
                else -> "WhatsApp"
            }
            "telegram_button" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "تلگرام"
                SupportedLanguage.PASHTO -> "ټلګرام"
                SupportedLanguage.ARABIC -> "تيليجرام"
                SupportedLanguage.TURKISH -> "Telegram"
                SupportedLanguage.SPANISH -> "Telegram"
                else -> "Telegram"
            }
            "about_us_description" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "سیستم جامع تجارت هوشمند رحیمی برای مدیریت فروش، انبارداری و تجارت عمده B2B"
                SupportedLanguage.PASHTO -> "د رحیمي هوښیار سوداګرۍ جامع مدیریت سیسټم"
                SupportedLanguage.ARABIC -> "نظام رحيمي للتجارة الذكية لإدارة المبيعات والمخزون والتجارة بالجملة"
                SupportedLanguage.TURKISH -> "Rahimy Akıllı Ticaret Satış, Stok ve B2B Yönetim Sistemi"
                SupportedLanguage.SPANISH -> "Sistema Inteligente de Gestión de Comercio, Inventario y B2B Rahimy"
                else -> "Rahimy Smart Commerce - Comprehensive Sales, Inventory & B2B Management System"
            }
            "contact_info" -> when (language) {
                SupportedLanguage.DARI, SupportedLanguage.PERSIAN -> "اطلاعات تماس"
                SupportedLanguage.PASHTO -> "د اړیکې معلومات"
                SupportedLanguage.ARABIC -> "معلومات الاتصال"
                SupportedLanguage.TURKISH -> "İletişim Bilgileri"
                SupportedLanguage.SPANISH -> "Información de Contacto"
                else -> "Contact Information"
            }
            else -> key
        }
    }
}
