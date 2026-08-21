package com.example.util

import com.example.data.local.entity.Product
import kotlin.math.max
import kotlin.math.min

data class ProductMatchCandidate(
    val existingProduct: Product,
    val matchScore: Double, // 0.0 to 1.0
    val matchReason: String, // "EXACT_BARCODE", "EXACT_SKU", "EXACT_NAME", "FUZZY_NAME_BRAND_MATCH"
    val confidenceLevel: MatchConfidenceLevel
)

enum class MatchConfidenceLevel {
    HIGH,   // >= 0.85 -> Auto-link candidate
    MEDIUM, // 0.60 .. 0.84 -> Human confirmation dialog required
    LOW     // < 0.60 -> Treat as new product
}

object ProductDuplicateDetector {

    /**
     * Finds top product match candidates from the local Room database
     */
    fun findMatches(
        detectedName: String,
        detectedBarcode: String = "",
        detectedSku: String = "",
        detectedBrand: String = "",
        existingProducts: List<Product>
    ): List<ProductMatchCandidate> {
        if (existingProducts.isEmpty()) return emptyList()

        val candidates = mutableListOf<ProductMatchCandidate>()

        val cleanBarcode = detectedBarcode.trim()
        val cleanSku = detectedSku.trim()
        val cleanName = normalizeString(detectedName)
        val cleanBrand = normalizeString(detectedBrand)

        for (existing in existingProducts) {
            val exBarcode = existing.barcode.trim()
            val exSku = existing.sku.trim()
            val exName = normalizeString(existing.name)
            val exBrand = normalizeString(existing.brand)

            // 1. Exact Barcode Match
            if (cleanBarcode.isNotEmpty() && exBarcode.isNotEmpty() && cleanBarcode.equals(exBarcode, ignoreCase = true)) {
                candidates.add(
                    ProductMatchCandidate(
                        existingProduct = existing,
                        matchScore = 1.0,
                        matchReason = "EXACT_BARCODE",
                        confidenceLevel = MatchConfidenceLevel.HIGH
                    )
                )
                continue
            }

            // 2. Exact SKU Match
            if (cleanSku.isNotEmpty() && exSku.isNotEmpty() && cleanSku.equals(exSku, ignoreCase = true)) {
                candidates.add(
                    ProductMatchCandidate(
                        existingProduct = existing,
                        matchScore = 0.98,
                        matchReason = "EXACT_SKU",
                        confidenceLevel = MatchConfidenceLevel.HIGH
                    )
                )
                continue
            }

            // 3. Exact Normalized Name Match
            if (cleanName.isNotEmpty() && cleanName == exName) {
                candidates.add(
                    ProductMatchCandidate(
                        existingProduct = existing,
                        matchScore = 0.95,
                        matchReason = "EXACT_NAME",
                        confidenceLevel = MatchConfidenceLevel.HIGH
                    )
                )
                continue
            }

            // 4. Fuzzy Similarity Match (Levenshtein + Jaccard token overlap)
            if (cleanName.isNotEmpty() && exName.isNotEmpty()) {
                val similarity = calculateHybridSimilarity(cleanName, exName, cleanBrand, exBrand)
                if (similarity >= 0.60) {
                    val level = if (similarity >= 0.85) MatchConfidenceLevel.HIGH else MatchConfidenceLevel.MEDIUM
                    candidates.add(
                        ProductMatchCandidate(
                            existingProduct = existing,
                            matchScore = similarity,
                            matchReason = "FUZZY_NAME_MATCH (${(similarity * 100).toInt()}%)",
                            confidenceLevel = level
                        )
                    )
                }
            }
        }

        return candidates.sortedByDescending { it.matchScore }
    }

    private fun normalizeString(input: String): String {
        return input.lowercase()
            .replace(Regex("(\\d+)([a-zA-Z\\u0600-\\u06FF]+)"), "$1 $2")
            .replace(Regex("([a-zA-Z\\u0600-\\u06FF]+)(\\d+)"), "$1 $2")
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF]"), " ") // Keep alphanumeric + Persian/Arabic chars
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun calculateHybridSimilarity(
        s1: String,
        s2: String,
        brand1: String = "",
        brand2: String = ""
    ): Double {
        // Token Jaccard similarity
        val tokens1 = s1.split(" ").filter { it.length > 1 }.toSet()
        val tokens2 = s2.split(" ").filter { it.length > 1 }.toSet()

        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2).size.toDouble()
        val union = tokens1.union(tokens2).size.toDouble()
        val jaccard = if (union > 0) intersection / union else 0.0

        // Levenshtein ratio
        val levDist = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length).toDouble()
        val levRatio = if (maxLen > 0) 1.0 - (levDist / maxLen) else 0.0

        var brandBonus = 0.0
        val b1 = brand1.lowercase().trim()
        val b2 = brand2.lowercase().trim()
        if (b1.isNotEmpty() && b2.isNotEmpty() && b1 == b2) {
            brandBonus = 0.1
        } else if (b1.isNotEmpty() && s2.contains(b1)) {
            brandBonus = 0.1
        } else if (b2.isNotEmpty() && s1.contains(b2)) {
            brandBonus = 0.1
        }

        val hybrid = (jaccard * 0.5) + (levRatio * 0.5) + brandBonus
        return hybrid.coerceIn(0.0, 1.0)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
