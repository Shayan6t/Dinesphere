package com.example.dinesphere

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
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
import org.json.JSONObject

class login : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var loginBtn: RelativeLayout
    private lateinit var passwordToggle: ImageView
    private lateinit var signUpLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var databaseHelper: DatabaseHelper

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Database Helper
        databaseHelper = DatabaseHelper(this)

        // Check if user is already logged in
        if (databaseHelper.getUserId() != null) {
            navigateToHome()
            return
        }

        // ✅ REQUEST NOTIFICATION PERMISSION (Android 13+)
        FCMTokenManager.requestNotificationPermission(this)

        // Initialize Views
        inputEmail = findViewById(R.id.input_email)
        inputPassword = findViewById(R.id.input_password)
        loginBtn = findViewById(R.id.login_btn)
        passwordToggle = findViewById(R.id.password_toggle1)
        signUpLink = findViewById(R.id.tv_signup_link)
        forgotPasswordLink = findViewById(R.id.forget_password)

        // Prevent click stealing by the background image button
        findViewById<View>(R.id.login_btn_back).isClickable = false

        // Set Click Listeners

        // 1. Forgot Password Navigation
        forgotPasswordLink.setOnClickListener {
            val intent = Intent(this, forget_password::class.java)
            startActivity(intent)
        }

        // 2. Sign Up Navigation
        signUpLink.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
        }

        passwordToggle.setOnClickListener {
            togglePasswordVisibility()
        }

        loginBtn.setOnClickListener {
            handleLogin()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordToggle.alpha = 1.0f
        } else {
            inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordToggle.alpha = 0.4f
        }
        inputPassword.setSelection(inputPassword.text.length)
    }

    private fun handleLogin() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (email.isEmpty()) {
            showToast("Please enter your email")
            inputEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showToast("Please enter your password")
            inputPassword.requestFocus()
            return
        }

        performLogin(email, password)
    }

    private fun performLogin(email: String, pass: String) {
        showToast("Logging in...")

        val url = Global.BASE_URL + "login.php"
        Log.d("LoginDebug", "Connecting to: $url")

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                Log.d("LoginDebug", "Response: $response")
                try {
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success")
                    val message = jsonResponse.optString("message")

                    if (success) {
                        if (jsonResponse.has("user")) {
                            val userObj = jsonResponse.getJSONObject("user")
                            val userId = userObj.optString("user_id")

                            // Save Session locally
                            val isSaved = databaseHelper.saveUserId(userId)
                            if (isSaved) {
                                // ✅ INITIALIZE FCM AFTER SUCCESSFUL LOGIN
                                FCMTokenManager.initializeFCM(this, userId)

                                showToast("Login Successful!")
                                navigateToHome()
                            } else {
                                showToast("Login failed: Could not save session")
                            }
                        }
                    } else {
                        showToast(message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Server Error: Invalid response format")
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                val errorMsg = when {
                    error.networkResponse == null -> "No Internet Connection"
                    error.networkResponse.statusCode == 404 -> "Server URL not found (404)"
                    error.networkResponse.statusCode == 500 -> "Server Internal Error (500)"
                    else -> "Connection Failed"
                }
                showToast(errorMsg)
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email
                params["password"] = pass
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun navigateToHome() {
        // Navigate to homepage instead of location1
        val intent = Intent(this, homepage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}