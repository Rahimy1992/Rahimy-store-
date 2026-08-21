package com.example.ui.screens.invoices

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ai.ExtractedInvoiceData
import com.example.data.ai.ExtractedInvoiceItem
import com.example.ui.AppViewModel
import com.example.ui.components.MultiImagePicker
import com.example.util.MatchConfidenceLevel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInvoiceScannerScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scannedData by viewModel.scannedInvoiceData.collectAsState()
    val isScanning by viewModel.isScanningInvoice.collectAsState()
    val enhancedImages by viewModel.enhancedInvoiceImages.collectAsState()
    val localization by viewModel.localization.collectAsState()
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var primaryImageIndex by remember { mutableStateOf(0) }
    var showDuplicateDialogForItem by remember { mutableStateOf<ExtractedInvoiceItem?>(null) }

    fun loadBitmapsFromUris(uris: List<String>): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        for (uriStr in uris) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bmp = BitmapFactory.decodeStream(stream, null, options)
                    if (bmp != null) bitmaps.add(bmp)
                }
            } catch (e: Exception) {
                // handle safely
            }
        }
        return bitmaps
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isFa) "اسکنر هوشمند فاکتور (AI OCR)" else "AI Invoice Scanner & OCR",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFa) "استخراج خودکار، لغو همپوشانی و پیشگیری از محصولات تکراری" else "Auto-extraction, duplicate detection & Stock-In",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scannedData != null) {
                        IconButton(onClick = { viewModel.clearScannedInvoice() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Scan")
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step 1: Image Capture Component with Continuous Camera & Multi Gallery
            if (scannedData == null) {
                MultiImagePicker(
                    imageUris = imageUris,
                    primaryIndex = primaryImageIndex,
                    onImagesChanged = { newUris -> imageUris = newUris },
                    onPrimaryIndexChanged = { idx -> primaryImageIndex = idx }
                )

                if (imageUris.isNotEmpty()) {
                    Button(
                        onClick = {
                            val bitmaps = loadBitmapsFromUris(imageUris)
                            viewModel.scanInvoiceWithAi(bitmaps, imageUris)
                        },
                        enabled = !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_start_ai_invoice_scan"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isFa) "در حال پردازش هوشمند تصویر..." else "Enhancing Image & Extracting with AI...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isFa) "اسکن و تحلیل هوشمند فاکتور" else "Scan & Analyze Invoice (${imageUris.size} Pages)")
                        }
                    }
                }
            } else {
                // Step 2: Extracted Invoice Review & Duplicate Resolution Component
                val invoice = scannedData!!

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Invoice Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Invoice #${invoice.invoiceNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = invoice.currency,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = invoice.supplierName,
                                        onValueChange = { newSup ->
                                            viewModel.updateScannedInvoiceData(invoice.copy(supplierName = newSup))
                                        },
                                        label = { Text("Supplier Name") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = invoice.invoiceNumber,
                                        onValueChange = { newInv ->
                                            viewModel.updateScannedInvoiceData(invoice.copy(invoiceNumber = newInv))
                                        },
                                        label = { Text("Invoice No.") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = invoice.issueDate,
                                        onValueChange = { newDate ->
                                            viewModel.updateScannedInvoiceData(invoice.copy(issueDate = newDate))
                                        },
                                        label = { Text("Issue Date") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = invoice.notes,
                                        onValueChange = { newNotes ->
                                            viewModel.updateScannedInvoiceData(invoice.copy(notes = newNotes))
                                        },
                                        label = { Text("Notes") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // Extracted Line Items Section
                    item {
                        Text(
                            text = "Line Items (${invoice.items.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    itemsIndexed(invoice.items) { index, item ->
                        InvoiceItemCard(
                            item = item,
                            onUpdate = { updatedItem ->
                                val updatedList = invoice.items.toMutableList()
                                updatedList[index] = updatedItem

                                val newSub = updatedList.sumOf { it.totalPrice }
                                val newGrand = newSub - invoice.discount + invoice.tax
                                viewModel.updateScannedInvoiceData(
                                    invoice.copy(items = updatedList, subtotal = newSub, grandTotal = newGrand)
                                )
                            },
                            onResolveDuplicate = { showDuplicateDialogForItem = item }
                        )
                    }

                    // Calculations Summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                                    Text("${String.format("%.2f", invoice.subtotal)} ${invoice.currency}")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tax", style = MaterialTheme.typography.bodyMedium)
                                    Text("${String.format("%.2f", invoice.tax)} ${invoice.currency}")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount", style = MaterialTheme.typography.bodyMedium)
                                    Text("-${String.format("%.2f", invoice.discount)} ${invoice.currency}")
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Grand Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${String.format("%.2f", invoice.grandTotal)} ${invoice.currency}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Final Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.processScannedInvoiceAsStockIn() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirm_stock_in")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFa) "ثبت خریدار و ورود به انبار" else "Process as Stock-In")
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearScannedInvoice() },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        Text(if (isFa) "انصراف" else "Cancel")
                    }
                }
            }
        }
    }

    // Duplicate Product Candidate Resolution Dialog
    val itemForDup = showDuplicateDialogForItem
    if (itemForDup != null && scannedData != null) {
        val candidate = itemForDup.matchCandidate
        val inv = scannedData!!

        AlertDialog(
            onDismissRequest = { showDuplicateDialogForItem = null },
            title = {
                Text(if (isFa) "تشخیص محصول مشابه در انبار" else "Existing Product Match Detected")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Detected item '${itemForDup.productName}' matches an existing inventory item:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (candidate != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = candidate.existingProduct.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("SKU: ${candidate.existingProduct.sku} | Barcode: ${candidate.existingProduct.barcode}")
                                Text("Current Stock: ${candidate.existingProduct.stockQuantity} | Selling Price: $${candidate.existingProduct.sellingPrice}")
                                Text(
                                    "Match Reason: ${candidate.matchReason}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Text("How would you like to handle this item?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (candidate != null) {
                            val updatedItems = inv.items.map { itm ->
                                if (itm.tempId == itemForDup.tempId) {
                                    itm.copy(
                                        resolvedProductId = candidate.existingProduct.id,
                                        productName = candidate.existingProduct.name,
                                        sku = candidate.existingProduct.sku
                                    )
                                } else itm
                            }
                            viewModel.updateScannedInvoiceData(inv.copy(items = updatedItems))
                        }
                        showDuplicateDialogForItem = null
                    }
                ) {
                    Text(if (isFa) "استفاده از محصول موجود" else "Use Existing Product")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val updatedItems = inv.items.map { itm ->
                            if (itm.tempId == itemForDup.tempId) {
                                itm.copy(resolvedProductId = null)
                            } else itm
                        }
                        viewModel.updateScannedInvoiceData(inv.copy(items = updatedItems))
                        showDuplicateDialogForItem = null
                    }
                ) {
                    Text(if (isFa) "ایجاد محصول جدید" else "Create New Product")
                }
            }
        )
    }
}

@Composable
private fun InvoiceItemCard(
    item: ExtractedInvoiceItem,
    onUpdate: (ExtractedInvoiceItem) -> Unit,
    onResolveDuplicate: () -> Unit
) {
    var name by remember(item.tempId) { mutableStateOf(item.productName) }
    var qtyStr by remember(item.tempId) { mutableStateOf(item.quantity.toString()) }
    var priceStr by remember(item.tempId) { mutableStateOf(item.unitPrice.toString()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(item.copy(productName = it))
                    },
                    label = { Text("Product Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                if (item.matchCandidate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onResolveDuplicate) {
                        Icon(
                            Icons.Default.FindInPage,
                            contentDescription = "Match",
                            tint = if (item.resolvedProductId != null) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                    }
                }
            }

            if (item.matchCandidate != null) {
                val candidate = item.matchCandidate!!
                Surface(
                    color = if (item.resolvedProductId != null) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (item.resolvedProductId != null)
                                "Linked to existing product: '${candidate.existingProduct.name}'"
                            else
                                "Possible match: '${candidate.existingProduct.name}' (${candidate.matchReason})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.resolvedProductId != null) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                        TextButton(onClick = onResolveDuplicate) {
                            Text("Change", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = {
                        qtyStr = it
                        val q = it.toIntOrNull() ?: 1
                        val p = priceStr.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(quantity = q, totalPrice = q * p))
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = {
                        priceStr = it
                        val p = it.toDoubleOrNull() ?: 0.0
                        val q = qtyStr.toIntOrNull() ?: 1
                        onUpdate(item.copy(unitPrice = p, totalPrice = q * p))
                    },
                    label = { Text("Unit Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = "Total: $${String.format("%.2f", item.totalPrice)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
