package com.example.dinesphere

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

object FCMTokenManager {

    private const val TAG = "FCMTokenManager"

    /**
     * Initialize FCM - Get token and send to backend
     * Call this after successful login
     */
    fun initializeFCM(context: Context, userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get FCM token
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM Token retrieved: $token")

                // Send token to backend
                updateTokenToBackend(context, userId, token)

            } catch (e: Exception) {
                Log.e(TAG, "Error getting FCM token", e)
            }
        }
    }

    /**
     * Send token to backend API
     */
    fun updateTokenToBackend(context: Context, userId: String, token: String) {
        val url = "${Global.BASE_URL}update_device_token.php"

        Log.d(TAG, "Updating token to backend for user: $userId")

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                try {
                    Log.d(TAG, "Backend response: $response")
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        Log.d(TAG, "Token updated successfully on backend")
                        // Save token locally
                        saveTokenLocally(context, token)
                    } else {
                        val message = json.optString("message", "Unknown error")
                        Log.e(TAG, "Backend error: $message")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response", e)
                }
            },
            { error ->
                Log.e(TAG, "Error updating token: ${error.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["user_id"] = userId
                params["device_token"] = token
                return params
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    /**
     * Save token locally to avoid unnecessary updates
     */
    private fun saveTokenLocally(context: Context, token: String) {
        context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: android.app.Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }
}