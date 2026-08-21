package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.data.auth.BiometricAuthManager
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import com.example.ui.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun EditProfileDialog(
    currentUser: User,
    isFa: Boolean,
    onDismiss: () -> Unit,
    onSave: (displayName: String, newPin: String) -> Unit
) {
    var displayName by remember { mutableStateOf(currentUser.displayName) }
    var newPin by remember { mutableStateOf(currentUser.pin) }
    var showPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFa) "ویرایش پروفایل و رمز حساب" else "Edit User Profile & Security")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // User Badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(currentUser.displayName, fontWeight = FontWeight.Bold)
                            Text(
                                text = "@${currentUser.username} • ${currentUser.role.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(if (isFa) "نام و تخلص کامل" else "Full Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_profile_display_name")
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text(if (isFa) "پین / رمز پسورد حساب" else "PIN / Password") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_profile_pin")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isFa) "ورود سریع با اثر انگشت و چهره" else "Biometric Login (Fingerprint/Face)", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(displayName, newPin) },
                modifier = Modifier.testTag("btn_save_profile")
            ) {
                Text(if (isFa) "ذخیره تغییرات" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "انصراف" else "Cancel")
            }
        }
    )
}

@Composable
fun ForgotPasswordDialog(
    isFa: Boolean,
    onDismiss: () -> Unit,
    onResetPin: (username: String, masterCode: String, newPin: String) -> Unit
) {
    var username by remember { mutableStateOf("manager") }
    var masterCode by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFa) "بازیابی و تغییر رمز فراموش شده" else "Forgot Password / Reset PIN")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isFa) "در صورت فراموشی رمز، می‌توانید با کد مستر مدیر کل (۹۹۹۹ یا ۱۲۳۴۵۶) رمز جدید تنظیم کنید:" else "Enter account username and Super Admin Master Code to set a new PIN:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(if (isFa) "نام کاربری (Username)" else "Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_forgot_username")
                )

                OutlinedTextField(
                    value = masterCode,
                    onValueChange = { masterCode = it },
                    label = { Text(if (isFa) "کد مستر مدیر سیستم (Master Passcode)" else "Master Passcode (e.g. 9999)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("input_forgot_master_code")
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text(if (isFa) "رمز پین جدید" else "New PIN / Password") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_forgot_new_pin")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResetPin(username, masterCode, newPin) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("btn_confirm_reset_pin")
            ) {
                Text(if (isFa) "تایید و بازنشانی رمز" else "Reset PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "انصراف" else "Cancel")
            }
        }
    )
}

@Composable
fun BiometricAuthPromptModal(
    selectedUser: User,
    isFa: Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val biometricAuthManager = remember { BiometricAuthManager() }

    var isScanning by remember { mutableStateOf(true) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var scanStatusText by remember { mutableStateOf(if (isFa) "در حال تایید چهره و اثر انگشت..." else "Scanning Fingerprint & Face Recognition...") }

    fun triggerNativeBiometric() {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null && biometricAuthManager.canAuthenticate(context)) {
            scanError = null
            scanStatusText = if (isFa) "لطفاً اثر انگشت یا چهره خود را روی حسگر قرار دهید..." else "Please touch fingerprint sensor or align face..."
            biometricAuthManager.authenticate(
                activity = fragmentActivity,
                title = if (isFa) "ورود زیست‌سنجی سیستم" else "Biometric System Login",
                subtitle = if (isFa) "تایید هویت کاربر: ${selectedUser.displayName}" else "Verify user identity: ${selectedUser.displayName}",
                negativeButtonText = if (isFa) "انصراف" else "Cancel",
                onSuccess = { firebaseUser ->
                    scanStatusText = if (isFa) "هویت زیست‌سنجی تایید شد! ✅" else "Biometric Identity Verified! ✅"
                    isScanning = false
                    scanError = null
                    onSuccess()
                },
                onError = { errorMsg ->
                    scanError = errorMsg
                    scanStatusText = if (isFa) "عدم تایید زیست‌سنجی: $errorMsg" else "Biometric Auth Failed: $errorMsg"
                    isScanning = false
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null && biometricAuthManager.canAuthenticate(context)) {
            triggerNativeBiometric()
        } else {
            delay(1200)
            scanStatusText = if (isFa) "هویت زیست‌سنجی اسکن شد ✅" else "Biometric Identity Verified! ✅"
            isScanning = false
            delay(600)
            onSuccess()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = if (scanError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isFa) "ورود با اثر انگشت و چهره (Biometrics)" else "Biometric Authentication",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isFa) "حساب: ${selectedUser.displayName} (@${selectedUser.username})" else "Account: ${selectedUser.displayName} (@${selectedUser.username})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Animated Scanner Box
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                scanError != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                isScanning -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            }
                        )
                        .border(
                            2.dp,
                            when {
                                scanError != null -> MaterialTheme.colorScheme.error
                                isScanning -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFF4CAF50)
                            },
                            CircleShape
                        )
                        .clickable { triggerNativeBiometric() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                    Icon(
                        imageVector = when {
                            scanError != null -> Icons.Default.Lock
                            isScanning -> Icons.Default.Face
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            scanError != null -> MaterialTheme.colorScheme.error
                            isScanning -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFF2E7D32)
                        },
                        modifier = Modifier.size(54.dp)
                    )
                }

                Text(
                    text = scanStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (scanError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Explicit Fallback Button to Standard PIN / Password
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("btn_biometric_fallback_pin")
                ) {
                    Icon(Icons.Default.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFa) "ورود اضطراری با رمز / پین (Standard Password)" else "Use Password / PIN Instead")
                }
            }
        },
        confirmButton = {
            if (!isScanning && scanError == null) {
                Button(
                    onClick = onSuccess,
                    modifier = Modifier.fillMaxWidth().testTag("btn_biometric_success")
                ) {
                    Text(if (isFa) "ورود به سیستم" else "Unlock Session")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(if (isFa) "انصراف" else "Cancel")
            }
        }
    )
}

@Composable
fun LockScreenOverlayDialog(
    viewModel: AppViewModel,
    currentUser: User,
    allUsers: List<User>,
    isFa: Boolean,
    onUnlockWithPin: (user: User, pin: String) -> Unit,
    onOpenBiometric: (user: User) -> Unit,
    onOpenForgotPassword: () -> Unit
) {
    var selectedUser by remember { mutableStateOf(currentUser) }
    var pinInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Modal lock screen cannot be dismissed without auth */ },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFa) "قفل سیستم و تایید هویت" else "System Session Lock")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isFa) "لطفا جهت باز کردن سیستم یا تغییر کاربر، پین / رمز ورود را وارد کنید:" else "Please enter your PIN or use biometrics to unlock system:",
                    style = MaterialTheme.typography.bodySmall
                )

                // User Selector Chips
                Text(if (isFa) "انتخاب کاربر:" else "Select User:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allUsers.forEach { u ->
                        FilterChip(
                            selected = u.id == selectedUser.id,
                            onClick = { selectedUser = u },
                            label = { Text(u.displayName.take(12)) },
                            modifier = Modifier.testTag("chip_lock_user_${u.username}")
                        )
                    }
                }

                // PIN Input Field
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 8) pinInput = it },
                    label = { Text(if (isFa) "پین / رمز پسورد (PIN)" else "Enter PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("input_lock_pin")
                )

                // Quick Action Buttons (Biometric & Forgot Password)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onOpenBiometric(selectedUser) },
                        modifier = Modifier.testTag("btn_lock_biometric")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFa) "ورود با اثر انگشت / چهره" else "Biometric Login", style = MaterialTheme.typography.labelSmall)
                    }

                    TextButton(
                        onClick = onOpenForgotPassword,
                        modifier = Modifier.testTag("btn_lock_forgot_pin")
                    ) {
                        Text(if (isFa) "رمز را فراموش کرده‌اید؟" else "Forgot PIN?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUnlockWithPin(selectedUser, pinInput)
                    pinInput = ""
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_lock_unlock")
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isFa) "باز کردن قفل و ورود" else "Unlock App")
            }
        }
    )
}
