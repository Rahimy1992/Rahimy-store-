package com.example.ui.screens.catalog

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ai.DetectedProduct
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogOcrScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalogBitmaps by viewModel.catalogBitmaps.collectAsState()
    val catalogUris by viewModel.catalogImageUris.collectAsState()
    val detectedProducts by viewModel.detectedProducts.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingOcr.collectAsState()
    val localization by viewModel.localization.collectAsState()
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    fun saveBitmapToLocalUri(bitmap: Bitmap): String {
        val file = File(context.cacheDir, "ocr_${System.currentTimeMillis()}_${(100..999).random()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file).toString()
    }

    // Camera launcher for catalog pages
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            val uriStr = saveBitmapToLocalUri(bmp)
            viewModel.addCatalogPageBitmap(bmp, uriStr)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to scan pages", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraSafely() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Multi-page gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = 2 // Safely downsample high-res camera captures for OCR
                    }
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream, null, options)
                    if (bmp != null) {
                        viewModel.addCatalogPageBitmap(bmp, uri.toString())
                    }
                }
            } catch (e: Exception) {
                // handle safely
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (isFa) "استخراج هوشمند کاتالوگ و فاکتور (OCR)" else "AI Catalog & Booklet OCR", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (isFa) "اسکن صفحات کاتالوگ یا لیست اجناس جهت استخراج و افزودن خودکار به انبار" else "Scan booklet pages or product lists to extract & create inventory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (detectedProducts.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearCatalogPages() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step 1: Booklet Page Upload Strip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (isFa) "۱. گرفتن یا انتخاب تصویر کاتالوگ (${catalogUris.size} صفحه)" else "1. Capture / Select Booklet Pages (${catalogUris.size} Pages)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { launchCameraSafely() },
                            modifier = Modifier.weight(1f).testTag("btn_ocr_camera")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFa) "اسکن صفحه" else "Scan Page")
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).testTag("btn_ocr_gallery")
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFa) "انتخاب عکس" else "Pick Pages")
                        }
                    }

                    // Scanned Pages Horizontal Strip
                    if (catalogUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(catalogUris) { index, uriStr ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(uriStr).build(),
                                        contentDescription = "Page $index",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(bottomEnd = 4.dp),
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            "P.${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeCatalogPage(index) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Page", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trigger OCR Analysis
                        Button(
                            onClick = { viewModel.analyzeCatalogPages() },
                            enabled = !isAnalyzing,
                            modifier = Modifier.fillMaxWidth().testTag("btn_start_ocr_analysis"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Catalog Pages...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract Products with AI OCR")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 2: Detected Products Review & Correction (Requirement 5)
            if (detectedProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "2. Review & Correct Extracted Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Review all prices and details before publishing into store inventory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val selectedCount = detectedProducts.count { it.isSelected }
                    Button(
                        onClick = { viewModel.bulkCreateFromOcr() },
                        enabled = selectedCount > 0,
                        modifier = Modifier.testTag("btn_bulk_create_ocr")
                    ) {
                        Text("Create ($selectedCount)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(detectedProducts) { index, item ->
                        DetectedProductCard(
                            item = item,
                            localization = localization,
                            onToggleSelect = { viewModel.toggleDetectedProductSelected(index) },
                            onUpdate = { updated -> viewModel.updateDetectedProduct(index, updated) }
                        )
                    }
                }
            } else if (catalogUris.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Capture continuous catalog/booklet photos to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectedProductCard(
    item: DetectedProduct,
    localization: com.example.domain.localization.LocalizationState,
    onToggleSelect: () -> Unit,
    onUpdate: (DetectedProduct) -> Unit
) {
    var name by remember(item.tempId) { mutableStateOf(item.name) }
    var sku by remember(item.tempId) { mutableStateOf(item.sku) }
    var sellingPriceStr by remember(item.tempId) { mutableStateOf(item.sellingPrice.toString()) }
    var costPriceStr by remember(item.tempId) { mutableStateOf(item.costPrice.toString()) }
    var category by remember(item.tempId) { mutableStateOf(item.category) }
    var stockStr by remember(item.tempId) { mutableStateOf(item.stockQuantity.toString()) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = if (item.isSelected) 1.5.dp else 0.5.dp,
            color = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth().testTag("detected_card_${item.tempId}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row with Checkbox & Confidence Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                    Text("Select for Import", style = MaterialTheme.typography.labelMedium)
                }

                Surface(
                    color = if (item.confidenceScore > 0.85f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Confidence: ${String.format("%.0f", item.confidenceScore * 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (item.confidenceScore > 0.85f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (item.hasPriceWarning && item.warningMessage.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ ${item.warningMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            // Editable Form Fields
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onUpdate(item.copy(name = it))
                },
                label = { Text("Detected Product Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = {
                        sku = it
                        onUpdate(item.copy(sku = it))
                    },
                    label = { Text("SKU") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        onUpdate(item.copy(category = it))
                    },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sellingPriceStr,
                    onValueChange = {
                        sellingPriceStr = it
                        val p = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(sellingPrice = p))
                    },
                    label = { Text("Selling Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = {
                        costPriceStr = it
                        val c = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(costPrice = c))
                    },
                    label = { Text("Cost Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stockStr,
                    onValueChange = {
                        stockStr = it
                        val s = it.toIntOrNull() ?: 0
                        onUpdate(item.copy(stockQuantity = s))
                    },
                    label = { Text("Initial Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            val curSelling = sellingPriceStr.toDoubleOrNull() ?: 0.0
            Text(
                text = "Regional Display: ${LocalizationManager.formatDualCurrency(curSelling, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
