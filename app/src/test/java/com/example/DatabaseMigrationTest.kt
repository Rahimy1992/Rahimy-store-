package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    @Test
    fun testMigration1To2PreservesDataAndAddsBrand() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_db"
        context.deleteDatabase(dbName)

        // Step 1: Create database under schema v1 manually
        val helperConfig = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `products` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `sku` TEXT NOT NULL,
                            `barcode` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `costPrice` REAL NOT NULL,
                            `sellingPrice` REAL NOT NULL,
                            `stockQuantity` INTEGER NOT NULL,
                            `minStockThreshold` INTEGER NOT NULL,
                            `isActive` INTEGER NOT NULL,
                            `imageUris` TEXT NOT NULL,
                            `primaryImageIndex` INTEGER NOT NULL,
                            `description` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Insert pre-existing product record into v1 (Rahimy Smart Commerce production data)
                    db.execSQL("""
                        INSERT INTO products (id, name, sku, barcode, category, costPrice, sellingPrice, stockQuantity, minStockThreshold, isActive, imageUris, primaryImageIndex, description, createdAt, updatedAt)
                        VALUES (101, 'Existing Smartphone', 'SKU-EXIST-101', '12345678', 'Electronics', 150.0, 220.0, 25, 5, 1, '[]', 0, 'Pre-migration item', 1700000000000, 1700000000000)
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        var sqliteDb = helper.writableDatabase

        // Verify pre-migration state: 1 product exists
        val cursorBefore = sqliteDb.query("SELECT * FROM products WHERE id = 101")
        assertTrue(cursorBefore.moveToFirst())
        assertEquals("Existing Smartphone", cursorBefore.getString(cursorBefore.getColumnIndexOrThrow("name")))
        cursorBefore.close()

        // Step 2: Execute SAFE Room Migration 1 -> 2
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
        AppDatabase.MIGRATION_1_2.migrate(sqliteDb)

        // Close and reopen connection to ensure SQLite schema metadata is fully refreshed
        sqliteDb.close()
        sqliteDb = helper.writableDatabase

        // Step 3: Verify data integrity after migration
        // - Existing product record 101 was PRESERVED without data loss
        // - New 'brand' column exists with default value ""
        val cursorAfter = sqliteDb.query("SELECT * FROM products WHERE id = 101")
        assertTrue("Existing product record must be preserved", cursorAfter.moveToFirst())
        val columnNames = cursorAfter.columnNames.toList()
        assertTrue("Brand column must exist after migration. Available columns: $columnNames", columnNames.contains("brand"))
        assertEquals(101L, cursorAfter.getLong(cursorAfter.getColumnIndexOrThrow("id")))
        assertEquals("Existing Smartphone", cursorAfter.getString(cursorAfter.getColumnIndexOrThrow("name")))
        assertEquals("SKU-EXIST-101", cursorAfter.getString(cursorAfter.getColumnIndexOrThrow("sku")))
        assertEquals(150.0, cursorAfter.getDouble(cursorAfter.getColumnIndexOrThrow("costPrice")), 0.001)
        assertEquals(220.0, cursorAfter.getDouble(cursorAfter.getColumnIndexOrThrow("sellingPrice")), 0.001)
        assertEquals(25, cursorAfter.getInt(cursorAfter.getColumnIndexOrThrow("stockQuantity")))
        assertEquals("Default brand should be empty string", "", cursorAfter.getString(cursorAfter.getColumnIndexOrThrow("brand")))
        cursorAfter.close()

        // Step 4: Insert a new product with custom brand post-migration
        sqliteDb.execSQL("""
            INSERT INTO products (id, name, sku, barcode, category, brand, costPrice, sellingPrice, stockQuantity, minStockThreshold, isActive, imageUris, primaryImageIndex, description, createdAt, updatedAt)
            VALUES (102, 'New Laptop', 'SKU-LAP-01', '87654321', 'Computers', 'RahimyTech', 500.0, 750.0, 10, 2, 1, '[]', 0, 'New product', 1700000000000, 1700000000000)
        """.trimIndent())

        val cursorNew = sqliteDb.query("SELECT * FROM products WHERE id = 102")
        assertTrue(cursorNew.moveToFirst())
        assertEquals("RahimyTech", cursorNew.getString(cursorNew.getColumnIndexOrThrow("brand")))
        cursorNew.close()

        // Count total products: both existing and new products are present
        val cursorCount = sqliteDb.query("SELECT COUNT(*) FROM products")
        assertTrue(cursorCount.moveToFirst())
        assertEquals(2, cursorCount.getInt(0))
        cursorCount.close()

        sqliteDb.close()
        context.deleteDatabase(dbName)
    }
}
