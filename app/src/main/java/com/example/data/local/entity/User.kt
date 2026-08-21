package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    SUPER_ADMIN,
    MANAGER,
    EMPLOYEE,
    VIEWER,
    CUSTOMER
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val pin: String = "1234",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
