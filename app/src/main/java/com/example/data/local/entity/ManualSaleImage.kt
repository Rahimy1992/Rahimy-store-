package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "manual_sale_images",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"])]
)
data class ManualSaleImage(
    @PrimaryKey(autoGenerate = true)
    val imageId: Long = 0,
    val saleId: Long,
    val localUri: String,
    val cloudUrl: String? = null,
    val source: String = "GALLERY", // "CAMERA" or "GALLERY"
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val displayOrder: Int = 0
)
