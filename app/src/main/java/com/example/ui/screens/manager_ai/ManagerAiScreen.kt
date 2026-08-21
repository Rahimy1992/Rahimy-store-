package com.example.ui.screens.manager_ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.ChatMessage
import com.example.data.ai.MessageSender
import com.example.data.local.entity.UserRole
import com.example.ui.AppViewModel

@Composable
fun ManagerAiScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.managerMessages.collectAsState()
    val isThinking by viewModel.isManagerAiThinking.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val localization by viewModel.localization.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")
    val isAuthorized = currentUser.role == UserRole.SUPER_ADMIN || currentUser.role == UserRole.MANAGER

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Manager Quick Business Queries (Requirement 8)
    val managerQueries = if (isFa) listOf(
        "سود ناخالص و حاشیه سود امروز چقدر است؟",
        "کدام اجناس کمبود موجودی دارند و نیاز به سفارش مجدد دارند؟",
        "فروش تحویل‌داران و صندوق‌داران چگونه توزیع شده است؟",
        "میانگین ارزش فاکتور (AOV) و فروش روزانه چقدر است؟",
        "خلاصه گزارش مدیریتی تجارت را ارائه بده."
    ) else listOf(
        "What is our gross profit and margin today?",
        "Which products are low in stock and need reordering?",
        "How are cashier sales distributed?",
        "What is our Average Order Value and daily sales?",
        "Provide an executive performance summary."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isFa) "هوش مصنوعی مدیریت تجارت" else "Management Business AI",
                    style = MaterialTheme.typography.headlineMedium
                )
                val roleNameFa = when (currentUser.role) {
                    UserRole.SUPER_ADMIN -> "مدیر کل"
                    UserRole.MANAGER -> "مدیر سیستم"
                    UserRole.EMPLOYEE -> "تحویل‌دار"
                    UserRole.CUSTOMER -> "مشتری"
                    else -> "مشاهده‌کننده"
                }
                Text(
                    text = if (isFa) "مشاور تحلیل ارشد • سطح دسترسی: $roleNameFa" else "Executive BI Advisor • Access: ${currentUser.role.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Surface(
                color = if (isAuthorized) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isAuthorized) Icons.Default.Analytics else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isAuthorized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (!isAuthorized) {
            // Security Lock View for unauthorized roles (Requirement 14)
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = if (isFa) "دسترسی محدود شده است" else "Access Restricted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (isFa) "دستیار هوش مصنوعی مدیریت حاوی اطلاعات حساس سود، قیمت تمام‌شده و عملکرد کارمندان است. دسترسی به آن به مدیران سیستم و مدیران کل محدود می‌باشد." else "Management AI Assistant contains sensitive live store profit, cost, and staff metrics. It is restricted strictly to Store Managers and Super Administrators.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (isFa) "جهت دسترسی، نقش کاربر را در بخش تنظیمات تغییر دهید." else "Switch active user role in Settings to access.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            // Quick Queries Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(managerQueries) { q ->
                    SuggestionChip(
                        onClick = { viewModel.sendManagerMessage(q) },
                        label = { Text(q, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("chip_mgr_${q.take(10)}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ManagerChatBubble(message = msg, isFa = isFa)
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFa) "مشاور تحلیل ارشد در حال ارزیابی آمار فروش، هزینه‌ها و سود تجارت است..." else "BI Advisor is evaluating store sales, costs, and profit metrics...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(if (isFa) "از مشاور تحلیل ارشد درباره فروش، سود و روندهای مالی بپرسید..." else "Ask BI Advisor about sales, profits, trends...") },
                    modifier = Modifier.weight(1f).testTag("manager_chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendManagerMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("btn_send_manager_chat")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ManagerChatBubble(message: ChatMessage, isFa: Boolean) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "مشاور تحلیل ارشد تجارت" else "Business Intelligence Advisor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.QueryStats,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
