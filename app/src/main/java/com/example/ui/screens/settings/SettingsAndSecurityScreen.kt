package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.SupportedLanguage
import com.example.ui.AppViewModel
import com.example.ui.components.AboutUsSection
import com.example.ui.components.BiometricAuthPromptModal
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.ForgotPasswordDialog

@Composable
fun SettingsAndSecurityScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val localization by viewModel.localization.collectAsState()
    val currencyConfigs by viewModel.currencyConfigs.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Roles & Security, 1 = Currency & Localization, 2 = Audit Logs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Text(
            text = if (isFa) "تنظیمات، صرافی و امنیت" else "Settings, Currency & Security",
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
            text = if (isFa) "کاربر فعال: ${currentUser.displayName} • نقش: $roleNameFa" else "Current Active: ${currentUser.displayName} • Role: ${currentUser.role.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- Persistent Quick Language Switcher Toggle Banner ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFa) "دکمه جابجایی زبان سیستم (Language Switcher)" else "Instant Language Switcher",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AssistChip(
                        onClick = { viewModel.setLanguage(localization.currentLanguage) },
                        label = {
                            Text(
                                text = localization.currentLanguage.nativeName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.testTag("chip_active_language_indicator")
                    )
                }

                Text(
                    text = if (isFa) "تغییر آنی زبان رابط کاربری بدون نیاز به راه‌اندازی مجدد اپلیکیشن:"
                           else "Instantly switch UI language without restarting the app:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SupportedLanguage.values()) { lang ->
                        val isSelected = localization.currentLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLanguage(lang) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            label = { Text(lang.nativeName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("btn_lang_switch_${lang.code}")
                        )
                    }
                }
            }
        }

        // Sub-tabs
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text(if (isFa) "مدیریت نقش‌ها و امنیت" else "RBAC & Security") },
                modifier = Modifier.testTag("tab_settings_rbac")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text(if (isFa) "ارز و زبان سیستم" else "Currency & Locale") },
                modifier = Modifier.testTag("tab_settings_currency")
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text(if (isFa) "گزارش‌های امنیتی (${auditLogs.size})" else "Audit Logs (${auditLogs.size})") },
                modifier = Modifier.testTag("tab_settings_audit")
            )
            Tab(
                selected = activeSubTab == 3,
                onClick = { activeSubTab = 3 },
                text = { Text(LocalizationManager.getString("about_us", localization.currentLanguage)) },
                modifier = Modifier.testTag("tab_settings_about_us")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSubTab) {
            0 -> SecurityAndUsersTab(viewModel, currentUser, allUsers)
            1 -> CurrencyAndLocaleTab(viewModel, localization, currencyConfigs)
            2 -> AuditLogsTab(viewModel, auditLogs, localization)
            3 -> AboutUsSection(language = localization.currentLanguage, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun SecurityAndUsersTab(
    viewModel: AppViewModel,
    currentUser: User,
    allUsers: List<User>
) {
    val localization by viewModel.localization.collectAsState()
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    val showProfileModal by viewModel.showProfileEditModal.collectAsState()
    val showForgotModal by viewModel.showForgotPasswordModal.collectAsState()
    val showBiometricModal by viewModel.showBiometricPromptModal.collectAsState()

    var newUsername by remember { mutableStateOf("") }
    var newDisplayName by remember { mutableStateOf("") }
    var selectedNewRole by remember { mutableStateOf(UserRole.EMPLOYEE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Active User Profile & Security Settings Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentUser.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFa) "نام کاربری: @${currentUser.username} • نقش: ${currentUser.role.name}" else "Username: @${currentUser.username} • Role: ${currentUser.role.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.setShowProfileEditModal(true) },
                        modifier = Modifier.testTag("btn_open_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "ویرایش پروفایل و رمز" else "Edit Profile & PIN")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setShowBiometricPromptModal(true) },
                        modifier = Modifier.weight(1f).testTag("btn_trigger_biometric_test")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFa) "تست اثر انگشت / چهره" else "Test Biometrics")
                    }

                    OutlinedButton(
                        onClick = { viewModel.setShowForgotPasswordModal(true) },
                        modifier = Modifier.weight(1f).testTag("btn_open_forgot_pin")
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFa) "فراموشی رمز / پین" else "Forgot Password?")
                    }

                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).testTag("btn_settings_logout")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFa) "خروج از حساب" else "Logout")
                    }
                }
            }
        }

        // 1. Role Switcher for Interactive Demo
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isFa) "تغییر سریع کاربر فعال (سیشن فعلی)" else "Switch Active User Session (Demo Switcher)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isFa) "جهت ارزیابی دسترسی‌ها (مثلا تحویل‌دار نمی‌تواند فاکتور ابطال کند یا نرخ صرافی تغییر دهد)." else "Quickly switch roles to test security boundaries (e.g. Employee cannot void or edit currency).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allUsers) { user ->
                        val isCurrent = user.id == currentUser.id
                        FilterChip(
                            selected = isCurrent,
                            onClick = { viewModel.switchUser(user) },
                            label = { Text("${user.displayName} (${user.role.name})") },
                            leadingIcon = {
                                if (isCurrent) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("chip_user_${user.username}")
                        )
                    }
                }
            }
        }

        // 2. User Creation Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFa) "ساخت حساب کاربر / کارمند جدید" else "Create New User / Staff Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text(if (isFa) "نام کاربری" else "Username") },
                        modifier = Modifier.weight(1f).testTag("input_new_username"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text(if (isFa) "نام و تخلص کامل" else "Full Name") },
                        modifier = Modifier.weight(1f).testTag("input_new_display_name"),
                        singleLine = true
                    )
                }

                Text(if (isFa) "تعیین نقش کاربر:" else "Assign Role:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.values().forEach { role ->
                        FilterChip(
                            selected = selectedNewRole == role,
                            onClick = { selectedNewRole = role },
                            label = { Text(role.name) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newDisplayName.isNotBlank()) {
                            val u = User(
                                username = newUsername.trim(),
                                displayName = newDisplayName.trim(),
                                role = selectedNewRole,
                                pin = "1234"
                            )
                            viewModel.createUser(u)
                            newUsername = ""
                            newDisplayName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_create_user")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFa) "ایجاد حساب کاربر" else "Create Account")
                }
            }
        }

        // Render Modals
        if (showProfileModal) {
            EditProfileDialog(
                currentUser = currentUser,
                isFa = isFa,
                onDismiss = { viewModel.setShowProfileEditModal(false) },
                onSave = { name, pin ->
                    viewModel.updateCurrentUserProfile(name, pin)
                    viewModel.setShowProfileEditModal(false)
                }
            )
        }

        if (showForgotModal) {
            ForgotPasswordDialog(
                isFa = isFa,
                onDismiss = { viewModel.setShowForgotPasswordModal(false) },
                onResetPin = { username, masterCode, newPin ->
                    viewModel.resetPinWithMasterCode(username, masterCode, newPin)
                }
            )
        }

        if (showBiometricModal) {
            BiometricAuthPromptModal(
                selectedUser = currentUser,
                isFa = isFa,
                onDismiss = { viewModel.setShowBiometricPromptModal(false) },
                onSuccess = {
                    viewModel.setShowBiometricPromptModal(false)
                }
            )
        }

        // 3. User List & Role Governance
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "User Accounts & Role Governance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                allUsers.forEach { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.displayName, fontWeight = FontWeight.SemiBold)
                            Text("@${user.username} • ${user.role.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Test promoting to Super Admin or Manager
                            if (user.role != UserRole.SUPER_ADMIN) {
                                TextButton(
                                    onClick = { viewModel.updateUserRole(user.id, UserRole.SUPER_ADMIN) },
                                    modifier = Modifier.testTag("btn_promote_admin_${user.id}")
                                ) {
                                    Text("Make Admin", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (user.role != UserRole.MANAGER) {
                                TextButton(
                                    onClick = { viewModel.updateUserRole(user.id, UserRole.MANAGER) },
                                    modifier = Modifier.testTag("btn_promote_mgr_${user.id}")
                                ) {
                                    Text("Make Mgr", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun CurrencyAndLocaleTab(
    viewModel: AppViewModel,
    localization: com.example.domain.localization.LocalizationState,
    currencyConfigs: List<com.example.data.local.entity.CurrencyConfig>
) {
    var editRateStr by remember { mutableStateOf(localization.exchangeRateToUSD.toString()) }
    var editMarkupStr by remember { mutableStateOf(localization.markupPercent.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

        // 1. Language & Localization Persistent Toggle
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "زبان سیستم و جهت نمایش (Persistent Language Toggle)" else "System Language & Locale Toggle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isFa) "زبان انتخابی ذخیره می‌شود و تمامی بخش‌های برنامه، تاریخ و مبالغ بلافاصله به‌روزرسانی خواهند شد:"
                           else "Selected language is saved persistently and dynamically refreshes the app's Locale, dates, and numbers:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SupportedLanguage.values()) { lang ->
                        val isSelected = localization.currentLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLanguage(lang) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text("${lang.nativeName} (${lang.englishName})") },
                            modifier = Modifier.testTag("chip_lang_${lang.code}")
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isFa) "زبان فعال: ${localization.currentLanguage.nativeName}" else "Active Language: ${localization.currentLanguage.englishName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFa) "کد زبان: ${localization.currentLanguage.code} • جهت چیدمان: ${if (localization.isRtl) "راست به چپ (RTL)" else "چپ به راست (LTR)"}"
                                       else "Code: ${localization.currentLanguage.code} • Direction: ${if (localization.isRtl) "RTL" else "LTR"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AssistChip(
                            onClick = { viewModel.setLanguage(localization.currentLanguage) },
                            label = { Text(if (isFa) "بازنشانی" else "Refresh") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }

        // 2. Timezone Selector (Requirement 11)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Business Timezone (Date/Time Reports)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val timezones = listOf("UTC", "Asia/Kabul", "Asia/Riyadh", "Europe/Istanbul", "Europe/Madrid", "America/New_York")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(timezones) { tz ->
                        FilterChip(
                            selected = localization.currentTimeZoneId == tz,
                            onClick = { viewModel.setTimeZone(tz) },
                            label = { Text(tz) }
                        )
                    }
                }
            }
        }

        // 3. Manager Currency Rate & Markup Editor (Requirement 10)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Regional Currency Display & Markup Rates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "USD reference rates and markup percentages used for product price and business data display. Every change is logged to Audit Logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencyConfigs) { cfg ->
                        val isSelected = localization.selectedCurrencyCode == cfg.currencyCode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setCurrencyByCode(cfg.currencyCode)
                                editRateStr = cfg.exchangeRateToUSD.toString()
                                editMarkupStr = cfg.markupPercent.toString()
                            },
                            label = { Text("${cfg.currencyCode} (${cfg.currencySymbol})") }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editRateStr,
                        onValueChange = { editRateStr = it },
                        label = { Text("${localization.selectedCurrencyCode} Rate per 1 USD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("input_exchange_rate"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editMarkupStr,
                        onValueChange = { editMarkupStr = it },
                        label = { Text("Markup Adj %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("input_markup_percent"),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        val rate = editRateStr.toDoubleOrNull() ?: 1.0
                        val markup = editMarkupStr.toDoubleOrNull() ?: 0.0
                        viewModel.updateCurrencySettings(localization.selectedCurrencyCode, rate, markup)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_save_currency_rates")
                ) {
                    Text("Save Rates & Record in Audit Log")
                }
            }
        }
    }
}

@Composable
private fun AuditLogsTab(
    viewModel: AppViewModel,
    auditLogs: List<com.example.data.local.entity.AuditLog>,
    localization: com.example.domain.localization.LocalizationState
) {
    val backupJson by viewModel.backupJson.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Backup Export Section (Phase 13)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("System Database Backup", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Export cryptographic snapshot of store records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.generateDatabaseBackup() },
                        modifier = Modifier.testTag("btn_export_backup")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }
                if (backupJson != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = backupJson!!,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Text(
            text = "Append-Only Audit Trail (Immutable Log)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "All security events, user creations, currency modifications, sales voids, and bulk deletions are strictly recorded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(auditLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.actionType,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = LocalizationManager.formatDateTime(log.timestamp, timeZoneId = localization.currentTimeZoneId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "User: ${log.username} (${log.userRole})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
