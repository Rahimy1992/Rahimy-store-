package com.example.ui.screens.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun BulkEditProductsDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onApply: (priceAdjPercent: Double?, newCategory: String?, stockAdj: Int?) -> Unit
) {
    var priceAdjStr by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var stockAdjStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bulk Edit Products",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Modifying $selectedCount selected items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Leave any field blank to keep current values unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = priceAdjStr,
                    onValueChange = { priceAdjStr = it },
                    label = { Text("Price Adjustment % (e.g. 10 for +10%, -5 for -5%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("bulk_price_adj_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("Assign New Category") },
                    modifier = Modifier.fillMaxWidth().testTag("bulk_category_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stockAdjStr,
                    onValueChange = { stockAdjStr = it },
                    label = { Text("Stock Quantity Adjustment (e.g. +25 or -10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("bulk_stock_adj_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val priceAdj = priceAdjStr.toDoubleOrNull()
                            val cat = newCategory.trim().ifBlank { null }
                            val stockAdj = stockAdjStr.toIntOrNull()
                            onApply(priceAdj, cat, stockAdj)
                        },
                        modifier = Modifier.testTag("bulk_apply_btn")
                    ) {
                        Text("Apply to $selectedCount Products")
                    }
                }
            }
        }
    }
}
