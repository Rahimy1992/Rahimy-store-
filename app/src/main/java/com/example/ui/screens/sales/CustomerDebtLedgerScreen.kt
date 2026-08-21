package com.example.ui.screens.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CustomerDebt
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel
import com.example.util.ExcelCsvExporter
import com.example.util.PrintInvoiceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerDebtLedgerScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localization by viewModel.localization.collectAsState()
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    val debts by viewModel.allDebts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0 = Active Debts, 1 = All, 2 = Settled

    var debtToPay by remember { mutableStateOf<CustomerDebt?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var paymentNotesInput by remember { mutableStateOf("") }

    var showAddManualDebtDialog by remember { mutableStateOf(false) }
    var newCustName by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }
    var newCustAmount by remember { mutableStateOf("") }
    var newCustNotes by remember { mutableStateOf("") }

    val filteredDebts = remember(debts, searchQuery, selectedFilter) {
        debts.filter { d ->
            val matchesQuery = searchQuery.isBlank() ||
                    d.customerName.contains(searchQuery, ignoreCase = true) ||
                    d.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                    d.saleInvoiceNumber.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                0 -> !d.isSettled // Active debts
                2 -> d.isSettled  // Settled
                else -> true      // All
            }
            matchesQuery && matchesFilter
        }
    }

    val totalActiveDebtUsd = remember(debts) {
        debts.filter { !it.isSettled }.sumOf { it.totalDebtUsd }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddManualDebtDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_debt")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFa) "دفترچه نسیه و حساب مشتریان" else "Customer Debt & Credit Ledger",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = if (isFa) "مدیریت طلبکاری‌ها، ثبت دریافتی و صورت‌حساب" else "Track customer credits, payments & share balance statements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        val file = ExcelCsvExporter.exportDebtsToCsv(context, debts, localization)
                        if (file != null) {
                            ExcelCsvExporter.shareCsvFile(
                                context,
                                file,
                                if (isFa) "خروجی اکسل بدهی مشتریان" else "Export Customer Debts CSV"
                            )
                        }
                    },
                    modifier = Modifier.testTag("btn_export_debts_csv")
                ) {
                    Text(if (isFa) "خروجی اکسل (CSV)" else "Export CSV")
                }
            }

            // Highlighting Total Debt Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "مجموع کل طلبکاری نسیه بازار:" else "Total Outstanding Credit:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = LocalizationManager.formatDualCurrency(
                                totalActiveDebtUsd,
                                localization.selectedCurrencyCode,
                                localization.selectedCurrencySymbol,
                                localization.exchangeRateToUSD,
                                localization.markupPercent
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Search Bar & Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isFa) "جستجوی نام مشتری یا شماره تماس..." else "Search customer or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_debt_search"),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text(if (isFa) "بدهکاران فعال (${debts.count { !it.isSettled }})" else "Active Debts") }
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text(if (isFa) "همه (${debts.size})" else "All") }
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text(if (isFa) "تسویه شده (${debts.count { it.isSettled }})" else "Settled") }
                )
            }

            // Debts List
            if (filteredDebts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFa) "هیچ حساب نسیه‌ای یافت نشد." else "No debt record found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDebts, key = { it.id }) { debt ->
                        DebtCardItem(
                            debt = debt,
                            isFa = isFa,
                            localization = localization,
                            onPayClick = {
                                debtToPay = debt
                                paymentAmountInput = debt.totalDebtUsd.toString()
                                paymentNotesInput = ""
                            },
                            onShareClick = {
                                val statement = PrintInvoiceManager.generateReceiptText(
                                    sale = com.example.data.local.entity.Sale(
                                        invoiceNumber = debt.saleInvoiceNumber.ifBlank { "DEBT-${debt.id}" },
                                        cashierId = 1L,
                                        cashierName = "Store Ledger",
                                        totalRevenue = debt.totalDebtUsd
                                    ),
                                    items = emptyList(),
                                    customerName = "${debt.customerName} (${debt.phoneNumber})",
                                    paymentType = if (isFa) "حساب نسیه / بدهکاری" else "Credit Ledger",
                                    localization = localization
                                )
                                PrintInvoiceManager.shareReceiptText(context, statement)
                            },
                            onDeleteClick = {
                                viewModel.deleteCustomerDebt(debt)
                            }
                        )
                    }
                }
            }
        }
    }

    // Payment Dialog
    debtToPay?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToPay = null },
            title = {
                Text(if (isFa) "ثبت دریافت و تسویه بدهی" else "Record Payment")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isFa) "مشتری: ${debt.customerName}" else "Customer: ${debt.customerName}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isFa) "کل بدهی فعلی: ${LocalizationManager.formatDualCurrency(debt.totalDebtUsd, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent)}"
                        else "Current Debt: $${debt.totalDebtUsd}",
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text(if (isFa) "مبلغ پرداختی (USD $)" else "Payment Amount (USD)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = paymentNotesInput,
                        onValueChange = { paymentNotesInput = it },
                        label = { Text(if (isFa) "یادداشت / توضیحات" else "Notes / Reference") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.recordDebtPayment(debt.id, amount, paymentNotesInput)
                            debtToPay = null
                        }
                    }
                ) {
                    Text(if (isFa) "تایید و ثبت دریافت" else "Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToPay = null }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }

    // Add Manual Debt Dialog
    if (showAddManualDebtDialog) {
        AlertDialog(
            onDismissRequest = { showAddManualDebtDialog = false },
            title = { Text(if (isFa) "ثبت بدهی جدید دستی" else "Add New Credit Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCustName,
                        onValueChange = { newCustName = it },
                        label = { Text(if (isFa) "نام و تخلص مشتری *" else "Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text(if (isFa) "شماره تماس" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustAmount,
                        onValueChange = { newCustAmount = it },
                        label = { Text(if (isFa) "مبلغ بدهی (USD $) *" else "Debt Amount (USD) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCustNotes,
                        onValueChange = { newCustNotes = it },
                        label = { Text(if (isFa) "توضیحات و بابت" else "Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = newCustAmount.toDoubleOrNull() ?: 0.0
                        if (newCustName.isNotBlank() && amt > 0) {
                            viewModel.recordCreditSale(
                                customerName = newCustName,
                                phoneNumber = newCustPhone,
                                saleAmountUsd = amt,
                                invoiceNumber = "MANUAL-${System.currentTimeMillis().toString().takeLast(4)}",
                                notes = newCustNotes
                            )
                            newCustName = ""
                            newCustPhone = ""
                            newCustAmount = ""
                            newCustNotes = ""
                            showAddManualDebtDialog = false
                        }
                    }
                ) {
                    Text(if (isFa) "ثبت در دفترچه" else "Save Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDebtDialog = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun DebtCardItem(
    debt: CustomerDebt,
    isFa: Boolean,
    localization: com.example.domain.localization.LocalizationState,
    onPayClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(debt.updatedAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (debt.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = debt.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (debt.isSettled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (debt.isSettled) (if (isFa) "تسویه شده" else "Settled") else (if (isFa) "بدهکار" else "Outstanding"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (debt.isSettled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (debt.phoneNumber.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(debt.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFa) "باقیمانده بدهی:" else "Balance:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = LocalizationManager.formatDualCurrency(
                            debt.totalDebtUsd,
                            localization.selectedCurrencyCode,
                            localization.selectedCurrencySymbol,
                            localization.exchangeRateToUSD,
                            localization.markupPercent
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (debt.isSettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (debt.notes.isNotBlank()) {
                Text(
                    text = "${if (isFa) "ملاحظات:" else "Notes:"} ${debt.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShareClick) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }

                if (!debt.isSettled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = onPayClick) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "دریافت بدهی" else "Record Pay")
                    }
                }
            }
        }
    }
}
