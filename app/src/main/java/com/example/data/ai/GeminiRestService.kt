package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiRestService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateText(prompt: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in Secrets Panel"))
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            
            val textPart = JSONObject()
            textPart.put("text", prompt)
            parts.put(textPart)

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            if (!systemInstruction.isNullOrBlank()) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                val sysTextPart = JSONObject()
                sysTextPart.put("text", systemInstruction)
                sysParts.put(sysTextPart)
                sysObj.put("parts", sysParts)
                root.put("systemInstruction", sysObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val maxRetries = 2
            var attempt = 0
            var lastError: Exception? = null

            while (attempt <= maxRetries) {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.code == 429 && attempt < maxRetries) {
                        // Rate limit handling with exponential backoff
                        attempt++
                        kotlinx.coroutines.delay(1000L * attempt)
                        continue
                    }

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Gemini API error ${response.code}: $responseBody"))
                    }

                    val json = JSONObject(responseBody)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val resParts = content?.optJSONArray("parts")
                        val text = resParts?.optJSONObject(0)?.optString("text") ?: ""
                        return@withContext Result.success(text)
                    } else {
                        return@withContext Result.failure(Exception("No candidates returned from Gemini"))
                    }
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < maxRetries) {
                        attempt++
                        kotlinx.coroutines.delay(1000L * attempt)
                    } else {
                        break
                    }
                }
            }
            Result.failure(lastError ?: Exception("Failed after retries"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateMultimodal(
        prompt: String,
        bitmaps: List<Bitmap>,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in Secrets Panel"))
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()

            // Add text prompt
            val textPart = JSONObject()
            textPart.put("text", prompt)
            parts.put(textPart)

            // Add image parts
            bitmaps.forEach { bmp ->
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", base64Data)
                imagePart.put("inlineData", inlineData)
                parts.put(imagePart)
            }

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            if (!systemInstruction.isNullOrBlank()) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                val sysTextPart = JSONObject()
                sysTextPart.put("text", systemInstruction)
                sysParts.put(sysTextPart)
                sysObj.put("parts", sysParts)
                root.put("systemInstruction", sysObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API error ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val resParts = content?.optJSONArray("parts")
                val text = resParts?.optJSONObject(0)?.optString("text") ?: ""
                Result.success(text)
            } else {
                Result.failure(Exception("No response from Gemini API"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
