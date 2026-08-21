package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.CloudBackupService
import com.example.data.cloud.CloudSyncManager
import com.example.data.cloud.SyncState
import com.example.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CloudReadinessTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCloudSyncManagerQueueAndOfflineReconciliation() = runBlocking {
        val syncManager = CloudSyncManager(context, database)
        assertEquals(SyncState.IDLE, syncManager.syncState.value)

        syncManager.enqueuePendingChange("PRODUCT", 1L, "UPSERT")
        syncManager.enqueuePendingChange("SALE", 101L, "UPSERT")

        val report = syncManager.performSync()
        assertTrue(report.isSuccess)
        assertEquals(SyncState.COMPLETED, syncManager.syncState.value)
        assertNotNull(syncManager.lastSyncReport.value)
    }

    @Test
    fun testCloudBackupServiceExport() = runBlocking {
        val backupService = CloudBackupService(context, database)
        val result = backupService.createEncryptedBackup(userId = 1L, username = "admin")

        assertTrue(result.isSuccess)
        assertNotNull(result.backupFilePath)
        assertTrue(result.backupFilePath!!.contains("backup_"))
    }
}
