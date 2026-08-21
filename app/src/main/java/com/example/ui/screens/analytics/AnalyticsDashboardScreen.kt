package com.example.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SalesDateFilter
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel

import androidx.compose.ui.platform.LocalContext
import com.example.util.ExcelCsvExporter

@Composable
fun AnalyticsDashboardScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics by viewModel.financialMetrics.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val localization by viewModel.localization.collectAsState()
    val selectedFilter by viewModel.salesDateFilter.collectAsState()

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    LaunchedEffect(Unit) {
        viewModel.refreshFinancialMetrics()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isFa) "گزارش‌ها و تحلیل مالی" else "Financial Analytics",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (isFa) "تحلیل سود و زیان لحظه‌ای، میانگین‌ها و سرعت فروش" else "Real-time P&L, Averages & Velocity Metrics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(
                    onClick = {
                        val file = ExcelCsvExporter.exportFinancialsToCsv(context, metrics, localization)
                        if (file != null) {
                            ExcelCsvExporter.shareCsvFile(context, file, if (isFa) "گزارش مالی سود و زیان" else "Financial P&L CSV Export")
                        }
                    },
                    modifier = Modifier.testTag("btn_export_analytics_csv")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.refreshFinancialMetrics() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        // Period Selection Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SalesDateFilter.TODAY to (if (isFa) "امروز" else "Today"),
                SalesDateFilter.THIS_WEEK to (if (isFa) "این هفته" else "This Week"),
                SalesDateFilter.THIS_MONTH to (if (isFa) "این ماه" else "This Month"),
                SalesDateFilter.THIS_YEAR to (if (isFa) "امسال" else "This Year")
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { viewModel.setSalesDateFilter(filter) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("analytics_filter_${filter.name}")
                )
            }
        }

        // Primary Revenue & Profit Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricHighlightCard(
                title = if (isFa) "مجموع درآمد" else "Total Revenue",
                value = LocalizationManager.formatDualCurrency(
                    metrics.totalRevenue,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ),
                subtitle = if (isFa) "${metrics.activeSalesCount} تراکنش موفق" else "${metrics.activeSalesCount} active transactions",
                icon = Icons.Outlined.Payments,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f).testTag("metric_total_revenue")
            )

            MetricHighlightCard(
                title = if (isFa) "سود ناخالص" else "Gross Profit",
                value = LocalizationManager.formatDualCurrency(
                    metrics.grossProfit,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ),
                subtitle = if (isFa) "حاشیه سود: ${String.format("%.1f", metrics.profitMarginPercent)}%" else "Margin: ${String.format("%.1f", metrics.profitMarginPercent)}%",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f).testTag("metric_gross_profit")
            )
        }

        // Cost and AOV Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricHighlightCard(
                title = if (isFa) "هزینه تمام‌شده اجناس (COGS)" else "Cost of Goods (COGS)",
                value = LocalizationManager.formatDualCurrency(
                    metrics.totalCost,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ),
                subtitle = if (isFa) "بر اساس قیمت خرید لحظه‌ای ثبت شده" else "Preserved snapshot unit costs",
                icon = Icons.Outlined.AccountBalanceWallet,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )

            MetricHighlightCard(
                title = if (isFa) "میانگین ارزش فاکتور (AOV)" else "Avg Order Value (AOV)",
                value = LocalizationManager.formatDualCurrency(
                    metrics.averageOrderValue,
                    localization.selectedCurrencyCode,
                    localization.selectedCurrencySymbol,
                    localization.exchangeRateToUSD,
                    localization.markupPercent
                ),
                subtitle = if (isFa) "بدون احتساب ${metrics.voidedSalesCount} فاکتور باطل‌شده" else "Excludes ${metrics.voidedSalesCount} voided sales",
                icon = Icons.Outlined.ShoppingCart,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f).testTag("metric_aov")
            )
        }

        // Requirement 12: Calculated Averages (Daily, Weekly, Monthly)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isFa) "سرعت فروش و میانگین‌ها" else "Sales Velocity & Averages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AverageItem(if (isFa) "میانگین روزانه" else "Avg Daily Sales", metrics.averageDailySales, localization)
                    AverageItem(if (isFa) "میانگین هفتگی" else "Avg Weekly Sales", metrics.averageWeeklySales, localization)
                    AverageItem(if (isFa) "میانگین ماهانه" else "Avg Monthly Sales", metrics.averageMonthlySales, localization)
                }
            }
        }

        // Top Selling & Most Profitable Products
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFa) "پر‌فروش‌ترین و پر‌سود‌ترین اجناس" else "Top Selling & Profitable Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (metrics.topSellingProducts.isEmpty()) {
                    Text(if (isFa) "هیچ داده فروشی برای این بازه ثبت نشده است." else "No sales data recorded for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    metrics.topSellingProducts.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(p.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(if (isFa) "${p.quantitySold} عدد فروخته شده" else "${p.quantitySold} units sold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    LocalizationManager.formatDualCurrency(p.totalRevenue, localization.selectedCurrencyCode, localization.selectedCurrencySymbol, localization.exchangeRateToUSD, localization.markupPercent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(if (isFa) "سود: $${String.format("%.2f", p.totalProfit)}" else "Profit: $${String.format("%.2f", p.totalProfit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Low Stock Alerts Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (lowStockProducts.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = if (lowStockProducts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lowStockProducts.isNotEmpty()) (if (isFa) "هشدار کمبود موجودی (${lowStockProducts.size} قلم)" else "Low Stock Reorder Alerts (${lowStockProducts.size} items)") else (if (isFa) "وضعیت موجودی: عالی" else "Inventory Health: Optimal"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (lowStockProducts.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (lowStockProducts.isEmpty()) {
                    Text(if (isFa) "تمام اجناس موجود در انبار بالاتر از حد مجاز کمبود موجودی هستند." else "All items in inventory are above their minimum stock thresholds.", style = MaterialTheme.typography.bodySmall)
                } else {
                    lowStockProducts.forEach { p ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(if (isFa) "${p.stockQuantity} عدد باقی‌مانده (حداقل: ${p.minStockThreshold})" else "${p.stockQuantity} units left (Min: ${p.minStockThreshold})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Staff Performance Leaderboard
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isFa) "عملکرد و جدول رده‌بندی کارمندان" else "Staff & Cashier Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (metrics.employeeSales.isEmpty()) {
                    Text(if (isFa) "هیچ فروشی توسط کارمندان در این بازه ثبت نشده است." else "No staff sales registered in period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    metrics.employeeSales.forEach { emp ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(emp.cashierName, style = MaterialTheme.typography.bodyMedium)
                            Text(if (isFa) "${emp.salesCount} فروش • $${String.format("%.2f", emp.totalRevenue)}" else "${emp.salesCount} sales • $${String.format("%.2f", emp.totalRevenue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricHighlightCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AverageItem(
    label: String,
    amount: Double,
    localization: com.example.domain.localization.LocalizationState
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = LocalizationManager.formatDualCurrency(
                amount,
                localization.selectedCurrencyCode,
                localization.selectedCurrencySymbol,
                localization.exchangeRateToUSD,
                localization.markupPercent
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
