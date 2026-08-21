package com.example.ui.screens.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.Product
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel
import com.example.ui.ProductStockFilter
import com.example.ui.ProductSortOption
import com.example.ui.components.DestructiveConfirmationDialog

import com.example.ui.components.BarcodeScannerDialog
import com.example.util.ExcelCsvExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: AppViewModel,
    onNavigateToCatalogOcr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val stockFilter by viewModel.productStockFilter.collectAsState()
    val sortOption by viewModel.productSortOption.collectAsState()
    val selectedIds by viewModel.selectedProductIds.collectAsState()
    val localization by viewModel.localization.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkEditDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val hasSelection = selectedIds.isNotEmpty()

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isFa) "افزودن محصول" else "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with AI Booklet Scanner Shortcut & Barcode & CSV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "موجودی انبار" else "Store Inventory",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isFa) "مجموع ${allProducts.size} قلم جنس (${allProducts.count { it.isActive }} فعال)" else "${allProducts.size} total items (${allProducts.count { it.isActive }} active)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.testTag("btn_camera_scan_product")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val file = ExcelCsvExporter.exportProductsToCsv(context, allProducts, localization)
                            if (file != null) {
                                ExcelCsvExporter.shareCsvFile(context, file, if (isFa) "خروجی موجودی انبار" else "Inventory CSV Export")
                            }
                        },
                        modifier = Modifier.testTag("btn_export_products_csv")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }

                    // AI Catalog/Booklet OCR Quick Action
                    FilledTonalButton(
                        onClick = onNavigateToCatalogOcr,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("btn_quick_scan_catalog")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scanner", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "کاتالوگ" else "AI Catalog")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(if (isFa) "جستجوی لحظه‌ای (نام، کد SKU، دسته‌بندی، بارکد)..." else "Real-time search (Name, SKU, Category, Barcode)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("btn_clear_search")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("product_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category & Stock Filter Chips Row
            val allCategoryOptions = listOf("All") + categories
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Categories
                items(allCategoryOptions) { cat ->
                    val isSelected = selectedCategory == cat
                    val displayCat = if (cat == "All") (if (isFa) "همه دسته‌ها" else "All Categories") else cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(cat) },
                        label = { Text(displayCat) },
                        modifier = Modifier.testTag("chip_category_$cat")
                    )
                }

                // Stock Filters
                item {
                    FilterChip(
                        selected = stockFilter == ProductStockFilter.IN_STOCK,
                        onClick = {
                            viewModel.setProductStockFilter(
                                if (stockFilter == ProductStockFilter.IN_STOCK) ProductStockFilter.ALL else ProductStockFilter.IN_STOCK
                            )
                        },
                        label = { Text(if (isFa) "موجود در انبار" else "In Stock") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("chip_stock_in_stock")
                    )
                }

                item {
                    FilterChip(
                        selected = stockFilter == ProductStockFilter.LOW_STOCK,
                        onClick = {
                            viewModel.setProductStockFilter(
                                if (stockFilter == ProductStockFilter.LOW_STOCK) ProductStockFilter.ALL else ProductStockFilter.LOW_STOCK
                            )
                        },
                        label = { Text(if (isFa) "کمبود موجودی" else "Low Stock") },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("chip_stock_low_stock")
                    )
                }

                item {
                    FilterChip(
                        selected = stockFilter == ProductStockFilter.OUT_OF_STOCK,
                        onClick = {
                            viewModel.setProductStockFilter(
                                if (stockFilter == ProductStockFilter.OUT_OF_STOCK) ProductStockFilter.ALL else ProductStockFilter.OUT_OF_STOCK
                            )
                        },
                        label = { Text(if (isFa) "اتمام موجودی" else "Out of Stock") },
                        leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("chip_stock_out_of_stock")
                    )
                }
            }

            // Real-Time Results Counter & Reset Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) "نمایش ${products.size} از ${allProducts.size} محصول" else "Showing ${products.size} of ${allProducts.size} products",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("txt_filter_count_status")
                )

                if (searchQuery.isNotEmpty() || selectedCategory != "All" || stockFilter != ProductStockFilter.ALL) {
                    TextButton(
                        onClick = { viewModel.clearSearchAndFilters() },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.testTag("btn_reset_filters")
                    ) {
                        Text(
                            text = if (isFa) "پاکسازی فیلترها" else "Clear Filters",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Bulk Action Bar (Requirement 3)
            AnimatedVisibility(visible = hasSelection) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "${selectedIds.size} مورد انتخاب شده" else "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Bulk Edit
                            IconButton(
                                onClick = { showBulkEditDialog = true },
                                modifier = Modifier.testTag("btn_bulk_edit")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Bulk Edit", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // Bulk Activate
                            IconButton(
                                onClick = { viewModel.bulkSetStatus(true) },
                                modifier = Modifier.testTag("btn_bulk_activate")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Bulk Activate", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // Bulk Deactivate
                            IconButton(
                                onClick = { viewModel.bulkSetStatus(false) },
                                modifier = Modifier.testTag("btn_bulk_deactivate")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = "Bulk Deactivate", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // Bulk Delete with confirmation
                            IconButton(
                                onClick = { showBulkDeleteConfirm = true },
                                modifier = Modifier.testTag("btn_bulk_delete")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Bulk Delete", tint = MaterialTheme.colorScheme.error)
                            }

                            // Deselect All
                            IconButton(onClick = { viewModel.selectAllProducts(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Deselect")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Select All row if items exist
            if (products.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = products.isNotEmpty() && products.all { selectedIds.contains(it.id) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.selectAllProducts(!allSelected) }
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { viewModel.selectAllProducts(it) },
                            modifier = Modifier.size(32.dp).testTag("checkbox_select_all")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFa) "انتخاب همه (${products.size})" else "Select All (${products.size})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = if (isFa) "نمایش دو ارزی: ${localization.selectedCurrencyCode} + USD" else "Dual Currency: ${localization.selectedCurrencyCode} + USD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isFa) "هیچ محصولی با این فیلتر یافت نشد" else "No products found matching filters",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Text(if (isFa) "ایجاد اولین محصول" else "Create First Product")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val isSelected = selectedIds.contains(product.id)
                        val isLowStock = product.stockQuantity <= product.minStockThreshold

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("product_card_${product.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (product.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Multi-select Checkbox
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleProductSelection(product.id) },
                                    modifier = Modifier.testTag("checkbox_product_${product.id}")
                                )

                                // Primary Product Image thumbnail
                                val primaryUri = product.imageUris.getOrNull(product.primaryImageIndex)
                                    ?: product.imageUris.firstOrNull()

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!primaryUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(primaryUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = "Default Product",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Photo Count Badge
                                    if (product.imageUris.size > 1) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                        ) {
                                            Text(
                                                text = "${product.imageUris.size} 📷",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Product Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (product.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!product.isActive) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (isFa) "غیرفعال" else "Inactive",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "SKU: ${product.sku} • ${product.category}${if (product.brand.isNotBlank()) " • " + product.brand else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Requirement 10: Dual Currency Display
                                    Text(
                                        text = LocalizationManager.formatDualCurrency(
                                            product.sellingPrice,
                                            localization.selectedCurrencyCode,
                                            localization.selectedCurrencySymbol,
                                            localization.exchangeRateToUSD,
                                            localization.markupPercent
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Stock info with low-stock badge
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isFa) "موجودی: ${product.stockQuantity} عدد" else "Stock: ${product.stockQuantity} units",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isLowStock) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isFa) "⚠️ کمبود موجودی" else "⚠️ Low Stock",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Quick Actions: Add to POS Cart & Edit
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { viewModel.addToCart(product) },
                                        enabled = product.isActive && product.stockQuantity > 0,
                                        modifier = Modifier.testTag("btn_add_to_cart_${product.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.AddShoppingCart,
                                            contentDescription = "Add to Cart",
                                            tint = if (product.isActive && product.stockQuantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    IconButton(
                                        onClick = { productToEdit = product },
                                        modifier = Modifier.testTag("btn_edit_product_${product.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Product")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Product
    if (showAddDialog) {
        ProductAddEditDialog(
            product = null,
            localization = localization,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.addProduct(it)
                showAddDialog = false
            }
        )
    }

    // Dialog: Edit Product
    if (productToEdit != null) {
        ProductAddEditDialog(
            product = productToEdit,
            localization = localization,
            onDismiss = { productToEdit = null },
            onSave = {
                viewModel.updateProduct(it)
                productToEdit = null
            }
        )
    }

    // Dialog: Bulk Edit (Requirement 3)
    if (showBulkEditDialog) {
        BulkEditProductsDialog(
            selectedCount = selectedIds.size,
            onDismiss = { showBulkEditDialog = false },
            onApply = { priceAdj, cat, stockAdj ->
                viewModel.bulkEditProducts(priceAdj, cat, stockAdj)
                showBulkEditDialog = false
            }
        )
    }

    // Dialog: Bulk Delete Confirmation (Requirement 3)
    if (showBulkDeleteConfirm) {
        DestructiveConfirmationDialog(
            title = if (isFa) "حذف ${selectedIds.size} محصول؟" else "Delete ${selectedIds.size} Products?",
            message = if (isFa) "این عمل باعث حذف دائمی ${selectedIds.size} محصول از دیتابیس فروشگاه می‌گردد و در گزارش‌های امنیتی ثبت خواهد شد." else "This action will permanently delete ${selectedIds.size} products from the store database. This operation is recorded in the Audit Log.",
            confirmButtonText = if (isFa) "تایید حذف ${selectedIds.size} محصول" else "Delete ${selectedIds.size} Products",
            onConfirm = {
                viewModel.bulkDeleteProducts()
                showBulkDeleteConfirm = false
            },
            onDismiss = { showBulkDeleteConfirm = false }
        )
    }

    // Dialog: Barcode Scanner
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            isFa = isFa,
            onBarcodeScanned = { code ->
                viewModel.setSearchQuery(code)
                showBarcodeScanner = false
            },
            onDismissRequest = { showBarcodeScanner = false }
        )
    }
}
