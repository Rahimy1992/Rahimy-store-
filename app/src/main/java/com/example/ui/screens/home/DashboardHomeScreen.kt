package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainDestination
import com.example.ui.AppViewModel
import com.example.ui.components.SalesAnalyticsChartCard

data class ColorMenuItem(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val destination: MainDestination
)

data class StatCardItem(
    val titleFa: String,
    val titleEn: String,
    val value: String,
    val subtitleFa: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeScreen(
    viewModel: AppViewModel,
    onNavigate: (MainDestination) -> Unit
) {
    val localization by viewModel.localization.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currencyConfigs by viewModel.currencyConfigs.collectAsState()
    val financialMetrics by viewModel.financialMetrics.collectAsState()

    val currencySymbol = localization.selectedCurrencySymbol
    val currencyCode = localization.selectedCurrencyCode

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "ps")

    // Stat items matching top of screenshot
    val statCards = listOf(
        StatCardItem(
            titleFa = "فروش امروز",
            titleEn = "Today's Sales",
            value = "${financialMetrics.totalRevenue.toInt()} $currencySymbol",
            subtitleFa = currencyCode,
            icon = Icons.Default.BarChart,
            containerColor = Color(0xFFE0F7FA), // Soft Cyan
            contentColor = Color(0xFF00838F)
        ),
        StatCardItem(
            titleFa = "مشتریان",
            titleEn = "Customers",
            value = "${allUsers.size} نفر",
            subtitleFa = "ثبت شده",
            icon = Icons.Default.People,
            containerColor = Color(0xFFEDE7F6), // Soft Purple
            contentColor = Color(0xFF5E35B1)
        ),
        StatCardItem(
            titleFa = "محصولات",
            titleEn = "Products",
            value = "${allProducts.size} قلم",
            subtitleFa = "موجود در انبار",
            icon = Icons.Outlined.Inventory2,
            containerColor = Color(0xFFFFF3E0), // Soft Orange
            contentColor = Color(0xFFE65100)
        ),
        StatCardItem(
            titleFa = "صرافی",
            titleEn = "Currencies",
            value = "${currencyConfigs.size} ارز فعال",
            subtitleFa = "تبدیل لحظه‌ای",
            icon = Icons.Default.CurrencyExchange,
            containerColor = Color(0xFFE8F5E9), // Soft Green
            contentColor = Color(0xFF2E7D32)
        )
    )

    // 9 Colorful Grid Menu Tiles matching screenshot exactly
    val colorMenuItems = listOf(
        ColorMenuItem(
            id = "sales",
            titleFa = "فروش",
            titleEn = "Sales / POS",
            subtitleFa = "مدیریت فروش و فاکتور",
            icon = Icons.Default.ShoppingCart,
            backgroundColor = Color(0xFF1E88E5), // Vibrant Blue
            destination = MainDestination.SALES
        ),
        ColorMenuItem(
            id = "manual_sales",
            titleFa = "فروش دستی",
            titleEn = "Manual Sales",
            subtitleFa = "ثبت مستقیم و عکس فاکتور",
            icon = Icons.Outlined.ReceiptLong,
            backgroundColor = Color(0xFF0284C7), // Light Blue
            destination = MainDestination.MANUAL_SALES
        ),
        ColorMenuItem(
            id = "inventory",
            titleFa = "موجودی",
            titleEn = "Inventory",
            subtitleFa = "کنترل موجودی و انبار",
            icon = Icons.Outlined.Inventory2,
            backgroundColor = Color(0xFFFB8C00), // Vivid Orange
            destination = MainDestination.PRODUCTS
        ),
        ColorMenuItem(
            id = "customers",
            titleFa = "مشتریان",
            titleEn = "Customers",
            subtitleFa = "مدیریت مشتریان",
            icon = Icons.Default.People,
            backgroundColor = Color(0xFF7E57C2), // Purple/Violet
            destination = MainDestination.CUSTOMER_AI
        ),
        ColorMenuItem(
            id = "suppliers",
            titleFa = "تأمین‌کنندگان",
            titleEn = "Suppliers",
            subtitleFa = "مدیریت تامین‌کنندگان",
            icon = Icons.Default.LocalShipping,
            backgroundColor = Color(0xFF43A047), // Emerald Green
            destination = MainDestination.PRODUCTS
        ),
        ColorMenuItem(
            id = "invoices",
            titleFa = "اسکن فاکتور AI",
            titleEn = "AI Invoice OCR",
            subtitleFa = "اسکن فاکتور و ورود هوشمند به انبار",
            icon = Icons.Outlined.DocumentScanner,
            backgroundColor = Color(0xFF00ACC1), // Dark Teal
            destination = MainDestination.AI_INVOICE_SCANNER
        ),
        ColorMenuItem(
            id = "expenses",
            titleFa = "مصارف",
            titleEn = "Expenses",
            subtitleFa = "ثبت مصارف و هزینه‌ها",
            icon = Icons.Default.AccountBalanceWallet,
            backgroundColor = Color(0xFFE53935), // Coral Red
            destination = MainDestination.ANALYTICS
        ),
        ColorMenuItem(
            id = "debts",
            titleFa = "بدهکاری‌ها",
            titleEn = "Receivables",
            subtitleFa = "ثبت طلب و بدهکاری‌ها",
            icon = Icons.Default.RequestQuote,
            backgroundColor = Color(0xFFFFB300), // Warm Amber
            destination = MainDestination.SALES
        ),
        ColorMenuItem(
            id = "exchange",
            titleFa = "صرافی",
            titleEn = "Exchange",
            subtitleFa = "تبدیل اسعار و نرخ‌ها",
            icon = Icons.Default.CurrencyExchange,
            backgroundColor = Color(0xFF03A9F4), // Bright Cyan
            destination = MainDestination.SETTINGS
        ),
        ColorMenuItem(
            id = "reports",
            titleFa = "گزارش‌ها",
            titleEn = "Reports",
            subtitleFa = "گزارش‌های دقیق و حرفه‌ای",
            icon = Icons.Default.BarChart,
            backgroundColor = Color(0xFF9C27B0), // Vivid Purple
            destination = MainDestination.ANALYTICS
        ),
        ColorMenuItem(
            id = "online_services",
            titleFa = "خدمات آنلاین",
            titleEn = "Online Services",
            subtitleFa = "وضعیت ابری و همگام‌سازی",
            icon = Icons.Default.CloudSync,
            backgroundColor = Color(0xFF00897B), // Teal Green
            destination = MainDestination.ONLINE_SERVICES
        ),
        ColorMenuItem(
            id = "support",
            titleFa = "پشتیبانی",
            titleEn = "Support",
            subtitleFa = "گفتوگو و پاسخ به درخواست‌ها",
            icon = Icons.Default.SupportAgent,
            backgroundColor = Color(0xFFD81B60), // Deep Pink
            destination = MainDestination.SUPPORT
        ),
        ColorMenuItem(
            id = "b2b_wholesale",
            titleFa = "تجارت عمده",
            titleEn = "B2B Wholesale",
            subtitleFa = "مشتریان، فاکتورها و سفارشات عمده",
            icon = Icons.Default.Business,
            backgroundColor = Color(0xFF3F51B5), // Indigo
            destination = MainDestination.B2B_WHOLESALE
        )
    )

    val selectedSalesFilter by viewModel.salesDateFilter.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
    ) {
        val isTablet = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- Header Banner Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1565C0),
                                Color(0xFF1E88E5)
                            )
                        )
                    )
                    .padding(bottom = 20.dp, top = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Main Tagline Badge
                    Surface(
                        color = Color(0xFFFFC107), // Gold yellow badge
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = if (isFa) "راه حل مدرن برای کسب‌وکار هوشمند" else "Modern Solution for Smart Business",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isFa) "مدیریت کامل کاروبار شما، در یک اپلیکیشن" else "Complete Business Management in One App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Feature Pill
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "سریع  |  ساده  |  مطمئن" else "Fast  |  Simple  |  Reliable",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isTablet) {
                // ==================== PHONE RESPONSIVE LAYOUT ====================
                // 1) 4 Top Stat Cards (2x2 Grid)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(item = statCards[0], isFa = isFa, modifier = Modifier.weight(1f))
                        StatCard(item = statCards[1], isFa = isFa, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(item = statCards[2], isFa = isFa, modifier = Modifier.weight(1f))
                        StatCard(item = statCards[3], isFa = isFa, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2) 3x3 Colorful Grid Menu
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isFa) "منوی اصلی خدمات" else "Main Services Menu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isFa) "منوی رنگی 9‌تایی" else "9 Color Tiles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (rowIndex in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (colIndex in 0..2) {
                                val itemIndex = rowIndex * 3 + colIndex
                                if (itemIndex < colorMenuItems.size) {
                                    val item = colorMenuItems[itemIndex]
                                    ColorMenuTile(
                                        item = item,
                                        isFa = isFa,
                                        onClick = { onNavigate(item.destination) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3) Sales Analytics Interactive Chart Card
                SalesAnalyticsChartCard(
                    financialMetrics = financialMetrics,
                    selectedFilter = selectedSalesFilter,
                    localization = localization,
                    onFilterChanged = { viewModel.setSalesDateFilter(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4) Features & Dari Operational Suggestions
                FeaturesHighlightCard(isFa = isFa, modifier = Modifier.padding(horizontal = 16.dp))

            } else {
                // ==================== TABLET / WIDE SCREEN RESPONSIVE LAYOUT ====================
                // 1) 4 Top Stat Cards in a Single Row across wide screen
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    statCards.forEach { card ->
                        StatCard(item = card, isFa = isFa, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2) Canonical Split 2-Column Panel (Left: Quick Actions Menu, Right: Analytics Dashboard)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT PANEL: 3x3 Menu Tiles + Active Features
                    Column(modifier = Modifier.weight(1.1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFa) "منوی اصلی خدمات (تبلت)" else "Main Services Menu (Tablet)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isFa) "دسترسی سریع" else "Quick Access",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (rowIndex in 0..2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    for (colIndex in 0..2) {
                                        val itemIndex = rowIndex * 3 + colIndex
                                        if (itemIndex < colorMenuItems.size) {
                                            val item = colorMenuItems[itemIndex]
                                            ColorMenuTile(
                                                item = item,
                                                isFa = isFa,
                                                onClick = { onNavigate(item.destination) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        FeaturesHighlightCard(isFa = isFa)
                    }

                    // RIGHT PANEL: Dedicated Live Analytics Side Dashboard
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(
                            text = if (isFa) "داشبورد تحلیلی و گزارشات لحظه‌ای" else "Live Analytics Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SalesAnalyticsChartCard(
                            financialMetrics = financialMetrics,
                            selectedFilter = selectedSalesFilter,
                            localization = localization,
                            onFilterChanged = { viewModel.setSalesDateFilter(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        DariOperationalSuggestionsCard(isFa = isFa)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FeaturesHighlightCard(
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFa) "امکانات فعال سیستم تجارت هوشمند" else "Smart Commerce Active Capabilities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val features = listOf(
                if (isFa) "مدیریت کامل فروش، فاکتورها و سیستم POS" else "Point of Sale & Invoicing (POS)",
                if (isFa) "کنترل دقیق موجودی انبار و هشدارهای کمبود جنس" else "Real-time Inventory & Stock Control",
                if (isFa) "دفتر حساب مشتریان، حساب بدهکاری و طلبکاری" else "Customer & Supplier Ledger Management",
                if (isFa) "ماژول اختصاصی صرافی و تبدیل نرخ‌های روزانه ارز" else "Multi-Currency Exchange Module",
                if (isFa) "اسکن هوشمند فاکتورها با فناوری OCR" else "Smart AI Invoice & Catalog OCR",
                if (isFa) "چاپ مستقیم حرارتی و بلوتوثی فاکتورها (ESC/POS)" else "Direct Bluetooth Thermal Printing (ESC/POS)",
                if (isFa) "کارکرد کامل افلاین و همگام‌سازی خودکار ابری" else "Seamless Offline & Cloud Sync"
            )

            features.forEach { feat ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

@Composable
fun DariOperationalSuggestionsCard(
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFa) "پیشنهادهای هوشمند عملیاتی به زبان دری:" else "Dari Operational Smart Proposals:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val buildSuggestions = listOf(
                if (isFa) "💡 چاپ مستقیم و سریع فاکتورها با پرینترهای حرارتی بلوتوثی ۵۸mm و ۸۰mm" else "Instant Bluetooth 58mm/80mm Thermal Receipts",
                if (isFa) "💡 ارسال اتوماتیک پیامک رسید معامله و صورت‌حساب به واتساپ مشتریان" else "Automated WhatsApp & SMS Receipts",
                if (isFa) "💡 تحلیل هوشمند و پیش‌بینی سود و زیان ماهوار با هوش مصنوعی" else "AI Monthly Profit & Loss Forecasting",
                if (isFa) "💡 همگام‌سازی ابری چند نمایندگی برای دفاتر صرافی و فروشگاه‌های زنجیره‌ای" else "Multi-Branch Cloud Sync for Exchange Offices"
            )

            buildSuggestions.forEach { suggestion ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    item: StatCardItem,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = item.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isFa) item.titleFa else item.titleEn,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = item.contentColor
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(item.contentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.subtitleFa,
                style = MaterialTheme.typography.labelSmall,
                color = item.contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ColorMenuTile(
    item: ColorMenuItem,
    isFa: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // Square card
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("menu_tile_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = item.backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // White Circular Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.titleFa,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // White Bold Label
            Text(
                text = if (isFa) item.titleFa else item.titleEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
