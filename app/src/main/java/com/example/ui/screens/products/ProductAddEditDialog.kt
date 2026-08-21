package com.example.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.Product
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.LocalizationState
import com.example.ui.components.MultiImagePicker

@Composable
fun ProductAddEditDialog(
    product: Product?,
    localization: LocalizationState,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isEdit = product != null
    var name by remember { mutableStateOf(product?.name ?: "") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "General") }
    var brand by remember { mutableStateOf(product?.brand ?: "") }
    var costPriceStr by remember { mutableStateOf(product?.costPrice?.toString() ?: "5.00") }
    var sellingPriceStr by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "10.00") }
    var stockQuantityStr by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "20") }
    var minStockStr by remember { mutableStateOf(product?.minStockThreshold?.toString() ?: "5") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var imageUris by remember { mutableStateOf(product?.imageUris ?: emptyList()) }
    var primaryImageIndex by remember { mutableStateOf(product?.primaryImageIndex ?: 0) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEdit) (if (isFa) "ویرایش محصول" else "Edit Product") else (if (isFa) "افزودن محصول جدید" else "Add New Product"),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFa) "مدیریت تصاویر، قیمت‌گذاری و موجودی انبار" else "Manage multi-photo assets, pricing and inventory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Requirement 1 & 2: Multi-Image Camera & Gallery component
                    MultiImagePicker(
                        imageUris = imageUris,
                        primaryIndex = primaryImageIndex,
                        onImagesChanged = { imageUris = it },
                        onPrimaryIndexChanged = { primaryImageIndex = it },
                        modifier = Modifier.testTag("multi_image_picker")
                    )

                    // Basic Details
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isFa) "نام محصول *" else "Product Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_product_name"),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text(if (isFa) "کد SKU *" else "SKU *") },
                            modifier = Modifier.weight(1f).testTag("input_product_sku"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text(if (isFa) "دسته‌بندی" else "Category") },
                            modifier = Modifier.weight(1f).testTag("input_product_category"),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text(if (isFa) "برند / سازنده" else "Brand / Manufacturer") },
                            modifier = Modifier.weight(1f).testTag("input_product_brand"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text(if (isFa) "بارکد" else "Barcode") },
                            modifier = Modifier.weight(1f).testTag("input_product_barcode"),
                            singleLine = true
                        )
                    }

                    // Pricing Section with Dual Currency Preview (Requirement 10)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isFa) "قیمت‌گذاری و ماليات (پایه دلار)" else "Pricing & Financials (USD Base)", style = MaterialTheme.typography.labelLarge)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = costPriceStr,
                                    onValueChange = { costPriceStr = it },
                                    label = { Text(if (isFa) "قیمت خرید ($)" else "Purchase Cost ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("input_cost_price"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sellingPriceStr,
                                    onValueChange = { sellingPriceStr = it },
                                    label = { Text(if (isFa) "قیمت فروش ($)" else "Selling Price ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("input_selling_price"),
                                    singleLine = true
                                )
                            }

                            val parsedSelling = sellingPriceStr.toDoubleOrNull() ?: 0.0
                            val parsedCost = costPriceStr.toDoubleOrNull() ?: 0.0
                            val margin = if (parsedSelling > 0.0) ((parsedSelling - parsedCost) / parsedSelling) * 100.0 else 0.0

                            Text(
                                text = if (isFa) "نمایش قیمت: ${LocalizationManager.formatDualCurrency(parsedSelling, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent)}" else "Display Price: ${LocalizationManager.formatDualCurrency(parsedSelling, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isFa) "حاشیه سود: ${String.format("%.1f", margin)}% (سود خالص: $${String.format("%.2f", parsedSelling - parsedCost)})" else "Gross Margin: ${String.format("%.1f", margin)}% (Profit: $${String.format("%.2f", parsedSelling - parsedCost)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Stock & Inventory
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = stockQuantityStr,
                            onValueChange = { stockQuantityStr = it },
                            label = { Text(if (isFa) "تعداد موجودی" else "Stock Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_stock_qty"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minStockStr,
                            onValueChange = { minStockStr = it },
                            label = { Text(if (isFa) "حد هشدار کمبود" else "Low Stock Alert At") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_min_stock"),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (isFa) "توضیحات و مشخصات محصول" else "Description & Specifications") },
                        modifier = Modifier.fillMaxWidth().height(90.dp).testTag("input_product_desc"),
                        maxLines = 3
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isFa) "انصراف" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val cost = costPriceStr.toDoubleOrNull()
                            val price = sellingPriceStr.toDoubleOrNull()
                            val stock = stockQuantityStr.toIntOrNull()
                            val minStock = minStockStr.toIntOrNull() ?: 5

                            if (name.isBlank()) {
                                validationError = if (isFa) "ورود نام محصول الزامی است" else "Product name is required"
                                return@Button
                            }
                            if (sku.isBlank()) {
                                validationError = if (isFa) "ورود کد SKU الزامی است" else "SKU is required"
                                return@Button
                            }
                            if (cost == null || cost < 0.0) {
                                validationError = if (isFa) "ورود قیمت خرید معتبر الزامی است" else "Valid cost price required"
                                return@Button
                            }
                            if (price == null || price <= 0.0) {
                                validationError = if (isFa) "ورود قیمت فروش معتبر الزامی است" else "Valid selling price required"
                                return@Button
                            }
                            if (stock == null || stock < 0) {
                                validationError = if (isFa) "ورود تعداد موجودی معتبر الزامی است" else "Valid stock quantity required"
                                return@Button
                            }

                            val newProd = Product(
                                id = product?.id ?: 0,
                                name = name.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim(),
                                category = category.trim().ifBlank { "General" },
                                brand = brand.trim(),
                                costPrice = cost,
                                sellingPrice = price,
                                stockQuantity = stock,
                                minStockThreshold = minStock,
                                isActive = product?.isActive ?: true,
                                imageUris = imageUris,
                                primaryImageIndex = primaryImageIndex.coerceIn(0, (imageUris.size - 1).coerceAtLeast(0)),
                                description = description.trim()
                            )
                            onSave(newProd)
                        },
                        modifier = Modifier.testTag("btn_save_product")
                    ) {
                        Text(if (isEdit) (if (isFa) "ذخیره تغییرات" else "Save Changes") else (if (isFa) "ایجاد محصول" else "Create Product"))
                    }
                }
            }
        }
    }
}
