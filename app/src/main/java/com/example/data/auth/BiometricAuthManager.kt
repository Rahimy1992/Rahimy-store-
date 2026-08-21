package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages Android BiometricPrompt (fingerprint & face recognition)
 * and seamlessly links authentication events to Firebase Auth session state.
 */
class BiometricAuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "BiometricAuthManager"
    }

    /**
     * Checks if the device supports and has enrolled Biometric authentication (Fingerprint / Face).
     */
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.d(TAG, "No biometric hardware available.")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.d(TAG, "Biometric hardware currently unavailable.")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.d(TAG, "No biometric credentials enrolled.")
                false
            }
            else -> false
        }
    }

    /**
     * Obtains the current authenticated Firebase User session.
     */
    fun getActiveFirebaseUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Triggers the native Android BiometricPrompt UI.
     * On successful hardware scan, validates and refreshes Firebase Auth session tokens.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Security Scan",
        subtitle: String = "Authenticate using Fingerprint or Face ID",
        negativeButtonText: String = "Cancel",
        onSuccess: (FirebaseUser?) -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(TAG, "Biometric authentication succeeded.")

                // Validate and refresh active Firebase Auth session
                val user = firebaseAuth.currentUser
                if (user != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Force token refresh to confirm valid Firebase Auth session
                            user.getIdToken(true).await()
                            Log.i(TAG, "Firebase Auth session token successfully validated for ${user.email}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Token refresh note: ${e.message}")
                        }
                    }
                }
                onSuccess(user)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "Biometric authentication error ($errorCode): $errString")
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed (unrecognized fingerprint/face).")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching BiometricPrompt: ${e.message}", e)
            onError(e.localizedMessage ?: "Biometric prompt error")
        }
    }
}
