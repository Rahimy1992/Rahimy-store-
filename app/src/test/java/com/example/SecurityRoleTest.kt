package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLog
import com.example.data.local.entity.User
import com.example.data.local.entity.UserRole
import com.example.data.repository.SecurityResult
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityRoleTest {

    private lateinit var db: AppDatabase
    private lateinit var userRepo: UserRepository

    private lateinit var superAdmin: User
    private lateinit var manager: User
    private lateinit var employee: User
    private lateinit var viewer: User
    private lateinit var customer: User

    @Before
    fun setup() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            userRepo = UserRepository(db.userDao(), db.auditLogDao())

            val id1 = db.userDao().insertUser(User(username = "admin_user", displayName = "Admin", role = UserRole.SUPER_ADMIN))
            val id2 = db.userDao().insertUser(User(username = "manager_user", displayName = "Manager", role = UserRole.MANAGER))
            val id3 = db.userDao().insertUser(User(username = "employee_user", displayName = "Employee", role = UserRole.EMPLOYEE))
            val id4 = db.userDao().insertUser(User(username = "viewer_user", displayName = "Viewer", role = UserRole.VIEWER))
            val id5 = db.userDao().insertUser(User(username = "customer_user", displayName = "Customer", role = UserRole.CUSTOMER))

            superAdmin = db.userDao().getUserById(id1)!!
            manager = db.userDao().getUserById(id2)!!
            employee = db.userDao().getUserById(id3)!!
            viewer = db.userDao().getUserById(id4)!!
            customer = db.userDao().getUserById(id5)!!
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUsersCannotChangeTheirOwnRole() {
        runBlocking {
            // Manager attempts to change their own role to SUPER_ADMIN
            val result1 = userRepo.updateUserRole(manager.id, UserRole.SUPER_ADMIN, actingUser = manager)
            assertTrue("Manager should not be able to modify their own role", result1 is SecurityResult.Denied)

            // Employee attempts to change their own role to MANAGER
            val result2 = userRepo.updateUserRole(employee.id, UserRole.MANAGER, actingUser = employee)
            assertTrue("Employee should not be able to modify their own role", result2 is SecurityResult.Denied)
        }
    }

    @Test
    fun testEmployeesCannotPromoteThemselvesOrOthers() {
        runBlocking {
            val result = userRepo.updateUserRole(viewer.id, UserRole.MANAGER, actingUser = employee)
            assertTrue("Employee cannot alter any user roles", result is SecurityResult.Denied)
        }
    }

    @Test
    fun testManagersCannotCreateOrPromoteToSuperAdmin() {
        runBlocking {
            // Manager attempts to promote employee to SUPER_ADMIN
            val result1 = userRepo.updateUserRole(employee.id, UserRole.SUPER_ADMIN, actingUser = manager)
            assertTrue("Manager cannot promote anyone to Super Admin", result1 is SecurityResult.Denied)

            // Manager attempts to create a new SUPER_ADMIN user
            val newAdmin = User(username = "new_admin", displayName = "New Admin", role = UserRole.SUPER_ADMIN)
            val result2 = userRepo.createUser(newAdmin, actingUser = manager)
            assertTrue("Manager cannot create Super Admin user", result2 is SecurityResult.Denied)
        }
    }

    @Test
    fun testManagersCannotAlterSuperAdminAccounts() {
        runBlocking {
            val result = userRepo.updateUserRole(superAdmin.id, UserRole.MANAGER, actingUser = manager)
            assertTrue("Manager cannot modify Super Admin accounts", result is SecurityResult.Denied)
        }
    }

    @Test
    fun testCustomerAndViewerCannotPerformAdministrativeActions() {
        runBlocking {
            val res1 = userRepo.updateUserRole(employee.id, UserRole.MANAGER, actingUser = customer)
            assertTrue("Customer cannot modify roles", res1 is SecurityResult.Denied)

            val res2 = userRepo.updateUserRole(employee.id, UserRole.MANAGER, actingUser = viewer)
            assertTrue("Viewer cannot modify roles", res2 is SecurityResult.Denied)
        }
    }

    @Test
    fun testRolePermissionsForSensitiveOperations() {
        // Currency settings
        assertTrue(userRepo.canModifyCurrencySettings(superAdmin))
        assertTrue(userRepo.canModifyCurrencySettings(manager))
        assertFalse(userRepo.canModifyCurrencySettings(employee))
        assertFalse(userRepo.canModifyCurrencySettings(viewer))
        assertFalse(userRepo.canModifyCurrencySettings(customer))

        // Void sales
        assertTrue(userRepo.canVoidSales(superAdmin))
        assertTrue(userRepo.canVoidSales(manager))
        assertFalse(userRepo.canVoidSales(employee))
        assertFalse(userRepo.canVoidSales(viewer))
        assertFalse(userRepo.canVoidSales(customer))
    }

    @Test
    fun testAuditLogsAreAppendOnly() {
        runBlocking {
            val initialLogs = db.auditLogDao().getAllLogs().first()
            val countBefore = initialLogs.size

            db.auditLogDao().insertLog(
                AuditLog(
                    userId = superAdmin.id,
                    username = superAdmin.username,
                    userRole = superAdmin.role.name,
                    actionType = "SECURITY_TEST",
                    description = "Security test log entry"
                )
            )

            val logsAfter = db.auditLogDao().getAllLogs().first()
            assertEquals(countBefore + 1, logsAfter.size)
            assertEquals("SECURITY_TEST", logsAfter[0].actionType)
        }
    }
}
