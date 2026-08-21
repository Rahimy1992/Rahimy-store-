package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.localization.LocalizationManager
import com.example.domain.localization.SupportedLanguage

const val RAHIMY_CONTACT_NUMBER = "+93703676227"

fun openDialer(context: Context, phoneNumber: String = RAHIMY_CONTACT_NUMBER) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsApp(context: Context, phoneNumber: String = RAHIMY_CONTACT_NUMBER) {
    val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")
    val appUri = Uri.parse("whatsapp://send?phone=$cleanNumber")
    val webUri = Uri.parse("https://wa.me/$cleanNumber")

    val appIntent = Intent(Intent.ACTION_VIEW, appUri)
    val webIntent = Intent(Intent.ACTION_VIEW, webUri)

    try {
        context.startActivity(appIntent)
    } catch (e: Exception) {
        // Fallback to web browser wa.me link if app is not installed
        try {
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open WhatsApp link", Toast.LENGTH_SHORT).show()
        }
    }
}

fun openTelegram(context: Context, phoneNumber: String = RAHIMY_CONTACT_NUMBER) {
    val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")
    val appUri = Uri.parse("tg://resolve?phone=$cleanNumber")
    val webUri = Uri.parse("https://t.me/+$cleanNumber")

    val appIntent = Intent(Intent.ACTION_VIEW, appUri)
    val webIntent = Intent(Intent.ACTION_VIEW, webUri)

    try {
        context.startActivity(appIntent)
    } catch (e: Exception) {
        // Fallback to web browser t.me link if app is not installed
        try {
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open Telegram link", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AboutUsSection(
    language: SupportedLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layoutDirection = language.layoutDirection

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("card_about_us"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = LocalizationManager.getString("about_us", language),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = LocalizationManager.getString("company_name", language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = LocalizationManager.getString("about_us_description", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Contact Details Display
                Text(
                    text = LocalizationManager.getString("contact_info", language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Phone / WhatsApp Number Display
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${LocalizationManager.getString("phone_whatsapp", language)}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = RAHIMY_CONTACT_NUMBER,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("txt_phone_whatsapp_number")
                            )
                        }
                    }

                    // Telegram Number Display
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF0088CC),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${LocalizationManager.getString("telegram", language)}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = RAHIMY_CONTACT_NUMBER,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0088CC),
                                modifier = Modifier.testTag("txt_telegram_number")
                            )
                        }
                    }
                }

                // Interactive Action Buttons: Call, WhatsApp, Telegram
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Call Button
                    Button(
                        onClick = { openDialer(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_call"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LocalizationManager.getString("call_button", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // WhatsApp Button
                    Button(
                        onClick = { openWhatsApp(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_whatsapp"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LocalizationManager.getString("whatsapp_button", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Telegram Button
                    Button(
                        onClick = { openTelegram(context) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_telegram"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0088CC),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LocalizationManager.getString("telegram_button", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
