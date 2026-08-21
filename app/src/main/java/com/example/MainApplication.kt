package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runDiagnosticStartupTask()
        initFirebaseAppCheck()
    }

    private fun runDiagnosticStartupTask() {
        try {
            val app = if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            } else {
                FirebaseApp.getInstance()
            }

            if (app != null) {
                val options = app.options
                val projectId = options.projectId
                val applicationId = options.applicationId
                val apiKey = options.apiKey
                val gcmSenderId = options.gcmSenderId
                val storageBucket = options.storageBucket

                val isPlaceholderKey = apiKey.contains("DummyKey", ignoreCase = true)
                val isPlaceholderProjectNum = gcmSenderId == "123456789012"

                Log.i(TAG, "=== FIREBASE STARTUP DIAGNOSTIC TASK ===")
                Log.i(TAG, "Firebase Project ID: $projectId")
                Log.i(TAG, "Application ID: $applicationId")
                Log.i(TAG, "FirebaseApp Name: ${app.name}")
                Log.i(TAG, "GCM Sender ID / Project Number: $gcmSenderId")
                Log.i(TAG, "Storage Bucket: $storageBucket")
                Log.i(TAG, "API Key Prefix: ${if (apiKey.length > 8) apiKey.take(8) + "..." else apiKey}")
                Log.i(TAG, "API Key Status: ${if (isPlaceholderKey) "PLACEHOLDER DETECTED" else "REAL KEY LOADED"}")
                Log.i(TAG, "Initialization Status: SUCCESSFUL (google-services.json loaded)")
                Log.i(TAG, "========================================")
            } else {
                Log.w(TAG, "FirebaseApp instance is null on startup verification")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Diagnostic Error during startup verification: ${e.message}", e)
        }
    }

    private fun initFirebaseAppCheck() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            
            // Initialize Firebase App Check with Recaptcha Enterprise/v3 provider factory using site key
            val recaptchaSiteKey = "6Ld_recaptcha_site_key_rahimy_smart_commerce"
            val factory = RecaptchaAppCheckProviderFactory.getInstance(recaptchaSiteKey)
            firebaseAppCheck.installAppCheckProviderFactory(factory)
            
            Log.d(TAG, "Firebase App Check successfully initialized with RecaptchaAppCheckProviderFactory")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}
