package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entity.SaleStatus
import com.example.data.local.entity.ScanStatus
import com.example.data.local.entity.UserRole
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSaleStatus(status: SaleStatus): String = status.name

    @TypeConverter
    fun toSaleStatus(value: String): SaleStatus = try {
        SaleStatus.valueOf(value)
    } catch (e: Exception) {
        SaleStatus.ACTIVE
    }

    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.EMPLOYEE
    }

    @TypeConverter
    fun fromScanStatus(status: ScanStatus): String = status.name

    @TypeConverter
    fun toScanStatus(value: String): ScanStatus = try {
        ScanStatus.valueOf(value)
    } catch (e: Exception) {
        ScanStatus.PENDING_REVIEW
    }
}
