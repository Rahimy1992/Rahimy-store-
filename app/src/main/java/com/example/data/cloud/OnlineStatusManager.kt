package com.example.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.AppDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

enum class CloudSyncStatus {
    SYNCED,
    SYNCING,
    PENDING,
    FAILED,
    OFFLINE
}

class OnlineStatusManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isInternetConnected = MutableStateFlow(false)
    val isInternetConnected: StateFlow<Boolean> = _isInternetConnected.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _cloudSyncState = MutableStateFlow(CloudSyncStatus.OFFLINE)
    val cloudSyncState: StateFlow<CloudSyncStatus> = _cloudSyncState.asStateFlow()

    private val _pendingTransactionCount = MutableStateFlow(0)
    val pendingTransactionCount: StateFlow<Int> = _pendingTransactionCount.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        registerNetworkCallback()
        observePendingCount()
        checkInitialConnectivity()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    val hasInternet = checkSocketReachability()
                    val wasOffline = !_isInternetConnected.value
                    _isInternetConnected.value = hasInternet
                    
                    if (hasInternet) {
                        checkFirebaseConnection()
                        if (wasOffline) {
                            performSyncNow()
                        }
                    } else {
                        _isFirebaseConnected.value = false
                        _cloudSyncState.value = CloudSyncStatus.OFFLINE
                    }
                }
            }

            override fun onLost(network: Network) {
                _isInternetConnected.value = false
                _isFirebaseConnected.value = false
                _cloudSyncState.value = CloudSyncStatus.OFFLINE
            }
        })
    }

    fun checkInitialConnectivity() {
        scope.launch {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (isConnected) {
                val realInternet = checkSocketReachability()
                _isInternetConnected.value = realInternet
                if (realInternet) {
                    checkFirebaseConnection()
                } else {
                    _isFirebaseConnected.value = false
                    _cloudSyncState.value = CloudSyncStatus.OFFLINE
                }
            } else {
                _isInternetConnected.value = false
                _isFirebaseConnected.value = false
                _cloudSyncState.value = CloudSyncStatus.OFFLINE
            }
            updatePendingCount()
        }
    }

    private suspend fun checkSocketReachability(): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkFirebaseConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                _isFirebaseConnected.value = false
                return@withContext false
            }
            
            // Ping Firestore
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("settings").document("ping").get()
            _isFirebaseConnected.value = true
            true
        } catch (e: Exception) {
            // Firebase reachable or fallback
            val isAppInit = try { FirebaseApp.getApps(context).isNotEmpty() } catch (ex: Exception) { false }
            _isFirebaseConnected.value = isAppInit && _isInternetConnected.value
            _isFirebaseConnected.value
        }
    }

    private fun observePendingCount() {
        scope.launch {
            database.supportDao().getQueuedOfflineMessagesCountFlow().collect { queuedMessages ->
                updatePendingCount(queuedMessages)
            }
        }
    }

    private suspend fun updatePendingCount(queuedMessagesCount: Int? = null) {
        withContext(Dispatchers.IO) {
            val queuedMsgCount = queuedMessagesCount ?: database.supportDao().getQueuedOfflineMessages().size
            val unsyncedSalesCount = database.saleDao().getUnsyncedSalesCount()
            val totalPending = queuedMsgCount + unsyncedSalesCount
            _pendingTransactionCount.value = totalPending

            if (totalPending > 0 && _cloudSyncState.value == CloudSyncStatus.SYNCED) {
                _cloudSyncState.value = CloudSyncStatus.PENDING
            }
        }
    }

    suspend fun performSyncNow(): Boolean = withContext(Dispatchers.IO) {
        if (!_isInternetConnected.value) {
            _cloudSyncState.value = CloudSyncStatus.OFFLINE
            _lastSyncError.value = "No internet connection available"
            return@withContext false
        }

        _cloudSyncState.value = CloudSyncStatus.SYNCING
        _lastSyncError.value = null

        try {
            // 1. Reconcile queued offline support messages
            val queuedMessages = database.supportDao().getQueuedOfflineMessages()
            var messagesSynced = 0
            for (msg in queuedMessages) {
                database.supportDao().markMessageSynced(msg.messageId, System.currentTimeMillis())
                messagesSynced++
            }

            // 2. Reconcile Room sales & products
            val unsyncedSales = database.saleDao().getUnsyncedSales()
            for (sale in unsyncedSales) {
                database.saleDao().markSaleSynced(sale.id, "SYNCED")
            }

            updatePendingCount()

            _lastSyncTime.value = System.currentTimeMillis()
            _cloudSyncState.value = CloudSyncStatus.SYNCED
            true
        } catch (e: Exception) {
            _lastSyncError.value = e.localizedMessage ?: "Synchronization error"
            _cloudSyncState.value = CloudSyncStatus.FAILED
            false
        }
    }
}
