package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.localization.SupportedLanguage
import com.example.ui.AppViewModel
import com.example.ui.screens.analytics.AnalyticsDashboardScreen
import com.example.ui.screens.catalog.CatalogOcrScreen
import com.example.ui.screens.customer_ai.CustomerAiChatScreen
import com.example.ui.screens.home.DashboardHomeScreen
import com.example.ui.screens.manager_ai.ManagerAiScreen
import com.example.ui.screens.products.ProductListScreen
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.screens.settings.SettingsAndSecurityScreen
import com.example.ui.screens.verification.VerificationMatrixScreen
import com.example.ui.components.BiometricAuthPromptModal
import com.example.ui.components.ForgotPasswordDialog
import com.example.ui.components.LockScreenOverlayDialog
import com.example.ui.theme.MyApplicationTheme

import com.example.ui.screens.b2b.B2bWholesaleDashboardScreen
import com.example.ui.screens.invoices.AiInvoiceScannerScreen
import com.example.ui.screens.manual_sales.ManualSalesScreen
import com.example.ui.screens.online_services.OnlineServicesDashboardScreen
import com.example.ui.screens.support.SupportChatScreen

enum class MainDestination(
    val titleFa: String,
    val titleEn: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("خانه", "Home", Icons.Default.Home, "nav_home"),
    SALES("فروش", "Sales", Icons.Default.ShoppingCart, "nav_sales"),
    MANUAL_SALES("فروش دستی", "Manual Sales", Icons.Outlined.ReceiptLong, "nav_manual_sales"),
    PRODUCTS("موجودی", "Inventory", Icons.Outlined.Inventory2, "nav_products"),
    ANALYTICS("گزارش‌ها", "Reports", Icons.Default.BarChart, "nav_analytics"),
    B2B_WHOLESALE("تجارت عمده", "B2B Wholesale", Icons.Default.Business, "nav_b2b"),
    ONLINE_SERVICES("خدمات آنلاین", "Online Services", Icons.Default.CloudSync, "nav_online_services"),
    SUPPORT("پشتیبانی", "Support", Icons.Default.SupportAgent, "nav_support"),
    MORE("بیشتر", "More", Icons.Default.MoreHoriz, "nav_more"),
    CATALOG_OCR("اسکن AI", "AI Catalog", Icons.Outlined.AutoAwesome, "nav_catalog_ocr"),
    AI_INVOICE_SCANNER("اسکن فاکتور", "AI Invoice", Icons.Outlined.DocumentScanner, "nav_ai_invoice_scanner"),
    CUSTOMER_AI("پشتیبان", "Customer AI", Icons.Outlined.SmartToy, "nav_customer_ai"),
    MANAGER_AI("مشاور مدیر", "Manager AI", Icons.Outlined.Psychology, "nav_manager_ai"),
    SETTINGS("تنظیمات", "Settings", Icons.Outlined.Settings, "nav_settings"),
    VERIFY("تاییدیه", "Verify", Icons.Outlined.Verified, "nav_verify")
}

class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Cleanly handle OS memory trimming for graphics and bitmap caches
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: AppViewModel) {
    val localization by viewModel.localization.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val showForgotModal by viewModel.showForgotPasswordModal.collectAsState()
    val showBiometricModal by viewModel.showBiometricPromptModal.collectAsState()
    val securityNotice by viewModel.securityNotice.collectAsState()
    val successNotice by viewModel.successNotice.collectAsState()
    val cart by viewModel.cart.collectAsState()

    var currentDestination by remember { mutableStateOf(MainDestination.HOME) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val layoutDirection = if (localization.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "ps")

    // Handle Snackbars
    LaunchedEffect(securityNotice) {
        if (securityNotice != null) {
            snackbarHostState.showSnackbar(
                message = "🔒 $securityNotice",
                duration = SnackbarDuration.Long
            )
            viewModel.clearNotices()
        }
    }

    LaunchedEffect(successNotice) {
        if (successNotice != null) {
            snackbarHostState.showSnackbar(
                message = "✅ $successNotice",
                duration = SnackbarDuration.Short
            )
            viewModel.clearNotices()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp
            val primaryTabs = listOf(
                MainDestination.HOME,
                MainDestination.SALES,
                MainDestination.PRODUCTS,
                MainDestination.ANALYTICS,
                MainDestination.MORE
            )

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rahimy Smart Commerce",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${localization.selectedCurrencyCode} (${localization.selectedCurrencySymbol})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            // Quick Language Switcher Dropdown
                            Box {
                                IconButton(
                                    onClick = { langMenuExpanded = true },
                                    modifier = Modifier.testTag("btn_topbar_language_switch")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = if (isFa) "تغییر زبان" else "Switch Language",
                                        tint = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = langMenuExpanded,
                                    onDismissRequest = { langMenuExpanded = false }
                                ) {
                                    SupportedLanguage.values().forEach { lang ->
                                        val isSelected = localization.currentLanguage == lang
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }
                                                    Text(
                                                        text = lang.nativeName,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.setLanguage(lang)
                                                langMenuExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_item_lang_${lang.code}")
                                        )
                                    }
                                }
                            }

                            // Lock App Session Button
                            IconButton(
                                onClick = { viewModel.lockApp() },
                                modifier = Modifier.testTag("btn_lock_session")
                            ) {
                                Icon(
                                    imageVector = if (isAppLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (isFa) "قفل سیشن" else "Lock Session",
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                            }

                            // Logout Button
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.testTag("btn_logout")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = if (isFa) "خروج از حساب" else "Logout",
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                            }

                            // Active User / Role Pill
                            Surface(
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { currentDestination = MainDestination.SETTINGS }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = androidx.compose.ui.graphics.Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val roleText = when (currentUser.role) {
                                        com.example.data.local.entity.UserRole.SUPER_ADMIN -> if (isFa) "مدیر کل" else "Super Admin"
                                        com.example.data.local.entity.UserRole.MANAGER -> if (isFa) "مدیر سیستم" else "Manager"
                                        com.example.data.local.entity.UserRole.EMPLOYEE -> if (isFa) "تحویل‌دار" else "Employee"
                                        com.example.data.local.entity.UserRole.CUSTOMER -> if (isFa) "مشتری" else "Customer"
                                        else -> if (isFa) "مشاهده‌کننده" else currentUser.role.name
                                    }
                                    Text(
                                        text = roleText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFF1565C0)
                        )
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        NavigationBar(
                            containerColor = androidx.compose.ui.graphics.Color.White,
                            tonalElevation = 8.dp
                        ) {
                            primaryTabs.forEach { dest ->
                                val isSelected = currentDestination == dest
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (dest == MainDestination.MORE) {
                                            showMoreSheet = true
                                        } else {
                                            currentDestination = dest
                                        }
                                    },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest == MainDestination.SALES && cart.isNotEmpty()) {
                                                    Badge { Text("${cart.sumOf { it.quantity }}") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = if (isFa) dest.titleFa else dest.titleEn,
                                                tint = if (isSelected) androidx.compose.ui.graphics.Color(0xFF1565C0) else androidx.compose.ui.graphics.Color(0xFF64748B)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isFa) dest.titleFa else dest.titleEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (isTablet) {
                        NavigationRail(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            primaryTabs.forEach { dest ->
                                val isSelected = currentDestination == dest
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (dest == MainDestination.MORE) {
                                            showMoreSheet = true
                                        } else {
                                            currentDestination = dest
                                        }
                                    },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest == MainDestination.SALES && cart.isNotEmpty()) {
                                                    Badge { Text("${cart.sumOf { it.quantity }}") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = if (isFa) dest.titleFa else dest.titleEn,
                                                tint = if (isSelected) androidx.compose.ui.graphics.Color(0xFF1565C0) else androidx.compose.ui.graphics.Color(0xFF64748B)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (isFa) dest.titleFa else dest.titleEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                when (currentDestination) {
                    MainDestination.HOME -> {
                        DashboardHomeScreen(
                            viewModel = viewModel,
                            onNavigate = { dest -> currentDestination = dest }
                        )
                    }
                    MainDestination.PRODUCTS -> {
                        ProductListScreen(
                            viewModel = viewModel,
                            onNavigateToCatalogOcr = { currentDestination = MainDestination.CATALOG_OCR }
                        )
                    }
                    MainDestination.SALES -> {
                        SalesScreen(viewModel = viewModel)
                    }
                    MainDestination.MANUAL_SALES -> {
                        ManualSalesScreen(viewModel = viewModel)
                    }
                    MainDestination.ANALYTICS -> {
                        AnalyticsDashboardScreen(viewModel = viewModel)
                    }
                    MainDestination.B2B_WHOLESALE -> {
                        B2bWholesaleDashboardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = MainDestination.HOME }
                        )
                    }
                    MainDestination.ONLINE_SERVICES -> {
                        OnlineServicesDashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { dest -> currentDestination = dest }
                        )
                    }
                    MainDestination.SUPPORT -> {
                        SupportChatScreen(viewModel = viewModel)
                    }
                    MainDestination.CATALOG_OCR -> {
                        CatalogOcrScreen(viewModel = viewModel)
                    }
                    MainDestination.AI_INVOICE_SCANNER -> {
                        AiInvoiceScannerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = MainDestination.HOME }
                        )
                    }
                    MainDestination.CUSTOMER_AI -> {
                        CustomerAiChatScreen(viewModel = viewModel)
                    }
                    MainDestination.MANAGER_AI -> {
                        ManagerAiScreen(viewModel = viewModel)
                    }
                    MainDestination.SETTINGS -> {
                        SettingsAndSecurityScreen(viewModel = viewModel)
                    }
                    MainDestination.VERIFY -> {
                        VerificationMatrixScreen(viewModel = viewModel)
                    }
                    MainDestination.MORE -> {
                        DashboardHomeScreen(
                            viewModel = viewModel,
                            onNavigate = { dest -> currentDestination = dest }
                        )
                    }
                }
            }
        }

            // More Options Sheet / Dialog
            if (showMoreSheet) {
                AlertDialog(
                    onDismissRequest = { showMoreSheet = false },
                    title = {
                        Text(
                            text = if (isFa) "خدمات و ماژول‌های بیشتر" else "More Modules & Tools",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                MainDestination.MANUAL_SALES to if (isFa) "فروش دستی و ثبت عکس فاکتور" else "Manual Sales & Photo Invoices",
                                MainDestination.ONLINE_SERVICES to if (isFa) "خدمات آنلاین و وضع ابری" else "Online Services & Cloud Status",
                                MainDestination.SUPPORT to if (isFa) "مرکز پشتیبانی و گفتوگو" else "Customer Support & Chat",
                                MainDestination.CATALOG_OCR to if (isFa) "اسکن هوشمند کاتالوگ و فاکتور (OCR)" else "AI Catalog & Invoice OCR",
                                MainDestination.CUSTOMER_AI to if (isFa) "پشتیبان چندزبانه مشتریان (AI)" else "Multilingual Customer AI",
                                MainDestination.MANAGER_AI to if (isFa) "مشاور هوشمند مدیریت (AI)" else "Management Advisor AI",
                                MainDestination.SETTINGS to if (isFa) "تنظیمات، کاربران و نرخ صرافی" else "Settings, Users & Exchange",
                                MainDestination.VERIFY to if (isFa) "تاییدیه نهایی عملکرد سیستم" else "System Verification Matrix"
                            ).forEach { (dest, label) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentDestination = dest
                                            showMoreSheet = false
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            dest.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMoreSheet = false }) {
                            Text(if (isFa) "بستن" else "Close")
                        }
                    }
                )
            }

            // Global Lock Screen Overlay Dialog
            if (isAppLocked) {
                LockScreenOverlayDialog(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    allUsers = allUsers,
                    isFa = isFa,
                    onUnlockWithPin = { user, pin ->
                        viewModel.unlockAppWithPin(user, pin)
                    },
                    onOpenBiometric = { user ->
                        viewModel.setShowBiometricPromptModal(true)
                    },
                    onOpenForgotPassword = {
                        viewModel.setShowForgotPasswordModal(true)
                    }
                )
            }

            if (showBiometricModal && isAppLocked) {
                BiometricAuthPromptModal(
                    selectedUser = currentUser,
                    isFa = isFa,
                    onDismiss = { viewModel.setShowBiometricPromptModal(false) },
                    onSuccess = {
                        viewModel.unlockAppWithBiometric(currentUser)
                    }
                )
            }

            if (showForgotModal && isAppLocked) {
                ForgotPasswordDialog(
                    isFa = isFa,
                    onDismiss = { viewModel.setShowForgotPasswordModal(false) },
                    onResetPin = { username, masterCode, newPin ->
                        viewModel.resetPinWithMasterCode(username, masterCode, newPin)
                    }
                )
            }
        }
    }
}
}
