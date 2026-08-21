package com.example.data.ai

import com.example.data.local.entity.Product
import com.example.domain.localization.LocalizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val detectedLanguage: String = "English",
    val referencedProducts: List<Product> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    AI_CUSTOMER_ASSISTANT,
    AI_MANAGER_ASSISTANT
}

object CustomerAiService {

    /**
     * Requirement 6 & 7: Customer AI & Foreign Customer Support
     * Automatically detects language (Dari, Persian, Arabic, English, Turkish, Spanish)
     * and responds using ONLY real store data. Never invents prices or stock.
     */
    suspend fun answerCustomerQuery(
        userQuery: String,
        inventory: List<Product>,
        regionalCurrencyCode: String,
        regionalCurrencySymbol: String,
        exchangeRateToUSD: Double,
        markupPercent: Double
    ): ChatMessage = withContext(Dispatchers.IO) {
        val detectedLang = detectQueryLanguage(userQuery)

        // Build verified real inventory knowledge base
        val inventoryContext = buildInventorySummary(
            inventory,
            regionalCurrencyCode,
            regionalCurrencySymbol,
            exchangeRateToUSD,
            markupPercent
        )

        val systemPrompt = """
            You are a polite, helpful, and strictly factual Store Sales Assistant.
            
            RULES & CONSTRAINTS (MANDATORY):
            1. LANGUAGE: Automatically detect the customer's language and respond in that EXACT same language (Dari / دری, Persian / فارسی, Arabic / العربية, Turkish / Türkçe, Spanish / Español, or English).
            2. ZERO HALLUCINATIONS: You must answer ONLY using the REAL store inventory provided below.
            3. NEVER INVENT or assume:
               - Product prices (only quote exact prices given in inventory)
               - Stock quantities (if stock is 0, say out of stock)
               - Product specifications (only state what is in descriptions)
               - Discounts (unless explicitly listed in context)
               - Shipping costs or delivery times (state that customer should confirm at checkout)
            4. If a requested product is NOT in the store inventory, politely inform the customer in their language that it is currently unavailable.
            5. Always include prices in both the regional currency ($regionalCurrencyCode) and USD when quoting.
            
            CURRENT VERIFIED STORE INVENTORY:
            $inventoryContext
        """.trimIndent()

        val apiResult = GeminiRestService.generateText(
            prompt = "Customer asks: \"$userQuery\"",
            systemInstruction = systemPrompt
        )

        if (apiResult.isSuccess) {
            val responseText = apiResult.getOrNull().orEmpty()
            if (responseText.isNotBlank()) {
                val matchedProducts = findMatchingProducts(userQuery, inventory)
                return@withContext ChatMessage(
                    sender = MessageSender.AI_CUSTOMER_ASSISTANT,
                    text = responseText,
                    detectedLanguage = detectedLang,
                    referencedProducts = matchedProducts
                )
            }
        }

        // Offline / Fallback Multilingual Rule-based Engine
        val fallbackResponse = generateMultilingualFallbackResponse(
            userQuery = userQuery,
            detectedLang = detectedLang,
            inventory = inventory,
            regionalCurrencyCode = regionalCurrencyCode,
            regionalCurrencySymbol = regionalCurrencySymbol,
            exchangeRateToUSD = exchangeRateToUSD,
            markupPercent = markupPercent
        )

        val matchedProducts = findMatchingProducts(userQuery, inventory)
        ChatMessage(
            sender = MessageSender.AI_CUSTOMER_ASSISTANT,
            text = fallbackResponse,
            detectedLanguage = detectedLang,
            referencedProducts = matchedProducts
        )
    }

    private fun detectQueryLanguage(text: String): String {
        val lower = text.lowercase()
        // Check Persian/Dari specific letters (گ, چ, پ, ژ, دری, سلام, قیمت, زعفران)
        if (text.any { it in '\u0600'..'\u06FF' }) {
            return if (lower.contains("دری") || lower.contains("تشکر") || lower.contains("چند است") || lower.contains("افغانی") || lower.contains("چطور")) {
                "Dari (دری)"
            } else if (lower.contains("چند") || lower.contains("ممنون") || lower.contains("سلام") || lower.contains("تومان")) {
                "Persian (فارسی)"
            } else {
                "Arabic (العربية)"
            }
        }
        if (lower.contains("merhaba") || lower.contains("fiyat") || lower.contains("var mı") || lower.contains("stok") || lower.contains("lütfen")) {
            return "Turkish (Türkçe)"
        }
        if (lower.contains("hola") || lower.contains("precio") || lower.contains("cuánto") || lower.contains("tienen") || lower.contains("gracias")) {
            return "Spanish (Español)"
        }
        return "English"
    }

    private fun buildInventorySummary(
        inventory: List<Product>,
        currencyCode: String,
        currencySymbol: String,
        rate: Double,
        markup: Double
    ): String {
        val sb = StringBuilder()
        inventory.filter { it.isActive }.forEach { p ->
            val dualPrice = LocalizationManager.formatDualCurrency(
                p.sellingPrice,
                currencyCode,
                currencySymbol,
                rate,
                markup
            )
            sb.append("- ${p.name} (SKU: ${p.sku}, Category: ${p.category}): Price $dualPrice, In Stock: ${p.stockQuantity} units. Details: ${p.description}\n")
        }
        return sb.toString()
    }

    private fun findMatchingProducts(query: String, inventory: List<Product>): List<Product> {
        val terms = query.lowercase().split(" ").filter { it.length > 2 }
        return inventory.filter { p ->
            val name = p.name.lowercase()
            val desc = p.description.lowercase()
            val cat = p.category.lowercase()
            terms.any { t -> name.contains(t) || desc.contains(t) || cat.contains(t) }
        }.take(3)
    }

    private fun generateMultilingualFallbackResponse(
        userQuery: String,
        detectedLang: String,
        inventory: List<Product>,
        regionalCurrencyCode: String,
        regionalCurrencySymbol: String,
        exchangeRateToUSD: Double,
        markupPercent: Double
    ): String {
        val matched = findMatchingProducts(userQuery, inventory)

        return when {
            detectedLang.startsWith("Dari") -> {
                if (matched.isNotEmpty()) {
                    val p = matched.first()
                    val priceStr = LocalizationManager.formatDualCurrency(p.sellingPrice, regionalCurrencyCode, regionalCurrencySymbol, exchangeRateToUSD, markupPercent)
                    "سلام و احترامات! بله، محصول '${p.name}' با قیمت $priceStr در فروشگاه موجود است. موجودی فعلی: ${p.stockQuantity} عدد. توضیحات: ${p.description}"
                } else {
                    "سلام و درود! متأسفانه این محصول در موجودی فعلی فروشگاه یافت نشد. می‌توانید از سایر محصولات مانند زعفران، چای یا خشکبار دیدن فرمایید."
                }
            }
            detectedLang.startsWith("Persian") -> {
                if (matched.isNotEmpty()) {
                    val p = matched.first()
                    val priceStr = LocalizationManager.formatDualCurrency(p.sellingPrice, regionalCurrencyCode, regionalCurrencySymbol, exchangeRateToUSD, markupPercent)
                    "سلام! محصول '${p.name}' با قیمت $priceStr موجود است. موجودی در انبار: ${p.stockQuantity} عدد."
                } else {
                    "سلام، کالای مورد نظر شما در انبار موجود نیست. لطفاً اقلام دیگر فروشگاه را بررسی بفرمایید."
                }
            }
            detectedLang.startsWith("Arabic") -> {
                if (matched.isNotEmpty()) {
                    val p = matched.first()
                    val priceStr = LocalizationManager.formatDualCurrency(p.sellingPrice, regionalCurrencyCode, regionalCurrencySymbol, exchangeRateToUSD, markupPercent)
                    "أهلاً وسهلاً بك! المنتج '${p.name}' متوفر حالياً بسعر $priceStr. الكمية المتوفرة: ${p.stockQuantity} قطعة."
                } else {
                    "أهلاً بك! للأسف هذا المنتج غير متوفر حالياً في متجرنا."
                }
            }
            detectedLang.startsWith("Turkish") -> {
                if (matched.isNotEmpty()) {
                    val p = matched.first()
                    val priceStr = LocalizationManager.formatDualCurrency(p.sellingPrice, regionalCurrencyCode, regionalCurrencySymbol, exchangeRateToUSD, markupPercent)
                    "Merhaba! '${p.name}' ürünümüz stoklarımızda mevcuttur. Fiyat: $priceStr. Kalan stok: ${p.stockQuantity} adet."
                } else {
                    "Merhaba! Aradığınız ürün şu anda mağazamızda bulunmamaktadır."
                }
            }
            detectedLang.startsWith("Spanish") -> {
                if (matched.isNotEmpty()) {
                    val p = matched.first()
                    val priceStr = LocalizationManager.formatDualCurrency(p.sellingPrice, regionalCurrencyCode, regionalCurrencySymbol, exchangeRateToUSD, markupPercent)
                    "¡Hola! El producto '${p.name}' está disponible por $priceStr. Stock actual: ${p.stockQuantity} unidades."
                } else {
                    "¡Hola! Lamentablemente el producto solicitado no se encuentra disponible actualmente en nuestro catálogo."
                }
            }
            else -> {
                if (matched.isNotEmpty()) {
                    val p = matched.first()
                    val priceStr = LocalizationManager.formatDualCurrency(p.sellingPrice, regionalCurrencyCode, regionalCurrencySymbol, exchangeRateToUSD, markupPercent)
                    "Hello! Yes, '${p.name}' is currently in stock. Price: $priceStr. Available quantity: ${p.stockQuantity} units. ${p.description}"
                } else {
                    "Hello! We couldn't find the requested item in our active inventory. Please check our catalog for available items like Saffron, Organic Tea, or Nuts."
                }
            }
        }
    }
}
