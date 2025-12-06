package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import org.json.JSONObject

class profile : AppCompatActivity() {

    private var currentUserId: String? = null
    private var currentUserEmail: String? = null

    private lateinit var databaseHelper: DatabaseHelper // Initialize DatabaseHelper

    // Views
    private lateinit var userNameView: TextView
    private lateinit var userIdView: TextView
    private lateinit var locationView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var editProfileBtn: TextView
    private lateinit var changePasswordBtn: TextView
    private lateinit var logoutBtn: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this) // Initialize helper

        // 1. Read User ID passed from homepage
        currentUserId = intent.getStringExtra("USER_ID")

        // 2. Bind views
        userNameView = findViewById(R.id.name)
        userIdView = findViewById(R.id.userid)
        locationView = findViewById(R.id.location)
        profileImageView = findViewById(R.id.profileImage)
        editProfileBtn = findViewById(R.id.editProfile)
        changePasswordBtn = findViewById(R.id.changePassword)
        logoutBtn = findViewById(R.id.logout)


        // 3. Load data from server immediately using the USER_ID
        if (!currentUserId.isNullOrEmpty()) {
            loadProfileDetails(currentUserId!!)
        } else {
            userNameView.text = "Guest User"
            userIdView.text = "#0000"
            Toast.makeText(this, "User ID missing. Cannot load profile.", Toast.LENGTH_SHORT).show()
        }

        // 4. Handle Navigation

        // Edit Profile Navigation
        editProfileBtn.setOnClickListener {
            if (!currentUserId.isNullOrEmpty()) {
                val intent = Intent(this, edit_profile::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please log in to edit profile.", Toast.LENGTH_SHORT).show()
            }
        }

        // Change Password Navigation
        changePasswordBtn.setOnClickListener {
            if (!currentUserEmail.isNullOrEmpty()) {
                val intent = Intent(this, forget_password::class.java)
                intent.putExtra("USER_EMAIL", currentUserEmail)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Error: User email not loaded yet.", Toast.LENGTH_SHORT).show()
            }
        }

        // >>> LOGOUT IMPLEMENTATION <<<
        logoutBtn.setOnClickListener {
            databaseHelper.clearSession() // Clear the session (user_id) from local storage

            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

            // Navigate back to Login and clear all activities in the stack
            val intent = Intent(this, login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

    }

    // Fetches full profile details from the server using the USER_ID
    private fun loadProfileDetails(userId: String) {
        val url = "${Global.BASE_URL}get_user_details.php?user_id=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    if (json.optBoolean("success")) {
                        val user = json.getJSONObject("user")

                        val firstName = user.optString("first_name", "")
                        val lastName = user.optString("last_name", "")
                        val profileImageUrl = user.optString("profile_image_url", null)
                        val address = user.optString("address", "Location not set")
                        currentUserEmail = user.optString("email", null)

                        // 1. Set Name
                        val fullName = "$firstName $lastName".trim()
                        userNameView.text = if (fullName.isNotEmpty()) fullName else user.optString("email", "DineSphere User")

                        // 2. Set User ID
                        userIdView.text = "#User $userId"

                        // 3. Set Location
                        locationView.text = address

                        
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Picasso.get()
                                .load(profileImageUrl)
                                .memoryPolicy(com.squareup.picasso.MemoryPolicy.NO_CACHE)
                                .networkPolicy(com.squareup.picasso.NetworkPolicy.NO_CACHE)
                                .placeholder(R.drawable.avatar)
                                .error(R.drawable.avatar)
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.avatar2)
                        }

                    } else {
                        Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing server data.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Connection error: Check API endpoint.", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }
}