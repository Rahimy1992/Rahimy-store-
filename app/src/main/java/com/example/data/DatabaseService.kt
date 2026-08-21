package com.example.data

import android.util.Log
import com.example.data.cloud.CloudProduct
import com.example.data.cloud.FirestoreCollections
import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service handling Firestore CRUD operations for Products,
 * fully integrated with the local SQLite Room database (ProductDao).
 */
class DatabaseService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val productDao: ProductDao? = null
) {
    private val productsCollection = firestore.collection(FirestoreCollections.PRODUCTS)

    /**
     * Creates or updates a product in Firestore and synchronizes it to the local Room database.
     */
    suspend fun createProduct(product: Product): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = if (product.id > 0) {
                productsCollection.document(product.id.toString())
            } else {
                productsCollection.document()
            }

            val cloudProduct = product.toCloudProduct(docId = docRef.id)
            docRef.set(cloudProduct, SetOptions.merge()).await()

            // Save or update locally in Room
            val localId = productDao?.insertProduct(product.copy(updatedAt = System.currentTimeMillis())) ?: product.id

            Log.d(TAG, "Product created/updated in Firestore and Room. Local ID: $localId, Cloud ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating product in Firestore: ${e.message}", e)
            // Fallback write to local Room for offline support
            try {
                productDao?.insertProduct(product)
            } catch (localEx: Exception) {
                Log.e(TAG, "Failed local Room fallback insert: ${localEx.message}", localEx)
            }
            Result.failure(e)
        }
    }

    /**
     * Retrieves a single product by its cloud ID from Firestore and updates Room.
     */
    suspend fun getProduct(cloudId: String): Result<Product?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = productsCollection.document(cloudId).get().await()
            val cloudProduct = snapshot.toObject(CloudProduct::class.java)
            val product = cloudProduct?.toLocalProduct()

            if (product != null) {
                productDao?.insertProduct(product)
            }

            Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product $cloudId from Firestore: ${e.message}", e)
            // Fallback to local Room lookup if numeric ID
            val numericId = cloudId.toLongOrNull()
            if (numericId != null && productDao != null) {
                val localProduct = productDao.getProductById(numericId)
                Result.success(localProduct)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches all products from Firestore and updates Room.
     */
    suspend fun getAllProductsFromCloud(): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = productsCollection.get().await()
            val cloudProducts = snapshot.toObjects(CloudProduct::class.java)
            val localProducts = cloudProducts.map { it.toLocalProduct() }

            if (localProducts.isNotEmpty()) {
                productDao?.insertProducts(localProducts)
            }

            Result.success(localProducts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all products from Firestore: ${e.message}", e)
            // Fallback to Room local database
            if (productDao != null) {
                val localProducts = productDao.getAllProductsSync()
                Result.success(localProducts)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Updates an existing product in Firestore and Room.
     */
    suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = product.id.toString()
            val cloudProduct = product.toCloudProduct(docId = docId)

            productsCollection.document(docId).set(cloudProduct, SetOptions.merge()).await()
            productDao?.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))

            Log.d(TAG, "Product ${product.id} updated in Firestore and Room")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product ${product.id} in Firestore: ${e.message}", e)
            // Local fallback
            try {
                productDao?.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            } catch (localEx: Exception) {
                Log.e(TAG, "Failed local Room update fallback: ${localEx.message}", localEx)
            }
            Result.failure(e)
        }
    }

    /**
     * Deletes a product from Firestore and Room.
     */
    suspend fun deleteProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = product.id.toString()
            productsCollection.document(docId).delete().await()
            productDao?.deleteProduct(product)

            Log.d(TAG, "Product ${product.id} deleted from Firestore and Room")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product ${product.id} from Firestore: ${e.message}", e)
            try {
                productDao?.deleteProduct(product)
            } catch (localEx: Exception) {
                Log.e(TAG, "Failed local Room deletion fallback: ${localEx.message}", localEx)
            }
            Result.failure(e)
        }
    }

    /**
     * Synchronizes all local Room products to Firestore.
     */
    suspend fun syncLocalToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val localProducts = productDao?.getAllProductsSync() ?: emptyList()
            var count = 0
            for (product in localProducts) {
                val docId = product.id.toString()
                val cloudProduct = product.toCloudProduct(docId = docId)
                productsCollection.document(docId).set(cloudProduct, SetOptions.merge()).await()
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing local products to cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "DatabaseService"

        fun Product.toCloudProduct(docId: String = id.toString()): CloudProduct {
            return CloudProduct(
                id = docId,
                name = name,
                sku = sku,
                barcode = barcode,
                category = category,
                brand = brand,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                stockQuantity = stockQuantity,
                minStockThreshold = minStockThreshold,
                description = description,
                imageUris = imageUris,
                primaryImageIndex = primaryImageIndex,
                isActive = isActive,
                updatedAt = updatedAt
            )
        }

        fun CloudProduct.toLocalProduct(): Product {
            val numericId = id.toLongOrNull() ?: 0L
            return Product(
                id = numericId,
                name = name,
                sku = sku,
                barcode = barcode,
                category = category,
                brand = brand,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                stockQuantity = stockQuantity,
                minStockThreshold = minStockThreshold,
                description = description,
                imageUris = imageUris,
                primaryImageIndex = primaryImageIndex,
                isActive = isActive,
                updatedAt = updatedAt
            )
        }
    }
}
