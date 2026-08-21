package com.example.ui.screens.manual_sales

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.Product
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleStatus
import com.example.data.local.entity.UserRole
import com.example.data.repository.ManualSaleImageInput
import com.example.domain.localization.SupportedLanguage
import com.example.ui.AppViewModel
import com.example.ui.components.MultiImagePicker
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSalesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localization by viewModel.localization.collectAsState()
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "ps")
    val currentUser by viewModel.currentUser.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allSales by viewModel.allSales.collectAsState()

    // Filter only manual sales
    val manualSales = remember(allSales) {
        allSales.filter { it.saleType == "MANUAL" }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = remember(isFa) {
        if (isFa) {
            listOf("فروش دستی جدید", "گزارش روزانه", "گزارش ماهانه", "گزارش سالانه", "تاریخچه و عکس‌ها")
        } else {
            listOf("New Manual Sale", "Daily Report", "Monthly Report", "Yearly Report", "History & Photos")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Header
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.padding(10.dp).size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isFa) "فروش دستی پیشرفته" else "Advanced Manual Sales",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "ثبت مستقیم فروش، ثبت چندتصویر و گزارشات مالی" else "Direct sale entry, multi-photo capture & financial analytics",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "آفلاین فعال" else "Offline Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> NewManualSaleTab(
                    viewModel = viewModel,
                    products = allProducts,
                    isFa = isFa,
                    onSaleCreated = {
                        selectedTabIndex = 4 // Navigate to history after creation
                    }
                )
                1 -> DailyManualSalesTab(
                    manualSales = manualSales,
                    isFa = isFa,
                    viewModel = viewModel
                )
                2 -> MonthlyManualSalesTab(
                    manualSales = manualSales,
                    isFa = isFa
                )
                3 -> YearlyManualSalesTab(
                    manualSales = manualSales,
                    isFa = isFa
                )
                4 -> ManualSalesHistoryTab(
                    manualSales = manualSales,
                    isFa = isFa,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewManualSaleTab(
    viewModel: AppViewModel,
    products: List<Product>,
    isFa: Boolean,
    onSaleCreated: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    var quantityText by remember { mutableStateOf("1") }
    var unitPriceText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("0") }
    var taxText by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf("AFN") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    // Multi-image state
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var primaryImageIndex by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    val quantity = quantityText.toIntOrNull() ?: 0
    val unitPrice = unitPriceText.toDoubleOrNull() ?: 0.0
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val tax = taxText.toDoubleOrNull() ?: 0.0

    val subtotal = quantity * unitPrice
    val finalTotal = (subtotal - discount + tax).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Product & Pricing Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "۱. انتخاب کالا و مشخصات قیمت" else "1. Product & Pricing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Product Picker Dropdown
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (${if (isFa) "موجودی: ${it.stockQuantity}" else "Stock: ${it.stockQuantity}"})" }
                                ?: if (isFa) "-- انتخاب کالا از انبار --" else "-- Select Product --",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isFa) "کالا / محصول" else "Product") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("manual_product_select"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false }
                        ) {
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(p.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = if (isFa) "${p.stockQuantity} عدد | $${p.sellingPrice}" else "${p.stockQuantity} in stock | $${p.sellingPrice}",
                                                color = Color(0xFF64748B),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = p
                                        unitPriceText = p.sellingPrice.toString()
                                        productDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text(if (isFa) "تعداد" else "Quantity") },
                            modifier = Modifier.weight(1f).testTag("manual_qty_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { unitPriceText = it },
                            label = { Text(if (isFa) "فی (قیمت واحد)" else "Unit Price") },
                            modifier = Modifier.weight(1f).testTag("manual_unit_price_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text(if (isFa) "تخفیف" else "Discount") },
                            modifier = Modifier.weight(1f).testTag("manual_discount_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = taxText,
                            onValueChange = { taxText = it },
                            label = { Text(if (isFa) "مالیات" else "Tax") },
                            modifier = Modifier.weight(1f).testTag("manual_tax_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Currency Selection
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isFa) "ارز معاملات:" else "Currency:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        listOf("AFN", "USD", "SAR", "TRY", "EUR").forEach { curr ->
                            FilterChip(
                                selected = currency == curr,
                                onClick = { currency = curr },
                                label = { Text(curr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1E293B),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Customer & Sale Metadata Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "۲. اطلاعات مشتری و روش پرداخت" else "2. Customer & Payment Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text(if (isFa) "نام مشتری (اختیاری)" else "Customer Name") },
                            modifier = Modifier.weight(1f).testTag("manual_customer_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text(if (isFa) "شماره تماس" else "Phone Number") },
                            modifier = Modifier.weight(1f).testTag("manual_customer_phone"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Payment Method
                    Text(
                        text = if (isFa) "روش پرداخت:" else "Payment Method:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "CASH" to if (isFa) "نقدی" else "Cash",
                            "CARD" to if (isFa) "کارت" else "Card",
                            "DIGITAL" to if (isFa) "دیجیتال" else "Digital",
                            "CREDIT" to if (isFa) "نسیه" else "Credit"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = paymentMethod == code,
                                onClick = { paymentMethod = code },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (isFa) "یادداشت و توضیحات" else "Notes / Observations") },
                        modifier = Modifier.fillMaxWidth().testTag("manual_notes_input"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )
                }
            }
        }

        // Multi-Image Capture/Picker Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isFa) "۳. عکس‌ها و فاکتورها (${imageUris.size})" else "3. Attached Photos (${imageUris.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (imageUris.isNotEmpty()) {
                            TextButton(onClick = { imageUris = emptyList() }) {
                                Text(if (isFa) "پاک کردن همه" else "Clear All", color = Color(0xFFEF4444))
                            }
                        }
                    }

                    Text(
                        text = if (isFa) "می‌توانید به طور پیوسته عکس‌های متعدد از کالا، فاکتور یا رسید مشتری بگیرید یا از گالری انتخاب نمایید."
                        else "Capture multiple product, invoice, or receipt photos continuously via camera or gallery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    // Reusable MultiImagePicker
                    MultiImagePicker(
                        imageUris = imageUris,
                        primaryIndex = primaryImageIndex,
                        onImagesChanged = { updated -> imageUris = updated },
                        onPrimaryIndexChanged = { idx -> primaryImageIndex = idx }
                    )
                }
            }
        }

        // Automatic Calculation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isFa) "خلاصه محاسبات سیستم" else "Automatic Price Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFa) "مجموع فرعی (Subtotal):" else "Subtotal:", color = Color(0xFFCBD5E1))
                        Text("${formatMoney(subtotal)} $currency", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFa) "تخفیف (-):" else "Discount (-):", color = Color(0xFFF87171))
                        Text("-${formatMoney(discount)} $currency", color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isFa) "مالیات (+):" else "Tax (+):", color = Color(0xFF38BDF8))
                        Text("+${formatMoney(tax)} $currency", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = Color(0xFF334155)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "مبلغ نهایی قابل پرداخت:" else "Final Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${formatMoney(finalTotal)} $currency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4ADE80)
                        )
                    }
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    val p = selectedProduct
                    if (p == null) {
                        Toast.makeText(context, if (isFa) "لطفاً ابتدا کالای مورد نظر را انتخاب کنید" else "Please select a product first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (quantity <= 0) {
                        Toast.makeText(context, if (isFa) "تعداد باید بزرگتر از صفر باشد" else "Quantity must be > 0", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (currentUser.role == UserRole.CUSTOMER) {
                        Toast.makeText(context, if (isFa) "مشتریان مجاز به ثبت فروش دستی نیستند" else "Customers cannot create manual sales", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    val imageInputs = imageUris.mapIndexed { idx, uriStr ->
                        val src = if (uriStr.contains("product_images")) "CAMERA" else "GALLERY"
                        ManualSaleImageInput(uriStr = uriStr, source = src)
                    }

                    viewModel.createManualSale(
                        productId = p.id,
                        quantity = quantity,
                        unitPrice = unitPrice,
                        discount = discount,
                        tax = tax,
                        currency = currency,
                        customerName = customerName.ifBlank { null },
                        customerPhone = customerPhone.ifBlank { null },
                        paymentMethod = paymentMethod,
                        notes = notes,
                        images = imageInputs,
                        onSuccess = {
                            isSubmitting = false
                            onSaleCreated()
                        }
                    )
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_manual_sale_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "ثبت و تایید نهایی فروش دستی" else "Confirm & Save Manual Sale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyManualSalesTab(
    manualSales: List<Sale>,
    isFa: Boolean,
    viewModel: AppViewModel
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    val activeTodaySales = remember(manualSales, selectedCalendar.timeInMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedCalendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        manualSales.filter { it.timestamp in startOfDay..endOfDay }
    }

    val activeSales = activeTodaySales.filter { it.status == SaleStatus.ACTIVE }
    val totalTransactions = activeSales.size
    val totalSubtotal = activeSales.sumOf { it.subtotal }
    val totalDiscount = activeSales.sumOf { it.discount }
    val totalTax = activeSales.sumOf { it.tax }
    val netSales = activeSales.sumOf { it.totalRevenue }

    val cashTotal = activeSales.filter { it.paymentMethod == "CASH" }.sumOf { it.totalRevenue }
    val cardTotal = activeSales.filter { it.paymentMethod == "CARD" }.sumOf { it.totalRevenue }
    val digitalTotal = activeSales.filter { it.paymentMethod == "DIGITAL" }.sumOf { it.totalRevenue }
    val creditTotal = activeSales.filter { it.paymentMethod == "CREDIT" }.sumOf { it.totalRevenue }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd (EEEE)", Locale.getDefault())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Selector Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = selectedCalendar.timeInMillis
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isFa) "گزارش فروش روزانه دستی" else "Daily Manual Sales Report",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = dateFormat.format(selectedCalendar.time),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = selectedCalendar.timeInMillis
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }
            }
        }

        // Summary Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = if (isFa) "تعداد تراکنش‌ها" else "Transactions",
                        value = "$totalTransactions",
                        subtitle = if (isFa) "فروش‌های فعال" else "Active Sales",
                        containerColor = Color(0xFF0F172A),
                        textColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isFa) "خالص فروش" else "Net Sales",
                        value = "${formatMoney(netSales)} AFN",
                        subtitle = if (isFa) "بعد از تخفیف و مالیات" else "After Discount & Tax",
                        containerColor = Color(0xFF16A34A),
                        textColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard(
                        title = if (isFa) "کل تخفیف‌ها" else "Total Discount",
                        value = "${formatMoney(totalDiscount)} AFN",
                        subtitle = if (isFa) "کاهش سود" else "Reduced Profit",
                        containerColor = Color(0xFFFEF2F2),
                        textColor = Color(0xFF991B1B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (isFa) "کل مالیات" else "Total Tax",
                        value = "${formatMoney(totalTax)} AFN",
                        subtitle = if (isFa) "افزوده شده" else "Tax Added",
                        containerColor = Color(0xFFF0F9FF),
                        textColor = Color(0xFF075985),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Payment Method Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFa) "تفکیک بر اساس روش پرداخت" else "Payment Method Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isFa) "نقدی (Cash):" else "Cash:", color = Color(0xFF64748B))
                        Text("${formatMoney(cashTotal)} AFN", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isFa) "کارت بانکی (Card):" else "Card:", color = Color(0xFF64748B))
                        Text("${formatMoney(cardTotal)} AFN", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isFa) "دیجیتال (Digital):" else "Digital:", color = Color(0xFF64748B))
                        Text("${formatMoney(digitalTotal)} AFN", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isFa) "نسیه (Credit):" else "Credit:", color = Color(0xFF64748B))
                        Text("${formatMoney(creditTotal)} AFN", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }
        }

        // Sales List for Today
        item {
            Text(
                text = if (isFa) "فروش‌های ثبت‌شده در این تاریخ (${activeTodaySales.size})"
                else "Manual Sales List for Selected Date (${activeTodaySales.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeTodaySales.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.Receipt, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isFa) "هیچ فروش دستی در این تاریخ ثبت نشده است" else "No manual sales recorded on this date.",
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(activeTodaySales) { sale ->
                ManualSaleCardItem(sale = sale, isFa = isFa, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun MonthlyManualSalesTab(
    manualSales: List<Sale>,
    isFa: Boolean
) {
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    val monthNames = remember(isFa) {
        if (isFa) listOf("حمل (حوت/جدی)", "ثور", "جوزا", "سرطان", "اسد", "سنبله", "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت")
        else listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }

    val monthlySales = remember(manualSales, selectedYear, selectedMonthIndex) {
        manualSales.filter { sale ->
            val cal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonthIndex
        }
    }

    val activeSales = monthlySales.filter { it.status == SaleStatus.ACTIVE }
    val totalTransactions = activeSales.size
    val totalDiscount = activeSales.sumOf { it.discount }
    val totalTax = activeSales.sumOf { it.tax }
    val netSales = activeSales.sumOf { it.totalRevenue }

    // Group by day of month
    val dailyBreakdown = remember(activeSales) {
        (1..31).map { day ->
            val salesForDay = activeSales.filter { sale ->
                val cal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                cal.get(Calendar.DAY_OF_MONTH) == day
            }
            day to salesForDay.sumOf { it.totalRevenue }
        }.filter { it.second > 0 || it.first <= Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month / Year Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "انتخاب ماه و سال گزارش" else "Select Month & Year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2024, 2025, 2026, 2027).forEach { yr ->
                            FilterChip(
                                selected = selectedYear == yr,
                                onClick = { selectedYear = yr },
                                label = { Text("$yr") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F172A),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(monthNames) { index, name ->
                            FilterChip(
                                selected = selectedMonthIndex == index,
                                onClick = { selectedMonthIndex = index },
                                label = { Text(name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = if (isFa) "تراکنش‌های ماهانه" else "Monthly Transactions",
                    value = "$totalTransactions",
                    subtitle = "${monthNames[selectedMonthIndex]} $selectedYear",
                    containerColor = Color(0xFF1E293B),
                    textColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (isFa) "مجموع فروش ماهانه" else "Total Monthly Sales",
                    value = "${formatMoney(netSales)} AFN",
                    subtitle = if (isFa) "مجموع خالص" else "Total Net Revenue",
                    containerColor = Color(0xFF16A34A),
                    textColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily Breakdown Table
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFa) "جدول تفکیک روزانه ماه" else "Daily Sales Table for Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    dailyBreakdown.forEach { (day, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "روز $day" else "Day $day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatMoney(amount)} AFN",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (amount > 0) Color(0xFF16A34A) else Color(0xFF94A3B8)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyManualSalesTab(
    manualSales: List<Sale>,
    isFa: Boolean
) {
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    val monthNames = remember(isFa) {
        if (isFa) listOf("حمل", "ثور", "جوزا", "سرطان", "اسد", "سنبله", "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت")
        else listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    val yearlySales = remember(manualSales, selectedYear) {
        manualSales.filter { sale ->
            val cal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            cal.get(Calendar.YEAR) == selectedYear
        }
    }

    val activeSales = yearlySales.filter { it.status == SaleStatus.ACTIVE }
    val totalTransactions = activeSales.size
    val netSales = activeSales.sumOf { it.totalRevenue }

    val monthlyBreakdown = remember(activeSales) {
        (0..11).map { monthIdx ->
            val salesForMonth = activeSales.filter { sale ->
                val cal = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
                cal.get(Calendar.MONTH) == monthIdx
            }
            monthIdx to Pair(salesForMonth.size, salesForMonth.sumOf { it.totalRevenue })
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Year Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isFa) "انتخاب سال گزارش مالی" else "Select Report Year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2024, 2025, 2026, 2027).forEach { yr ->
                            FilterChip(
                                selected = selectedYear == yr,
                                onClick = { selectedYear = yr },
                                label = { Text("$yr") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F172A),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = if (isFa) "تراکنش‌های سالانه" else "Annual Transactions",
                    value = "$totalTransactions",
                    subtitle = "$selectedYear",
                    containerColor = Color(0xFF0F172A),
                    textColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (isFa) "کل فروش سالانه" else "Annual Total Revenue",
                    value = "${formatMoney(netSales)} AFN",
                    subtitle = if (isFa) "خالص درآمد" else "Net Income",
                    containerColor = Color(0xFF16A34A),
                    textColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Monthly Breakdown List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFa) "تفکیک ماهانه سال $selectedYear" else "Monthly Breakdown for $selectedYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    monthlyBreakdown.forEach { (monthIdx, data) ->
                        val (count, revenue) = data
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthNames[monthIdx],
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${formatMoney(revenue)} AFN",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (revenue > 0) Color(0xFF16A34A) else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = if (isFa) "$count تراکنش" else "$count sales",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSalesHistoryTab(
    manualSales: List<Sale>,
    isFa: Boolean,
    viewModel: AppViewModel
) {
    if (manualSales.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isFa) "هیچ فروش دستی ثبت نشده است" else "No manual sales found.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(manualSales) { sale ->
                ManualSaleCardItem(sale = sale, isFa = isFa, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ManualSaleCardItem(
    sale: Sale,
    isFa: Boolean,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    var showVoidDialog by remember { mutableStateOf(false) }
    var voidReason by remember { mutableStateOf("") }

    val images by viewModel.saleRepo.getImagesForSale(sale.id).collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("manual_sale_card_${sale.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Invoice Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (sale.status == SaleStatus.ACTIVE) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (sale.status == SaleStatus.ACTIVE) (if (isFa) "فعال" else "Active") else (if (isFa) "باطل شده" else "Voided"),
                            color = if (sale.status == SaleStatus.ACTIVE) Color(0xFF15803D) else Color(0xFFB91C1C),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(sale.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(if (isFa) "مشتری:" else "Customer:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text(sale.customerName ?: if (isFa) "مشتری عمومی" else "General Customer", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isFa) "روش پرداخت:" else "Payment:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text(sale.paymentMethod, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (isFa) "مبلغ کل فاکتور:" else "Total Amount:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatMoney(sale.totalRevenue)} ${sale.currency}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF16A34A)
                )
            }

            if (sale.notes.isNotBlank()) {
                Text(
                    text = "${if (isFa) "یادداشت:" else "Note:"} ${sale.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )
            }

            // Attached Photos
            if (images.isNotEmpty()) {
                Text(
                    text = if (isFa) "تصاویر ضمیمه شده (${images.size}):" else "Attached Photos (${images.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(images) { img ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(img.localUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Attached photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "#${img.displayOrder + 1}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Void Action Button for Authorized Users
            if (sale.status == SaleStatus.ACTIVE && currentUser.role in listOf(UserRole.SUPER_ADMIN, UserRole.MANAGER)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = { showVoidDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFa) "ابطال این فروش" else "Void Sale")
                    }
                }
            }
        }
    }

    // Void Dialog
    if (showVoidDialog) {
        AlertDialog(
            onDismissRequest = { showVoidDialog = false },
            title = { Text(if (isFa) "ابطال فروش دستی" else "Void Manual Sale") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isFa) "دلیل ابطال این فاکتور را وارد نمایید. موجودی کالای انبار بازگردانده خواهد شد." else "Provide a reason for voiding this sale. Product stock will be restored.")
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text(if (isFa) "دلیل ابطال" else "Void Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isBlank()) {
                            Toast.makeText(context, if (isFa) "لطفاً دلیل ابطال را وارد کنید" else "Please enter a void reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.voidSale(sale.id, voidReason)
                        showVoidDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (isFa) "تایید ابطال" else "Confirm Void")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoidDialog = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
        }
    }
}

private fun formatMoney(amount: Double): String {
    val formatter = NumberFormat.getInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0
    return formatter.format(amount)
}
