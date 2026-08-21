package com.example.util

import com.example.data.local.entity.Sale
import java.security.MessageDigest

data class DuplicateSaleCheckResult(
    val isDuplicate: Boolean,
    val existingSale: Sale? = null,
    val duplicateReason: String = ""
)

object SalesDuplicateDetector {

    /**
     * Generates a deterministic SHA-256 fingerprint hash for a document/invoice transaction.
     * Prevents scanning/saving the exact same invoice twice.
     */
    fun computeDocumentHash(
        supplierOrCustomer: String,
        invoiceNumber: String,
        dateFormatted: String,
        grandTotal: Double,
        itemCount: Int
    ): String {
        val raw = "${supplierOrCustomer.trim().lowercase()}|${invoiceNumber.trim().lowercase()}|$dateFormatted|${String.format("%.2f", grandTotal)}|$itemCount"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if a proposed sale is a duplicate of an existing active sale.
     * - Checks exact transactionId/invoiceNumber
     * - Checks exact source document hash
     * - Allows distinct sales on the same day if invoice/hash/items differ
     */
    fun checkDuplicate(
        proposedInvoiceNumber: String,
        proposedDocumentHash: String = "",
        existingSales: List<Sale>
    ): DuplicateSaleCheckResult {
        if (existingSales.isEmpty()) return DuplicateSaleCheckResult(false)

        val cleanInvoice = proposedInvoiceNumber.trim()

        for (sale in existingSales) {
            // Only compare with active (non-void) sales
            if (sale.status.name == "VOID" || sale.status.name == "CANCELLED") continue

            // 1. Check exact invoice number match
            if (cleanInvoice.isNotEmpty() && sale.invoiceNumber.equals(cleanInvoice, ignoreCase = true)) {
                return DuplicateSaleCheckResult(
                    isDuplicate = true,
                    existingSale = sale,
                    duplicateReason = "Invoice number '$cleanInvoice' already exists in sales records."
                )
            }

            // 2. Check source document hash stored in notes if available
            if (proposedDocumentHash.isNotEmpty() && sale.notes.contains(proposedDocumentHash)) {
                return DuplicateSaleCheckResult(
                    isDuplicate = true,
                    existingSale = sale,
                    duplicateReason = "An identical invoice document with matching content has already been saved."
                )
            }
        }

        return DuplicateSaleCheckResult(false)
    }
}
