package com.example.dinesphere

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class notifications : AppCompatActivity() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var backButton: ImageButton
    private lateinit var emptyStateImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Initialize views
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        backButton = findViewById(R.id.back)
        emptyStateImage = findViewById(R.id.hand)

        // Setup RecyclerView
        setupRecyclerView()

        // Back button functionality
        backButton.setOnClickListener {
            finish()
        }

        // Load notifications
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(emptyList())
        notificationsRecyclerView.adapter = notificationAdapter
    }

    private fun loadNotifications() {
        val userId = databaseHelper.getUserId()

        if (userId == null) {
            Log.e("NotificationDebug", "User ID is null")
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show()
            showEmptyState(true)
            return
        }

        val url = "${Global.BASE_URL}notification(get).php?user_id=$userId"
        Log.d("NotificationDebug", "Loading notifications from: $url")
        Log.d("NotificationDebug", "User ID: $userId")

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    Log.d("NotificationDebug", "Raw response: $response")

                    // Check if response is HTML error
                    if (response.trim().startsWith("<") || response.contains("Fatal error") || response.contains("Hello from")) {
                        Log.e("NotificationDebug", "Received HTML/error response instead of JSON")
                        Toast.makeText(this, "Server error. Please check if notification(get).php exists", Toast.LENGTH_LONG).show()
                        showEmptyState(true)
                        return@StringRequest
                    }

                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)

                    if (success) {
                        val notificationsArray = json.getJSONArray("notifications")
                        val notifications = mutableListOf<NotificationModel>()

                        for (i in 0 until notificationsArray.length()) {
                            val item = notificationsArray.getJSONObject(i)
                            val notification = NotificationModel(
                                notificationId = item.getInt("notification_id"),
                                restaurantId = item.getInt("restaurant_id"),
                                title = item.getString("title"),
                                message = item.getString("message"),
                                createdAt = item.getString("created_at"),
                                businessName = item.getString("business_name"),
                                restaurantImage = item.optString("restaurant_image", null)
                            )
                            notifications.add(notification)
                        }

                        Log.d("NotificationDebug", "Loaded ${notifications.size} notifications")
                        notificationAdapter.updateNotifications(notifications)

                        // Show/hide empty state
                        showEmptyState(notifications.isEmpty())

                        if (notifications.isEmpty()) {
                            Toast.makeText(
                                this,
                                "No notifications from saved restaurants",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val message = json.optString("message", "Failed to load notifications")
                        Log.e("NotificationDebug", "API returned success=false: $message")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        showEmptyState(true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("NotificationDebug", "Error parsing response: ${e.message}")
                    Log.e("NotificationDebug", "Response was: $response")
                    Toast.makeText(this, "Error loading notifications. Check logs.", Toast.LENGTH_LONG).show()
                    showEmptyState(true)
                }
            },
            { error ->
                Log.e("NotificationDebug", "Volley error: ${error.message}")
                Log.e("NotificationDebug", "Network response code: ${error.networkResponse?.statusCode}")

                val errorMsg = when (error.networkResponse?.statusCode) {
                    404 -> "API endpoint not found. Please upload notification(get).php"
                    500 -> "Server error. Check PHP file for errors"
                    else -> "Network error: ${error.message}"
                }

                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                showEmptyState(true)
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateImage.visibility = if (show) View.VISIBLE else View.GONE
        notificationsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
}