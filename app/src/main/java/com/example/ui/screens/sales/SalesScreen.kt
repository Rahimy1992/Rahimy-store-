package com.example.ui.screens.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.data.local.entity.SaleStatus
import com.example.data.repository.SalesDateFilter
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.BluetoothThermalPrinterDialog
import com.example.util.ExcelCsvExporter
import com.example.util.PrintInvoiceManager
import com.example.ui.screens.sales.CustomerDebtLedgerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allSales by viewModel.allSales.collectAsState()
    val selectedFilter by viewModel.salesDateFilter.collectAsState()
    val selectedYear by viewModel.selectedSalesYear.collectAsState()
    val selectedMonth by viewModel.selectedSalesMonth.collectAsState()
    val multipleMonths by viewModel.multipleSelectedMonths.collectAsState()
    val selectedSaleIds by viewModel.selectedSaleIds.collectAsState()
    val localization by viewModel.localization.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = POS Register / Cart, 1 = Sales History, 2 = Debt Ledger
    var saleToVoid by remember { mutableStateOf<Sale?>(null) }
    var showBulkVoidDialog by remember { mutableStateOf(false) }
    var voidReason by remember { mutableStateOf("") }
    var viewedSaleDetails by remember { mutableStateOf<Pair<Sale, List<SaleItem>>?>(null) }

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showBluetoothPrinterDialog by remember { mutableStateOf(false) }
    var printingSaleAndItems by remember { mutableStateOf<Pair<Sale, List<SaleItem>>?>(null) }
    var checkoutPaymentMethod by remember { mutableStateOf("CASH") } // CASH, CARD, CREDIT_LEDGER
    var checkoutCustName by remember { mutableStateOf("") }
    var checkoutCustPhone by remember { mutableStateOf("") }
    var checkoutNotes by remember { mutableStateOf("") }

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    val monthsListFa = listOf("حمل / جدی", "ثور / دلو", "جوزا / حوت", "سرطان", "اسد", "سنبله", "میزان", "عقرب", "قوس", "جدی", "دلو", "حوت")
    val monthsListEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthsList = if (isFa) monthsListFa else monthsListEn

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header & Tab Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "فروش و صندوق POS" else "Sales & POS Register",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    val roleNameFa = when (currentUser.role) {
                        com.example.data.local.entity.UserRole.SUPER_ADMIN -> "مدیر کل"
                        com.example.data.local.entity.UserRole.MANAGER -> "مدیر سیستم"
                        com.example.data.local.entity.UserRole.EMPLOYEE -> "تحویل‌دار"
                        com.example.data.local.entity.UserRole.CUSTOMER -> "مشتری"
                        else -> "مشاهده‌کننده"
                    }
                    Text(
                        text = if (isFa) "تحویل‌دار: ${currentUser.displayName} ($roleNameFa)" else "Cashier: ${currentUser.displayName} (${currentUser.role.name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.testTag("btn_camera_scan_pos")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val file = ExcelCsvExporter.exportSalesToCsv(context, allSales, localization)
                            if (file != null) {
                                ExcelCsvExporter.shareCsvFile(context, file, if (isFa) "خروجی گزارش فروشات" else "Sales History CSV")
                            }
                        },
                        modifier = Modifier.testTag("btn_export_sales_csv")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Sales CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "سبد (${cart.sumOf { it.quantity }})" else "Cart (${cart.sumOf { it.quantity }})")
                        }
                    },
                    modifier = Modifier.testTag("tab_pos_register")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "تاریخچه (${allSales.size})" else "History (${allSales.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_sales_history")
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "دفترچه نسیه" else "Debt Ledger")
                        }
                    },
                    modifier = Modifier.testTag("tab_debt_ledger")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeTab == 0) {
                // ==================== TAB 0: POS REGISTER & CART ====================
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.AddShoppingCart,
                                contentDescription = "Empty Cart",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(if (isFa) "سبد خرید فعلاً خالی است" else "Cart is currently empty", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isFa) "به تب اجناس بروید و آیکون سبد را فشار دهید تا اجناس اضافه شوند." else "Go to Products tab and tap the cart icon to add items.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "SKU: ${item.product.sku} • ${LocalizationManager.formatDualCurrency(item.product.sellingPrice, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity - 1) },
                                                modifier = Modifier.size(32.dp).testTag("btn_cart_dec_${item.product.id}")
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                            }

                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )

                                            IconButton(
                                                onClick = { viewModel.updateCartQuantity(item.product.id, item.quantity + 1) },
                                                modifier = Modifier.size(32.dp).testTag("btn_cart_inc_${item.product.id}")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Cart Summary Card
                        val totalUsd = cart.sumOf { it.quantity * it.product.sellingPrice }
                        val totalCostUsd = cart.sumOf { it.quantity * it.product.costPrice }
                        val profitUsd = totalUsd - totalCostUsd

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isFa) "مجموع کل قابل پرداخت:" else "Total Amount:", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = LocalizationManager.formatDualCurrency(
                                            totalUsd,
                                            localization.selectedCurrencyCode,
                                            localization.selectedCurrencySymbol,
                                            localization.exchangeRateToUSD,
                                            localization.markupPercent
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (isFa) "سود تخمینی این فاکتور: $${String.format("%.2f", profitUsd)} (${String.format("%.1f", if (totalUsd > 0) (profitUsd / totalUsd) * 100 else 0.0)}%)" else "Estimated Gross Profit: $${String.format("%.2f", profitUsd)} (${String.format("%.1f", if (totalUsd > 0) (profitUsd / totalUsd) * 100 else 0.0)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearCart() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isFa) "پاکسازی" else "Clear")
                                    }

                                    Button(
                                        onClick = { showCheckoutDialog = true },
                                        modifier = Modifier.weight(2f).testTag("btn_complete_checkout")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isFa) "تکمیل و ثبت فروش" else "Complete Sale")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (activeTab == 1) {
                // ==================== TAB 1: SALES HISTORY & EXTENSIVE FILTERS ====================
                Column(modifier = Modifier.weight(1f)) {
                    // Date Filter Chips (Requirement 4: Today, Week, Month, Year, etc.)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SalesDateFilter.values()) { filter ->
                            val isSelected = selectedFilter == filter
                            val label = when (filter) {
                                SalesDateFilter.TODAY -> if (isFa) "امروز" else "Today"
                                SalesDateFilter.THIS_WEEK -> if (isFa) "این هفته" else "This Week"
                                SalesDateFilter.THIS_MONTH -> if (isFa) "این ماه" else "This Month"
                                SalesDateFilter.THIS_YEAR -> if (isFa) "امسال" else "This Year"
                                SalesDateFilter.SELECTED_MONTH -> if (isFa) "انتخاب ماه" else "Select Month"
                                SalesDateFilter.MULTIPLE_MONTHS -> if (isFa) "چندین ماه" else "Multi Months"
                                SalesDateFilter.SELECTED_YEAR -> if (isFa) "انتخاب سال" else "Select Year"
                                SalesDateFilter.CUSTOM_RANGE -> if (isFa) "بازه دلخواه" else "Custom Range"
                                SalesDateFilter.ALL_TIME -> if (isFa) "همه زمان‌ها" else "All Time"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSalesDateFilter(filter) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("filter_sales_${filter.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary Sub-filter controls for selected month/multi-month/year
                    if (selectedFilter == SalesDateFilter.SELECTED_MONTH) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(monthsList.indices.toList()) { mIndex ->
                                FilterChip(
                                    selected = selectedMonth == mIndex,
                                    onClick = { viewModel.setSelectedSalesMonth(mIndex) },
                                    label = { Text(monthsList[mIndex]) }
                                )
                            }
                        }
                    } else if (selectedFilter == SalesDateFilter.MULTIPLE_MONTHS) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(monthsList.indices.toList()) { mIndex ->
                                val isChecked = multipleMonths.contains(mIndex)
                                FilterChip(
                                    selected = isChecked,
                                    onClick = { viewModel.toggleMultipleMonth(mIndex) },
                                    label = { Text(monthsList[mIndex]) }
                                )
                            }
                        }
                    } else if (selectedFilter == SalesDateFilter.SELECTED_YEAR) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2024, 2025, 2026, 2027).forEach { yr ->
                                FilterChip(
                                    selected = selectedYear == yr,
                                    onClick = { viewModel.setSelectedSalesYear(yr) },
                                    label = { Text("$yr") }
                                )
                            }
                        }
                    }

                    // Bulk Void Selection Bar (Requirement 4: "Select multiple sales, VOID/CANCEL rather than destructive deletion")
                    AnimatedVisibility(visible = selectedSaleIds.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedSaleIds.size} sales selected",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Button(
                                    onClick = { showBulkVoidDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("btn_bulk_void")
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Void Selected")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sales List
                    if (allSales.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allSales, key = { it.id }) { sale ->
                                val isSelected = selectedSaleIds.contains(sale.id)
                                val isVoid = sale.status == SaleStatus.VOID || sale.status == SaleStatus.CANCELLED

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                val details = viewModel.saleRepo.getSaleDetails(sale.id)
                                                if (details.first != null) {
                                                    viewedSaleDetails = Pair(details.first!!, details.second)
                                                }
                                            }
                                        }
                                        .testTag("sale_card_${sale.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isVoid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleSaleSelection(sale.id) },
                                            enabled = !isVoid,
                                            modifier = Modifier.testTag("checkbox_sale_${sale.id}")
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = sale.invoiceNumber,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = if (isVoid) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = sale.status.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isVoid) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Cashier: ${sale.cashierName} • ${LocalizationManager.formatDateTime(sale.timestamp, timeZoneId = localization.currentTimeZoneId)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (isVoid && !sale.voidReason.isNullOrBlank()) {
                                                Text(
                                                    text = "Void Reason: ${sale.voidReason}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = LocalizationManager.formatDualCurrency(
                                                    sale.totalRevenue,
                                                    localization.selectedCurrencyCode,
                                                    localization.selectedCurrencySymbol,
                                                    localization.exchangeRateToUSD,
                                                    localization.markupPercent
                                                ),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isVoid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val items = viewModel.saleRepo.getSaleDetails(sale.id).second
                                                        PrintInvoiceManager.printInvoiceHtml(
                                                            context = context,
                                                            sale = sale,
                                                            items = items,
                                                            localization = localization
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = "Print Invoice", tint = MaterialTheme.colorScheme.primary)
                                            }

                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val items = viewModel.saleRepo.getSaleDetails(sale.id).second
                                                        val text = PrintInvoiceManager.generateReceiptText(sale, items, localization = localization)
                                                        PrintInvoiceManager.shareReceiptText(context, text)
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Share Invoice", tint = MaterialTheme.colorScheme.primary)
                                            }

                                            // Void button for active sales
                                            if (!isVoid) {
                                                IconButton(
                                                    onClick = {
                                                        saleToVoid = sale
                                                        voidReason = ""
                                                    },
                                                    modifier = Modifier.testTag("btn_void_sale_${sale.id}")
                                                ) {
                                                    Icon(Icons.Default.Cancel, contentDescription = "Void Sale", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== TAB 2: CUSTOMER DEBT LEDGER ====================
                CustomerDebtLedgerScreen(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialog: Void Single Sale (Requirement 4)
    if (saleToVoid != null) {
        AlertDialog(
            onDismissRequest = { saleToVoid = null },
            title = { Text("Void Sale ${saleToVoid?.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voiding this sale will restore inventory stock and record this transaction as VOID in the financial log.")
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Void Reason (Required) *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_void_reason"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isNotBlank()) {
                            viewModel.voidSale(saleToVoid!!.id, voidReason.trim())
                            saleToVoid = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_void")
                ) {
                    Text("Confirm Void")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { saleToVoid = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Bulk Void Selected Sales
    if (showBulkVoidDialog) {
        AlertDialog(
            onDismissRequest = { showBulkVoidDialog = false },
            title = { Text("Bulk Void ${selectedSaleIds.size} Sales") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("All ${selectedSaleIds.size} selected sales will be marked VOID. All product stocks will be restored.")
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Bulk Void Reason *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_bulk_void_reason"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voidReason.isNotBlank()) {
                            viewModel.bulkVoidSelectedSales(voidReason.trim())
                            showBulkVoidDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Void All Selected")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBulkVoidDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Sale Details with Immutable Cost & Price Snapshots (Requirement 13)
    if (viewedSaleDetails != null) {
        val (sale, items) = viewedSaleDetails!!
        Dialog(onDismissRequest = { viewedSaleDetails = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sale.invoiceNumber, style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { viewedSaleDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text("Status: ${sale.status.name} • Cashier: ${sale.cashierName}")
                    Text("Date: ${LocalizationManager.formatDateTime(sale.timestamp, timeZoneId = localization.currentTimeZoneId)}")

                    HorizontalDivider()

                    Text("Purchased Items (Preserved Snapshot Prices):", style = MaterialTheme.typography.titleSmall)

                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        "Qty: ${item.quantity} × $${item.unitPriceSnapshot} (Snapshot Cost: ${if (item.unitCostSnapshot != null) "$${item.unitCostSnapshot}" else "N/A"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    LocalizationManager.formatDualCurrency(item.subtotal, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Profit:", fontWeight = FontWeight.Bold)
                        val profitDisplay = if (sale.grossProfit != null) {
                            "$${String.format("%.2f", sale.grossProfit)} (${String.format("%.1f", sale.profitMarginPercent ?: 0.0)}%)"
                        } else {
                            "N/A (Cost unavailable)"
                        }
                        Text(profitDisplay, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Paid:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            LocalizationManager.formatDualCurrency(sale.totalRevenue, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                printingSaleAndItems = sale to items
                                showBluetoothPrinterDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("btn_thermal_print_modal")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFa) "چاپ مستقیم با پرینتر حرارتی بلوتوث 🖨️" else "Bluetooth Thermal Print 🖨️")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    PrintInvoiceManager.printInvoiceHtml(
                                        context = context,
                                        sale = sale,
                                        items = items,
                                        localization = localization
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isFa) "چاپ استاندارد / PDF" else "Standard PDF Print")
                            }

                            OutlinedButton(
                                onClick = {
                                    val text = PrintInvoiceManager.generateReceiptText(sale, items, localization = localization)
                                    PrintInvoiceManager.shareReceiptText(context, text)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isFa) "ارسال فاکتور" else "Share Invoice")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Barcode Scanner for POS
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            isFa = isFa,
            onBarcodeScanned = { barcode ->
                val matched = allProducts.find { it.barcode == barcode || it.sku == barcode }
                if (matched != null) {
                    viewModel.addToCart(matched)
                } else {
                    viewModel.setSearchQuery(barcode)
                }
                showBarcodeScanner = false
            },
            onDismissRequest = { showBarcodeScanner = false }
        )
    }

    // Dialog: Enhanced Checkout & Payment Modal
    if (showCheckoutDialog) {
        val totalUsd = cart.sumOf { it.quantity * it.product.sellingPrice }

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text(if (isFa) "تکمیل و ثبت نهایی فروش" else "Complete Sale & Checkout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${if (isFa) "مجموع قابل پرداخت:" else "Total Amount:"} ${LocalizationManager.formatDualCurrency(totalUsd, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (isFa) "انتخاب روش پرداخت:" else "Select Payment Method:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = checkoutPaymentMethod == "CASH",
                            onClick = { checkoutPaymentMethod = "CASH" },
                            label = { Text(if (isFa) "نقدی" else "Cash") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = checkoutPaymentMethod == "CARD",
                            onClick = { checkoutPaymentMethod = "CARD" },
                            label = { Text(if (isFa) "کارت / حواله" else "Card") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = checkoutPaymentMethod == "CREDIT_LEDGER",
                            onClick = { checkoutPaymentMethod = "CREDIT_LEDGER" },
                            label = { Text(if (isFa) "فروش نسیه" else "Credit Ledger") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (checkoutPaymentMethod == "CREDIT_LEDGER") {
                        OutlinedTextField(
                            value = checkoutCustName,
                            onValueChange = { checkoutCustName = it },
                            label = { Text(if (isFa) "نام مشتری بدهکار *" else "Customer Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkoutCustPhone,
                            onValueChange = { checkoutCustPhone = it },
                            label = { Text(if (isFa) "شماره تماس" else "Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = checkoutNotes,
                        onValueChange = { checkoutNotes = it },
                        label = { Text(if (isFa) "توضیحات و بابت" else "Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.checkout(
                            paymentMethod = checkoutPaymentMethod,
                            notes = checkoutNotes,
                            customerName = checkoutCustName,
                            customerPhone = checkoutCustPhone
                        )
                        showCheckoutDialog = false
                        checkoutCustName = ""
                        checkoutCustPhone = ""
                        checkoutNotes = ""
                    }
                ) {
                    Text(if (isFa) "ثبت و صدور فاکتور" else "Confirm Checkout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }

    // Bluetooth Thermal Printer Dialog
    if (showBluetoothPrinterDialog && printingSaleAndItems != null) {
        val (sale, items) = printingSaleAndItems!!
        BluetoothThermalPrinterDialog(
            sale = sale,
            items = items,
            localization = localization,
            onDismiss = {
                showBluetoothPrinterDialog = false
                printingSaleAndItems = null
            }
        )
    }
}
