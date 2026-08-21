package com.example.ui.screens.online_services

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import com.example.MainDestination
import com.example.data.cloud.CloudSyncStatus
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineServicesDashboardScreen(
    viewModel: AppViewModel,
    onNavigate: (MainDestination) -> Unit
) {
    val localization by viewModel.localization.collectAsState()
    val language = localization.currentLanguage

    val isInternetConnected by viewModel.isInternetConnected.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val syncStatus by viewModel.cloudSyncStatus.collectAsState()
    val pendingTransactions by viewModel.pendingTransactionCount.collectAsState()
    val openTicketsCount by viewModel.openTicketsCount.collectAsState()
    val unreadMessagesCount by viewModel.unreadSupportMessagesCount.collectAsState()
    val isSyncing by viewModel.isSyncingInProgress.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val lastSyncError by viewModel.lastSyncError.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LocalizationManager.getString("online_services", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (isInternetConnected && isFirebaseConnected) "Firebase Real-time Backend Active" else "Room Local-First Offline Mode Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerManualSync() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = LocalizationManager.getString("sync_now", language),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("online_services_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInternetConnected && isFirebaseConnected) {
                        Color(0xFFE8F5E9) // Mint light green
                    } else {
                        Color(0xFFFFF3E0) // Warm amber
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (isInternetConnected && isFirebaseConnected) Color(0xFF2E7D32) else Color(0xFFE65100),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInternetConnected && isFirebaseConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isInternetConnected && isFirebaseConnected) "پایگاه داده ابری فایربیس متصل است" else "سیستم به صورت آفلاین (Local-First) فعال است",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isInternetConnected && isFirebaseConnected) Color(0xFF1B5E20) else Color(0xFFBF360C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isInternetConnected && isFirebaseConnected)
                                "تمام معاملات به صورت زنده با Firestore همگام‌سازی می‌شوند."
                            else
                                "تمامی اطلاعات به صورت مطمئن در دیتابیس محلی Room ذخیره شده و پس از اتصال متصل خواهد شد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF37474F)
                        )
                    }
                }
            }

            // Sync Error/Success Notice Banner
            AnimatedVisibility(visible = lastSyncError != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lastSyncError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Text(
                text = "وضعیت ارتباطات و همگام‌سازی ابری",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Status Grid 2x2
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Internet Status Card
                    StatusIndicatorCard(
                        modifier = Modifier.weight(1f),
                        title = LocalizationManager.getString("internet", language),
                        statusText = if (isInternetConnected) LocalizationManager.getString("online", language) else LocalizationManager.getString("offline", language),
                        isPositive = isInternetConnected,
                        icon = if (isInternetConnected) Icons.Default.Wifi else Icons.Default.WifiOff
                    )

                    // Firebase Status Card
                    StatusIndicatorCard(
                        modifier = Modifier.weight(1f),
                        title = LocalizationManager.getString("firebase", language),
                        statusText = if (isFirebaseConnected) LocalizationManager.getString("connected", language) else LocalizationManager.getString("disconnected", language),
                        isPositive = isFirebaseConnected,
                        icon = if (isFirebaseConnected) Icons.Default.LocalFireDepartment else Icons.Outlined.CloudOff
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cloud Sync Card
                    val (syncLabel, syncPositive) = when (syncStatus) {
                        CloudSyncStatus.SYNCED -> LocalizationManager.getString("synced", language) to true
                        CloudSyncStatus.SYNCING -> LocalizationManager.getString("syncing", language) to true
                        CloudSyncStatus.PENDING -> LocalizationManager.getString("pending", language) to false
                        CloudSyncStatus.FAILED -> LocalizationManager.getString("failed", language) to false
                        CloudSyncStatus.OFFLINE -> LocalizationManager.getString("offline", language) to false
                    }

                    StatusIndicatorCard(
                        modifier = Modifier.weight(1f),
                        title = LocalizationManager.getString("cloud_sync", language),
                        statusText = syncLabel,
                        isPositive = syncPositive,
                        icon = Icons.Default.Sync
                    )

                    // Pending Transactions Count Card
                    StatusIndicatorCard(
                        modifier = Modifier.weight(1f),
                        title = LocalizationManager.getString("pending_transactions", language),
                        statusText = "$pendingTransactions موارد",
                        isPositive = pendingTransactions == 0,
                        icon = Icons.Default.PendingActions
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Support Summary Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizationManager.getString("support", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = LocalizationManager.getString("open_tickets", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$openTicketsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Text(
                                text = LocalizationManager.getString("unread_messages", language),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$unreadMessagesCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (unreadMessagesCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.triggerManualSync() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_sync_now"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = LocalizationManager.getString("syncing", language))
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizationManager.getString("sync_now", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onNavigate(MainDestination.SUPPORT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_contact_support"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizationManager.getString("contact_support", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                ElevatedButton(
                    onClick = { onNavigate(MainDestination.SUPPORT) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_my_support_tickets"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizationManager.getString("my_tickets", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (lastSyncTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "آخرین همگام‌سازی: ${LocalizationManager.formatDateTime(lastSyncTime!!, "yyyy-MM-dd HH:mm:ss")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun StatusIndicatorCard(
    modifier: Modifier = Modifier,
    title: String,
    statusText: String,
    isPositive: Boolean,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (isPositive) Color(0xFF388E3C) else Color(0xFFD32F2F),
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
