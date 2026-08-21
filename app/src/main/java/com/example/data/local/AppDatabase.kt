package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CatalogScanDao
import com.example.data.local.dao.CurrencyConfigDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.SaleItemDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AuditLog
import com.example.data.local.entity.CatalogScan
import com.example.data.local.entity.CurrencyConfig
import com.example.data.local.entity.Product
import com.example.data.local.dao.CustomerDebtDao
import com.example.data.local.entity.CustomerDebt
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.data.local.entity.SaleStatus
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.local.dao.B2bDao
import com.example.data.local.dao.SupportDao
import com.example.data.local.entity.B2bDeliveryEntity
import com.example.data.local.entity.B2bInvoiceEntity
import com.example.data.local.entity.B2bOrderEntity
import com.example.data.local.entity.B2bOrderItemEntity
import com.example.data.local.entity.B2bPaymentEntity
import com.example.data.local.entity.B2bQuotationEntity
import com.example.data.local.entity.B2bQuotationItemEntity
import com.example.data.local.entity.B2bReturnEntity
import com.example.data.local.entity.BusinessCustomerEntity
import com.example.data.local.entity.SupportMessageEntity
import com.example.data.local.entity.SupportTicketEntity
import com.example.data.local.entity.WholesalePriceListEntity
import com.example.data.local.dao.ManualSaleImageDao
import com.example.data.local.entity.ManualSaleImage
import com.example.data.local.entity.WholesalePriceTierEntity

@Database(
    entities = [
        Product::class,
        Sale::class,
        SaleItem::class,
        User::class,
        AuditLog::class,
        CurrencyConfig::class,
        CatalogScan::class,
        CustomerDebt::class,
        SupportTicketEntity::class,
        SupportMessageEntity::class,
        BusinessCustomerEntity::class,
        WholesalePriceListEntity::class,
        WholesalePriceTierEntity::class,
        B2bQuotationEntity::class,
        B2bQuotationItemEntity::class,
        B2bOrderEntity::class,
        B2bOrderItemEntity::class,
        B2bInvoiceEntity::class,
        B2bPaymentEntity::class,
        B2bReturnEntity::class,
        B2bDeliveryEntity::class,
        ManualSaleImage::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun currencyConfigDao(): CurrencyConfigDao
    abstract fun catalogScanDao(): CatalogScanDao
    abstract fun customerDebtDao(): CustomerDebtDao
    abstract fun supportDao(): SupportDao
    abstract fun b2bDao(): B2bDao
    abstract fun manualSaleImageDao(): ManualSaleImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `customer_debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `totalDebtUsd` REAL NOT NULL,
                        `lastTransactionType` TEXT NOT NULL,
                        `lastAmountUsd` REAL NOT NULL,
                        `notes` TEXT NOT NULL,
                        `saleInvoiceNumber` TEXT NOT NULL,
                        `isSettled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `support_tickets` (
                        `ticketId` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `userName` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `status` TEXT NOT NULL DEFAULT 'OPEN',
                        `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lastMessage` TEXT NOT NULL,
                        `unreadCount` INTEGER NOT NULL,
                        `assignedTo` TEXT,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `support_messages` (
                        `messageId` TEXT NOT NULL PRIMARY KEY,
                        `ticketId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `senderRole` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `attachmentUrl` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `deliveredAt` INTEGER,
                        `readAt` INTEGER,
                        `isQueuedOffline` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `business_customers` (
                        `businessId` TEXT NOT NULL PRIMARY KEY,
                        `businessName` TEXT NOT NULL,
                        `businessType` TEXT NOT NULL,
                        `ownerName` TEXT NOT NULL,
                        `contactPerson` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `country` TEXT NOT NULL,
                        `taxId` TEXT,
                        `registrationNumber` TEXT,
                        `customerCode` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `paymentTerms` TEXT NOT NULL,
                        `creditLimit` REAL NOT NULL,
                        `currentBalance` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wholesale_price_lists` (
                        `priceListId` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `businessType` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wholesale_price_tiers` (
                        `tierId` TEXT NOT NULL PRIMARY KEY,
                        `priceListId` TEXT,
                        `productId` INTEGER NOT NULL,
                        `customerBusinessId` TEXT,
                        `minQuantity` INTEGER NOT NULL,
                        `maxQuantity` INTEGER,
                        `tierName` TEXT NOT NULL,
                        `priceUsd` REAL NOT NULL,
                        `discountPercent` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_quotations` (
                        `quotationId` TEXT NOT NULL PRIMARY KEY,
                        `businessId` TEXT NOT NULL,
                        `businessName` TEXT NOT NULL,
                        `subtotalUsd` REAL NOT NULL,
                        `discountUsd` REAL NOT NULL,
                        `taxUsd` REAL NOT NULL,
                        `shippingUsd` REAL NOT NULL,
                        `totalUsd` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `validUntil` INTEGER NOT NULL,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_quotation_items` (
                        `itemId` TEXT NOT NULL PRIMARY KEY,
                        `quotationId` TEXT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `productName` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitPriceUsd` REAL NOT NULL,
                        `discountUsd` REAL NOT NULL,
                        `subtotalUsd` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_orders` (
                        `orderId` TEXT NOT NULL PRIMARY KEY,
                        `quotationId` TEXT,
                        `businessId` TEXT NOT NULL,
                        `businessName` TEXT NOT NULL,
                        `orderStatus` TEXT NOT NULL,
                        `paymentTerms` TEXT NOT NULL,
                        `subtotalUsd` REAL NOT NULL,
                        `discountUsd` REAL NOT NULL,
                        `taxUsd` REAL NOT NULL,
                        `shippingUsd` REAL NOT NULL,
                        `totalUsd` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `deliveryAddress` TEXT NOT NULL,
                        `expectedDeliveryDate` INTEGER,
                        `customerNotes` TEXT,
                        `internalNotes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_order_items` (
                        `itemId` TEXT NOT NULL PRIMARY KEY,
                        `orderId` TEXT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `productName` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitPriceUsd` REAL NOT NULL,
                        `discountUsd` REAL NOT NULL,
                        `subtotalUsd` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_invoices` (
                        `invoiceId` TEXT NOT NULL PRIMARY KEY,
                        `orderId` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `businessName` TEXT NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `subtotalUsd` REAL NOT NULL,
                        `discountUsd` REAL NOT NULL,
                        `taxUsd` REAL NOT NULL,
                        `shippingUsd` REAL NOT NULL,
                        `totalUsd` REAL NOT NULL,
                        `paidAmountUsd` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `paymentStatus` TEXT NOT NULL,
                        `paymentTerms` TEXT NOT NULL,
                        `issueDate` INTEGER NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `notes` TEXT,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_payments` (
                        `paymentId` TEXT NOT NULL PRIMARY KEY,
                        `businessId` TEXT NOT NULL,
                        `invoiceId` TEXT NOT NULL,
                        `amountUsd` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `referenceNumber` TEXT NOT NULL,
                        `receivedBy` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `notes` TEXT,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_returns` (
                        `returnId` TEXT NOT NULL PRIMARY KEY,
                        `orderId` TEXT NOT NULL,
                        `invoiceId` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `refundAmountUsd` REAL NOT NULL,
                        `restockInventory` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `notes` TEXT,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `b2b_deliveries` (
                        `deliveryId` TEXT NOT NULL PRIMARY KEY,
                        `orderId` TEXT NOT NULL,
                        `businessId` TEXT NOT NULL,
                        `deliveryAddress` TEXT NOT NULL,
                        `contactPerson` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `deliveryStatus` TEXT NOT NULL,
                        `shippingCostUsd` REAL NOT NULL,
                        `trackingNumber` TEXT,
                        `driverName` TEXT,
                        `expectedDeliveryDate` INTEGER,
                        `deliveredAt` INTEGER,
                        `isSynced` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN saleType TEXT NOT NULL DEFAULT 'POS'")
                db.execSQL("ALTER TABLE sales ADD COLUMN subtotal REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN tax REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE sales ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
                db.execSQL("ALTER TABLE sales ADD COLUMN customerName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sales ADD COLUMN customerPhone TEXT DEFAULT NULL")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `manual_sale_images` (
                        `imageId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleId` INTEGER NOT NULL,
                        `localUri` TEXT NOT NULL,
                        `cloudUrl` TEXT,
                        `source` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_sale_images_saleId` ON `manual_sale_images` (`saleId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rahimy_smart_commerce_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // 1. Initial Users
            val superAdmin = User(
                username = "admin",
                displayName = "Super Administrator",
                role = UserRole.SUPER_ADMIN,
                pin = "9999"
            )
            val manager = User(
                username = "manager",
                displayName = "Store Manager",
                role = UserRole.MANAGER,
                pin = "1111"
            )
            val employee = User(
                username = "cashier1",
                displayName = "Sarah Cashier",
                role = UserRole.EMPLOYEE,
                pin = "2222"
            )
            val employee2 = User(
                username = "cashier2",
                displayName = "Ahmad Sales",
                role = UserRole.EMPLOYEE,
                pin = "3333"
            )
            val viewer = User(
                username = "viewer",
                displayName = "Auditor / Read-Only Viewer",
                role = UserRole.VIEWER,
                pin = "0000"
            )
            val customer = User(
                username = "customer",
                displayName = "Guest Customer",
                role = UserRole.CUSTOMER,
                pin = ""
            )

            val adminId = database.userDao().insertUser(superAdmin)
            val managerId = database.userDao().insertUser(manager)
            val cashier1Id = database.userDao().insertUser(employee)
            val cashier2Id = database.userDao().insertUser(employee2)
            database.userDao().insertUser(viewer)
            database.userDao().insertUser(customer)

            // 2. Initial Currencies
            val currencies = listOf(
                CurrencyConfig(
                    currencyCode = "USD",
                    currencySymbol = "$",
                    displayName = "US Dollar (Default)",
                    exchangeRateToUSD = 1.0,
                    markupPercent = 0.0,
                    isPrimaryRegional = false
                ),
                CurrencyConfig(
                    currencyCode = "AFN",
                    currencySymbol = "؋",
                    displayName = "Afghan Afghani (AFN)",
                    exchangeRateToUSD = 71.50,
                    markupPercent = 2.0,
                    isPrimaryRegional = true
                ),
                CurrencyConfig(
                    currencyCode = "SAR",
                    currencySymbol = "﷼",
                    displayName = "Saudi Riyal (SAR)",
                    exchangeRateToUSD = 3.75,
                    markupPercent = 1.5,
                    isPrimaryRegional = false
                ),
                CurrencyConfig(
                    currencyCode = "TRY",
                    currencySymbol = "₺",
                    displayName = "Turkish Lira (TRY)",
                    exchangeRateToUSD = 33.20,
                    markupPercent = 2.5,
                    isPrimaryRegional = false
                ),
                CurrencyConfig(
                    currencyCode = "EUR",
                    currencySymbol = "€",
                    displayName = "Euro (EUR)",
                    exchangeRateToUSD = 0.92,
                    markupPercent = 0.0,
                    isPrimaryRegional = false
                )
            )
            database.currencyConfigDao().insertCurrencies(currencies)

            // 3. Initial Products with rich multi-images, categories, costs, and selling prices
            val products = listOf(
                Product(
                    name = "Premium Afghan Saffron (5g)",
                    sku = "SAF-001",
                    barcode = "89012345001",
                    category = "Spices & Herbs",
                    brand = "Herat Gold",
                    costPrice = 12.00,
                    sellingPrice = 24.50,
                    stockQuantity = 45,
                    minStockThreshold = 10,
                    description = "Grade A pure natural saffron threads from Herat with vibrant red color and rich aroma."
                ),
                Product(
                    name = "Organic Black Tea (500g)",
                    sku = "TEA-002",
                    barcode = "89012345002",
                    category = "Beverages",
                    brand = "Khyber Tea",
                    costPrice = 3.20,
                    sellingPrice = 6.90,
                    stockQuantity = 80,
                    minStockThreshold = 15,
                    description = "Premium high-grown loose leaf aromatic black tea blend with cardamom notes."
                ),
                Product(
                    name = "Cold-Pressed Extra Virgin Olive Oil (1L)",
                    sku = "OIL-003",
                    barcode = "89012345003",
                    category = "Oils & Cooking",
                    brand = "Zaytoun Estate",
                    costPrice = 7.50,
                    sellingPrice = 14.80,
                    stockQuantity = 32,
                    minStockThreshold = 8,
                    description = "Single-estate 100% cold pressed unfiltered extra virgin olive oil."
                ),
                Product(
                    name = "Roasted Pistachios Salted (400g)",
                    sku = "NUT-004",
                    barcode = "89012345004",
                    category = "Nuts & Dried Fruits",
                    brand = "Khorasan Harvest",
                    costPrice = 5.80,
                    sellingPrice = 11.50,
                    stockQuantity = 4, // Low stock for alerts!
                    minStockThreshold = 10,
                    description = "Crunchy lightly salted jumbo roasted pistachios rich in fiber and antioxidants."
                ),
                Product(
                    name = "Natural Raw Honeycomb (450g)",
                    sku = "HNY-005",
                    barcode = "89012345005",
                    category = "Natural Sweets",
                    brand = "Pamir Pure",
                    costPrice = 6.00,
                    sellingPrice = 13.00,
                    stockQuantity = 18,
                    minStockThreshold = 5,
                    description = "Pure wildflower raw mountain honeycomb without artificial additives."
                ),
                Product(
                    name = "Almond Kernels Organic (500g)",
                    sku = "NUT-006",
                    barcode = "89012345006",
                    category = "Nuts & Dried Fruits",
                    brand = "Khorasan Harvest",
                    costPrice = 4.50,
                    sellingPrice = 9.80,
                    stockQuantity = 3, // Low stock for alerts!
                    minStockThreshold = 8,
                    description = "Sweet nonpareil raw whole almonds freshly shelled."
                ),
                Product(
                    name = "Green Cardamom Pods (100g)",
                    sku = "SPC-007",
                    barcode = "89012345007",
                    category = "Spices & Herbs",
                    brand = "Spice Route",
                    costPrice = 4.00,
                    sellingPrice = 8.50,
                    stockQuantity = 50,
                    minStockThreshold = 12,
                    description = "Bold green aromatic cardamom pods with intense citrusy sweetness."
                ),
                Product(
                    name = "Turkish Ground Coffee (250g)",
                    sku = "COF-008",
                    barcode = "89012345008",
                    category = "Beverages",
                    brand = "Sultan Roast",
                    costPrice = 2.80,
                    sellingPrice = 5.90,
                    stockQuantity = 60,
                    minStockThreshold = 10,
                    description = "Finely stone-ground medium roast arabica coffee with foam."
                )
            )
            val pIds = database.productDao().insertProducts(products)

            // 4. Initial Audit Log
            database.auditLogDao().insertLog(
                AuditLog(
                    userId = adminId,
                    username = "admin",
                    userRole = "SUPER_ADMIN",
                    actionType = "SYSTEM_INITIALIZED",
                    description = "Initial system startup, default currencies (USD, AFN, SAR, TRY, EUR) and inventory loaded."
                )
            )

            // 5. Seed Realistic Historical Sales for analytics (Today, This Week, This Month, Past Month)
            val now = System.currentTimeMillis()
            val oneDayMillis = 86400000L
            
            // Today sale 1
            val sale1 = Sale(
                invoiceNumber = "INV-2026-001",
                cashierId = cashier1Id,
                cashierName = "Sarah Cashier",
                totalRevenue = 60.50,
                totalCost = 30.20,
                grossProfit = 30.30,
                profitMarginPercent = 50.08,
                status = SaleStatus.ACTIVE,
                timestamp = now - 3600000L * 2 // 2 hours ago today
            )
            val sId1 = database.saleDao().insertSale(sale1)
            database.saleItemDao().insertSaleItems(listOf(
                SaleItem(saleId = sId1, productId = pIds[0], productName = products[0].name, productSku = products[0].sku, quantity = 2, unitCostSnapshot = 12.00, unitPriceSnapshot = 24.50, subtotal = 49.00, costTotal = 24.00, grossProfit = 25.00),
                SaleItem(saleId = sId1, productId = pIds[3], productName = products[3].name, productSku = products[3].sku, quantity = 1, unitCostSnapshot = 5.80, unitPriceSnapshot = 11.50, subtotal = 11.50, costTotal = 5.80, grossProfit = 5.70)
            ))

            // Today sale 2
            val sale2 = Sale(
                invoiceNumber = "INV-2026-002",
                cashierId = cashier2Id,
                cashierName = "Ahmad Sales",
                totalRevenue = 34.60,
                totalCost = 16.70,
                grossProfit = 17.90,
                profitMarginPercent = 51.73,
                status = SaleStatus.ACTIVE,
                timestamp = now - 3600000L * 4 // 4 hours ago
            )
            val sId2 = database.saleDao().insertSale(sale2)
            database.saleItemDao().insertSaleItems(listOf(
                SaleItem(saleId = sId2, productId = pIds[1], productName = products[1].name, productSku = products[1].sku, quantity = 2, unitCostSnapshot = 3.20, unitPriceSnapshot = 6.90, subtotal = 13.80, costTotal = 6.40, grossProfit = 7.40),
                SaleItem(saleId = sId2, productId = pIds[2], productName = products[2].name, productSku = products[2].sku, quantity = 1, unitCostSnapshot = 7.50, unitPriceSnapshot = 14.80, subtotal = 14.80, costTotal = 7.50, grossProfit = 7.30),
                SaleItem(saleId = sId2, productId = pIds[7], productName = products[7].name, productSku = products[7].sku, quantity = 1, unitCostSnapshot = 2.80, unitPriceSnapshot = 5.90, subtotal = 5.90, costTotal = 2.80, grossProfit = 3.10)
            ))

            // Earlier this week (3 days ago)
            val sale3 = Sale(
                invoiceNumber = "INV-2026-003",
                cashierId = cashier1Id,
                cashierName = "Sarah Cashier",
                totalRevenue = 92.00,
                totalCost = 45.00,
                grossProfit = 47.00,
                profitMarginPercent = 51.08,
                status = SaleStatus.ACTIVE,
                timestamp = now - oneDayMillis * 3
            )
            val sId3 = database.saleDao().insertSale(sale3)
            database.saleItemDao().insertSaleItems(listOf(
                SaleItem(saleId = sId3, productId = pIds[4], productName = products[4].name, productSku = products[4].sku, quantity = 4, unitCostSnapshot = 6.00, unitPriceSnapshot = 13.00, subtotal = 52.00, costTotal = 24.00, grossProfit = 28.00),
                SaleItem(saleId = sId3, productId = pIds[5], productName = products[5].name, productSku = products[5].sku, quantity = 4, unitCostSnapshot = 4.50, unitPriceSnapshot = 9.80, subtotal = 39.20, costTotal = 18.00, grossProfit = 21.20)
            ))

            // Earlier this month (12 days ago)
            val sale4 = Sale(
                invoiceNumber = "INV-2026-004",
                cashierId = cashier2Id,
                cashierName = "Ahmad Sales",
                totalRevenue = 125.00,
                totalCost = 61.20,
                grossProfit = 63.80,
                profitMarginPercent = 51.04,
                status = SaleStatus.ACTIVE,
                timestamp = now - oneDayMillis * 12
            )
            val sId4 = database.saleDao().insertSale(sale4)
            database.saleItemDao().insertSaleItems(listOf(
                SaleItem(saleId = sId4, productId = pIds[0], productName = products[0].name, productSku = products[0].sku, quantity = 4, unitCostSnapshot = 12.00, unitPriceSnapshot = 24.50, subtotal = 98.00, costTotal = 48.00, grossProfit = 50.00),
                SaleItem(saleId = sId4, productId = pIds[6], productName = products[6].name, productSku = products[6].sku, quantity = 3, unitCostSnapshot = 4.00, unitPriceSnapshot = 8.50, subtotal = 25.50, costTotal = 12.00, grossProfit = 13.50)
            ))

            // Voided sale example (preserves history with reason)
            val saleVoided = Sale(
                invoiceNumber = "INV-2026-005",
                cashierId = cashier1Id,
                cashierName = "Sarah Cashier",
                totalRevenue = 49.00,
                totalCost = 24.00,
                grossProfit = 25.00,
                profitMarginPercent = 51.02,
                status = SaleStatus.VOID,
                voidReason = "Customer returned items due to wrong size/selection.",
                voidedByUserId = managerId,
                voidedAt = now - oneDayMillis * 5,
                timestamp = now - oneDayMillis * 5
            )
            val sIdV = database.saleDao().insertSale(saleVoided)
            database.saleItemDao().insertSaleItems(listOf(
                SaleItem(saleId = sIdV, productId = pIds[0], productName = products[0].name, productSku = products[0].sku, quantity = 2, unitCostSnapshot = 12.00, unitPriceSnapshot = 24.50, subtotal = 49.00, costTotal = 24.00, grossProfit = 25.00)
            ))

            // 6. Seed Sample Debts / Ledger accounts
            database.customerDebtDao().insertDebt(
                CustomerDebt(
                    customerName = "حاجی محمد نوری",
                    phoneNumber = "0799123456",
                    totalDebtUsd = 45.00,
                    lastTransactionType = "CREDIT_SALE",
                    lastAmountUsd = 45.00,
                    notes = "خرید زعفران و چای نسیه - تحویل در مغازه",
                    saleInvoiceNumber = "INV-2026-003",
                    isSettled = false
                )
            )
            database.customerDebtDao().insertDebt(
                CustomerDebt(
                    customerName = "استاد احمد شاه",
                    phoneNumber = "0788654321",
                    totalDebtUsd = 120.00,
                    lastTransactionType = "CREDIT_SALE",
                    lastAmountUsd = 120.00,
                    notes = "خرید روغن زیتون و عسل نسیه",
                    saleInvoiceNumber = "INV-2026-004",
                    isSettled = false
                )
            )

            // 7. Seed Initial Support Tickets & Messages
            val sampleTicket1 = SupportTicketEntity(
                ticketId = "TICK-1001",
                userId = "6", // Guest Customer ID
                userName = "Guest Customer",
                subject = "استعلام نحوه ارسال سفارش زعفران به هرات",
                category = "SALES",
                status = "OPEN",
                priority = "HIGH",
                createdAt = now - 7200000L,
                updatedAt = now - 3600000L,
                lastMessage = "سلام، آیا ارسال مستقیم به ولایت هرات با پیک اختصاصی انجام می شود؟",
                unreadCount = 1,
                assignedTo = null,
                isSynced = true
            )
            val sampleTicket2 = SupportTicketEntity(
                ticketId = "TICK-1002",
                userId = "3", // Sarah Cashier ID
                userName = "Sarah Cashier",
                subject = "درخواست چاپگر فاکتور حرارتی جدید برای صندوق اول",
                category = "TECHNICAL",
                status = "IN_PROGRESS",
                priority = "MEDIUM",
                createdAt = now - oneDayMillis,
                updatedAt = now - 1800000L,
                lastMessage = "چاپگر جایگزین ثبت گردید و توسط تیم فنی تحویل داده می‌شود.",
                unreadCount = 0,
                assignedTo = "Store Manager",
                isSynced = true
            )

            database.supportDao().insertTickets(listOf(sampleTicket1, sampleTicket2))

            database.supportDao().insertMessages(
                listOf(
                    SupportMessageEntity(
                        messageId = "MSG-1001-1",
                        ticketId = "TICK-1001",
                        senderId = "6",
                        senderName = "Guest Customer",
                        senderRole = "CUSTOMER",
                        text = "سلام، آیا ارسال مستقیم به ولایت هرات با پیک اختصاصی انجام می شود؟",
                        createdAt = now - 7200000L,
                        deliveredAt = now - 7190000L,
                        readAt = null,
                        isQueuedOffline = false
                    ),
                    SupportMessageEntity(
                        messageId = "MSG-1002-1",
                        ticketId = "TICK-1002",
                        senderId = "3",
                        senderName = "Sarah Cashier",
                        senderRole = "EMPLOYEE",
                        text = "با سلام، کاغذ حرارتی پرینتر صندوق ۱ تمام شده و رول‌های 80mm نیاز است.",
                        createdAt = now - oneDayMillis,
                        deliveredAt = now - oneDayMillis + 5000L,
                        readAt = now - oneDayMillis + 60000L,
                        isQueuedOffline = false
                    ),
                    SupportMessageEntity(
                        messageId = "MSG-1002-2",
                        ticketId = "TICK-1002",
                        senderId = "2",
                        senderName = "Store Manager",
                        senderRole = "MANAGER",
                        text = "چاپگر جایگزین ثبت گردید و توسط تیم فنی تحویل داده می‌شود.",
                        createdAt = now - 1800000L,
                        deliveredAt = now - 1790000L,
                        readAt = now - 1000000L,
                        isQueuedOffline = false
                    )
                )
            )
        }
    }
}
