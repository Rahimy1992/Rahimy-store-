package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AuditLog
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import kotlinx.coroutines.flow.Flow

sealed class SecurityResult<out T> {
    data class Success<out T>(val data: T) : SecurityResult<T>()
    data class Denied(val reason: String) : SecurityResult<Nothing>()
}

class UserRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)

    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    /**
     * Requirement 14 Security Rule:
     * - Managers can create employees.
     * - Manager cannot create Super Admin.
     * - Employees and Customers cannot create users.
     */
    suspend fun createUser(
        newUser: User,
        actingUser: User
    ): SecurityResult<Long> {
        // Enforce role permissions
        when (actingUser.role) {
            UserRole.SUPER_ADMIN -> {
                // Super Admin can create any role
            }
            UserRole.MANAGER -> {
                if (newUser.role == UserRole.SUPER_ADMIN) {
                    return SecurityResult.Denied("Security Violation: Managers cannot create or promote to Super Admin.")
                }
            }
            UserRole.EMPLOYEE, UserRole.CUSTOMER, UserRole.VIEWER -> {
                return SecurityResult.Denied("Access Denied: Only Managers and Super Admins can create staff accounts.")
            }
        }

        val existing = userDao.getUserByUsername(newUser.username)
        if (existing != null) {
            return SecurityResult.Denied("Username '${newUser.username}' already exists.")
        }

        val id = userDao.insertUser(newUser)
        auditLogDao.insertLog(
            AuditLog(
                userId = actingUser.id,
                username = actingUser.username,
                userRole = actingUser.role.name,
                actionType = "USER_CREATED",
                description = "Created user '${newUser.username}' with role '${newUser.role.name}'",
                detailsJson = "{\"createdUserId\":$id,\"role\":\"${newUser.role.name}\"}"
            )
        )
        return SecurityResult.Success(id)
    }

    /**
     * Requirement 14 Security Rule:
     * - Users CANNOT modify their own role.
     * - Manager CANNOT promote themselves or anyone to Super Admin.
     * - Employee CANNOT promote themselves.
     * - Customer CANNOT gain administrative access.
     */
    suspend fun updateUserRole(
        targetUserId: Long,
        newRole: UserRole,
        actingUser: User
    ): SecurityResult<Unit> {
        val targetUser = userDao.getUserById(targetUserId)
            ?: return SecurityResult.Denied("User not found.")

        // Rule: Users cannot modify their own role
        if (actingUser.id == targetUserId) {
            return SecurityResult.Denied("Security Violation: Users are strictly forbidden from modifying their own role.")
        }

        // Rule: Customer and Viewer cannot perform administrative operations
        if (actingUser.role == UserRole.CUSTOMER || actingUser.role == UserRole.VIEWER) {
            return SecurityResult.Denied("Access Denied: Customer and Viewer accounts cannot perform administrative actions.")
        }

        // Rule: Employee cannot promote themselves or anyone
        if (actingUser.role == UserRole.EMPLOYEE) {
            return SecurityResult.Denied("Access Denied: Employees do not have permission to alter user roles.")
        }

        // Rule: Manager cannot promote anyone to Super Admin
        if (actingUser.role == UserRole.MANAGER && newRole == UserRole.SUPER_ADMIN) {
            return SecurityResult.Denied("Security Violation: Managers cannot promote accounts to Super Admin.")
        }

        // Rule: Manager cannot modify Super Admin accounts
        if (actingUser.role == UserRole.MANAGER && targetUser.role == UserRole.SUPER_ADMIN) {
            return SecurityResult.Denied("Security Violation: Managers cannot alter Super Admin permissions.")
        }

        userDao.updateUserRole(targetUserId, newRole)
        auditLogDao.insertLog(
            AuditLog(
                userId = actingUser.id,
                username = actingUser.username,
                userRole = actingUser.role.name,
                actionType = "USER_ROLE_CHANGED",
                description = "Changed role of '${targetUser.username}' from '${targetUser.role.name}' to '${newRole.name}'",
                detailsJson = "{\"targetUserId\":$targetUserId,\"oldRole\":\"${targetUser.role.name}\",\"newRole\":\"${newRole.name}\"}"
            )
        )
        return SecurityResult.Success(Unit)
    }

    suspend fun updateUserProfile(user: User): SecurityResult<Unit> {
        userDao.updateUser(user)
        auditLogDao.insertLog(
            AuditLog(
                userId = user.id,
                username = user.username,
                userRole = user.role.name,
                actionType = "USER_PROFILE_UPDATED",
                description = "Updated profile for '${user.username}' (DisplayName: '${user.displayName}')"
            )
        )
        return SecurityResult.Success(Unit)
    }

    suspend fun updateUserPin(userId: Long, oldPin: String, newPin: String): SecurityResult<Unit> {
        val user = userDao.getUserById(userId) ?: return SecurityResult.Denied("User not found.")
        if (user.pin.isNotBlank() && user.pin != oldPin) {
            return SecurityResult.Denied("Incorrect current PIN.")
        }
        userDao.updateUserPin(userId, newPin)
        auditLogDao.insertLog(
            AuditLog(
                userId = user.id,
                username = user.username,
                userRole = user.role.name,
                actionType = "USER_PIN_CHANGED",
                description = "Changed PIN for '${user.username}'"
            )
        )
        return SecurityResult.Success(Unit)
    }

    suspend fun resetPinWithMasterCode(username: String, masterCode: String, newPin: String): SecurityResult<Unit> {
        // Master code verification (e.g. "9999" or "123456" or "admin")
        val validMasterCodes = listOf("9999", "123456", "admin")
        if (!validMasterCodes.contains(masterCode.trim())) {
            return SecurityResult.Denied("Invalid Master Passcode. Please contact Super Admin.")
        }
        val user = userDao.getUserByUsername(username.trim())
            ?: return SecurityResult.Denied("User '@$username' not found.")
        
        userDao.updateUserPin(user.id, newPin)
        auditLogDao.insertLog(
            AuditLog(
                userId = user.id,
                username = user.username,
                userRole = user.role.name,
                actionType = "USER_PIN_RESET_MASTER",
                description = "PIN reset for user '${user.username}' using Super Admin Master Passcode"
            )
        )
        return SecurityResult.Success(Unit)
    }

    /**
     * Requirement 14: Check permissions for sensitive operations
     */
    fun canModifyCurrencySettings(user: User): Boolean {
        return user.role == UserRole.SUPER_ADMIN || user.role == UserRole.MANAGER
    }

    fun canViewFinancialReports(user: User): Boolean {
        return user.role == UserRole.SUPER_ADMIN || user.role == UserRole.MANAGER || user.role == UserRole.VIEWER
    }

    fun canAccessManagementAi(user: User): Boolean {
        return user.role == UserRole.SUPER_ADMIN || user.role == UserRole.MANAGER
    }

    fun canPerformBulkOperations(user: User): Boolean {
        return user.role == UserRole.SUPER_ADMIN || user.role == UserRole.MANAGER
    }

    fun canVoidSales(user: User): Boolean {
        return user.role == UserRole.SUPER_ADMIN || user.role == UserRole.MANAGER
    }
}
