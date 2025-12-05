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

class signup : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirm: EditText
    private lateinit var signupBtn: RelativeLayout
    private lateinit var loginLink: TextView
    private lateinit var databaseHelper: DatabaseHelper

    // Toggle buttons
    private lateinit var togglePass1: ImageView
    private lateinit var togglePass2: ImageView
    private var isPass1Visible = false
    private var isPass2Visible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this)

        // Initialize Views
        inputEmail = findViewById(R.id.input_email)
        inputPassword = findViewById(R.id.input_password)
        inputConfirm = findViewById(R.id.input_confirm)
        signupBtn = findViewById(R.id.signup_btn)
        loginLink = findViewById(R.id.tv_login_link)
        togglePass1 = findViewById(R.id.password_toggle1)
        togglePass2 = findViewById(R.id.password_toggle2)

        // Fix click stealing
        findViewById<View>(R.id.signup_btn_back).isClickable = false

        // Navigation to Login
        loginLink.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }

        // Toggle Visibility Logic
        togglePass1.setOnClickListener {
            isPass1Visible = toggleVisibility(inputPassword, togglePass1, isPass1Visible)
        }
        togglePass2.setOnClickListener {
            isPass2Visible = toggleVisibility(inputConfirm, togglePass2, isPass2Visible)
        }

        // Signup Action
        signupBtn.setOnClickListener {
            handleSignup()
        }
    }

    private fun toggleVisibility(editText: EditText, toggleIcon: ImageView, isVisible: Boolean): Boolean {
        if (!isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.alpha = 1.0f
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.alpha = 0.4f
        }
        editText.setSelection(editText.text.length)
        return !isVisible
    }

    private fun handleSignup() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()
        val confirm = inputConfirm.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showToast("Please fill in all fields")
            return
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }

        if (password != confirm) {
            showToast("Passwords do not match")
            return
        }

        performSignup(email, password)
    }

    private fun performSignup(email: String, pass: String) {
        showToast("Creating account...")

        val url = Global.BASE_URL + "signup.php"
        Log.d("SignupDebug", "Connecting to: $url")

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                // --- DEBUGGING LOGS ---
                Log.d("SignupDebug", "Raw Response: $response")

                try {
                    // Check for HTML response (Common Error)
                    if (response.trim().startsWith("<") || response.contains("<!DOCTYPE html>")) {
                        Log.e("SignupDebug", "Error: Server returned HTML instead of JSON.")
                        showToast("Server Error: Check Logcat for details (404/500)")
                        return@Listener
                    }

                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.optBoolean("success")
                    val message = jsonResponse.optString("message")

                    if (success) {
                        val userId = jsonResponse.optString("user_id")
                        databaseHelper.saveUserId(userId)
                        showToast("Account Created Successfully!")
                        navigateToHome()
                    } else {
                        showToast("Failed: $message")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SignupDebug", "JSON Parse Error: ${e.message}")
                    showToast("Error: Invalid Response Format")
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                val statusCode = error.networkResponse?.statusCode ?: 0
                Log.e("SignupDebug", "Volley Error Code: $statusCode")

                if (statusCode == 404) {
                    showToast("Error 404: File not found on server")
                } else if (statusCode == 500) {
                    showToast("Error 500: Server internal error")
                } else {
                    showToast("Connection failed. Check Internet.")
                }
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
        val intent = Intent(this, location1::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}