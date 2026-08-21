package com.example.ui.screens.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.B2bInvoiceEntity
import com.example.data.local.entity.B2bOrderEntity
import com.example.data.local.entity.B2bPaymentEntity
import com.example.data.local.entity.B2bQuotationEntity
import com.example.data.local.entity.BusinessCustomerEntity
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.LocalizationState
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch

enum class B2bSection {
    CUSTOMERS,
    DASHBOARD,
    PRICES,
    ORDERS,
    QUOTATIONS,
    INVOICES,
    PAYMENTS,
    CREDIT_STATEMENTS,
    RETURNS,
    SUPPORT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2bWholesaleDashboardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val localization by viewModel.localization.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val businessCustomers by viewModel.businessCustomers.collectAsState()
    val b2bQuotations by viewModel.b2bQuotations.collectAsState()
    val b2bOrders by viewModel.b2bOrders.collectAsState()
    val b2bInvoices by viewModel.b2bInvoices.collectAsState()
    val b2bPayments by viewModel.b2bPayments.collectAsState()
    val b2bReturns by viewModel.b2bReturns.collectAsState()

    var activeSection by remember { mutableStateOf(B2bSection.DASHBOARD) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showCreateOrderDialog by remember { mutableStateOf(false) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }

    val isRtl = localization.isRtl
    val currencySymbol = localization.selectedCurrencySymbol

    val isManagerOrAdmin = currentUser?.role?.name in listOf("MANAGER", "SUPER_ADMIN")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LocalizationManager.getString("b2b_title", localization.currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rahimy Smart B2B Wholesale Engine",
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
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF00897B).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00897B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "B2B Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00897B)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal Navigation Chips for B2B Subsections
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(B2bSection.values()) { section ->
                    val isSelected = activeSection == section
                    val labelKey = when (section) {
                        B2bSection.CUSTOMERS -> "b2b_customers"
                        B2bSection.DASHBOARD -> "b2b_reports"
                        B2bSection.PRICES -> "b2b_prices"
                        B2bSection.ORDERS -> "b2b_orders"
                        B2bSection.QUOTATIONS -> "b2b_quotations"
                        B2bSection.INVOICES -> "b2b_invoices"
                        B2bSection.PAYMENTS -> "b2b_payments"
                        B2bSection.CREDIT_STATEMENTS -> "b2b_credit_accounts"
                        B2bSection.RETURNS -> "b2b_discounts"
                        B2bSection.SUPPORT -> "b2b_support"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeSection = section },
                        label = {
                            Text(
                                text = LocalizationManager.getString(labelKey, localization.currentLanguage),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            val icon = when (section) {
                                B2bSection.CUSTOMERS -> Icons.Default.Business
                                B2bSection.DASHBOARD -> Icons.Default.Assessment
                                B2bSection.PRICES -> Icons.Default.Sell
                                B2bSection.ORDERS -> Icons.Default.ShoppingCart
                                B2bSection.QUOTATIONS -> Icons.Default.ReceiptLong
                                B2bSection.INVOICES -> Icons.Default.RequestQuote
                                B2bSection.PAYMENTS -> Icons.Default.Payments
                                B2bSection.CREDIT_STATEMENTS -> Icons.Default.AccountBalance
                                B2bSection.RETURNS -> Icons.Default.AssignmentReturn
                                B2bSection.SUPPORT -> Icons.Default.SupportAgent
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            HorizontalDivider()

            // Main Subsection View
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeSection) {
                    B2bSection.DASHBOARD -> B2bMetricsDashboardView(
                        customers = businessCustomers,
                        orders = b2bOrders,
                        invoices = b2bInvoices,
                        payments = b2bPayments,
                        locState = localization,
                        onNewOrderClick = { showCreateOrderDialog = true },
                        onNewCustomerClick = { showAddCustomerDialog = true }
                    )
                    B2bSection.CUSTOMERS -> BusinessCustomersListView(
                        customers = businessCustomers,
                        locState = localization,
                        onAddCustomer = { showAddCustomerDialog = true }
                    )
                    B2bSection.PRICES -> WholesalePricingView(viewModel = viewModel)
                    B2bSection.ORDERS -> B2bOrdersListView(
                        orders = b2bOrders,
                        locState = localization,
                        onCreateOrderClick = { showCreateOrderDialog = true }
                    )
                    B2bSection.QUOTATIONS -> B2bQuotationsView(
                        quotations = b2bQuotations,
                        viewModel = viewModel,
                        locState = localization
                    )
                    B2bSection.INVOICES -> B2bInvoicesListView(
                        invoices = b2bInvoices,
                        locState = localization,
                        onRecordPaymentClick = { showRecordPaymentDialog = true }
                    )
                    B2bSection.PAYMENTS -> B2bPaymentsListView(
                        payments = b2bPayments,
                        locState = localization,
                        onRecordPaymentClick = { showRecordPaymentDialog = true }
                    )
                    B2bSection.CREDIT_STATEMENTS -> CreditAccountsStatementView(
                        customers = businessCustomers,
                        locState = localization
                    )
                    B2bSection.RETURNS -> B2bReturnsView(
                        returns = b2bReturns,
                        locState = localization
                    )
                    B2bSection.SUPPORT -> B2bSupportIntegrationView()
                }
            }
        }
    }

    // Dialogs
    if (showAddCustomerDialog) {
        AddBusinessCustomerDialog(
            viewModel = viewModel,
            onDismiss = { showAddCustomerDialog = false }
        )
    }

    if (showCreateOrderDialog) {
        CreateB2bOrderDialog(
            viewModel = viewModel,
            customers = businessCustomers,
            onDismiss = { showCreateOrderDialog = false }
        )
    }

    if (showRecordPaymentDialog) {
        RecordB2bPaymentDialog(
            viewModel = viewModel,
            customers = businessCustomers,
            invoices = b2bInvoices.filter { it.paymentStatus != "PAID" },
            onDismiss = { showRecordPaymentDialog = false }
        )
    }
}

@Composable
fun B2bMetricsDashboardView(
    customers: List<BusinessCustomerEntity>,
    orders: List<B2bOrderEntity>,
    invoices: List<B2bInvoiceEntity>,
    payments: List<B2bPaymentEntity>,
    locState: com.example.domain.localization.LocalizationState,
    onNewOrderClick: () -> Unit,
    onNewCustomerClick: () -> Unit
) {
    val totalSalesUsd = orders.filter { it.orderStatus != "CANCELLED" }.sumOf { it.totalUsd }
    val totalReceivablesUsd = customers.sumOf { it.currentBalance }
    val totalPaidUsd = payments.sumOf { it.amountUsd }
    val pendingOrdersCount = orders.count { it.orderStatus in listOf("PENDING_APPROVAL", "DRAFT", "CONFIRMED") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خلاصه وضعیت تجارت عمده",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNewOrderClick, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سفارش جدید")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("مجموع فروش B2B", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = LocalizationManager.formatDualCurrency(
                                totalSalesUsd,
                                locState.selectedCurrencyCode,
                                locState.selectedCurrencySymbol,
                                locState.exchangeRateToUSD,
                                locState.markupPercent
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                ElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("بدهی کل مشتریان", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = LocalizationManager.formatDualCurrency(
                                totalReceivablesUsd,
                                locState.selectedCurrencyCode,
                                locState.selectedCurrencySymbol,
                                locState.exchangeRateToUSD,
                                locState.markupPercent
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF1565C0))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("مشتریان فعال B2B", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${customers.size} کسب‌وکار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ElevatedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color(0xFFEF6C00))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("سفارش‌های در حال پردازش", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "$pendingOrdersCount سفارش",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF6C00)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "مشتریان عمده برتر (Top B2B Accounts)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(customers.take(5)) { customer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = customer.businessName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(customer.businessName, fontWeight = FontWeight.Bold)
                            Text("${customer.contactPerson} • ${customer.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "سقف اعتبار: $${customer.creditLimit}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "بدهی فعلی: $${customer.currentBalance}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (customer.currentBalance > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessCustomersListView(
    customers: List<BusinessCustomerEntity>,
    locState: com.example.domain.localization.LocalizationState,
    onAddCustomer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حساب‌های تجاری و مشتریان عمده",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onAddCustomer, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.AddBusiness, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("افزودن شرکت")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (customers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هیچ شرکت یا مشتری عمده‌ای ثبت نشده است.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(customers) { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(c.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (c.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = c.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (c.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("مالک: ${c.ownerName} | مسئول تماس: ${c.contactPerson}", style = MaterialTheme.typography.bodyMedium)
                            Text("تلفن: ${c.phone} | ایمیل: ${c.email}", style = MaterialTheme.typography.bodySmall)
                            Text("آدرس: ${c.address}, ${c.city}", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("شرایط پرداخت: ${c.paymentTerms}", style = MaterialTheme.typography.labelMedium)
                                Text("سقف اعتبار: $${c.creditLimit}", style = MaterialTheme.typography.labelMedium)
                                Text("بدهی: $${c.currentBalance}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WholesalePricingView(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("تنظیمات قیمت‌گذاری عمده و پلکانی (Price Tiers)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("تعیین تخفیف و قیمت ویژه بر اساس تعداد سفارش (Retail, Wholesale, Distributor, VIP, Bulk)")
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("قوانین پیش‌فرض پلکان عمده‌فروشی", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• سفارش ۱ تا ۹ عدد: قیمت خرده‌فروشی (Retail Price)")
                Text("• سفارش ۱۰ تا ۴۹ عدد: ۱۲٪ تخفیف عمده (Wholesale Tier)")
                Text("• سفارش ۵۰ تا ۹۹ عدد: ۲۰٪ تخفیف توزیع‌کننده (Distributor Tier)")
                Text("• سفارش ۱۰۰ عدد به بالا: ۲۵٪ تخفیف خریدار عمده (Bulk Tier)")
            }
        }
    }
}

@Composable
fun B2bOrdersListView(
    orders: List<B2bOrderEntity>,
    locState: com.example.domain.localization.LocalizationState,
    onCreateOrderClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سفارش‌های عمده ثبت‌شده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onCreateOrderClick, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("سفارش جدید")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هیچ سفارش عمده‌ای موجود نیست.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(orders) { o ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(o.orderId, fontWeight = FontWeight.Bold)
                                Text(o.orderStatus, color = Color(0xFF00897B), fontWeight = FontWeight.SemiBold)
                            }
                            Text("خریدار: ${o.businessName}", style = MaterialTheme.typography.bodyMedium)
                            Text("آدرس تحویل: ${o.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                            Text("مبلغ کل: $${o.totalUsd} (${o.currency})", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun B2bQuotationsView(
    quotations: List<B2bQuotationEntity>,
    viewModel: AppViewModel,
    locState: com.example.domain.localization.LocalizationState
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("پیش‌فاکتورها (Proforma Invoices / Quotations)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (quotations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هیچ پیش‌فاکتوری ثبت نشده است.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quotations) { q ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(q.quotationId, fontWeight = FontWeight.Bold)
                                Text(q.status, color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
                            }
                            Text("مشتری: ${q.businessName}")
                            Text("مبلغ: $${q.totalUsd}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun B2bInvoicesListView(
    invoices: List<B2bInvoiceEntity>,
    locState: com.example.domain.localization.LocalizationState,
    onRecordPaymentClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("فاکتورهای صادرشده B2B", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onRecordPaymentClick, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ثبت دریافت وجه")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هیچ فاکتوری یافت نشد.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(invoices) { inv ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("فاکتور: ${inv.invoiceNumber}", fontWeight = FontWeight.Bold)
                                Text(inv.paymentStatus, color = if (inv.paymentStatus == "PAID") Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                            Text("خریدار: ${inv.businessName}")
                            Text("مبلغ فاکتور: $${inv.totalUsd} | دریافتی: $${inv.paidAmountUsd}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun B2bPaymentsListView(
    payments: List<com.example.data.local.entity.B2bPaymentEntity>,
    locState: com.example.domain.localization.LocalizationState,
    onRecordPaymentClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تاریخچه پرداخت‌ها و دریافتی‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onRecordPaymentClick, shape = RoundedCornerShape(12.dp)) {
                Text("ثبت پرداخت جدید")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (payments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("پرداختی ثبت نشده است.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(payments) { p ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("شناسه: ${p.paymentId}", fontWeight = FontWeight.Bold)
                                Text("$${p.amountUsd}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Text("روش: ${p.paymentMethod} | شماره پیگیری: ${p.referenceNumber}")
                            Text("دریافت‌کننده: ${p.receivedBy}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditAccountsStatementView(
    customers: List<BusinessCustomerEntity>,
    locState: com.example.domain.localization.LocalizationState
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("حساب‌های اعتباری و صورتحساب مشتریان (Customer Statement)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(customers) { c ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(c.businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("سقف اعتبار: $${c.creditLimit}")
                        Text("بدهی جاری: $${c.currentBalance}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Text("اعتبار باقی‌مانده: $${(c.creditLimit - c.currentBalance).coerceAtLeast(0.0)}", color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@Composable
fun B2bReturnsView(
    returns: List<com.example.data.local.entity.B2bReturnEntity>,
    locState: com.example.domain.localization.LocalizationState
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("مدیریت مرجوعی‌ها و تخفیف‌های ویژه (Returns)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (returns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("مرجوعی ثبت نشده است.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(returns) { r ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("مرجوعی: ${r.returnId}", fontWeight = FontWeight.Bold)
                            Text("علت: ${r.reason}")
                            Text("مبلغ استرداد: $${r.refundAmountUsd}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun B2bSupportIntegrationView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("پشتیبانی اختصاصی خریداران عمده B2B", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "ارتباط مستقیم با واحد فروش، پیگیری سفارشات عمده و استعلام قیمت تخصصی.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Dialogs
@Composable
fun AddBusinessCustomerDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("10000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن شرکت / مشتری عمده جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام شرکت / کسب‌وکار") })
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("نام مالک") })
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("مسئول ارتباطات") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("شماره تماس") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("ایمیل") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("آدرس") })
                OutlinedTextField(value = creditLimit, onValueChange = { creditLimit = it }, label = { Text("سقف اعتبار (USD)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        coroutineScope.launch {
                            val newCustomer = BusinessCustomerEntity(
                                businessId = "BUS-" + (100..999).random(),
                                businessName = name,
                                ownerName = owner.ifBlank { "N/A" },
                                contactPerson = contact.ifBlank { name },
                                phone = phone.ifBlank { "+93700000000" },
                                email = email.ifBlank { "business@mail.com" },
                                address = address.ifBlank { "Kabul" },
                                city = "Kabul",
                                customerCode = "CUST-" + (1000..9999).random(),
                                creditLimit = creditLimit.toDoubleOrNull() ?: 5000.0
                            )
                            viewModel.b2bRepo.saveBusinessCustomer(newCustomer)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text("ثبت شرکت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun CreateB2bOrderDialog(
    viewModel: AppViewModel,
    customers: List<BusinessCustomerEntity>,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var address by remember { mutableStateOf("Shar-e-Naw, Kabul") }
    var qtyText by remember { mutableStateOf("20") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایجاد سفارش عمده جدید (B2B Bulk Order)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("انتخاب خریدار عمده:", fontWeight = FontWeight.Bold)
                customers.forEach { customer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomer = customer }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCustomer?.businessId == customer.businessId,
                            onClick = { selectedCustomer = customer }
                        )
                        Text("${customer.businessName} (اعتبار باقی‌مانده: $${customer.creditLimit - customer.currentBalance})")
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("تعداد سفارش کالا (پلکان تخفیف)") }
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("آدرس تحویل") }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCustomer != null && !isSubmitting,
                onClick = {
                    val customer = selectedCustomer ?: return@Button
                    val qty = qtyText.toIntOrNull() ?: 10
                    isSubmitting = true
                    coroutineScope.launch {
                        val priceAndTier = viewModel.b2bRepo.calculateWholesaleUnitPrice(
                            productId = 1L,
                            quantity = qty,
                            businessId = customer.businessId
                        )
                        val price = priceAndTier.first
                        val tier = priceAndTier.second
                        val item = com.example.data.local.entity.B2bOrderItemEntity(
                            itemId = "ITEM-1",
                            orderId = "",
                            productId = 1L,
                            productName = "Smart Cash Register POS Terminal",
                            sku = "POS-SYS-101",
                            quantity = qty,
                            unitPriceUsd = price,
                            subtotalUsd = price * qty
                        )

                        viewModel.b2bRepo.createDirectOrder(
                            businessId = customer.businessId,
                            items = listOf(item),
                            deliveryAddress = address
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("تأیید و صدور سفارش")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun RecordB2bPaymentDialog(
    viewModel: AppViewModel,
    customers: List<BusinessCustomerEntity>,
    invoices: List<B2bInvoiceEntity>,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedInvoice by remember { mutableStateOf(invoices.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var refNo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت دریافت وجه / پرداخت B2B") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("انتخاب فاکتور:", fontWeight = FontWeight.Bold)
                if (invoices.isEmpty()) {
                    Text("هیچ فاکتور تسویه‌نشده‌ای وجود ندارد.")
                } else {
                    invoices.forEach { inv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedInvoice = inv
                                    amountText = (inv.totalUsd - inv.paidAmountUsd).toString()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedInvoice?.invoiceId == inv.invoiceId,
                                onClick = {
                                    selectedInvoice = inv
                                    amountText = (inv.totalUsd - inv.paidAmountUsd).toString()
                                }
                            )
                            Text("${inv.invoiceNumber} - ${inv.businessName} (مانده: $${inv.totalUsd - inv.paidAmountUsd})")
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ دریافتی (USD)") }
                )

                OutlinedTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    label = { Text("شماره پیگیری یا فیش بانکی") }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedInvoice != null && amountText.isNotBlank(),
                onClick = {
                    val inv = selectedInvoice ?: return@Button
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    coroutineScope.launch {
                        viewModel.b2bRepo.recordPayment(
                            businessId = inv.businessId,
                            invoiceId = inv.invoiceId,
                            amountUsd = amt,
                            referenceNumber = refNo
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("ثبت و بروزرسانی حساب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
