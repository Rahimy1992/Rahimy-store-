package com.example.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.local.entity.Sale
import com.example.data.local.entity.SaleItem
import com.example.domain.localization.LocalizationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val device: BluetoothDevice
)

object BluetoothThermalPrinterManager {

    private const val TAG = "BluetoothThermalPrinter"
    // Standard SPP UUID for Bluetooth serial printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun getBluetoothAdapter(): BluetoothAdapter? {
        return try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (e: Exception) {
            null
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return getBluetoothAdapter()?.isEnabled == true
    }

    fun getPairedDevices(): List<DiscoveredBluetoothDevice> {
        val adapter = getBluetoothAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            val paired = adapter.bondedDevices ?: emptySet()
            paired.map { device ->
                DiscoveredBluetoothDevice(
                    name = device.name ?: "Unknown Printer",
                    address = device.address,
                    isPaired = true,
                    device = device
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching paired devices: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendRawDataToPrinter(
        device: BluetoothDevice,
        data: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            Log.i(TAG, "Connecting to printer: ${device.address}")
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            outputStream.write(data)
            outputStream.flush()

            Log.i(TAG, "Print data successfully sent to thermal printer!")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data to thermal printer: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Converts sale invoice to ESC/POS bytes for 58mm / 80mm thermal receipts
     */
    fun formatEscPosInvoice(
        sale: Sale,
        items: List<SaleItem>,
        localization: LocalizationState
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // ESC/POS Commands
        val initPrinter = byteArrayOf(0x1B, 0x40) // Initialize
        val alignCenter = byteArrayOf(0x1B, 0x61, 0x01) // Center
        val alignLeft = byteArrayOf(0x1B, 0x61, 0x00) // Left
        val textBoldOn = byteArrayOf(0x1B, 0x45, 0x01) // Bold on
        val textBoldOff = byteArrayOf(0x1B, 0x45, 0x00) // Bold off
        val feedAndCut = byteArrayOf(0x1D, 0x56, 0x41, 0x10) // Cut paper

        bytes.addAll(initPrinter.toList())
        bytes.addAll(alignCenter.toList())
        bytes.addAll(textBoldOn.toList())

        val receiptHeader = """
            ================================
                 RAHIMY SMART COMMERCE
               صرافی و صندوق هوشمند رحیمی
            ================================
        """.trimIndent()

        bytes.addAll(receiptHeader.toByteArray(Charsets.UTF_8).toList())
        bytes.add(0x0A.toByte())

        bytes.addAll(alignLeft.toList())
        bytes.addAll(textBoldOff.toList())

        val isFa = localization.isRtl || localization.currentLanguage.code in listOf("fa", "prs", "ps")

        val receiptDetails = PrintInvoiceManager.generateReceiptText(
            sale = sale,
            items = items,
            localization = localization
        )

        bytes.addAll(receiptDetails.toByteArray(Charsets.UTF_8).toList())
        bytes.add(0x0A.toByte())
        bytes.add(0x0A.toByte())
        bytes.addAll(feedAndCut.toList())

        return bytes.toByteArray()
    }
}
