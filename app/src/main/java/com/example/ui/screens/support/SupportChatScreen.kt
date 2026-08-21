package com.example.ui.screens.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SupportMessageEntity
import com.example.data.local.entity.SupportStatus
import com.example.data.local.entity.SupportTicketEntity
import com.example.data.local.entity.UserRole
import com.example.domain.localization.LocalizationManager
import com.example.ui.AppViewModel
import com.example.ui.components.AboutUsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    viewModel: AppViewModel
) {
    val localization by viewModel.localization.collectAsState()
    val language = localization.currentLanguage
    val currentUser by viewModel.currentUser.collectAsState()
    val isInternetConnected by viewModel.isInternetConnected.collectAsState()

    val tickets by viewModel.supportTickets.collectAsState()
    val selectedTicket by viewModel.selectedSupportTicket.collectAsState()
    val messages by viewModel.supportMessages.collectAsState()

    var showNewTicketDialog by remember { mutableStateOf(false) }
    var selectedFilterStatus by remember { mutableStateOf<String?>(null) }
    var messageTextInput by remember { mutableStateOf("") }
    var attachmentUrlInput by remember { mutableStateOf<String?>(null) }

    val userRole = currentUser.role.name
    val canManageTickets = userRole == UserRole.SUPER_ADMIN.name || userRole == UserRole.MANAGER.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTicket != null) selectedTicket!!.subject else LocalizationManager.getString("support_chat", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    if (selectedTicket != null) {
                        IconButton(onClick = { viewModel.selectSupportTicket(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedTicket == null) {
                        Button(
                            onClick = { showNewTicketDialog = true },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("btn_create_ticket"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = LocalizationManager.getString("contact_support", language))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTicket == null) {
                // Ticket List View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // About Us & Direct Contact Banner
                    AboutUsSection(
                        language = language,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilterStatus == null,
                            onClick = { selectedFilterStatus = null },
                            label = { Text("همه") }
                        )
                        FilterChip(
                            selected = selectedFilterStatus == "OPEN",
                            onClick = { selectedFilterStatus = "OPEN" },
                            label = { Text(LocalizationManager.getString("open", language)) }
                        )
                        FilterChip(
                            selected = selectedFilterStatus == "IN_PROGRESS",
                            onClick = { selectedFilterStatus = "IN_PROGRESS" },
                            label = { Text(LocalizationManager.getString("in_progress", language)) }
                        )
                        FilterChip(
                            selected = selectedFilterStatus == "RESOLVED",
                            onClick = { selectedFilterStatus = "RESOLVED" },
                            label = { Text(LocalizationManager.getString("resolved", language)) }
                        )
                    }

                    val filteredTickets = tickets.filter { ticket ->
                        selectedFilterStatus == null || ticket.status == selectedFilterStatus
                    }

                    if (filteredTickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.ConfirmationNumber,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "هیچ درخواست پشتیبانی یافت نشد",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showNewTicketDialog = true }) {
                                    Text(LocalizationManager.getString("contact_support", language))
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredTickets, key = { it.ticketId }) { ticket ->
                                TicketItemCard(
                                    ticket = ticket,
                                    onClick = { viewModel.selectSupportTicket(ticket) },
                                    language = language
                                )
                            }
                        }
                    }
                }
            } else {
                // Active Ticket Chat Screen
                val currentT = selectedTicket!!
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar for Active Ticket Details & RBAC Actions
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentT.subject,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "کاربر: ${currentT.userName} | کد: ${currentT.ticketId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                StatusBadge(status = currentT.status, language = language)
                            }

                            // Staff / SuperAdmin Status Dropdown Changer
                            if (canManageTickets) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "تغییر وضعیت:",
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    var statusExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { statusExpanded = true },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(currentT.status, fontSize = 12.sp)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }

                                        DropdownMenu(
                                            expanded = statusExpanded,
                                            onDismissRequest = { statusExpanded = false }
                                        ) {
                                            listOf("OPEN", "IN_PROGRESS", "WAITING_FOR_CUSTOMER", "RESOLVED", "CLOSED").forEach { st ->
                                                DropdownMenuItem(
                                                    text = { Text(st) },
                                                    onClick = {
                                                        viewModel.updateSupportTicketStatus(currentT.ticketId, st)
                                                        statusExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Message List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.messageId }) { msg ->
                            val isMe = msg.senderId == currentUser.id.toString() || msg.senderId == currentUser.username
                            MessageBubble(
                                message = msg,
                                isMe = isMe,
                                language = language,
                                onRetry = {
                                    viewModel.sendSupportMessage(
                                        ticketId = currentT.ticketId,
                                        text = msg.text,
                                        attachmentUrl = msg.attachmentUrl
                                    )
                                }
                            )
                        }
                    }

                    // Offline Warning Notice if typing while disconnected
                    if (!isInternetConnected) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "پیام در صف آفلاین ذخیره شده و پس از اتصال خودکار ارسال می‌شود.",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Input Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                attachmentUrlInput = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f"
                            }) {
                                Icon(
                                    Icons.Outlined.AttachFile,
                                    contentDescription = "Attach",
                                    tint = if (attachmentUrlInput != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }

                            OutlinedTextField(
                                value = messageTextInput,
                                onValueChange = { messageTextInput = it },
                                placeholder = { Text(LocalizationManager.getString("send_message", language)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("support_chat_input"),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (messageTextInput.isNotBlank()) {
                                        viewModel.sendSupportMessage(
                                            ticketId = currentT.ticketId,
                                            text = messageTextInput.trim(),
                                            attachmentUrl = attachmentUrlInput
                                        )
                                        messageTextInput = ""
                                        attachmentUrlInput = null
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .testTag("btn_send_message")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // New Ticket Modal Dialog
        if (showNewTicketDialog) {
            NewTicketDialog(
                onDismiss = { showNewTicketDialog = false },
                onCreate = { subject, category, priority, initialMsg ->
                    viewModel.createSupportTicket(
                        subject = subject,
                        category = category,
                        priority = priority,
                        initialMessage = initialMsg
                    )
                    showNewTicketDialog = false
                },
                language = language
            )
        }
    }
}

@Composable
private fun TicketItemCard(
    ticket: SupportTicketEntity,
    onClick: () -> Unit,
    language: com.example.domain.localization.SupportedLanguage
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("ticket_item_${ticket.ticketId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = ticket.status, language = language)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = ticket.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کد: ${ticket.ticketId} | ${ticket.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (ticket.unreadCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("${ticket.unreadCount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: SupportMessageEntity,
    isMe: Boolean,
    language: com.example.domain.localization.SupportedLanguage,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.senderName + " (" + message.senderRole + ")",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (message.attachmentUrl != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📎 فایل ضمیمه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocalizationManager.formatDateTime(message.createdAt, "HH:mm"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (message.isQueuedOffline) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "Queued Offline",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRetry() }
                        )
                    } else {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: String,
    language: com.example.domain.localization.SupportedLanguage
) {
    val (labelKey, bgColor) = when (status) {
        "OPEN" -> "open" to Color(0xFF1E88E5)
        "IN_PROGRESS" -> "in_progress" to Color(0xFFFB8C00)
        "WAITING_FOR_CUSTOMER" -> "waiting_for_customer" to Color(0xFF8E24AA)
        "RESOLVED" -> "resolved" to Color(0xFF43A047)
        "CLOSED" -> "closed" to Color(0xFF757575)
        else -> status to Color.Gray
    }

    Surface(
        color = bgColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = LocalizationManager.getString(labelKey, language),
            color = bgColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTicketDialog(
    onDismiss: () -> Unit,
    onCreate: (subject: String, category: String, priority: String, initialMsg: String) -> Unit,
    language: com.example.domain.localization.SupportedLanguage
) {
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = LocalizationManager.getString("contact_support", language),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("موضوع درخواست *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ticket_subject_input")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("توضیحات و پیام *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("ticket_message_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && message.isNotBlank()) {
                        onCreate(subject, category, priority, message)
                    }
                },
                modifier = Modifier.testTag("btn_submit_ticket")
            ) {
                Text(LocalizationManager.getString("send_message", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
