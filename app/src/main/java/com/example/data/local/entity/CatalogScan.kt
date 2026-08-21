package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}

@Entity(tableName = "catalog_scans")
data class CatalogScan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val scanTimestamp: Long = System.currentTimeMillis(),
    val pageCount: Int,
    val imageUris: List<String>,
    val detectedProductsCount: Int,
    val status: ScanStatus = ScanStatus.PENDING_REVIEW,
    val notes: String = ""
)
