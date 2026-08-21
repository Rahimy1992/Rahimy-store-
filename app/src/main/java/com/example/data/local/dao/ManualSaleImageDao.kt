package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ManualSaleImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualSaleImageDao {
    @Query("SELECT * FROM manual_sale_images WHERE saleId = :saleId ORDER BY displayOrder ASC")
    fun getImagesForSale(saleId: Long): Flow<List<ManualSaleImage>>

    @Query("SELECT * FROM manual_sale_images WHERE saleId = :saleId ORDER BY displayOrder ASC")
    suspend fun getImagesForSaleSync(saleId: Long): List<ManualSaleImage>

    @Query("SELECT * FROM manual_sale_images WHERE saleId IN (:saleIds) ORDER BY displayOrder ASC")
    suspend fun getImagesForSalesSync(saleIds: List<Long>): List<ManualSaleImage>

    @Query("SELECT * FROM manual_sale_images ORDER BY createdAt DESC")
    fun getAllImages(): Flow<List<ManualSaleImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ManualSaleImage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ManualSaleImage): Long

    @Query("DELETE FROM manual_sale_images WHERE imageId = :imageId")
    suspend fun deleteImage(imageId: Long)

    @Query("DELETE FROM manual_sale_images WHERE saleId = :saleId")
    suspend fun deleteImagesForSale(saleId: Long)

    @Query("SELECT * FROM manual_sale_images WHERE syncStatus = 'PENDING'")
    suspend fun getUnsyncedImages(): List<ManualSaleImage>

    @Query("UPDATE manual_sale_images SET cloudUrl = :cloudUrl, syncStatus = :syncStatus WHERE imageId = :imageId")
    suspend fun updateImageSyncStatus(imageId: Long, cloudUrl: String, syncStatus: String)
}
