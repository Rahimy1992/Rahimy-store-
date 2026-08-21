package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.domain.localization.LocalizationState
import com.example.util.BluetoothThermalPrinterManager
import com.example.util.DiscoveredBluetoothDevice
import kotlinx.coroutines.launch

@Composable
fun BluetoothThermalPrinterDialog(
    sale: Sale,
    items: List<SaleItem>,
    localization: LocalizationState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

    var pairedDevices by remember { mutableStateOf(emptyList<DiscoveredBluetoothDevice>()) }
    var selectedDevice by remember { mutableStateOf<DiscoveredBluetoothDevice?>(null) }
    var isPrinting by remember { mutableStateOf(false) }
    var printStatusText by remember { mutableStateOf<String?>(null) }

    fun refreshDevices() {
        if (!BluetoothThermalPrinterManager.isBluetoothEnabled()) {
            printStatusText = if (isFa) "بلوتوث دستگاه خاموش است. لطفاً بلوتوث را روشن کنید." else "Bluetooth is turned off."
            pairedDevices = emptyList()
        } else {
            pairedDevices = BluetoothThermalPrinterManager.getPairedDevices()
            if (pairedDevices.isEmpty()) {
                printStatusText = if (isFa) "هیچ پرینتر جفت‌شده‌ای پیدا نشد. ابتدا پرینتر حرارتی را جفت کنید." else "No paired Bluetooth printers found."
            } else {
                printStatusText = null
                if (selectedDevice == null) {
                    selectedDevice = pairedDevices.firstOrNull()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDevices()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFa) "چاپ مستقیم با پرینتر حرارتی بلوتوث" else "Bluetooth Thermal Printer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (isFa) "پرینتر حرارتی ۵۸mm یا ۸۰mm بلوتوثی را جهت چاپ فاکتور #${sale.invoiceNumber} انتخاب کنید:"
                           else "Select Bluetooth ESC/POS Thermal Printer for Invoice #${sale.invoiceNumber}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (pairedDevices.isNotEmpty()) {
                    Text(
                        text = if (isFa) "دستگاه‌های بلوتوث آماده به کار:" else "Paired Thermal Printers:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pairedDevices) { dev ->
                            val isSelected = selectedDevice?.address == dev.address
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDevice = dev }
                                    .testTag("item_printer_${dev.address}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedDevice = dev }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dev.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "MAC: ${dev.address}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.BluetoothConnected,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (printStatusText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = printStatusText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { refreshDevices() },
                    modifier = Modifier.fillMaxWidth().testTag("btn_refresh_printers")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFa) "جستجوی مجدد پرینترها" else "Refresh Bluetooth Devices")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dev = selectedDevice
                    if (dev == null) {
                        Toast.makeText(context, if (isFa) "لطفاً یک پرینتر انتخاب کنید." else "Please select a printer", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isPrinting = true
                    printStatusText = if (isFa) "در حال ارسال اطلاعات به پرینتر حرارتی..." else "Sending data to printer..."

                    scope.launch {
                        val escPosBytes = BluetoothThermalPrinterManager.formatEscPosInvoice(
                            sale = sale,
                            items = items,
                            localization = localization
                        )

                        val result = BluetoothThermalPrinterManager.sendRawDataToPrinter(dev.device, escPosBytes)

                        isPrinting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, if (isFa) "فاکتور با موفقیت روی پرینتر چاپ شد! ✅" else "Receipt printed successfully! ✅", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "Unknown error"
                            printStatusText = if (isFa) "خطا در چاپ: $err" else "Print error: $err"
                        }
                    }
                },
                enabled = !isPrinting && selectedDevice != null,
                modifier = Modifier.testTag("btn_start_thermal_print")
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (isFa) "چاپ فاکتور (Thermal Print)" else "Print Receipt Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFa) "انصراف" else "Cancel")
            }
        }
    )
}
